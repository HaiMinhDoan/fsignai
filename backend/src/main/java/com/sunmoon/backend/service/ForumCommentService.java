package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.forum.ForumCommentRequest;
import com.sunmoon.backend.dto.request.forum.ForumModerateRequest;
import com.sunmoon.backend.dto.response.forum.ForumCommentResponse;

import java.util.List;
import java.util.UUID;

public interface ForumCommentService {

    List<ForumCommentResponse> listForPost(UUID postId, UUID currentUserId);

    ForumCommentResponse create(UUID authorId, UUID postId, ForumCommentRequest request);

    void delete(UUID authorId, UUID id);

    ForumCommentResponse moderate(UUID actingAdminId, UUID id, ForumModerateRequest request);
}
