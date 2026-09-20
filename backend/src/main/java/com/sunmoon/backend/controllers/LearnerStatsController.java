package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.gamification.LearnerStatsResponse;
import com.sunmoon.backend.service.LearnerStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@Tag(name = "Học tập - Thống kê", description = "Số liệu chuỗi ngày, sao và cấp độ cho thanh đầu trang")
@RestController
@RequiredArgsConstructor
public class LearnerStatsController {

    private final LearnerStatsService learnerStatsService;

    @Operation(summary = "Số liệu tổng hợp của người học hiện tại",
            description = "Gộp chuỗi ngày, tổng sao và cấp độ vào một lần gọi vì chúng luôn hiện cùng nhau ở header")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/api/v1/learn/stats")
    public ResponseEntity<ResponseData<LearnerStatsResponse>> myStats() {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        return ok(learnerStatsService.getStats(userId), "LEARNER_STATS_SUCCESS");
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
