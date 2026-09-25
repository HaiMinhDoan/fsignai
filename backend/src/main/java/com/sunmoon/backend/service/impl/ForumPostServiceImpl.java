package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.ForumPostStatus;
import com.sunmoon.backend.constant.enums.NotificationType;
import com.sunmoon.backend.constant.enums.ReactionTargetType;
import com.sunmoon.backend.dto.request.forum.ForumModerateRequest;
import com.sunmoon.backend.dto.request.forum.ForumPostRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.forum.ForumPostResponse;
import com.sunmoon.backend.dto.response.forum.MediaResponse;
import com.sunmoon.backend.entity.forum.MediaAsset;
import com.sunmoon.backend.service.ForumMediaService;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.entity.forum.ForumCategory;
import com.sunmoon.backend.entity.forum.ForumPost;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.*;
import com.sunmoon.backend.service.AuditLogService;
import com.sunmoon.backend.service.ForumPostService;
import com.sunmoon.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumPostServiceImpl implements ForumPostService {

    private final ForumPostRepository forumPostRepository;
    private final ForumMediaService forumMediaService;
    private final ForumCategoryRepository forumCategoryRepository;
    private final ForumReactionRepository forumReactionRepository;
    private final UserRepository userRepository;
    private final SignRepository signRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Override
    public PageResponse<ForumPostResponse> listPublished(UUID categoryId, UUID currentUserId, Pageable pageable) {
        Page<ForumPost> page = forumPostRepository.findFeed(ForumPostStatus.PUBLISHED, categoryId, pageable);
        Set<UUID> likedIds = likedTargetIds(currentUserId, page.getContent().stream().map(ForumPost::getId).collect(Collectors.toSet()));
        Map<UUID, List<MediaResponse>> media = forumMediaService
                .ofPosts(page.getContent().stream().map(ForumPost::getId).toList());
        return PageResponse.of(page, p -> toResponse(p, likedIds.contains(p.getId()), media.get(p.getId())));
    }

    @Override
    @Transactional
    public ForumPostResponse detail(UUID id, UUID currentUserId, boolean bumpView) {
        ForumPost post = forumPostRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết"));

        boolean isAuthor = currentUserId != null && post.getAuthor().getId().equals(currentUserId);
        if (post.getStatus() != ForumPostStatus.PUBLISHED && !isAuthor) {
            throw new NotFoundException("Không tìm thấy bài viết");
        }

        if (bumpView) {
            forumPostRepository.incrementViewCount(id);
            post.setViewCount(post.getViewCount() + 1);
        }

        boolean liked = currentUserId != null && forumReactionRepository
                .findByUser_IdAndTargetTypeAndTargetId(currentUserId, ReactionTargetType.POST, id).isPresent();
        return toResponse(post, liked);
    }

    @Override
    @Transactional
    public ForumPostResponse create(UUID authorId, ForumPostRequest request) {
        ForumCategory category = forumCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chuyên mục"));
        if (Boolean.TRUE.equals(category.getIsLocked())) {
            throw forbidden("Chuyên mục này đang bị khoá, không đăng bài mới được");
        }

        Sign sign = request.getSignId() == null ? null : signRepository.findById(request.getSignId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy từ được gắn"));

        phaiCoNoiDung(request);
        MediaAsset titleMedia = forumMediaService.resolveTitleMedia(authorId, request.getTitleMediaId());

        OffsetDateTime now = OffsetDateTime.now();
        ForumPost post = ForumPost.builder()
                .category(category)
                .author(userRepository.getReferenceById(authorId))
                .titleVi(chuanHoaTieuDe(request.getTitleVi()))
                .titleMedia(titleMedia)
                .bodyMd(request.getBodyMd())
                .sign(sign)
                .status(ForumPostStatus.PUBLISHED)
                .publishedAt(now)
                .lastActivityAt(now)
                .build();

        post = forumPostRepository.save(post);
        forumMediaService.attachToPost(post, authorId, request.getMediaIds());
        return toResponse(post, false);
    }

    @Override
    @Transactional
    public ForumPostResponse update(UUID authorId, UUID id, ForumPostRequest request) {
        ForumPost post = requireOwned(authorId, id);
        if (post.getStatus() == ForumPostStatus.REMOVED) {
            throw new ConflictException("Bài đã gỡ, không sửa được nữa");
        }

        ForumCategory category = forumCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chuyên mục"));
        Sign sign = request.getSignId() == null ? null : signRepository.findById(request.getSignId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy từ được gắn"));

        phaiCoNoiDung(request);
        post.setCategory(category);
        post.setTitleVi(chuanHoaTieuDe(request.getTitleVi()));
        post.setTitleMedia(forumMediaService.resolveTitleMedia(authorId, request.getTitleMediaId()));
        post.setBodyMd(request.getBodyMd());
        post.setSign(sign);
        forumMediaService.attachToPost(post, authorId, request.getMediaIds());

        boolean liked = forumReactionRepository
                .findByUser_IdAndTargetTypeAndTargetId(authorId, ReactionTargetType.POST, id).isPresent();
        return toResponse(forumPostRepository.save(post), liked);
    }

    @Override
    @Transactional
    public void delete(UUID authorId, UUID id) {
        ForumPost post = requireOwned(authorId, id);
        post.setStatus(ForumPostStatus.REMOVED);
        forumPostRepository.save(post);
    }

    @Override
    public PageResponse<ForumPostResponse> adminFilter(ForumPostStatus status, UUID categoryId, Pageable pageable) {
        Page<ForumPost> page = forumPostRepository.findAdminFeed(status, categoryId, pageable);
        return PageResponse.of(page, p -> toResponse(p, false));
    }

    @Override
    @Transactional
    public ForumPostResponse moderate(UUID actingAdminId, UUID id, ForumModerateRequest request) {
        ForumPost post = forumPostRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết"));
        ForumPostStatus before = post.getStatus();
        post.setStatus(request.getStatus());
        if (request.getStatus() == ForumPostStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(OffsetDateTime.now());
        }
        ForumPost saved = forumPostRepository.save(post);
        auditLogService.record(actingAdminId, "forum.post.moderate", "forum_posts", id,
                Map.of("status", before), Map.of("status", request.getStatus()));

        boolean justTakenDown = before != ForumPostStatus.HIDDEN && before != ForumPostStatus.REMOVED
                && (request.getStatus() == ForumPostStatus.HIDDEN || request.getStatus() == ForumPostStatus.REMOVED);
        if (justTakenDown) {
            notificationService.notify(saved.getAuthor().getId(), NotificationType.MODERATION_RESULT,
                    "Bài viết của bạn đã bị gỡ",
                    "Bài \"" + saved.getTitleVi() + "\" đã bị quản trị viên gỡ khỏi Diễn đàn.",
                    null, true);
        }
        return toResponse(saved, false);
    }

    @Override
    @Transactional
    public ForumPostResponse setPinned(UUID actingAdminId, UUID id, boolean pinned) {
        ForumPost post = forumPostRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết"));
        post.setIsPinned(pinned);
        ForumPost saved = forumPostRepository.save(post);
        auditLogService.record(actingAdminId, "forum.post.pin", "forum_posts", id, null, Map.of("isPinned", pinned));
        return toResponse(saved, false);
    }

    @Override
    @Transactional
    public ForumPostResponse setLocked(UUID actingAdminId, UUID id, boolean locked) {
        ForumPost post = forumPostRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết"));
        post.setIsLocked(locked);
        ForumPost saved = forumPostRepository.save(post);
        auditLogService.record(actingAdminId, "forum.post.lock", "forum_posts", id, null, Map.of("isLocked", locked));
        return toResponse(saved, false);
    }

    private ForumPost requireOwned(UUID authorId, UUID id) {
        ForumPost post = forumPostRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết"));
        if (!post.getAuthor().getId().equals(authorId)) {
            throw forbidden("Đây không phải bài viết của bạn");
        }
        return post;
    }

    /**
     * Bài phải nói được điều gì đó: chữ hoặc hình, ở tiêu đề hoặc ở nội dung.
     *
     * Không ép có tiêu đề chữ: với người điếc, tiếng Việt viết là ngôn ngữ thứ hai,
     * ra hiệu tiêu đề bằng video là cách nói tự nhiên hơn.
     */
    private void phaiCoNoiDung(ForumPostRequest r) {
        boolean coChu = (r.getTitleVi() != null && !r.getTitleVi().isBlank())
                || (r.getBodyMd() != null && !r.getBodyMd().isBlank());
        boolean coHinh = r.getTitleMediaId() != null
                || (r.getMediaIds() != null && !r.getMediaIds().isEmpty());
        if (!coChu && !coHinh) {
            throw new CommonException("Bài viết cần ít nhất một dòng chữ, một video ký hiệu hoặc một tấm ảnh");
        }
    }

    private static String chuanHoaTieuDe(String tieuDe) {
        if (tieuDe == null || tieuDe.isBlank()) {
            return null;
        }
        return tieuDe.trim();
    }

    private CommonException forbidden(String message) {
        CommonException ex = new CommonException(message);
        ex.setHttpStatus(HttpStatus.FORBIDDEN);
        return ex;
    }

    private Set<UUID> likedTargetIds(UUID userId, Set<UUID> postIds) {
        if (userId == null || postIds.isEmpty()) return Set.of();
        return forumReactionRepository
                .findByUser_IdAndTargetTypeAndTargetIdIn(userId, ReactionTargetType.POST, postIds)
                .stream().map(r -> r.getTargetId()).collect(Collectors.toSet());
    }

    private ForumPostResponse toResponse(ForumPost p, boolean myReaction) {
        return toResponse(p, myReaction, forumMediaService.ofPosts(List.of(p.getId())).get(p.getId()));
    }

    /**
     * Bản nhận sẵn danh sách media.
     *
     * Danh sách bài phải nạp media của CẢ TRANG trong một truy vấn; gọi bản trên cho
     * từng bài là 20 bài thành 20 vòng hỏi CSDL.
     */
    private ForumPostResponse toResponse(ForumPost p, boolean myReaction, List<MediaResponse> media) {
        User author = p.getAuthor();
        return ForumPostResponse.builder()
                .id(p.getId())
                .categoryId(p.getCategory().getId())
                .categoryNameVi(p.getCategory().getNameVi())
                .authorId(author.getId())
                .authorName(author.getFullName())
                .authorAvatarUrl(author.getAvatarFile() == null ? null : author.getAvatarFile().getPublicUrl())
                .titleVi(p.getTitleVi())
                .titleMedia(forumMediaService.toResponse(p.getTitleMedia()))
                .bodyMd(p.getBodyMd())
                .media(media == null ? List.of() : media)
                .signId(p.getSign() == null ? null : p.getSign().getId())
                .signWordVi(p.getSign() == null ? null : p.getSign().getWordVi())
                .viewCount(p.getViewCount())
                .commentCount(p.getCommentCount())
                .reactionCount(p.getReactionCount())
                .myReaction(myReaction)
                .isPinned(p.getIsPinned())
                .isLocked(p.getIsLocked())
                .status(p.getStatus())
                .publishedAt(p.getPublishedAt())
                .lastActivityAt(p.getLastActivityAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
