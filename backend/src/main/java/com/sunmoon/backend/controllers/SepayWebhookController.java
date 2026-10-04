package com.sunmoon.backend.controllers;

import com.sunmoon.backend.configs.PaymentProperties;
import com.sunmoon.backend.service.PaymentService;
import com.sunmoon.backend.service.impl.PaymentServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/**
 * Webhook SePay — KHÔNG có @RequireAuth vì SePay không mang token người dùng; thay vào đó xác
 * thực theo cấu hình ở trang webhook của SePay (docs: developer.sepay.vn/vi/sepay-webhooks/xac-thuc):
 *
 *   API Key:      Authorization: Apikey <SEPAY_WEBHOOK_API_KEY>
 *   HMAC-SHA256:  X-SePay-Signature: sha256=<hex(HMAC(secret, "{timestamp}.{raw body}"))>
 *                 X-SePay-Timestamp: <unix giây>, lệch tối đa ±5 phút
 *
 * Chưa cấu hình cả hai thì TỪ CHỐI mọi lời gọi: webhook mở toang là ai cũng tự cộng Premium được.
 * Phải trả 200 {"success": true} trong 30 giây, không thì SePay gửi lại (tối đa 7 lần / 5 giờ).
 */
@Slf4j
@Tag(name = "Gói dịch vụ & thanh toán")
@RestController
@RequestMapping("/api/v1/payments/sepay")
@RequiredArgsConstructor
public class SepayWebhookController {

    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300;

    private final PaymentService paymentService;
    private final PaymentProperties props;

    @Operation(summary = "Webhook SePay báo giao dịch chuyển khoản",
            description = "Khai báo URL này ở my.sepay.vn → Webhooks. Nhận thân yêu cầu thô để kiểm chữ ký HMAC.")
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> webhook(
            @RequestBody byte[] body,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-SePay-Signature", required = false) String signature,
            @RequestHeader(value = "X-SePay-Timestamp", required = false) String timestamp) {

        if (!xacThuc(body, authorization, signature, timestamp)) {
            log.warn("[SePay] webhook bị từ chối: sai hoặc thiếu thông tin xác thực");
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        try {
            paymentService.handleWebhook(body);
        } catch (PaymentServiceImpl.DuplicateWebhook dup) {
            // Lượt gửi lại song song — đã ghi nhận ở lượt kia
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    private boolean xacThuc(byte[] body, String authorization, String signature, String timestamp) {
        String apiKey = props.getWebhookApiKey();
        String secret = props.getWebhookHmacSecret();
        if (apiKey.isBlank() && secret.isBlank()) {
            return false;
        }
        if (!apiKey.isBlank() && authorization != null && authorization.startsWith("Apikey ")) {
            return bang(authorization.substring("Apikey ".length()).trim(), apiKey);
        }
        if (!secret.isBlank() && signature != null && timestamp != null) {
            long ts;
            try {
                ts = Long.parseLong(timestamp.trim());
            } catch (NumberFormatException e) {
                return false;
            }
            if (Math.abs(System.currentTimeMillis() / 1000 - ts) > TIMESTAMP_TOLERANCE_SECONDS) {
                return false;      // chặn phát lại một yêu cầu cũ đã bị nghe lén
            }
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                mac.update((timestamp.trim() + ".").getBytes(StandardCharsets.UTF_8));
                // Ký trên BYTE GỐC của thân yêu cầu — parse rồi serialize lại là lệch chữ ký
                String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
                return bang(signature.trim(), expected);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /** So sánh thời gian hằng — không để lộ khoá qua độ trễ phản hồi */
    private static boolean bang(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
