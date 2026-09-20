package com.sunmoon.backend.dto.request.auth;

import com.sunmoon.backend.constant.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserStatusRequest {

    @NotNull(message = "Thiếu trạng thái tài khoản")
    UserStatus status;

    /** Bắt buộc có ý nghĩa khi status = BANNED, để lại lịch sử vì sao khoá */
    String banReason;

    /** null = khoá vĩnh viễn khi status = BANNED */
    OffsetDateTime bannedUntil;
}
