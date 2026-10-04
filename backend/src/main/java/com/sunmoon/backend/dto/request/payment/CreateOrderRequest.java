package com.sunmoon.backend.dto.request.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderRequest {

    /** Hiện chỉ có PREMIUM — gói Free không cần thanh toán */
    @NotBlank(message = "Chưa chọn gói")
    String planCode;
}
