package com.sunmoon.backend.dto.response.auth;

import com.sunmoon.backend.constant.enums.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAdminResponse {

    UUID id;
    String email;
    Boolean emailVerified;
    String fullName;
    String avatarUrl;

    AccountKind accountKind;
    AgeRange ageRange;
    UserType userType;
    Region region;

    VslRole vslRole;
    VslRoleStatus vslRoleStatus;
    String vslRoleEvidence;
    String vslRoleVerifiedByName;
    OffsetDateTime vslRoleVerifiedAt;

    UserStatus status;
    OffsetDateTime bannedUntil;
    String banReason;

    List<String> roleCodes;

    OffsetDateTime lastLoginAt;
    OffsetDateTime createdAt;
}
