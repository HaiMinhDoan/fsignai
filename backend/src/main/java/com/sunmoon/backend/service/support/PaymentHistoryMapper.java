package com.sunmoon.backend.service.support;

import com.sunmoon.backend.dto.response.payment.PaymentHistoryResponse;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.payment.PaymentOrder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Dựng một dòng lịch sử giao dịch — dùng chung cho trang cá nhân và CMS */
public final class PaymentHistoryMapper {

    /** Thay cho "không lọc thời gian": truyền null vào truy vấn thì Postgres không đoán được kiểu tham số */
    public static final OffsetDateTime MIN_TIME = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    public static final OffsetDateTime MAX_TIME = OffsetDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private PaymentHistoryMapper() {
    }

    /** Đơn PENDING đã quá hạn hiện là EXPIRED: không có job nào chuyển trạng thái trong CSDL */
    public static String effectiveStatus(PaymentOrder o, OffsetDateTime now) {
        if (PaymentOrder.PENDING.equals(o.getStatus()) && !o.getExpiresAt().isAfter(now)) {
            return PaymentOrder.EXPIRED;
        }
        return o.getStatus();
    }

    public static PaymentHistoryResponse toResponse(PaymentOrder o, boolean withUser, OffsetDateTime now) {
        PaymentHistoryResponse.PaymentHistoryResponseBuilder b = PaymentHistoryResponse.builder()
                .id(o.getId())
                .planCode(o.getPlanCode())
                .amount(o.getAmount())
                .paidAmount(o.getPaidAmount())
                .paymentCode(o.getPaymentCode())
                .status(effectiveStatus(o, now))
                .durationDays(o.getDurationDays())
                .createdAt(o.getCreatedAt())
                .expiresAt(o.getExpiresAt())
                .paidAt(o.getPaidAt())
                .sepayTransactionId(o.getSepayTransactionId());
        if (withUser) {
            User u = o.getUser();
            b.userId(u.getId()).userName(u.getFullName()).userEmail(u.getEmail());
        }
        return b.build();
    }
}
