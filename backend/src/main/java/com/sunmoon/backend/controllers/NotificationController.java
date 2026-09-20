package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
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

import java.util.Map;
import java.util.UUID;

@Tag(name = "Thông báo", description = "Thông báo trong app của chính người dùng")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Danh sách thông báo của tôi")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping
    public ResponseEntity<ResponseData<PageResponse<NotificationResponse>>> listMine(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(notificationService.listMine(me, PageRequest.of(page, size)), "NOTIFICATION_LIST_SUCCESS");
    }

    @Operation(summary = "Số thông báo chưa đọc")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/unread-count")
    public ResponseEntity<ResponseData<Map<String, Long>>> unreadCount() {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(Map.of("count", notificationService.unreadCount(me)), "NOTIFICATION_UNREAD_COUNT_SUCCESS");
    }

    @Operation(summary = "Đánh dấu một thông báo đã đọc")
    @RequireAuth(roles = {RoleType.ALL})
    @PutMapping("/{id}/read")
    public ResponseEntity<ResponseData<Void>> markRead(@PathVariable UUID id) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        notificationService.markRead(me, id);
        return ok(null, "NOTIFICATION_READ_SUCCESS");
    }

    @Operation(summary = "Đánh dấu tất cả đã đọc")
    @RequireAuth(roles = {RoleType.ALL})
    @PutMapping("/read-all")
    public ResponseEntity<ResponseData<Void>> markAllRead() {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        notificationService.markAllRead(me);
        return ok(null, "NOTIFICATION_READ_ALL_SUCCESS");
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
