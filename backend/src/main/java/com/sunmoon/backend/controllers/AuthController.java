package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.AuthInfo;
import com.sunmoon.backend.dto.request.auth.*;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.auth.AuthTokenResponse;
import com.sunmoon.backend.dto.response.auth.UserProfileResponse;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.service.AuthService;
import com.sunmoon.backend.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Tag(name = "Xác thực", description = "Đăng ký, đăng nhập, làm mới token và quản lý mật khẩu")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    // ==================== CONG KHAI ====================

    @Operation(summary = "Đăng ký tài khoản",
            description = "Tạo tài khoản bằng email và mật khẩu. Tài khoản mới luôn nhận vai trò "
                    + "STUDENT; khai vai trò chuyên môn khác Người học sẽ vào hàng đợi chờ admin xác minh.")
    @PostMapping("/register")
    public ResponseEntity<ResponseData<AuthTokenResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        AuthTokenResponse result = authService.register(request, userAgent(http), clientIp(http));
        return ok(result, "AUTH_REGISTER_SUCCESS", http);
    }

    @Operation(summary = "Đăng nhập")
    @PostMapping("/login")
    public ResponseEntity<ResponseData<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        AuthTokenResponse result = authService.login(request, userAgent(http), clientIp(http));
        return ok(result, "AUTH_LOGIN_SUCCESS", http);
    }

    @Operation(summary = "Làm mới access token",
            description = "Refresh token được XOAY: token cũ bị thu hồi ngay khi cấp token mới.")
    @PostMapping("/refresh")
    public ResponseEntity<ResponseData<AuthTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        AuthTokenResponse result = authService.refresh(request, userAgent(http), clientIp(http));
        return ok(result, "AUTH_REFRESH_SUCCESS", http);
    }

    @Operation(summary = "Quên mật khẩu",
            description = "Luôn trả về thành công dù email có tồn tại hay không, "
                    + "để không lộ danh sách email đã đăng ký.")
    @PostMapping("/forgot-password")
    public ResponseEntity<ResponseData<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        authService.forgotPassword(request);
        return ok(null, "AUTH_FORGOT_PASSWORD_SENT", http);
    }

    @Operation(summary = "Đặt lại mật khẩu bằng mã đã gửi qua email")
    @PostMapping("/reset-password")
    public ResponseEntity<ResponseData<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        authService.resetPassword(request);
        return ok(null, "AUTH_RESET_PASSWORD_SUCCESS", http);
    }

    // ==================== CAN DANG NHAP ====================

    @Operation(summary = "Thông tin người dùng đang đăng nhập")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/me")
    public ResponseEntity<ResponseData<UserProfileResponse>> me(HttpServletRequest http) {
        return ok(authService.getProfile(currentUserId()), "AUTH_PROFILE_SUCCESS", http);
    }

    @Operation(summary = "Đăng xuất",
            description = "Access token được đưa vào blacklist Redis (JWT không thu hồi được), "
                    + "refresh token bị thu hồi trong CSDL.")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/logout")
    public ResponseEntity<ResponseData<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request, HttpServletRequest http) {
        String accessToken = jwtService.getTokenFromAuthHeader(http.getHeader("Authorization"));
        authService.logout(accessToken, request == null ? null : request.getRefreshToken());
        return ok(null, "AUTH_LOGOUT_SUCCESS", http);
    }

    @Operation(summary = "Đổi mật khẩu",
            description = "Đổi thành công sẽ thu hồi toàn bộ phiên đăng nhập cũ trên mọi thiết bị.")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/change-password")
    public ResponseEntity<ResponseData<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, HttpServletRequest http) {
        authService.changePassword(currentUserId(), request);
        return ok(null, "AUTH_CHANGE_PASSWORD_SUCCESS", http);
    }

    // ==================== private ====================

    private java.util.UUID currentUserId() {
        AuthInfo authInfo = SecurityContextHolder.getAuthInfo();
        if (authInfo == null || authInfo.getId() == null) {
            CommonException ex = new CommonException("Chưa đăng nhập");
            ex.setHttpStatus(HttpStatus.UNAUTHORIZED);
            throw ex;
        }
        return authInfo.getId();
    }

    private String userAgent(HttpServletRequest http) {
        return http.getHeader("User-Agent");
    }

    /** Ưu tiên X-Forwarded-For vì backend sẽ chạy sau nginx */
    private String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }

    /**
     * Endpoint xác thực nằm ngoài interceptor nên SecurityContextHolder chưa có path/lang.
     * Lấy thẳng từ request để ResponseData vẫn đủ thông tin như các API khác.
     */
    private <T> ResponseEntity<ResponseData<T>> ok(T data, String messageCode, HttpServletRequest http) {
        String lang = SecurityContextHolder.getLang();
        if (lang == null) {
            lang = http.getHeader("lang") == null ? "vi" : http.getHeader("lang");
        }
        String path = SecurityContextHolder.getPath();
        if (path == null) {
            path = http.getRequestURI();
        }
        return ResponseEntity.status(HttpStatus.OK).body(ResponseData.<T>builder()
                .status(HttpStatus.OK.value())
                .messageCode(messageCode)
                .data(data)
                .lang(lang)
                .path(path)
                .timestamp(new Date())
                .build());
    }
}
