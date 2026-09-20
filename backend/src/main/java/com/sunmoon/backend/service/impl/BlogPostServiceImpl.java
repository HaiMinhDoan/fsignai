package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.BlogCategory;
import com.sunmoon.backend.dto.request.notification.BlogPostRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.notification.BlogPostResponse;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.notification.BlogPost;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.BlogPostRepository;
import com.sunmoon.backend.repository.FileAttachmentRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.BlogPostService;
import com.sunmoon.backend.service.impl.util.VietnameseTextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlogPostServiceImpl implements BlogPostService {

    private final BlogPostRepository blogPostRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final UserRepository userRepository;

    @Override
    public PageResponse<BlogPostResponse> adminFilter(BlogCategory category, Boolean isPublished, Pageable pageable) {
        Page<BlogPost> page = blogPostRepository.adminFilter(category, isPublished, pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Override
    public BlogPostResponse detail(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public BlogPostResponse create(UUID authorId, BlogPostRequest request) {
        BlogPost entity = BlogPost.builder()
                .slug(resolveSlug(request, null))
                .titleVi(request.getTitleVi().trim())
                .excerptVi(request.getExcerptVi())
                .contentMd(request.getContentMd())
                .coverFile(resolveCover(request.getCoverFileId()))
                .author(userRepository.getReferenceById(authorId))
                .category(request.getCategory() == null ? BlogCategory.BLOG : request.getCategory())
                .isPublished(Boolean.TRUE.equals(request.getIsPublished()))
                .publishedAt(Boolean.TRUE.equals(request.getIsPublished()) ? OffsetDateTime.now() : null)
                .build();
        return toResponse(blogPostRepository.save(entity));
    }

    @Override
    @Transactional
    public BlogPostResponse update(UUID id, BlogPostRequest request) {
        BlogPost entity = findOrThrow(id);

        boolean wasPublished = entity.getIsPublished();
        entity.setSlug(resolveSlug(request, id));
        entity.setTitleVi(request.getTitleVi().trim());
        entity.setExcerptVi(request.getExcerptVi());
        entity.setContentMd(request.getContentMd());
        entity.setCoverFile(resolveCover(request.getCoverFileId()));
        entity.setCategory(request.getCategory() == null ? entity.getCategory() : request.getCategory());

        boolean nowPublished = Boolean.TRUE.equals(request.getIsPublished());
        entity.setIsPublished(nowPublished);
        if (!wasPublished && nowPublished) {
            entity.setPublishedAt(OffsetDateTime.now());
        } else if (!nowPublished) {
            entity.setPublishedAt(null);
        }

        return toResponse(blogPostRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        blogPostRepository.delete(findOrThrow(id));
    }

    @Override
    public PageResponse<BlogPostResponse> publicFeed(BlogCategory category, Pageable pageable) {
        Page<BlogPost> page = blogPostRepository.publicFeed(category, pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Override
    @Transactional
    public BlogPostResponse publicDetail(String slug) {
        BlogPost entity = blogPostRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết"));
        blogPostRepository.incrementViewCount(entity.getId());
        entity.setViewCount(entity.getViewCount() + 1);
        return toResponse(entity);
    }

    private BlogPost findOrThrow(UUID id) {
        return blogPostRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết"));
    }

    private FileAttachment resolveCover(UUID coverFileId) {
        if (coverFileId == null) return null;
        return fileAttachmentRepository.findById(coverFileId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ảnh bìa"));
    }

    private String resolveSlug(BlogPostRequest request, UUID currentId) {
        String base = (request.getSlug() == null || request.getSlug().isBlank())
                ? VietnameseTextUtil.toSlug(request.getTitleVi())
                : VietnameseTextUtil.toSlug(request.getSlug());
        if (base.isEmpty()) {
            throw new ConflictException("Không sinh được slug từ tiêu đề");
        }
        String candidate = base.length() > 160 ? base.substring(0, 160) : base;
        int suffix = 2;
        while (taken(candidate, currentId)) {
            String tail = "-" + suffix++;
            int keep = Math.min(base.length(), 160 - tail.length());
            candidate = base.substring(0, keep) + tail;
        }
        return candidate;
    }

    private boolean taken(String slug, UUID currentId) {
        return currentId == null
                ? blogPostRepository.existsBySlug(slug)
                : blogPostRepository.existsBySlugAndIdNot(slug, currentId);
    }

    private BlogPostResponse toResponse(BlogPost b) {
        return BlogPostResponse.builder()
                .id(b.getId())
                .slug(b.getSlug())
                .titleVi(b.getTitleVi())
                .excerptVi(b.getExcerptVi())
                .contentMd(b.getContentMd())
                .coverUrl(b.getCoverFile() == null ? null : b.getCoverFile().getPublicUrl())
                .authorName(b.getAuthor() == null ? null : b.getAuthor().getFullName())
                .category(b.getCategory())
                .viewCount(b.getViewCount())
                .isPublished(b.getIsPublished())
                .publishedAt(b.getPublishedAt())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
