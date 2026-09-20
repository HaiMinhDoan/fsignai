package com.sunmoon.backend.dto.request.auth;

import com.sunmoon.backend.constant.enums.VslRoleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VslRoleDecisionRequest {

    /** Chỉ nhận VERIFIED hoặc REJECTED - service tự chặn hai giá trị còn lại */
    @NotNull(message = "Thiếu quyết định duyệt")
    VslRoleStatus decision;
}
