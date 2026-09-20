package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.catalog.LessonItemRequest;
import com.sunmoon.backend.dto.request.catalog.LessonRequest;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.catalog.LessonItemResponse;
import com.sunmoon.backend.dto.response.catalog.LessonResponse;
import com.sunmoon.backend.service.LessonService;
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

@Tag(name = "CMS - Bài học", description = "Quản lý bài học và nội dung bên trong bài học")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    // ==================== BÀI HỌC ====================

    @Operation(summary = "Danh sách bài học của một khoá")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @GetMapping("/courses/{courseId}/lessons")
    public ResponseEntity<ResponseData<List<LessonResponse>>> list(@PathVariable UUID courseId) {
        return ok(lessonService.listByCourse(courseId), "LESSON_LIST_SUCCESS");
    }

    @Operation(summary = "Tạo bài học trong khoá")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/courses/{courseId}/lessons")
    public ResponseEntity<ResponseData<LessonResponse>> create(
            @PathVariable UUID courseId, @Valid @RequestBody LessonRequest request) {
        return ok(lessonService.createLesson(courseId, request), "LESSON_CREATED");
    }

    @Operation(summary = "Sắp xếp lại thứ tự bài học trong khoá")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/courses/{courseId}/lessons/reorder")
    public ResponseEntity<ResponseData<Void>> reorder(
            @PathVariable UUID courseId, @Valid @RequestBody ReorderRequest request) {
        lessonService.reorderLessons(courseId, request);
        return ok(null, "LESSON_REORDERED");
    }

    @Operation(summary = "Chi tiết bài học kèm toàn bộ nội dung")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<ResponseData<LessonResponse>> detail(@PathVariable UUID lessonId) {
        return ok(lessonService.getDetail(lessonId), "LESSON_DETAIL_SUCCESS");
    }

    @Operation(summary = "Cập nhật bài học")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/lessons/{lessonId}")
    public ResponseEntity<ResponseData<LessonResponse>> update(
            @PathVariable UUID lessonId, @Valid @RequestBody LessonRequest request) {
        return ok(lessonService.updateLesson(lessonId, request), "LESSON_UPDATED");
    }

    @Operation(summary = "Xoá bài học")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<ResponseData<Void>> delete(@PathVariable UUID lessonId) {
        lessonService.deleteLesson(lessonId);
        return ok(null, "LESSON_DELETED");
    }

    // ==================== NỘI DUNG BÀI HỌC ====================

    @Operation(summary = "Thêm một nội dung vào bài học")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/lessons/{lessonId}/items")
    public ResponseEntity<ResponseData<LessonItemResponse>> addItem(
            @PathVariable UUID lessonId, @Valid @RequestBody LessonItemRequest request) {
        return ok(lessonService.addItem(lessonId, request), "LESSON_ITEM_ADDED");
    }

    @Operation(summary = "Thêm nhiều từ vựng vào bài học cùng lúc",
            description = "Bỏ qua các từ đã có sẵn trong bài, trả về số từ thực sự được thêm.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/lessons/{lessonId}/items/signs")
    public ResponseEntity<ResponseData<Map<String, Integer>>> addSigns(
            @PathVariable UUID lessonId, @RequestBody AddSignsRequest request) {
        int added = lessonService.addSigns(lessonId, request.signIds());
        return ok(Map.of("added", added, "skipped",
                (request.signIds() == null ? 0 : request.signIds().size()) - added),
                "LESSON_ITEM_ADDED");
    }

    @Operation(summary = "Xoá một nội dung khỏi bài học")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/lessons/{lessonId}/items/{itemId}")
    public ResponseEntity<ResponseData<Void>> removeItem(
            @PathVariable UUID lessonId, @PathVariable UUID itemId) {
        lessonService.removeItem(lessonId, itemId);
        return ok(null, "LESSON_ITEM_REMOVED");
    }

    @Operation(summary = "Sắp xếp lại nội dung trong bài học")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/lessons/{lessonId}/items/reorder")
    public ResponseEntity<ResponseData<Void>> reorderItems(
            @PathVariable UUID lessonId, @Valid @RequestBody ReorderRequest request) {
        lessonService.reorderItems(lessonId, request);
        return ok(null, "LESSON_ITEM_REORDERED");
    }

    public record AddSignsRequest(List<UUID> signIds) {}

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
