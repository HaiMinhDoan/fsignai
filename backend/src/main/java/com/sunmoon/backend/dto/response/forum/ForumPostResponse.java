package com.sunmoon.backend.dto.response.forum;

import com.sunmoon.backend.constant.enums.ForumPostStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Dùng cho cả danh sách (bodyMd rỗng khi liệt kê) lẫn chi tiết */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumPostResponse {

    UUID id;
    UUID categoryId;
    String categoryNameVi;
    UUID authorId;
    String authorName;
    String authorAvatarUrl;

    String titleVi;
    String bodyMd;

    UUID signId;
    String signWordVi;

    Integer viewCount;
    Integer commentCount;
    Integer reactionCount;
    Boolean myReaction;

    Boolean isPinned;
    Boolean isLocked;
    ForumPostStatus status;
    OffsetDateTime publishedAt;
    OffsetDateTime lastActivityAt;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
