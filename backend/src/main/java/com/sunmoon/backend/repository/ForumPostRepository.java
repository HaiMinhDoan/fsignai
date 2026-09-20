package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.ForumPostStatus;
import com.sunmoon.backend.entity.forum.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, UUID> {

    @Query("""
            SELECT p FROM ForumPost p
            WHERE p.status = :status
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
            ORDER BY p.isPinned DESC, p.lastActivityAt DESC
            """)
    Page<ForumPost> findFeed(@Param("status") ForumPostStatus status,
                              @Param("categoryId") UUID categoryId,
                              Pageable pageable);

    @Query("""
            SELECT p FROM ForumPost p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
            ORDER BY p.createdAt DESC
            """)
    Page<ForumPost> findAdminFeed(@Param("status") ForumPostStatus status,
                                   @Param("categoryId") UUID categoryId,
                                   Pageable pageable);

    @Modifying
    @Query("UPDATE ForumPost p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE ForumPost p SET p.commentCount = p.commentCount + :delta, p.lastActivityAt = :now WHERE p.id = :id")
    void bumpCommentCount(@Param("id") UUID id, @Param("delta") int delta, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE ForumPost p SET p.reactionCount = p.reactionCount + :delta WHERE p.id = :id")
    void bumpReactionCount(@Param("id") UUID id, @Param("delta") int delta);

    long countByCategory_Id(UUID categoryId);

    long countByCategory_IdAndStatusNot(UUID categoryId, ForumPostStatus excludedStatus);
}
