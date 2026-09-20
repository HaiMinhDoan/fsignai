package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.ForumCommentStatus;
import com.sunmoon.backend.entity.forum.ForumComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumComment, UUID> {

    List<ForumComment> findByPost_IdAndStatusOrderByCreatedAtAsc(UUID postId, ForumCommentStatus status);

    Page<ForumComment> findByStatusOrderByCreatedAtDesc(ForumCommentStatus status, Pageable pageable);

    long countByPost_Id(UUID postId);

    @Modifying
    @Query("UPDATE ForumComment c SET c.reactionCount = c.reactionCount + :delta WHERE c.id = :id")
    void bumpReactionCount(@Param("id") UUID id, @Param("delta") int delta);
}
