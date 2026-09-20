package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.auth.AuditLogResponse;
import com.sunmoon.backend.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "CMS - Nhật ký Audit", description = "Lịch sử thao tác nhạy cảm của quản trị viên")
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "Lọc nhật ký audit")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @GetMapping
    public ResponseEntity<ResponseData<PageResponse<AuditLogResponse>>> filter(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ok(auditLogService.filter(actorId, action, entityType, PageRequest.of(page, size)),
                "AUDIT_LOG_LIST_SUCCESS");
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
