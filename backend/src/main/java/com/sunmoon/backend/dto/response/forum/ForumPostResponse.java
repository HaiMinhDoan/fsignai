package com.sunmoon.backend.dto.response.forum;

import com.sunmoon.backend.constant.enums.ForumPostStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;
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
    /** Ảnh đại diện động của tác giả (video ≤ 5 giây), null nếu chỉ có ảnh tĩnh */
    String authorAvatarVideoUrl;

    String titleVi;
    String bodyMd;

    /** Video ký hiệu thay cho tiêu đề chữ; null nếu tiêu đề chỉ có chữ */
    MediaResponse titleMedia;

    /** Video ký hiệu và ảnh trong nội dung bài */
    List<MediaResponse> media;

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
