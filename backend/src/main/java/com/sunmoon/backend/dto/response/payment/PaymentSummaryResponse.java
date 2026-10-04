package com.sunmoon.backend.dto.response.payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Tổng hợp nhanh cho đầu trang Giao dịch của CMS, theo cùng bộ lọc thời gian */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentSummaryResponse {
    long revenue;
    long paidOrders;
    long pendingOrders;
    long expiredOrders;
    long payingUsers;
    /** Giao dịch tiền vào không khớp đơn nào — cần người xử lý tay */
    long unmatchedTransactions;
}
