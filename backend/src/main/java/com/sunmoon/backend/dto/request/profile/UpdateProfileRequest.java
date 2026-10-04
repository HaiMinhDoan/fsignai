package com.sunmoon.backend.dto.request.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 150, message = "Tên hiển thị tối đa 150 ký tự")
    String fullName;

    /** Để trống = xoá phần giới thiệu */
    @Size(max = 500, message = "Giới thiệu tối đa 500 ký tự")
    String bio;
}
