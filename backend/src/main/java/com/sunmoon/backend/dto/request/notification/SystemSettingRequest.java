package com.sunmoon.backend.dto.request.notification;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemSettingRequest {

    /** Chỉ dùng khi tạo mới - sửa key của bản ghi có sẵn không được hỗ trợ, xoá rồi tạo lại */
    @NotBlank(message = "Thiếu khoá cấu hình")
    @Size(max = 120)
    String key;

    @NotNull(message = "Thiếu giá trị cấu hình")
    JsonNode value;

    String description;
}
