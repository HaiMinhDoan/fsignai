package com.sunmoon.backend.dto.response.payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một dòng lịch sử giao dịch. Người học xem đơn của chính mình (không kèm phần user*),
 * trang quản trị xem của mọi người (có user*).
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentHistoryResponse {
    UUID id;
    String planCode;
    long amount;
    Long paidAmount;
    String paymentCode;
    /**
     * PENDING | PAID | EXPIRED | CANCELLED. Đơn còn PENDING trong CSDL nhưng đã quá hạn được
     * trả về EXPIRED — không có job nào chuyển trạng thái, chỉ chuyển khi có người mở đơn ra xem.
     */
    String status;
    Integer durationDays;
    OffsetDateTime createdAt;
    OffsetDateTime expiresAt;
    OffsetDateTime paidAt;
    Long sepayTransactionId;

    // ===== Chỉ có ở trang quản trị =====
    UUID userId;
    String userName;
    String userEmail;
}
