package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.payment.PaymentHistoryResponse;
import com.sunmoon.backend.dto.response.payment.PaymentSummaryResponse;
import com.sunmoon.backend.dto.response.payment.SepayTransactionResponse;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Giao dịch của người dùng cho CMS: đơn thanh toán và giao dịch SePay để đối soát */
public interface PaymentAdminService {

    PageResponse<PaymentHistoryResponse> orders(UUID userId, String keyword, String status,
                                                OffsetDateTime from, OffsetDateTime to, Pageable pageable);

    PageResponse<SepayTransactionResponse> transactions(Boolean matched, String keyword,
                                                        OffsetDateTime from, OffsetDateTime to, Pageable pageable);

    PaymentSummaryResponse summary(OffsetDateTime from, OffsetDateTime to);
}
