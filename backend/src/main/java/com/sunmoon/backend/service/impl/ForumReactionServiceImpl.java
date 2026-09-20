package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.ReactionTargetType;
import com.sunmoon.backend.constant.enums.ReactionType;
import com.sunmoon.backend.entity.forum.ForumReaction;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.ForumCommentRepository;
import com.sunmoon.backend.repository.ForumPostRepository;
import com.sunmoon.backend.repository.ForumReactionRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.ForumReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForumReactionServiceImpl implements ForumReactionService {

    private final ForumReactionRepository forumReactionRepository;
    private final ForumPostRepository forumPostRepository;
    private final ForumCommentRepository forumCommentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public boolean toggleLike(UUID userId, ReactionTargetType targetType, UUID targetId) {
        assertTargetExists(targetType, targetId);

        var existing = forumReactionRepository
                .findByUser_IdAndTargetTypeAndTargetId(userId, targetType, targetId);

        if (existing.isPresent()) {
            forumReactionRepository.delete(existing.get());
            bumpCount(targetType, targetId, -1);
            return false;
        }

        ForumReaction reaction = ForumReaction.builder()
                .user(userRepository.getReferenceById(userId))
                .targetType(targetType)
                .targetId(targetId)
                .reaction(ReactionType.LIKE)
                .build();
        forumReactionRepository.save(reaction);
        bumpCount(targetType, targetId, 1);
        return true;
    }

    private void assertTargetExists(ReactionTargetType targetType, UUID targetId) {
        boolean exists = targetType == ReactionTargetType.POST
                ? forumPostRepository.existsById(targetId)
                : forumCommentRepository.existsById(targetId);
        if (!exists) {
            throw new NotFoundException("Không tìm thấy nội dung để thích");
        }
    }

    private void bumpCount(ReactionTargetType targetType, UUID targetId, int delta) {
        if (targetType == ReactionTargetType.POST) {
            forumPostRepository.bumpReactionCount(targetId, delta);
        } else {
            forumCommentRepository.bumpReactionCount(targetId, delta);
        }
    }
}
