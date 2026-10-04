package com.sunmoon.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunmoon.backend.configs.PaymentProperties;
import com.sunmoon.backend.dto.response.payment.PaymentOrderResponse;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.payment.PaymentOrder;
import com.sunmoon.backend.entity.payment.SepayTransaction;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.PaymentOrderRepository;
import com.sunmoon.backend.repository.SepayTransactionRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.PaymentService;
import com.sunmoon.backend.service.support.SubscriptionGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final ObjectMapper JSON = new ObjectMapper();

    // Bỏ các ký tự dễ đọc nhầm (0/O, 1/I) — người dùng có thể phải gõ tay nội dung chuyển khoản
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentOrderRepository orderRepository;
    private final SepayTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PaymentProperties props;

    // ==================== TẠO ĐƠN ====================

    @Override
    @Transactional
    public PaymentOrderResponse createOrder(UUID userId, String planCode) {
        if (!SubscriptionGuard.PREMIUM.equalsIgnoreCase(planCode)) {
            throw new CommonException("Gói không hợp lệ hoặc không cần thanh toán");
        }
        if (!props.isBankConfigured()) {
            // Không dựng QR trỏ tới tài khoản rỗng: người dùng chuyển tiền đi mà không ai nhận được
            CommonException ex = new CommonException("Thanh toán chưa được cấu hình. Vui lòng quay lại sau.");
            ex.setHttpStatus(HttpStatus.SERVICE_UNAVAILABLE);
            throw ex;
        }

        OffsetDateTime now = OffsetDateTime.now();
        // Bấm "Nâng cấp" nhiều lần thì dùng lại đơn đang chờ — tránh một người cầm nhiều mã khác nhau
        Optional<PaymentOrder> cu = orderRepository.findReusable(userId, SubscriptionGuard.PREMIUM, now);
        if (cu.isPresent() && cu.get().getAmount() == props.getPremiumPrice()) {
            return toResponse(cu.get(), null);
        }

        PaymentOrder order = orderRepository.save(PaymentOrder.builder()
                .user(userRepository.getReferenceById(userId))
                .planCode(SubscriptionGuard.PREMIUM)
                .amount(props.getPremiumPrice())
                .paymentCode(maMoi())
                .durationDays(props.getPremiumDurationDays())
                .expiresAt(now.plusMinutes(props.getOrderTtlMinutes()))
                .build());
        return toResponse(order, null);
    }

    @Override
    @Transactional
    public PaymentOrderResponse getOrder(UUID userId, UUID orderId) {
        PaymentOrder order = orderRepository.findById(orderId)
                .filter(o -> o.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn thanh toán"));
        if (PaymentOrder.PENDING.equals(order.getStatus()) && order.getExpiresAt().isBefore(OffsetDateTime.now())) {
            order.setStatus(PaymentOrder.EXPIRED);
            order.setUpdatedAt(OffsetDateTime.now());
        }
        return toResponse(order, PaymentOrder.PAID.equals(order.getStatus()) ? order.getUser().getPremiumUntil() : null);
    }

    // ==================== WEBHOOK ====================

    @Override
    @Transactional
    public String handleWebhook(byte[] rawBody) {
        JsonNode p;
        try {
            p = JSON.readTree(rawBody);
        } catch (Exception e) {
            return "bỏ qua: thân yêu cầu không phải JSON";
        }
        if (!p.hasNonNull("id")) {
            return "bỏ qua: thiếu id giao dịch";
        }
        long sepayId = p.get("id").asLong();

        // Chống trùng: SePay gửi lại tối đa 7 lần trong 5 giờ nếu lần trước không nhận được phản hồi
        if (transactionRepository.existsBySepayId(sepayId)) {
            return "bỏ qua: giao dịch " + sepayId + " đã xử lý";
        }

        SepayTransaction tx = SepayTransaction.builder()
                .sepayId(sepayId)
                .gateway(text(p, "gateway"))
                .transactionDate(text(p, "transactionDate"))
                .accountNumber(text(p, "accountNumber"))
                .subAccount(text(p, "subAccount"))
                .code(text(p, "code"))
                .content(text(p, "content"))
                .transferType(text(p, "transferType"))
                .description(text(p, "description"))
                .transferAmount(p.path("transferAmount").isNumber() ? p.get("transferAmount").asLong() : null)
                .accumulated(p.path("accumulated").isNumber() ? p.get("accumulated").asLong() : null)
                .referenceCode(text(p, "referenceCode"))
                .rawPayload(p)
                .build();

        String note = khop(tx);
        tx.setNote(note);
        try {
            transactionRepository.saveAndFlush(tx);
        } catch (DataIntegrityViolationException dup) {
            // Hai lượt gửi lại chạy song song: lượt kia đã ghi trước — ràng buộc UNIQUE là chốt cuối
            throw new DuplicateWebhook();
        }
        log.info("[SePay] giao dịch {}: {}", sepayId, note);
        return note;
    }

    /** Khớp một giao dịch với đơn chờ. Trả ghi chú; gia hạn Premium nếu khớp. */
    private String khop(SepayTransaction tx) {
        if (!"in".equalsIgnoreCase(tx.getTransferType())) {
            return "bỏ qua: không phải tiền vào";
        }
        if (!props.getAccountNumber().isBlank() && tx.getAccountNumber() != null
                && !props.getAccountNumber().equals(tx.getAccountNumber())) {
            return "bỏ qua: tiền vào tài khoản khác (" + tx.getAccountNumber() + ")";
        }

        String code = maTrongGiaoDich(tx);
        if (code == null) {
            return "không khớp: không tìm thấy mã thanh toán trong nội dung";
        }
        Optional<PaymentOrder> maybe = orderRepository.findByCodeForUpdate(code);
        if (maybe.isEmpty()) {
            return "không khớp: mã " + code + " không thuộc đơn nào";
        }
        PaymentOrder order = maybe.get();
        tx.setMatchedOrderId(order.getId());

        if (PaymentOrder.PAID.equals(order.getStatus())) {
            return "đã khớp trước đó: đơn " + code + " đã thanh toán, giao dịch này cần hoàn tiền tay";
        }
        long nhan = tx.getTransferAmount() == null ? 0 : tx.getTransferAmount();
        if (nhan < order.getAmount()) {
            return "thiếu tiền: đơn " + code + " cần " + order.getAmount() + "đ, nhận " + nhan + "đ — xử lý tay";
        }

        // Đơn đã quá hạn nhưng tiền vẫn đến đúng mã: vẫn ghi nhận — tiền đã về tài khoản thật
        OffsetDateTime now = OffsetDateTime.now();
        order.setStatus(PaymentOrder.PAID);
        order.setPaidAt(now);
        order.setPaidAmount(nhan);
        order.setSepayTransactionId(tx.getSepayId());
        order.setUpdatedAt(now);
        orderRepository.save(order);

        User user = order.getUser();
        OffsetDateTime goc = user.getPremiumUntil() != null && user.getPremiumUntil().isAfter(now)
                ? user.getPremiumUntil() : now;          // còn hạn thì cộng nối tiếp, hết hạn thì tính từ hôm nay
        user.setPremiumUntil(goc.plusDays(order.getDurationDays()));
        userRepository.save(user);
        return "đã khớp: đơn " + code + ", Premium tới " + user.getPremiumUntil();
    }

    /**
     * Mã thanh toán trong giao dịch. Ưu tiên trường `code` do SePay tự nhận diện (cần cấu hình đúng
     * "Cấu trúc mã thanh toán"); không có thì tự tìm trong nội dung chuyển khoản — ngân hàng hay
     * viết hoa, thêm khoảng trắng hoặc tiền tố "MBVCB…" vào trước.
     */
    private String maTrongGiaoDich(SepayTransaction tx) {
        Pattern mau = Pattern.compile(Pattern.quote(props.getPaymentCodePrefix()) + "[A-Z0-9]{" + CODE_LENGTH + "}",
                Pattern.CASE_INSENSITIVE);
        for (String nguon : new String[]{tx.getCode(), tx.getContent(), tx.getDescription()}) {
            if (nguon == null) continue;
            Matcher m = mau.matcher(nguon.replace(" ", ""));
            if (m.find()) {
                return m.group().toUpperCase();
            }
        }
        return null;
    }

    private String maMoi() {
        for (int lan = 0; lan < 10; lan++) {
            StringBuilder sb = new StringBuilder(props.getPaymentCodePrefix());
            for (int i = 0; i < CODE_LENGTH; i++) sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
            String code = sb.toString().toUpperCase();
            if (!orderRepository.existsByPaymentCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không sinh được mã thanh toán không trùng");
    }

    private PaymentOrderResponse toResponse(PaymentOrder o, OffsetDateTime premiumUntil) {
        String qr = props.getQrBaseUrl()
                + "?acc=" + enc(props.getAccountNumber())
                + "&bank=" + enc(props.getBankCode())
                + "&amount=" + o.getAmount()
                + "&des=" + enc(o.getPaymentCode());
        return PaymentOrderResponse.builder()
                .id(o.getId())
                .planCode(o.getPlanCode())
                .amount(o.getAmount())
                .paymentCode(o.getPaymentCode())
                .status(o.getStatus())
                .qrUrl(qr)
                .bankCode(props.getBankCode())
                .accountNumber(props.getAccountNumber())
                .accountName(props.getAccountName())
                .expiresAt(o.getExpiresAt())
                .paidAt(o.getPaidAt())
                .premiumUntil(premiumUntil)
                .build();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String text(JsonNode p, String field) {
        JsonNode v = p.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    /** Giao dịch đã được một lượt webhook song song ghi trước — coi như đã xử lý */
    public static class DuplicateWebhook extends RuntimeException {
        public DuplicateWebhook() {
            super("Giao dịch đã được xử lý");
        }
    }
}
