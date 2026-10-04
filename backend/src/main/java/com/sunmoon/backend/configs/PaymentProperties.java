package com.sunmoon.backend.configs;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cấu hình gói dịch vụ và SePay. Mọi giá trị nhạy cảm (số tài khoản, khoá webhook) lấy từ biến
 * môi trường — để trống trong application.properties, KHÔNG ghi thẳng vào mã nguồn.
 */
@Getter
@Component
public class PaymentProperties {

    /** Mã ngân hàng theo danh sách của SePay/VietQR, ví dụ MBBank, Vietcombank, TPBank */
    @Value("${sepay.bank-code:}")
    private String bankCode;

    @Value("${sepay.account-number:}")
    private String accountNumber;

    /** Tên chủ tài khoản, chỉ để hiện cho người chuyển khoản đối chiếu */
    @Value("${sepay.account-name:}")
    private String accountName;

    @Value("${sepay.qr-base-url:https://qr.sepay.vn/img}")
    private String qrBaseUrl;

    /**
     * Tiền tố mã thanh toán. PHẢI trùng với cấu hình ở SePay:
     * Công ty → Cấu hình chung → Cấu trúc mã thanh toán. Lệch là webhook đến với code = null.
     */
    @Value("${sepay.payment-code-prefix:SIGNAI}")
    private String paymentCodePrefix;

    /** Xác thực webhook kiểu "API Key": header  Authorization: Apikey <khoá> */
    @Value("${sepay.webhook-api-key:}")
    private String webhookApiKey;

    /** Xác thực webhook kiểu HMAC-SHA256 (X-SePay-Signature / X-SePay-Timestamp) */
    @Value("${sepay.webhook-hmac-secret:}")
    private String webhookHmacSecret;

    /** Mã thanh toán sống bao lâu kể từ lúc tạo */
    @Value("${sepay.order-ttl-minutes:30}")
    private int orderTtlMinutes;

    @Value("${plan.premium.price:199000}")
    private long premiumPrice;

    /** Mỗi lần thanh toán cộng thêm bấy nhiêu ngày Premium */
    @Value("${plan.premium.duration-days:30}")
    private int premiumDurationDays;

    public boolean isBankConfigured() {
        return !accountNumber.isBlank() && !bankCode.isBlank();
    }
}
