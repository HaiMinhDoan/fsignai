package com.sunmoon.backend.dto.request.content;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Soạn một bước trong hướng dẫn thực hiện ký hiệu — ảnh do một API riêng
 * (multipart) đảm nhiệm, giống cách video ký hiệu tách khỏi thông tin từ.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignStepRequest {

    @NotNull(message = "Cần có thứ tự bước")
    @Min(value = 1, message = "Thứ tự bước bắt đầu từ 1")
    Integer stepOrder;

    String titleVi;

    /** Bắt buộc: ảnh không được là kênh thông tin duy nhất (docs/05-design-system.md §2.2) */
    @NotBlank(message = "Cần có mô tả cho bước này")
    String descriptionVi;

    /** LEFT_HAND | RIGHT_HAND | BOTH_HANDS | FACE | MOUTH | SHOULDER | CHEST */
    String bodyFocus;

    @DecimalMin(value = "0.0", inclusive = false, message = "Thời gian giữ phải lớn hơn 0")
    BigDecimal holdSeconds;
}
