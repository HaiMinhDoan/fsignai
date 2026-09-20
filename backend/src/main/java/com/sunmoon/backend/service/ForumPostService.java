package com.sunmoon.backend.service;

import com.sunmoon.backend.constant.enums.ForumPostStatus;
import com.sunmoon.backend.dto.request.forum.ForumModerateRequest;
import com.sunmoon.backend.dto.request.forum.ForumPostRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.forum.ForumPostResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ForumPostService {

    /** Chỉ bài PUBLISHED, ghim lên trước - dùng cho danh sách công khai */
    PageResponse<ForumPostResponse> listPublished(UUID categoryId, UUID currentUserId, Pageable pageable);

    /** Tác giả luôn xem được bài của mình dù trạng thái gì; người khác chỉ xem được bài PUBLISHED */
    ForumPostResponse detail(UUID id, UUID currentUserId, boolean bumpView);

    ForumPostResponse create(UUID authorId, ForumPostRequest request);

    ForumPostResponse update(UUID authorId, UUID id, ForumPostRequest request);

    void delete(UUID authorId, UUID id);

    // ===== CMS =====
    PageResponse<ForumPostResponse> adminFilter(ForumPostStatus status, UUID categoryId, Pageable pageable);

    ForumPostResponse moderate(UUID actingAdminId, UUID id, ForumModerateRequest request);

    ForumPostResponse setPinned(UUID actingAdminId, UUID id, boolean pinned);

    ForumPostResponse setLocked(UUID actingAdminId, UUID id, boolean locked);
}
