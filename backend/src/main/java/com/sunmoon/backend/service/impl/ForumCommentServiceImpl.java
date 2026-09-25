package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.ForumCommentStatus;
import com.sunmoon.backend.constant.enums.ForumPostStatus;
import com.sunmoon.backend.constant.enums.NotificationType;
import com.sunmoon.backend.constant.enums.ReactionTargetType;
import com.sunmoon.backend.dto.request.forum.ForumCommentRequest;
import com.sunmoon.backend.dto.request.forum.ForumModerateRequest;
import com.sunmoon.backend.dto.response.forum.ForumCommentResponse;
import com.sunmoon.backend.dto.response.forum.MediaResponse;
import com.sunmoon.backend.service.ForumMediaService;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.forum.ForumComment;
import com.sunmoon.backend.entity.forum.ForumPost;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.exception.customize.InvalidFieldException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.ForumCommentRepository;
import com.sunmoon.backend.repository.ForumPostRepository;
import com.sunmoon.backend.repository.ForumReactionRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.AuditLogService;
import com.sunmoon.backend.service.ForumCommentService;
import com.sunmoon.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
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
public class ForumCommentServiceImpl implements ForumCommentService {

    private final ForumCommentRepository forumCommentRepository;
    private final ForumMediaService forumMediaService;
    private final ForumPostRepository forumPostRepository;
    private final ForumReactionRepository forumReactionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Override
    public List<ForumCommentResponse> listForPost(UUID postId, UUID currentUserId) {
        List<ForumComment> comments = forumCommentRepository
                .findByPost_IdAndStatusOrderByCreatedAtAsc(postId, ForumCommentStatus.PUBLISHED);

        Set<UUID> likedIds = currentUserId == null ? Set.of() : forumReactionRepository
                .findByUser_IdAndTargetTypeAndTargetIdIn(currentUserId, ReactionTargetType.COMMENT,
                        comments.stream().map(ForumComment::getId).collect(Collectors.toSet()))
                .stream().map(r -> r.getTargetId()).collect(Collectors.toSet());

        Map<UUID, List<MediaResponse>> media = forumMediaService
                .ofComments(comments.stream().map(ForumComment::getId).toList());
        return comments.stream()
                .map(c -> toResponse(c, likedIds.contains(c.getId()), media.get(c.getId())))
                .toList();
    }

    @Override
    @Transactional
    public ForumCommentResponse create(UUID authorId, UUID postId, ForumCommentRequest request) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết"));
        if (post.getStatus() != ForumPostStatus.PUBLISHED) {
            throw new InvalidFieldException("Bài viết này chưa hiện công khai, không bình luận được");
        }
        if (Boolean.TRUE.equals(post.getIsLocked())) {
            throw new InvalidFieldException("Bài viết đã bị khoá bình luận");
        }

        ForumComment parent = null;
        short depth = 0;
        if (request.getParentId() != null) {
            parent = forumCommentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy bình luận gốc"));
            if (!parent.getPost().getId().equals(postId)) {
                throw new InvalidFieldException("Bình luận gốc không thuộc bài viết này");
            }
            if (parent.getDepth() >= 1) {
                throw new InvalidFieldException("Chỉ trả lời được tối đa 2 cấp");
            }
            depth = 1;
        }

        boolean coChu = request.getBodyText() != null && !request.getBodyText().isBlank();
        boolean coHinh = request.getMediaIds() != null && !request.getMediaIds().isEmpty();
        if (!coChu && !coHinh) {
            throw new InvalidFieldException("Bình luận cần ít nhất một dòng chữ, một video ký hiệu hoặc một tấm ảnh");
        }

        ForumComment comment = ForumComment.builder()
                .post(post)
                .author(userRepository.getReferenceById(authorId))
                .parent(parent)
                .depth(depth)
                .bodyText(coChu ? request.getBodyText().trim() : null)
                .status(ForumCommentStatus.PUBLISHED)
                .build();
        ForumComment saved = forumCommentRepository.save(comment);
        forumMediaService.attachToComment(saved, authorId, request.getMediaIds());

        forumPostRepository.bumpCommentCount(postId, 1, OffsetDateTime.now());

        // Bao cho nguoi duoc tra loi (neu la reply) hoac tac gia bai (neu la binh luan goc) -
        // trong app thoi, khong gui email vi day la hoat dong thuong xuyen, de spam
        UUID recipientId = parent != null ? parent.getAuthor().getId() : post.getAuthor().getId();
        if (!recipientId.equals(authorId)) {
            notificationService.notify(recipientId, NotificationType.FORUM_REPLY,
                    parent != null ? "Có người trả lời bình luận của bạn" : "Có bình luận mới trong bài viết của bạn",
                    coChu ? saved.getBodyText() : "Đã gửi một video ký hiệu", null, false);
        }

        return toResponse(saved, false);
    }

    @Override
    @Transactional
    public void delete(UUID authorId, UUID id) {
        ForumComment comment = forumCommentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bình luận"));
        if (!comment.getAuthor().getId().equals(authorId)) {
            CommonException ex = new CommonException("Đây không phải bình luận của bạn");
            ex.setHttpStatus(HttpStatus.FORBIDDEN);
            throw ex;
        }
        if (comment.getStatus() != ForumCommentStatus.REMOVED) {
            comment.setStatus(ForumCommentStatus.REMOVED);
            forumCommentRepository.save(comment);
            forumPostRepository.bumpCommentCount(comment.getPost().getId(), -1, OffsetDateTime.now());
        }
    }

    @Override
    @Transactional
    public ForumCommentResponse moderate(UUID actingAdminId, UUID id, ForumModerateRequest request) {
        if (request.getStatus() == ForumPostStatus.DRAFT) {
            throw new InvalidFieldException("Bình luận không có trạng thái nháp");
        }
        ForumComment comment = forumCommentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bình luận"));

        ForumCommentStatus before = comment.getStatus();
        ForumCommentStatus newStatus = ForumCommentStatus.valueOf(request.getStatus().name());
        boolean wasCounted = before == ForumCommentStatus.PUBLISHED;
        boolean willCount = newStatus == ForumCommentStatus.PUBLISHED;
        comment.setStatus(newStatus);
        ForumComment saved = forumCommentRepository.save(comment);

        if (wasCounted != willCount) {
            forumPostRepository.bumpCommentCount(comment.getPost().getId(), willCount ? 1 : -1, OffsetDateTime.now());
        }

        auditLogService.record(actingAdminId, "forum.comment.moderate", "forum_comments", id,
                Map.of("status", before), Map.of("status", newStatus));

        boolean justTakenDown = before != ForumCommentStatus.HIDDEN && before != ForumCommentStatus.REMOVED
                && (newStatus == ForumCommentStatus.HIDDEN || newStatus == ForumCommentStatus.REMOVED);
        if (justTakenDown) {
            notificationService.notify(saved.getAuthor().getId(), NotificationType.MODERATION_RESULT,
                    "Bình luận của bạn đã bị gỡ",
                    "Bình luận \"" + saved.getBodyText() + "\" đã bị quản trị viên gỡ.",
                    null, true);
        }
        return toResponse(saved, false);
    }

    private ForumCommentResponse toResponse(ForumComment c, boolean myReaction) {
        return toResponse(c, myReaction, forumMediaService.ofComments(List.of(c.getId())).get(c.getId()));
    }

    /** Bản nhận sẵn media - danh sách bình luận nạp media một lần cho cả bài */
    private ForumCommentResponse toResponse(ForumComment c, boolean myReaction, List<MediaResponse> media) {
        User author = c.getAuthor();
        return ForumCommentResponse.builder()
                .id(c.getId())
                .postId(c.getPost().getId())
                .parentId(c.getParent() == null ? null : c.getParent().getId())
                .depth(c.getDepth())
                .authorId(author.getId())
                .authorName(author.getFullName())
                .authorAvatarUrl(author.getAvatarFile() == null ? null : author.getAvatarFile().getPublicUrl())
                .bodyText(c.getBodyText())
                .media(media == null ? List.of() : media)
                .reactionCount(c.getReactionCount())
                .myReaction(myReaction)
                .status(c.getStatus())
                .editedAt(c.getEditedAt())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
