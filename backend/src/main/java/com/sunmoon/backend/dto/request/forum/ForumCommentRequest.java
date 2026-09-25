package com.sunmoon.backend.dto.request.forum;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumCommentRequest {

    /** null = bình luận gốc (depth 0); có giá trị = trả lời (depth 1, tối đa 2 cấp) */
    UUID parentId;

    /** Chữ. Được phép trống nếu bình luận có video ký hiệu hoặc ảnh. */
    String bodyText;

    /** Video ký hiệu và ảnh đính kèm, theo đúng thứ tự muốn hiện */
    java.util.List<java.util.UUID> mediaIds;
}
