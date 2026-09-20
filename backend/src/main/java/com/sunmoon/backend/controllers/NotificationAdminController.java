package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.NotificationChannel;
import com.sunmoon.backend.constant.enums.NotificationStatus;
import com.sunmoon.backend.constant.enums.NotificationType;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.notification.NotificationResponse;
import com.sunmoon.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "CMS - Thông báo", description = "Xem lịch sử thông báo đã gửi (trong app và email) để hỗ trợ/gỡ lỗi")
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class NotificationAdminController {

    private final NotificationService notificationService;

    @Operation(summary = "Lọc lịch sử thông báo đã gửi")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @GetMapping
    public ResponseEntity<ResponseData<PageResponse<NotificationResponse>>> filter(
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ok(notificationService.adminFilter(type, channel, status, PageRequest.of(page, size)),
                "NOTIFICATION_ADMIN_LIST_SUCCESS");
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
