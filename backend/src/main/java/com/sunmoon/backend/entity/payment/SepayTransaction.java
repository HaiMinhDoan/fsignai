package com.sunmoon.backend.entity.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Một giao dịch SePay báo về qua webhook — lưu cả khi không khớp đơn nào, để đối soát tay */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "sepay_transactions")
public class SepayTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** id giao dịch phía SePay — UNIQUE, chặn cộng tiền hai lần khi webhook gửi lại */
    @Column(name = "sepay_id", nullable = false, unique = true)
    private Long sepayId;

    private String gateway;

    @Column(name = "transaction_date")
    private String transactionDate;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "sub_account")
    private String subAccount;

    private String code;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "transfer_type")
    private String transferType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "transfer_amount")
    private Long transferAmount;

    private Long accumulated;

    @Column(name = "reference_code")
    private String referenceCode;

    @Type(JsonType.class)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode rawPayload;

    @Column(name = "matched_order_id")
    private UUID matchedOrderId;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
