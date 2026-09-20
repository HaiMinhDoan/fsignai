package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.BlogCategory;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.notification.BlogPostRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.notification.BlogPostResponse;
import com.sunmoon.backend.service.BlogPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "CMS - Blog", description = "Bài viết blog / our mission hiển thị ở trang chủ")
@RestController
@RequestMapping("/api/v1/admin/blog")
@RequiredArgsConstructor
public class BlogPostController {

    private final BlogPostService blogPostService;

    @Operation(summary = "Lọc bài blog")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @GetMapping
    public ResponseEntity<ResponseData<PageResponse<BlogPostResponse>>> filter(
            @RequestParam(required = false) BlogCategory category,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ok(blogPostService.adminFilter(category, isPublished, PageRequest.of(page, size)), "BLOG_LIST_SUCCESS");
    }

    @Operation(summary = "Chi tiết bài blog")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<BlogPostResponse>> detail(@PathVariable UUID id) {
        return ok(blogPostService.detail(id), "BLOG_DETAIL_SUCCESS");
    }

    @Operation(summary = "Tạo bài blog")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping
    public ResponseEntity<ResponseData<BlogPostResponse>> create(@Valid @RequestBody BlogPostRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(blogPostService.create(me, request), "BLOG_CREATED");
    }

    @Operation(summary = "Sửa bài blog")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/{id}")
    public ResponseEntity<ResponseData<BlogPostResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody BlogPostRequest request) {
        return ok(blogPostService.update(id, request), "BLOG_UPDATED");
    }

    @Operation(summary = "Xoá bài blog")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> delete(@PathVariable UUID id) {
        blogPostService.delete(id);
        return ok(null, "BLOG_DELETED");
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
