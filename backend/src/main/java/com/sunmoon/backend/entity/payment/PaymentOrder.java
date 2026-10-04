package com.sunmoon.backend.entity.payment;

import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Một lần người học bấm "Nâng cấp": giữ mã chuyển khoản và số tiền phải trả */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "payment_orders")
public class PaymentOrder {

    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";
    public static final String EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "plan_code", nullable = false, length = 20)
    private String planCode;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "payment_code", nullable = false, unique = true, length = 40)
    private String paymentCode;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = PENDING;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "paid_amount")
    private Long paidAmount;

    @Column(name = "sepay_transaction_id")
    private Long sepayTransactionId;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
