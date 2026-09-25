package com.sunmoon.backend.dto.response.forum;

import com.sunmoon.backend.constant.enums.ForumCommentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumCommentResponse {

    UUID id;
    UUID postId;
    UUID parentId;
    Short depth;

    UUID authorId;
    String authorName;
    String authorAvatarUrl;

    String bodyText;

    /** Video ký hiệu và ảnh đính kèm bình luận */
    List<MediaResponse> media;
    Integer reactionCount;
    Boolean myReaction;
    ForumCommentStatus status;

    OffsetDateTime editedAt;
    OffsetDateTime createdAt;
}
