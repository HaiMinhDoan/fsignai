package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.AccountKind;
import com.sunmoon.backend.constant.enums.UserStatus;
import com.sunmoon.backend.constant.enums.VslRoleStatus;
import com.sunmoon.backend.dto.request.auth.UserRolesRequest;
import com.sunmoon.backend.dto.request.auth.UserStatusRequest;
import com.sunmoon.backend.dto.request.auth.VslRoleDecisionRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.auth.UserAdminResponse;
import com.sunmoon.backend.entity.auth.Role;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.auth.UserRole;
import com.sunmoon.backend.entity.auth.UserRoleId;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.exception.customize.InvalidFieldException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.constant.enums.NotificationType;
import com.sunmoon.backend.repository.RoleRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.repository.UserRoleRepository;
import com.sunmoon.backend.service.AuditLogService;
import com.sunmoon.backend.service.NotificationService;
import com.sunmoon.backend.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Override
    public PageResponse<UserAdminResponse> filter(String keyword, UserStatus status, AccountKind accountKind,
                                                    VslRoleStatus vslRoleStatus, Pageable pageable) {
        String normalizedKeyword = (keyword == null || keyword.isBlank())
                ? null : "%" + keyword.trim().toLowerCase() + "%";
        Page<User> page = userRepository.adminFilter(normalizedKeyword, status, accountKind, vslRoleStatus, pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Override
    public UserAdminResponse detail(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserAdminResponse setStatus(UUID actingAdminId, UUID id, UserStatusRequest request) {
        if (id.equals(actingAdminId)) {
            throw forbidden("Không thể tự đổi trạng thái tài khoản của chính mình");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        UserStatus before = user.getStatus();
        user.setStatus(request.getStatus());
        if (request.getStatus() == UserStatus.BANNED) {
            user.setBanReason(request.getBanReason());
            user.setBannedUntil(request.getBannedUntil());
        } else {
            user.setBanReason(null);
            user.setBannedUntil(null);
        }

        User saved = userRepository.save(user);
        auditLogService.record(actingAdminId, "user.status.update", "users", id,
                Map.of("status", before), Map.of("status", request.getStatus(), "banReason", String.valueOf(request.getBanReason())));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public UserAdminResponse setRoles(UUID actingAdminId, UUID id, UserRolesRequest request) {
        if (id.equals(actingAdminId)) {
            throw forbidden("Không thể tự đổi vai trò của chính mình - nhờ một quản trị viên khác thao tác");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        List<String> before = userRoleRepository.findRoleCodesByUserId(id);

        List<Role> roles = request.getRoleCodes().stream()
                .distinct()
                .map(code -> roleRepository.findByCode(code)
                        .orElseThrow(() -> new InvalidFieldException("Không tìm thấy vai trò: " + code)))
                .toList();

        userRoleRepository.deleteAll(userRoleRepository.findAllByUserId(id));
        User admin = userRepository.getReferenceById(actingAdminId);
        for (Role role : roles) {
            userRoleRepository.save(UserRole.builder()
                    .id(new UserRoleId(id, role.getId()))
                    .user(user)
                    .role(role)
                    .grantedBy(admin)
                    .build());
        }

        auditLogService.record(actingAdminId, "user.roles.update", "users", id,
                Map.of("roleCodes", before), Map.of("roleCodes", request.getRoleCodes()));
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserAdminResponse decideVslRole(UUID actingAdminId, UUID id, VslRoleDecisionRequest request) {
        if (request.getDecision() != VslRoleStatus.VERIFIED && request.getDecision() != VslRoleStatus.REJECTED) {
            throw new InvalidFieldException("Quyết định duyệt chỉ nhận VERIFIED hoặc REJECTED");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        VslRoleStatus before = user.getVslRoleStatus();
        user.setVslRoleStatus(request.getDecision());
        user.setVslRoleVerifiedBy(userRepository.getReferenceById(actingAdminId));
        user.setVslRoleVerifiedAt(OffsetDateTime.now());

        User saved = userRepository.save(user);
        auditLogService.record(actingAdminId, "user.vsl_role.decide", "users", id,
                Map.of("vslRoleStatus", before), Map.of("vslRoleStatus", request.getDecision()));

        boolean verified = request.getDecision() == VslRoleStatus.VERIFIED;
        notificationService.notify(id, NotificationType.ROLE_VERIFIED,
                verified ? "Hồ sơ chuyên môn VSL đã được duyệt" : "Hồ sơ chuyên môn VSL bị từ chối",
                verified
                        ? "Chúc mừng! Vai trò " + saved.getVslRole() + " bạn khai báo đã được xác minh."
                        : "Vai trò " + saved.getVslRole() + " bạn khai báo chưa được xác minh. Bạn có thể khai lại với minh chứng rõ ràng hơn.",
                null, true);

        return toResponse(saved);
    }

    private CommonException forbidden(String message) {
        CommonException ex = new CommonException(message);
        ex.setHttpStatus(HttpStatus.FORBIDDEN);
        return ex;
    }

    private UserAdminResponse toResponse(User u) {
        return UserAdminResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .emailVerified(u.getEmailVerifiedAt() != null)
                .fullName(u.getFullName())
                .avatarUrl(u.getAvatarFile() == null ? null : u.getAvatarFile().getPublicUrl())
                .accountKind(u.getAccountKind())
                .ageRange(u.getAgeRange())
                .userType(u.getUserType())
                .region(u.getRegion())
                .vslRole(u.getVslRole())
                .vslRoleStatus(u.getVslRoleStatus())
                .vslRoleEvidence(u.getVslRoleEvidence())
                .vslRoleVerifiedByName(u.getVslRoleVerifiedBy() == null ? null : u.getVslRoleVerifiedBy().getFullName())
                .vslRoleVerifiedAt(u.getVslRoleVerifiedAt())
                .status(u.getStatus())
                .bannedUntil(u.getBannedUntil())
                .banReason(u.getBanReason())
                .roleCodes(userRoleRepository.findRoleCodesByUserId(u.getId()))
                .lastLoginAt(u.getLastLoginAt())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
