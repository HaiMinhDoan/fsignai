package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.ForumPostStatus;
import com.sunmoon.backend.dto.request.forum.ForumCategoryRequest;
import com.sunmoon.backend.dto.response.forum.ForumCategoryResponse;
import com.sunmoon.backend.entity.forum.ForumCategory;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.ForumCategoryRepository;
import com.sunmoon.backend.repository.ForumPostRepository;
import com.sunmoon.backend.service.ForumCategoryService;
import com.sunmoon.backend.service.impl.util.VietnameseTextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForumCategoryServiceImpl implements ForumCategoryService {

    private final ForumCategoryRepository forumCategoryRepository;
    private final ForumPostRepository forumPostRepository;

    @Override
    public List<ForumCategoryResponse> listAll() {
        return forumCategoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ForumCategoryResponse> listPublished() {
        return forumCategoryRepository.findAllByIsPublishedTrueOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ForumCategoryResponse create(ForumCategoryRequest request) {
        ForumCategory entity = ForumCategory.builder()
                .slug(resolveSlug(request, null))
                .nameVi(request.getNameVi().trim())
                .descriptionVi(request.getDescriptionVi())
                .iconName(request.getIconName())
                .displayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder())
                .isLocked(request.getIsLocked() != null && request.getIsLocked())
                .isPublished(request.getIsPublished() == null || request.getIsPublished())
                .build();
        return toResponse(forumCategoryRepository.save(entity));
    }

    @Override
    @Transactional
    public ForumCategoryResponse update(UUID id, ForumCategoryRequest request) {
        ForumCategory entity = forumCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chuyên mục"));

        entity.setSlug(resolveSlug(request, id));
        entity.setNameVi(request.getNameVi().trim());
        entity.setDescriptionVi(request.getDescriptionVi());
        entity.setIconName(request.getIconName());
        if (request.getDisplayOrder() != null) entity.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsLocked() != null) entity.setIsLocked(request.getIsLocked());
        if (request.getIsPublished() != null) entity.setIsPublished(request.getIsPublished());

        return toResponse(forumCategoryRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ForumCategory entity = forumCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chuyên mục"));
        if (forumPostRepository.countByCategory_Id(id) > 0) {
            // Ngay ca bai da bi go mem (REMOVED) van con hang thuc trong forum_posts, va
            // category_id la FK ON DELETE RESTRICT - phai chan o day de bao loi de hieu,
            // khong de tho DB nem ra loi rang buoc du lieu chung chung.
            throw new ConflictException("Chuyên mục đang có bài viết (kể cả bài đã gỡ), không xoá được");
        }
        forumCategoryRepository.delete(entity);
    }

    private String resolveSlug(ForumCategoryRequest request, UUID currentId) {
        String base = (request.getSlug() == null || request.getSlug().isBlank())
                ? VietnameseTextUtil.toSlug(request.getNameVi())
                : VietnameseTextUtil.toSlug(request.getSlug());
        if (base.isEmpty()) {
            throw new ConflictException("Không sinh được slug từ tên chuyên mục");
        }
        String candidate = base.length() > 120 ? base.substring(0, 120) : base;
        int suffix = 2;
        while (taken(candidate, currentId)) {
            String tail = "-" + suffix++;
            int keep = Math.min(base.length(), 120 - tail.length());
            candidate = base.substring(0, keep) + tail;
        }
        return candidate;
    }

    private boolean taken(String slug, UUID currentId) {
        return currentId == null
                ? forumCategoryRepository.existsBySlug(slug)
                : forumCategoryRepository.existsBySlugAndIdNot(slug, currentId);
    }

    private ForumCategoryResponse toResponse(ForumCategory entity) {
        return ForumCategoryResponse.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .nameVi(entity.getNameVi())
                .descriptionVi(entity.getDescriptionVi())
                .iconName(entity.getIconName())
                .displayOrder(entity.getDisplayOrder())
                .isLocked(entity.getIsLocked())
                .isPublished(entity.getIsPublished())
                .postCount(forumPostRepository.countByCategory_IdAndStatusNot(entity.getId(), ForumPostStatus.REMOVED))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
