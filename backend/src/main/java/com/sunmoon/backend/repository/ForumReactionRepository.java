package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.ReactionTargetType;
import com.sunmoon.backend.entity.forum.ForumReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ForumReactionRepository extends JpaRepository<ForumReaction, UUID> {

    Optional<ForumReaction> findByUser_IdAndTargetTypeAndTargetId(
            UUID userId, ReactionTargetType targetType, UUID targetId);

    List<ForumReaction> findByUser_IdAndTargetTypeAndTargetIdIn(
            UUID userId, ReactionTargetType targetType, Set<UUID> targetIds);
}
