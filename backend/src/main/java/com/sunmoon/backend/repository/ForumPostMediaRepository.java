package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.forum.ForumPostMedia;
import com.sunmoon.backend.entity.forum.ForumPostMediaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ForumPostMediaRepository extends JpaRepository<ForumPostMedia, ForumPostMediaId> {

    List<ForumPostMedia> findAllByPostIdOrderByDisplayOrderAsc(UUID postId);

    void deleteAllByPostId(UUID postId);

    boolean existsByMediaId(UUID mediaId);
}
