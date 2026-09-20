package com.sunmoon.backend.dto.response.auth;

import com.sunmoon.backend.constant.enums.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Thong tin nguoi dung tra ve sau khi dang nhap.
 *
 * Bon truong dau (userId, username, realName, avatar) co ten HOI LA so voi
 * phan con lai cua du an - do la co y: chung khop dung GetUserInfoModel cua
 * vben-admin, nen store nguoi dung cua vben dung duoc ngay ma khong phai
 * viet them lop chuyen doi.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponse {

    // ===== Phan khop voi vben GetUserInfoModel =====
    UUID userId;
    String username;   // email
    String realName;   // ho ten
    String avatar;
    String desc;
    String homePath;
    List<RoleInfo> roles;

    // ===== Phan rieng cua SignAI =====
    String email;
    Boolean emailVerified;

    UserType userType;
    AccountKind accountKind;
    AgeRange ageRange;
    Region region;
    String address;

    VslRole vslRole;
    VslRoleStatus vslRoleStatus;

    UserStatus status;

    /** Da tra loi 4 cau onboarding chua - frontend dung de dieu huong */
    Boolean onboardingCompleted;

    OffsetDateTime lastLoginAt;
    OffsetDateTime createdAt;

    /** vben doc roles[].value de dung cho phan quyen menu */
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoleInfo {
        String roleName;
        String value;
    }
}
