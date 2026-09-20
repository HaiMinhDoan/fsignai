package com.sunmoon.backend.dto.request.auth;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Thay TOÀN BỘ vai trò hiện có của người dùng bằng danh sách này */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRolesRequest {

    @NotEmpty(message = "Phải giữ ít nhất một vai trò")
    List<String> roleCodes;
}
