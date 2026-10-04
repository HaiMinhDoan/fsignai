package com.sunmoon.backend.dto.response.payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Giao dịch SePay báo về — kể cả giao dịch không khớp đơn nào, để kế toán đối soát tay */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SepayTransactionResponse {
    UUID id;
    Long sepayId;
    String gateway;
    String transactionDate;
    String accountNumber;
    String code;
    String content;
    String transferType;
    Long transferAmount;
    String referenceCode;
    UUID matchedOrderId;
    String matchedPaymentCode;
    String matchedUserName;
    /** Vì sao khớp / không khớp */
    String note;
    OffsetDateTime createdAt;
}
