package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.AccountKind;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.constant.enums.UserStatus;
import com.sunmoon.backend.constant.enums.VerificationPurpose;
import com.sunmoon.backend.constant.enums.VslRole;
import com.sunmoon.backend.constant.enums.VslRoleStatus;
import com.sunmoon.backend.dto.AuthInfo;
import com.sunmoon.backend.dto.request.auth.*;
import com.sunmoon.backend.dto.response.auth.AuthTokenResponse;
import com.sunmoon.backend.dto.response.auth.UserProfileResponse;
import com.sunmoon.backend.entity.auth.*;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.*;
import com.sunmoon.backend.service.AuthService;
import com.sunmoon.backend.service.JwtService;
import com.sunmoon.backend.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** Access token song 24 gio - khop voi JwtServiceImpl.generateToken() */
    private static final long ACCESS_TOKEN_TTL_SECONDS = 24 * 60 * 60L;
    private static final long REFRESH_TOKEN_TTL_DAYS = 7L;
    private static final long RESET_TOKEN_TTL_MINUTES = 30L;

    private static final String BLACKLIST_PREFIX = "token-blacklist";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final OnboardingResponseRepository onboardingResponseRepository;

    private final JwtService jwtService;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    // ==================== DANG KY ====================

    @Override
    @Transactional
    public AuthTokenResponse register(RegisterRequest request, String userAgent, String ipAddress) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailNormalized(normalizedEmail)) {
            throw new ConflictException("Email này đã được đăng ký");
        }

        // Chon "Giao vien" o form dang ky tuong duong tu khai chuyen mon TEACHER -
        // ghi de vslRole nguoi goi co the da gui, tranh truong hop hai lua chon
        // lech nhau (accountKind=TEACHER nhung vslRole=LEARNER).
        VslRole vslRole = request.getAccountKind() == AccountKind.TEACHER
                ? VslRole.TEACHER
                : (request.getVslRole() == null ? VslRole.LEARNER : request.getVslRole());

        // Khai vai tro chuyen mon khac LEARNER thi phai cho admin xac minh.
        // Chua duyet thi gop y cua ho co trong so 0 khi hieu chinh nguong cham diem.
        boolean claimsExpertRole = vslRole != VslRole.LEARNER;

        User user = User.builder()
                .email(request.getEmail().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .accountKind(request.getAccountKind())
                .ageRange(request.getAgeRange())
                .userType(request.getUserType())
                .region(request.getRegion())
                .vslRole(vslRole)
                .vslRoleStatus(claimsExpertRole ? VslRoleStatus.PENDING : VslRoleStatus.SELF_DECLARED)
                .vslRoleEvidence(request.getVslRoleEvidence())
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);

        // Moi tai khoan moi deu la STUDENT. Vai tro quan tri do admin cap sau.
        Role studentRole = roleRepository.findByCode(RoleType.STUDENT)
                .orElseThrow(() -> new CommonException(
                        "Thiếu vai trò STUDENT trong CSDL. Kiểm tra migration V10 đã chạy chưa."));

        userRoleRepository.save(UserRole.builder()
                .id(new UserRoleId(saved.getId(), studentRole.getId()))
                .user(saved)
                .role(studentRole)
                .build());

        // Tao cau hinh mac dinh ngay, de dashboard va job nhac hoc khong phai
        // kiem tra null o khap noi
        userSettingsRepository.save(UserSettings.builder().user(saved).build());

        return issueTokens(saved, userAgent, ipAddress);
    }

    // ==================== DANG NHAP ====================

    @Override
    @Transactional
    public AuthTokenResponse login(LoginRequest request, String userAgent, String ipAddress) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailNormalized(normalizedEmail)
                .orElseThrow(this::invalidCredentials);

        // Tai khoan chi dang nhap bang Google thi passwordHash rong
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new CommonException("Tài khoản này đăng nhập bằng Google, chưa đặt mật khẩu");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        assertLoginAllowed(user);

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        return issueTokens(user, userAgent, ipAddress);
    }

    /**
     * Cung mot thong diep cho ca "email khong ton tai" lan "sai mat khau".
     * Phan biet hai truong hop nay cho phep do xem email nao da dang ky.
     */
    private CommonException invalidCredentials() {
        CommonException ex = new CommonException("Email hoặc mật khẩu không đúng");
        ex.setHttpStatus(HttpStatus.UNAUTHORIZED);
        return ex;
    }

    private void assertLoginAllowed(User user) {
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new CommonException("Tài khoản đã bị vô hiệu hoá");
        }
        if (user.getStatus() == UserStatus.BANNED) {
            boolean stillBanned = user.getBannedUntil() == null
                    || user.getBannedUntil().isAfter(OffsetDateTime.now());
            if (stillBanned) {
                String reason = user.getBanReason() == null ? "" : ": " + user.getBanReason();
                throw new CommonException("Tài khoản đang bị khoá" + reason);
            }
            // Het han khoa thi tu mo lai, khong bat admin thao tac tay
            user.setStatus(UserStatus.ACTIVE);
            user.setBannedUntil(null);
            user.setBanReason(null);
        }
    }

    // ==================== LAM MOI TOKEN ====================

    @Override
    @Transactional
    public AuthTokenResponse refresh(RefreshTokenRequest request, String userAgent, String ipAddress) {
        String rawToken = request.getRefreshToken();
        String tokenHash = sha256(rawToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> {
                    CommonException ex = new CommonException("Refresh token không hợp lệ hoặc đã bị thu hồi");
                    ex.setHttpStatus(HttpStatus.UNAUTHORIZED);
                    return ex;
                });

        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            CommonException ex = new CommonException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
            ex.setHttpStatus(HttpStatus.UNAUTHORIZED);
            throw ex;
        }

        User user = stored.getUser();
        assertLoginAllowed(user);

        // XOAY token: thu hoi token vua dung truoc khi cap cai moi.
        // Neu khong xoay, mot refresh token bi lo se dung duoc mai cho toi khi het han.
        stored.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(stored);

        return issueTokens(user, userAgent, ipAddress);
    }

    // ==================== DANG XUAT ====================

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // JWT khong thu hoi duoc, nen chan bang blacklist trong Redis.
        // AuthInterceptor da doc dung khoa nay san roi.
        if (accessToken != null && !accessToken.isBlank()) {
            long ttl = remainingSeconds(accessToken);
            if (ttl > 0) {
                // Chi giu den luc token het han - khong cần giu lau hon
                redisService.set(redisService.buildKey(BLACKLIST_PREFIX, accessToken), "1", ttl);
            }
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(sha256(refreshToken))
                    .ifPresent(rt -> {
                        rt.setRevokedAt(OffsetDateTime.now());
                        refreshTokenRepository.save(rt);
                    });
        }
    }

    private long remainingSeconds(String accessToken) {
        try {
            Date expiration = jwtService.getClaimsFromToken(accessToken).getExpirationTime();
            if (expiration == null) {
                return ACCESS_TOKEN_TTL_SECONDS;
            }
            return Math.max(0, (expiration.getTime() - System.currentTimeMillis()) / 1000);
        } catch (Exception e) {
            // Token hong thi khong cần blacklist - no vốn đã không dùng được
            return 0;
        }
    }

    // ==================== HO SO ====================

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        return toProfile(user, userRoleRepository.findRoleCodesByUserId(userId));
    }

    // ==================== MAT KHAU ====================

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Optional<User> maybeUser = userRepository.findByEmailNormalized(normalizedEmail);

        // Khong ton tai thi van tra ve binh thuong - khong lo danh sach email da dang ky
        if (maybeUser.isEmpty()) {
            log.info("Yêu cầu đặt lại mật khẩu cho email không tồn tại: {}", normalizedEmail);
            return;
        }

        User user = maybeUser.get();
        OffsetDateTime now = OffsetDateTime.now();

        // Vo hieu cac link cu de chi co mot link song tai mot thoi diem
        verificationTokenRepository.invalidateAll(user.getId(), VerificationPurpose.PASSWORD_RESET, now);

        String rawToken = generateRawToken();
        verificationTokenRepository.save(VerificationToken.builder()
                .user(user)
                .tokenHash(sha256(rawToken))
                .purpose(VerificationPurpose.PASSWORD_RESET)
                .expiresAt(now.plusMinutes(RESET_TOKEN_TTL_MINUTES))
                .build());

        // TODO: gui email that qua Spring Mail (spring-boot-starter-mail da co trong pom).
        // Tam ghi log de dev lay duoc link khi chua cau hinh SMTP.
        log.info("[ĐẶT LẠI MẬT KHẨU] {} -> token: {}", user.getEmail(), rawToken);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationToken token = verificationTokenRepository
                .findByTokenHashAndUsedAtIsNull(sha256(request.getToken()))
                .orElseThrow(() -> new CommonException("Mã đặt lại mật khẩu không hợp lệ hoặc đã dùng"));

        if (token.getPurpose() != VerificationPurpose.PASSWORD_RESET) {
            throw new CommonException("Mã không dùng cho việc đặt lại mật khẩu");
        }
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new CommonException("Mã đặt lại mật khẩu đã hết hạn, vui lòng yêu cầu lại");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsedAt(OffsetDateTime.now());
        verificationTokenRepository.save(token);

        // Doi mat khau thi moi phien dang nhap cu phai chet - neu khong,
        // ke da chiem tai khoan van tiep tuc dung duoc refresh token cu.
        refreshTokenRepository.revokeAllByUserId(user.getId(), OffsetDateTime.now());
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new CommonException("Mật khẩu hiện tại không đúng");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(user.getId(), OffsetDateTime.now());
    }

    // ==================== private ====================

    private AuthTokenResponse issueTokens(User user, String userAgent, String ipAddress) {
        List<String> roleCodes = userRoleRepository.findRoleCodesByUserId(user.getId());

        AuthInfo authInfo = AuthInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getEmail())
                .roles(new HashSet<>(roleCodes))
                .build();

        String accessToken = jwtService.generateToken(authInfo, userAgent);
        String refreshTokenRaw = jwtService.generateRefreshToken(authInfo, userAgent);

        // Luu HASH chu khong luu token goc: lo CSDL cung khong dung duoc de dang nhap
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(refreshTokenRaw))
                .expiresAt(OffsetDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS))
                .userAgent(truncate(userAgent, 255))
                .ipAddress(truncate(ipAddress, 64))
                .build());

        return AuthTokenResponse.builder()
                .token(accessToken)
                .refreshToken(refreshTokenRaw)
                .expiresIn(ACCESS_TOKEN_TTL_SECONDS)
                .user(toProfile(user, roleCodes))
                .build();
    }

    private UserProfileResponse toProfile(User user, List<String> roleCodes) {
        List<UserProfileResponse.RoleInfo> roles = roleCodes.stream()
                .map(code -> UserProfileResponse.RoleInfo.builder()
                        .roleName(code)
                        .value(code)
                        .build())
                .toList();

        boolean onboarded = onboardingResponseRepository.existsByUserIdAndCompletedAtIsNotNull(user.getId());

        return UserProfileResponse.builder()
                .userId(user.getId())
                .username(user.getEmail())
                .realName(user.getFullName())
                .avatar(user.getAvatarFile() == null ? null : user.getAvatarFile().getPublicUrl())
                .homePath(resolveHomePath(roleCodes))
                .roles(roles)
                .email(user.getEmail())
                .emailVerified(user.getEmailVerifiedAt() != null)
                .userType(user.getUserType())
                .accountKind(user.getAccountKind())
                .ageRange(user.getAgeRange())
                .region(user.getRegion())
                .address(user.getAddress())
                .vslRole(user.getVslRole())
                .vslRoleStatus(user.getVslRoleStatus())
                .status(user.getStatus())
                .onboardingCompleted(onboarded)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /** Nguoi quan tri vao thang CMS, nguoi hoc vao bang dieu khien hoc tap */
    private String resolveHomePath(List<String> roleCodes) {
        boolean isStaff = roleCodes.stream().anyMatch(code ->
                RoleType.SYSTEM_ADMIN.equals(code)
                        || RoleType.CONTENT_EDITOR.equals(code)
                        || RoleType.MODERATOR.equals(code)
                        || RoleType.VSL_REVIEWER.equals(code));
        return isStaff ? "/content/sign" : "/dashboard";
    }

    /**
     * Bam SHA-256 cho refresh token va token dat lai mat khau.
     * Khong dung BCrypt o day: BCrypt co muoi ngau nhien nen khong tra cuu duoc
     * theo hash, ma hai loai token nay lai can tim trong CSDL. Chung la chuoi
     * ngau nhien 256 bit nen khong so bi do nhu mat khau nguoi dung.
     */
    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new CommonException("Không băm được token", e);
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
