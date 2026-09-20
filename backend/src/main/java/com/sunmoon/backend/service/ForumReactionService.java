package com.sunmoon.backend.service;

import com.sunmoon.backend.constant.enums.ReactionTargetType;

import java.util.UUID;

public interface ForumReactionService {

    /** Bật lại tắt LIKE. Trả về true nếu sau lệnh này là ĐANG thích, false nếu vừa bỏ thích */
    boolean toggleLike(UUID userId, ReactionTargetType targetType, UUID targetId);
}
