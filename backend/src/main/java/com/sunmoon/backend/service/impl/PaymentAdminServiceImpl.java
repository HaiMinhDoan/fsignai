package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.payment.PaymentHistoryResponse;
import com.sunmoon.backend.dto.response.payment.PaymentSummaryResponse;
import com.sunmoon.backend.dto.response.payment.SepayTransactionResponse;
import com.sunmoon.backend.entity.payment.PaymentOrder;
import com.sunmoon.backend.entity.payment.SepayTransaction;
import com.sunmoon.backend.repository.AnalyticsQueryRepository;
import com.sunmoon.backend.repository.PaymentOrderRepository;
import com.sunmoon.backend.repository.SepayTransactionRepository;
import com.sunmoon.backend.service.PaymentAdminService;
import com.sunmoon.backend.service.support.PaymentHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.sunmoon.backend.repository.AnalyticsQueryRepository.num;

@Service
@RequiredArgsConstructor
public class PaymentAdminServiceImpl implements PaymentAdminService {

    private final PaymentOrderRepository orderRepository;
    private final SepayTransactionRepository transactionRepository;
    private final AnalyticsQueryRepository analytics;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentHistoryResponse> orders(UUID userId, String keyword, String status,
                                                       OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        OffsetDateTime now = OffsetDateTime.now();
        return PageResponse.of(
                orderRepository.history(userId, like(keyword), upperOrNull(status), lo(from), hi(to), now, pageable),
                o -> PaymentHistoryMapper.toResponse(o, true, now));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SepayTransactionResponse> transactions(Boolean matched, String keyword,
                                                               OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        Page<SepayTransaction> page = transactionRepository.filter(matched, like(keyword), lo(from), hi(to), pageable);
        // Gắn mã đơn + tên người trả cho các giao dịch đã khớp trong MỘT lần truy vấn
        Map<UUID, PaymentOrder> donKhop = orderRepository.findAllById(page.getContent().stream()
                        .map(SepayTransaction::getMatchedOrderId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(PaymentOrder::getId, Function.identity()));
        return PageResponse.of(page, t -> {
            PaymentOrder o = t.getMatchedOrderId() == null ? null : donKhop.get(t.getMatchedOrderId());
            return SepayTransactionResponse.builder()
                    .id(t.getId())
                    .sepayId(t.getSepayId())
                    .gateway(t.getGateway())
                    .transactionDate(t.getTransactionDate())
                    .accountNumber(t.getAccountNumber())
                    .code(t.getCode())
                    .content(t.getContent())
                    .transferType(t.getTransferType())
                    .transferAmount(t.getTransferAmount())
                    .referenceCode(t.getReferenceCode())
                    .matchedOrderId(t.getMatchedOrderId())
                    .matchedPaymentCode(o == null ? null : o.getPaymentCode())
                    .matchedUserName(o == null ? null : o.getUser().getFullName())
                    .note(t.getNote())
                    .createdAt(t.getCreatedAt())
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSummaryResponse summary(OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime now = OffsetDateTime.now();
        Object[] rev = analytics.revenueTotals(lo(from), hi(to));
        return PaymentSummaryResponse.builder()
                .revenue(num(rev[0]))
                .paidOrders(num(rev[1]))
                .payingUsers(num(rev[2]))
                .pendingOrders(analytics.ordersPending(now))
                .expiredOrders(analytics.ordersExpired(lo(from), hi(to), now))
                .unmatchedTransactions(analytics.unmatchedTransactions(lo(from), hi(to)))
                .build();
    }

    private static OffsetDateTime lo(OffsetDateTime t) {
        return t == null ? PaymentHistoryMapper.MIN_TIME : t;
    }

    private static OffsetDateTime hi(OffsetDateTime t) {
        return t == null ? PaymentHistoryMapper.MAX_TIME : t;
    }

    private static String like(String keyword) {
        return keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";
    }

    private static String upperOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim().toUpperCase();
    }
}
