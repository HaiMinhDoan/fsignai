package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.payment.PaymentHistoryResponse;
import com.sunmoon.backend.dto.response.payment.PaymentSummaryResponse;
import com.sunmoon.backend.dto.response.payment.SepayTransactionResponse;
import com.sunmoon.backend.service.PaymentAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

@Tag(name = "CMS - Giao dịch", description = "Đơn thanh toán của người dùng và giao dịch SePay để đối soát")
@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final PaymentAdminService paymentAdminService;

    @Operation(summary = "Danh sách đơn thanh toán",
            description = "Lọc theo người dùng, từ khoá (tên/email/mã thanh toán), trạng thái PENDING|PAID|EXPIRED "
                    + "và khoảng NGÀY TẠO đơn (ISO-8601, vd 2026-10-01T00:00:00+07:00).")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @GetMapping("/orders")
    public ResponseEntity<ResponseData<PageResponse<PaymentHistoryResponse>>> orders(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ok(paymentAdminService.orders(userId, keyword, status, from, to, PageRequest.of(page, Math.min(size, 100))),
                "PAYMENT_ORDER_LIST_SUCCESS");
    }

    @Operation(summary = "Giao dịch SePay báo về", description = "matched=false: tiền vào nhưng không khớp đơn nào, cần xử lý tay")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @GetMapping("/transactions")
    public ResponseEntity<ResponseData<PageResponse<SepayTransactionResponse>>> transactions(
            @RequestParam(required = false) Boolean matched,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ok(paymentAdminService.transactions(matched, keyword, from, to, PageRequest.of(page, Math.min(size, 100))),
                "SEPAY_TRANSACTION_LIST_SUCCESS");
    }

    @Operation(summary = "Tổng hợp doanh thu theo khoảng NGÀY THANH TOÁN")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @GetMapping("/summary")
    public ResponseEntity<ResponseData<PaymentSummaryResponse>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return ok(paymentAdminService.summary(from, to), "PAYMENT_SUMMARY_SUCCESS");
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
