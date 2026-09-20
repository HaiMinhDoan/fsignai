package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.forum.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForumCategoryRepository extends JpaRepository<ForumCategory, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<ForumCategory> findBySlug(String slug);

    List<ForumCategory> findAllByOrderByDisplayOrderAsc();

    List<ForumCategory> findAllByIsPublishedTrueOrderByDisplayOrderAsc();
}
