package com.sunmoon.backend.dto.response.auth;

import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthTokenResponse {

    // Ten "token" (khong phai accessToken) de khop dung LoginResultModel
    // cua vben - store cua no doc thang data.token.
    String token;

    String refreshToken;

    @Builder.Default
    String tokenType = "Bearer";

    /** Thoi gian song cua access token, tinh bang giay */
    Long expiresIn;

    UserProfileResponse user;
}
