package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.auth.*;
import com.sunmoon.backend.dto.response.auth.AuthTokenResponse;
import com.sunmoon.backend.dto.response.auth.UserProfileResponse;

import java.util.UUID;

public interface AuthService {

    AuthTokenResponse register(RegisterRequest request, String userAgent, String ipAddress);

    AuthTokenResponse login(LoginRequest request, String userAgent, String ipAddress);

    /** Cap access token moi va XOAY refresh token (token cu bi thu hoi ngay) */
    AuthTokenResponse refresh(RefreshTokenRequest request, String userAgent, String ipAddress);

    /** Dua access token vao blacklist Redis va thu hoi refresh token */
    void logout(String accessToken, String refreshToken);

    UserProfileResponse getProfile(UUID userId);

    /**
     * Luon tra ve binh thuong du email co ton tai hay khong.
     * Bao "email nay chua dang ky" la lo cho ke tan cong biet tai khoan nao co that.
     */
    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);
}
