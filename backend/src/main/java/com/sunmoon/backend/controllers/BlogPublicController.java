package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.BlogCategory;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.notification.BlogPostResponse;
import com.sunmoon.backend.service.BlogPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Blog công khai", description = "Bài blog/our mission - không cần đăng nhập")
@RestController
@RequestMapping("/api/v1/blog")
@RequiredArgsConstructor
public class BlogPublicController {

    private final BlogPostService blogPostService;

    @Operation(summary = "Danh sách bài blog đã xuất bản")
    @GetMapping
    public ResponseEntity<ResponseData<PageResponse<BlogPostResponse>>> list(
            @RequestParam(required = false) BlogCategory category,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ok(blogPostService.publicFeed(category, PageRequest.of(page, size)), "BLOG_PUBLIC_LIST_SUCCESS");
    }

    @Operation(summary = "Chi tiết một bài blog theo slug, tự tăng lượt xem")
    @GetMapping("/{slug}")
    public ResponseEntity<ResponseData<BlogPostResponse>> detail(@PathVariable String slug) {
        return ok(blogPostService.publicDetail(slug), "BLOG_PUBLIC_DETAIL_SUCCESS");
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
