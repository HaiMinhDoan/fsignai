package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.progress.AchievementRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.progress.AchievementResponse;
import com.sunmoon.backend.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "CMS - Huy hiệu", description = "Quản lý huy hiệu thành tích")
@RestController
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @Operation(summary = "Lọc huy hiệu có phân trang")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/api/v1/admin/achievements/filter")
    public ResponseEntity<ResponseData<PageResponse<AchievementResponse>>> filter(
            @RequestBody BaseFilterRequest request) {
        return ok(achievementService.search(request), "ACHIEVEMENT_FILTER_SUCCESS");
    }

    @Operation(summary = "Chi tiết một huy hiệu")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @GetMapping("/api/v1/admin/achievements/{id}")
    public ResponseEntity<ResponseData<AchievementResponse>> detail(@PathVariable UUID id) {
        return ok(achievementService.getDetail(id), "ACHIEVEMENT_DETAIL_SUCCESS");
    }

    @Operation(summary = "Tạo huy hiệu mới")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/api/v1/admin/achievements")
    public ResponseEntity<ResponseData<AchievementResponse>> create(
            @Valid @RequestBody AchievementRequest request) {
        return ok(achievementService.create(request), "ACHIEVEMENT_CREATED");
    }

    @Operation(summary = "Cập nhật huy hiệu")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/api/v1/admin/achievements/{id}")
    public ResponseEntity<ResponseData<AchievementResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody AchievementRequest request) {
        return ok(achievementService.update(id, request), "ACHIEVEMENT_UPDATED");
    }

    @Operation(summary = "Xoá huy hiệu")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/api/v1/admin/achievements/{id}")
    public ResponseEntity<ResponseData<Void>> delete(@PathVariable UUID id) {
        achievementService.delete(id);
        return ok(null, "ACHIEVEMENT_DELETED");
    }

    @Operation(summary = "Bật hoặc tắt hàng loạt")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/api/v1/admin/achievements/set-active")
    public ResponseEntity<ResponseData<Map<String, Integer>>> setActive(
            @RequestBody SetActiveRequest request) {
        int affected = achievementService.setActive(request.ids(), request.active());
        return ok(Map.of("affected", affected),
                request.active() ? "ACHIEVEMENT_ACTIVATED" : "ACHIEVEMENT_DEACTIVATED");
    }

    @Operation(summary = "Danh sách huy hiệu cho người học",
            description = "Kèm đã đạt hay chưa và ngày đạt, để trang hồ sơ hiện được ngay không cần gọi thêm API")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/api/v1/learn/achievements")
    public ResponseEntity<ResponseData<List<AchievementResponse>>> myAchievements() {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        return ok(achievementService.listForLearner(userId), "ACHIEVEMENT_LIST_SUCCESS");
    }

    public record SetActiveRequest(List<UUID> ids, boolean active) {}

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
