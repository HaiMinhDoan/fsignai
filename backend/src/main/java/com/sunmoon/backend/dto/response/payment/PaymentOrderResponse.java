package com.sunmoon.backend.dto.response.payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Đủ thông tin để người học chuyển khoản: QR, ngân hàng, số tài khoản, số tiền, nội dung */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentOrderResponse {
    UUID id;
    String planCode;
    long amount;
    /** Nội dung chuyển khoản — người dùng phải ghi ĐÚNG chuỗi này */
    String paymentCode;
    String status;          // PENDING | PAID | EXPIRED
    String qrUrl;
    String bankCode;
    String accountNumber;
    String accountName;
    OffsetDateTime expiresAt;
    OffsetDateTime paidAt;
    /** Premium sau khi đơn này được thanh toán (null nếu chưa) */
    OffsetDateTime premiumUntil;
}
