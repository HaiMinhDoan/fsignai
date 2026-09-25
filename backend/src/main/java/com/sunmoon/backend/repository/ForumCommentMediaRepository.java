package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.forum.ForumCommentMedia;
import com.sunmoon.backend.entity.forum.ForumCommentMediaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ForumCommentMediaRepository extends JpaRepository<ForumCommentMedia, ForumCommentMediaId> {

    List<ForumCommentMedia> findAllByCommentIdOrderByDisplayOrderAsc(UUID commentId);

    void deleteAllByCommentId(UUID commentId);

    boolean existsByMediaId(UUID mediaId);
}
