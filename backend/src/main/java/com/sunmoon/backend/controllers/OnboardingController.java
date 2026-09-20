package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.auth.OnboardingRequest;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.auth.OnboardingResultResponse;
import com.sunmoon.backend.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Onboarding", description = "4 câu hỏi sau đăng ký để cá nhân hoá lộ trình học")
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(summary = "Câu trả lời onboarding hiện tại của tôi")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping
    public ResponseEntity<ResponseData<OnboardingResultResponse>> getMine() {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(onboardingService.getMine(me), "ONBOARDING_GET_SUCCESS");
    }

    @Operation(summary = "Lưu câu trả lời (từng câu một hoặc trả lời xong hết)",
            description = "Field nào gửi null thì giữ nguyên giá trị đã lưu trước đó. complete=true mới bắt buộc đủ 4 câu.")
    @RequireAuth(roles = {RoleType.ALL})
    @PutMapping
    public ResponseEntity<ResponseData<OnboardingResultResponse>> save(
            @Valid @RequestBody OnboardingRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(onboardingService.save(me, request), "ONBOARDING_SAVE_SUCCESS");
    }

    private <T> ResponseEntity<ResponseData<T>> ok(T data, String messageCode) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseData.<T>builder()
                .status(HttpStatus.OK.value())
                .messageCode(messageCode)
                .data(data)
                .lang(SecurityContextHolder.getLang())
                .path(SecurityContextHolder.getPath())
                .timestamp(new java.util.Date())
                .build());
    }
}
