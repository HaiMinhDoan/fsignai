package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.notification.SystemSettingRequest;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.notification.SystemSettingResponse;
import com.sunmoon.backend.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "CMS - Cài đặt hệ thống", description = "Cấu hình dạng khoá-giá trị, chỉnh được mà không cần deploy lại")
@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @Operation(summary = "Toàn bộ cấu hình hệ thống")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @GetMapping
    public ResponseEntity<ResponseData<List<SystemSettingResponse>>> list() {
        return ok(systemSettingService.listAll(), "SETTING_LIST_SUCCESS");
    }

    @Operation(summary = "Thêm cấu hình mới")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @PostMapping
    public ResponseEntity<ResponseData<SystemSettingResponse>> create(
            @Valid @RequestBody SystemSettingRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(systemSettingService.create(me, request), "SETTING_CREATED");
    }

    @Operation(summary = "Sửa giá trị cấu hình")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @PutMapping("/{id}")
    public ResponseEntity<ResponseData<SystemSettingResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody SystemSettingRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(systemSettingService.update(me, id, request), "SETTING_UPDATED");
    }

    @Operation(summary = "Xoá cấu hình")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> delete(@PathVariable UUID id) {
        systemSettingService.delete(id);
        return ok(null, "SETTING_DELETED");
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
