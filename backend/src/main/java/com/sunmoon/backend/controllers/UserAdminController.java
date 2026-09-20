package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.AccountKind;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.constant.enums.UserStatus;
import com.sunmoon.backend.constant.enums.VslRoleStatus;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.auth.UserRolesRequest;
import com.sunmoon.backend.dto.request.auth.UserStatusRequest;
import com.sunmoon.backend.dto.request.auth.VslRoleDecisionRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.auth.UserAdminResponse;
import com.sunmoon.backend.repository.RoleRepository;
import com.sunmoon.backend.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "CMS - Người dùng", description = "Quản lý tài khoản, phân quyền, duyệt hồ sơ chuyên môn VSL")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final RoleRepository roleRepository;

    @Operation(summary = "Danh sách vai trò để gán cho người dùng")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @GetMapping("/roles")
    public ResponseEntity<ResponseData<List<Map<String, String>>>> roles() {
        List<Map<String, String>> data = roleRepository.findAll().stream()
                .map(r -> Map.of("code", r.getCode(), "nameVi", r.getNameVi()))
                .toList();
        return ok(data, "ROLE_LIST_SUCCESS");
    }

    @Operation(summary = "Lọc danh sách người dùng")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @GetMapping
    public ResponseEntity<ResponseData<PageResponse<UserAdminResponse>>> filter(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) AccountKind accountKind,
            @RequestParam(required = false) VslRoleStatus vslRoleStatus,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ok(userAdminService.filter(keyword, status, accountKind, vslRoleStatus, PageRequest.of(page, size)),
                "USER_LIST_SUCCESS");
    }

    @Operation(summary = "Chi tiết một người dùng")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<UserAdminResponse>> detail(@PathVariable UUID id) {
        return ok(userAdminService.detail(id), "USER_DETAIL_SUCCESS");
    }

    @Operation(summary = "Đổi trạng thái tài khoản (khoá/mở/vô hiệu hoá)")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @PutMapping("/{id}/status")
    public ResponseEntity<ResponseData<UserAdminResponse>> setStatus(
            @PathVariable UUID id, @Valid @RequestBody UserStatusRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(userAdminService.setStatus(me, id, request), "USER_STATUS_UPDATED");
    }

    @Operation(summary = "Thay toàn bộ vai trò của người dùng")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @PutMapping("/{id}/roles")
    public ResponseEntity<ResponseData<UserAdminResponse>> setRoles(
            @PathVariable UUID id, @Valid @RequestBody UserRolesRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(userAdminService.setRoles(me, id, request), "USER_ROLES_UPDATED");
    }

    @Operation(summary = "Duyệt/từ chối hồ sơ chuyên môn VSL tự khai")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.VSL_REVIEWER})
    @PutMapping("/{id}/vsl-role")
    public ResponseEntity<ResponseData<UserAdminResponse>> decideVslRole(
            @PathVariable UUID id, @Valid @RequestBody VslRoleDecisionRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(userAdminService.decideVslRole(me, id, request), "USER_VSL_ROLE_DECIDED");
    }

    private <T> ResponseEntity<ResponseData<T>> ok(T data, String messageCode) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseData.<T>builder()
                .status(HttpStatus.OK.value())
                .messageCode(messageCode)
                .data(data)
                .lang(SecurityContextHolder.getLang())
                .path(SecurityContextHolder.getPath())
                .timestamp(new java.util.Date())
                .build());
    }
}
