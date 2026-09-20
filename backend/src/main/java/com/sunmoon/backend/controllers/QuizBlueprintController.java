package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.practice.QuizBlueprintRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.practice.GenerateQuestionsResult;
import com.sunmoon.backend.dto.response.practice.QuizBlueprintResponse;
import com.sunmoon.backend.service.QuizBlueprintService;
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

@Tag(name = "CMS - Cấu hình đề trộn",
        description = "Khai báo luật sinh đề tự động theo chủ đề, cấp độ, đơn vị ngôn ngữ")
@RestController
@RequestMapping("/api/v1/admin/quiz-blueprints")
@RequiredArgsConstructor
public class QuizBlueprintController {

    private final QuizBlueprintService blueprintService;

    @Operation(summary = "Lọc cấu hình đề trộn có phân trang")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/filter")
    public ResponseEntity<ResponseData<PageResponse<QuizBlueprintResponse>>> filter(
            @RequestBody BaseFilterRequest request) {
        return ok(blueprintService.search(request), "QUIZ_BLUEPRINT_FILTER_SUCCESS");
    }

    @Operation(summary = "Chi tiết một cấu hình đề trộn")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<QuizBlueprintResponse>> detail(@PathVariable UUID id) {
        return ok(blueprintService.getDetail(id), "QUIZ_BLUEPRINT_DETAIL_SUCCESS");
    }

    @Operation(summary = "Tạo cấu hình đề trộn mới")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping
    public ResponseEntity<ResponseData<QuizBlueprintResponse>> create(
            @Valid @RequestBody QuizBlueprintRequest request) {
        return ok(blueprintService.create(request), "QUIZ_BLUEPRINT_CREATED");
    }

    @Operation(summary = "Cập nhật cấu hình đề trộn")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/{id}")
    public ResponseEntity<ResponseData<QuizBlueprintResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody QuizBlueprintRequest request) {
        return ok(blueprintService.update(id, request), "QUIZ_BLUEPRINT_UPDATED");
    }

    @Operation(summary = "Xoá cấu hình đề trộn")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> delete(@PathVariable UUID id) {
        blueprintService.delete(id);
        return ok(null, "QUIZ_BLUEPRINT_DELETED");
    }

    @Operation(summary = "Bật hoặc tắt hàng loạt")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/set-active")
    public ResponseEntity<ResponseData<Map<String, Integer>>> setActive(
            @RequestBody SetActiveRequest request) {
        int affected = blueprintService.setActive(request.ids(), request.active());
        return ok(Map.of("affected", affected),
                request.active() ? "QUIZ_BLUEPRINT_ACTIVATED" : "QUIZ_BLUEPRINT_DEACTIVATED");
    }

    @Operation(summary = "Rút thử một đề theo cấu hình hiện tại",
            description = "Không ghi vào đâu cả — chỉ để soát lại luật trước khi bật dùng. "
                    + "Dùng hạt cố định theo id cấu hình nên rút thử nhiều lần ra cùng một đề; "
                    + "lúc người học thi thật, mỗi lượt thi dùng hạt riêng để đề khác nhau mỗi lần.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/{id}/preview")
    public ResponseEntity<ResponseData<GenerateQuestionsResult>> preview(@PathVariable UUID id) {
        return ok(blueprintService.preview(id), "QUIZ_BLUEPRINT_PREVIEW");
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
