package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.response.payment.PaymentOrderResponse;

import java.util.UUID;

/**
 * Thanh toán chuyển khoản qua SePay.
 *
 * Luồng: tạo đơn (mã thanh toán ngẫu nhiên) → người học quét QR → SePay gọi webhook →
 * khớp mã với đơn → gia hạn Premium. Trang thanh toán hỏi trạng thái đơn mỗi vài giây.
 */
public interface PaymentService {

    PaymentOrderResponse createOrder(UUID userId, String planCode);

    PaymentOrderResponse getOrder(UUID userId, UUID orderId);

    /**
     * Xử lý một lượt webhook đã qua xác thực. Không ném lỗi với giao dịch không khớp đơn nào:
     * trả lỗi là SePay gửi lại tối đa 7 lần trong 5 giờ, mà gửi lại cũng không khớp hơn được.
     *
     * @return ghi chú kết quả (đã khớp / bỏ qua vì sao) để ghi log
     */
    String handleWebhook(byte[] rawBody);
}
