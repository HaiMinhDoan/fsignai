package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.BlogCategory;
import com.sunmoon.backend.entity.notification.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<BlogPost> findBySlugAndIsPublishedTrue(String slug);

    @Query("""
            SELECT b FROM BlogPost b
            WHERE (:category IS NULL OR b.category = :category)
              AND (:isPublished IS NULL OR b.isPublished = :isPublished)
            ORDER BY b.createdAt DESC
            """)
    Page<BlogPost> adminFilter(@Param("category") BlogCategory category,
                                @Param("isPublished") Boolean isPublished,
                                Pageable pageable);

    @Query("""
            SELECT b FROM BlogPost b
            WHERE b.isPublished = true
              AND (:category IS NULL OR b.category = :category)
            ORDER BY b.publishedAt DESC
            """)
    Page<BlogPost> publicFeed(@Param("category") BlogCategory category, Pageable pageable);

    @Modifying
    @Query("UPDATE BlogPost b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") UUID id);
}
