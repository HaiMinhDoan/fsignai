package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.AuthInfo;
import com.sunmoon.backend.dto.request.payment.CreateOrderRequest;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.payment.PaymentOrderResponse;
import com.sunmoon.backend.dto.response.payment.PlanResponse;
import com.sunmoon.backend.dto.response.payment.SubscriptionResponse;
import com.sunmoon.backend.service.PaymentService;
import com.sunmoon.backend.service.support.SubscriptionGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Tag(name = "Gói dịch vụ & thanh toán", description = "Gói Free / Premium, tạo đơn chuyển khoản SePay")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final SubscriptionGuard subscriptionGuard;

    @Operation(summary = "Danh sách gói dịch vụ")
    @GetMapping("/plans")
    public ResponseEntity<ResponseData<List<PlanResponse>>> plans() {
        return ok(subscriptionGuard.plans(), "PLAN_LIST_SUCCESS");
    }

    @Operation(summary = "Gói hiện tại của tôi")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/payments/me")
    public ResponseEntity<ResponseData<SubscriptionResponse>> me() {
        AuthInfo auth = SecurityContextHolder.getAuthInfo();
        return ok(subscriptionGuard.current(auth.getId(), auth), "SUBSCRIPTION_SUCCESS");
    }

    @Operation(summary = "Tạo đơn nâng cấp",
            description = "Trả QR + thông tin chuyển khoản. Bấm nhiều lần thì dùng lại đơn đang chờ còn hạn.")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/payments/orders")
    public ResponseEntity<ResponseData<PaymentOrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ok(paymentService.createOrder(SecurityContextHolder.getAuthInfo().getId(), request.getPlanCode()),
                "PAYMENT_ORDER_CREATED");
    }

    @Operation(summary = "Trạng thái đơn thanh toán", description = "Trang thanh toán hỏi lại mỗi 3 giây cho tới khi PAID")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/payments/orders/{id}")
    public ResponseEntity<ResponseData<PaymentOrderResponse>> getOrder(@PathVariable UUID id) {
        return ok(paymentService.getOrder(SecurityContextHolder.getAuthInfo().getId(), id), "PAYMENT_ORDER_SUCCESS");
    }

    private <T> ResponseEntity<ResponseData<T>> ok(T data, String messageCode) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseData.<T>builder()
                .status(HttpStatus.OK.value())
                .messageCode(messageCode)
                .data(data)
                .lang(SecurityContextHolder.getLang())
                .path(SecurityContextHolder.getPath())
                .timestamp(new Date())
                .build());
    }
}
