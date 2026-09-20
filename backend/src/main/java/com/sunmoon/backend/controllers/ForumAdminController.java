package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.ForumPostStatus;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.forum.*;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.forum.ForumCategoryResponse;
import com.sunmoon.backend.dto.response.forum.ForumCommentResponse;
import com.sunmoon.backend.dto.response.forum.ForumPostResponse;
import com.sunmoon.backend.dto.response.forum.ForumReportResponse;
import com.sunmoon.backend.service.ForumCategoryService;
import com.sunmoon.backend.service.ForumCommentService;
import com.sunmoon.backend.service.ForumPostService;
import com.sunmoon.backend.service.ForumReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "CMS - Diễn đàn", description = "Quản lý chuyên mục, kiểm duyệt bài/bình luận, xử lý báo cáo")
@RestController
@RequestMapping("/api/v1/admin/forum")
@RequiredArgsConstructor
public class ForumAdminController {

    private final ForumCategoryService forumCategoryService;
    private final ForumPostService forumPostService;
    private final ForumCommentService forumCommentService;
    private final ForumReportService forumReportService;

    // ===== Chuyên mục =====

    @Operation(summary = "Toàn bộ chuyên mục kể cả chưa xuất bản")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR, RoleType.MODERATOR})
    @GetMapping("/categories")
    public ResponseEntity<ResponseData<List<ForumCategoryResponse>>> categories() {
        return ok(forumCategoryService.listAll(), "FORUM_CATEGORY_LIST_SUCCESS");
    }

    @Operation(summary = "Tạo chuyên mục")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/categories")
    public ResponseEntity<ResponseData<ForumCategoryResponse>> createCategory(
            @Valid @RequestBody ForumCategoryRequest request) {
        return ok(forumCategoryService.create(request), "FORUM_CATEGORY_CREATED");
    }

    @Operation(summary = "Sửa chuyên mục")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/categories/{id}")
    public ResponseEntity<ResponseData<ForumCategoryResponse>> updateCategory(
            @PathVariable UUID id, @Valid @RequestBody ForumCategoryRequest request) {
        return ok(forumCategoryService.update(id, request), "FORUM_CATEGORY_UPDATED");
    }

    @Operation(summary = "Xoá chuyên mục (chặn nếu còn bài viết)")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ResponseData<Void>> deleteCategory(@PathVariable UUID id) {
        forumCategoryService.delete(id);
        return ok(null, "FORUM_CATEGORY_DELETED");
    }

    // ===== Bài viết =====

    @Operation(summary = "Lọc bài viết theo trạng thái/chuyên mục")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.MODERATOR})
    @GetMapping("/posts")
    public ResponseEntity<ResponseData<PageResponse<ForumPostResponse>>> posts(
            @RequestParam(required = false) ForumPostStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ok(forumPostService.adminFilter(status, categoryId, PageRequest.of(page, size)),
                "FORUM_POST_LIST_SUCCESS");
    }

    @Operation(summary = "Đổi trạng thái bài viết (ẩn/gỡ/khôi phục)")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.MODERATOR})
    @PutMapping("/posts/{id}/status")
    public ResponseEntity<ResponseData<ForumPostResponse>> moderatePost(
            @PathVariable UUID id, @Valid @RequestBody ForumModerateRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(forumPostService.moderate(me, id, request), "FORUM_POST_MODERATED");
    }

    @Operation(summary = "Ghim/bỏ ghim bài viết")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.MODERATOR})
    @PutMapping("/posts/{id}/pin")
    public ResponseEntity<ResponseData<ForumPostResponse>> pinPost(
            @PathVariable UUID id, @RequestParam boolean pinned) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(forumPostService.setPinned(me, id, pinned), "FORUM_POST_PIN_UPDATED");
    }

    @Operation(summary = "Khoá/mở bình luận cho bài viết")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.MODERATOR})
    @PutMapping("/posts/{id}/lock")
    public ResponseEntity<ResponseData<ForumPostResponse>> lockPost(
            @PathVariable UUID id, @RequestParam boolean locked) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(forumPostService.setLocked(me, id, locked), "FORUM_POST_LOCK_UPDATED");
    }

    // ===== Bình luận =====

    @Operation(summary = "Đổi trạng thái bình luận (ẩn/gỡ/khôi phục)")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.MODERATOR})
    @PutMapping("/comments/{id}/status")
    public ResponseEntity<ResponseData<ForumCommentResponse>> moderateComment(
            @PathVariable UUID id, @Valid @RequestBody ForumModerateRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(forumCommentService.moderate(me, id, request), "FORUM_COMMENT_MODERATED");
    }

    // ===== Báo cáo vi phạm =====

    @Operation(summary = "Danh sách báo cáo, mặc định chỉ hiện OPEN/REVIEWING")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.MODERATOR})
    @GetMapping("/reports")
    public ResponseEntity<ResponseData<PageResponse<ForumReportResponse>>> reports(
            @RequestParam(defaultValue = "true") boolean openOnly,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ok(forumReportService.filter(openOnly, PageRequest.of(page, size)), "FORUM_REPORT_LIST_SUCCESS");
    }

    @Operation(summary = "Xử lý một báo cáo (đánh dấu đã xử lý/bỏ qua)")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.MODERATOR})
    @PutMapping("/reports/{id}")
    public ResponseEntity<ResponseData<ForumReportResponse>> handleReport(
            @PathVariable UUID id, @Valid @RequestBody ForumReportHandleRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(forumReportService.handle(me, id, request), "FORUM_REPORT_HANDLED");
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
