package com.sunmoon.backend.service;

import com.sunmoon.backend.constant.enums.BlogCategory;
import com.sunmoon.backend.dto.request.notification.BlogPostRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.notification.BlogPostResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BlogPostService {

    // ===== CMS =====
    PageResponse<BlogPostResponse> adminFilter(BlogCategory category, Boolean isPublished, Pageable pageable);

    BlogPostResponse detail(UUID id);

    BlogPostResponse create(UUID authorId, BlogPostRequest request);

    BlogPostResponse update(UUID id, BlogPostRequest request);

    void delete(UUID id);

    // ===== Công khai =====
    PageResponse<BlogPostResponse> publicFeed(BlogCategory category, Pageable pageable);

    BlogPostResponse publicDetail(String slug);
}
