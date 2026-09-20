package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.ai.ExemplarJobStatusResponse;
import com.sunmoon.backend.dto.response.ai.ExemplarResponse;
import com.sunmoon.backend.service.ExemplarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Tag(name = "CMS - Mẫu chấm điểm AI", description = "Sinh và quản lý exemplar để chấm ký hiệu")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AiAdminController {

    private final ExemplarService exemplarService;

    @Operation(summary = "Các mẫu chấm điểm của một từ")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR, RoleType.VSL_REVIEWER})
    @GetMapping("/signs/{signId}/exemplars")
    public ResponseEntity<ResponseData<List<ExemplarResponse>>> list(@PathVariable UUID signId) {
        return ok(exemplarService.listForSign(signId), "EXEMPLAR_LIST_SUCCESS");
    }

    @Operation(summary = "Sinh lại mẫu cho một từ",
            description = "Chạy MediaPipe trên từng video của từ. Đồng bộ — vài giây mỗi video.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/signs/{signId}/exemplars/rebuild")
    public ResponseEntity<ResponseData<List<ExemplarResponse>>> rebuild(@PathVariable UUID signId) {
        return ok(exemplarService.rebuildForSign(signId), "EXEMPLAR_REBUILT");
    }

    @Operation(summary = "Bật/tắt một mẫu", description = "Tắt mẫu xấu (quay hỏng, ký sai) mà không cần xoá")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PatchMapping("/exemplars/{exemplarId}/active")
    public ResponseEntity<ResponseData<Void>> setActive(@PathVariable UUID exemplarId,
                                                        @RequestParam boolean active) {
        exemplarService.setActive(exemplarId, active);
        return ok(null, "EXEMPLAR_UPDATED");
    }

    @Operation(summary = "Sinh mẫu hàng loạt (chạy nền)",
            description = "Xử lý các video chưa có mẫu ở phiên bản hiện hành. Chỉ một job chạy một lúc. "
                    + "Theo dõi tại GET /admin/ai/exemplars/status.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/ai/exemplars/build-missing")
    public ResponseEntity<ResponseData<ExemplarJobStatusResponse>> buildMissing(
            @RequestParam(defaultValue = "200") int limit,
            @RequestParam(defaultValue = "false") boolean retryFailed) {
        return ok(exemplarService.startBuildJob(limit, retryFailed), "EXEMPLAR_JOB_STARTED");
    }

    @Operation(summary = "Tiến độ sinh mẫu và toàn cảnh độ phủ")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR, RoleType.VSL_REVIEWER})
    @GetMapping("/ai/exemplars/status")
    public ResponseEntity<ResponseData<ExemplarJobStatusResponse>> status() {
        return ok(exemplarService.jobStatus(), "EXEMPLAR_JOB_STATUS");
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
