package com.sunmoon.backend.dto.request.forum;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Nội dung bình luận không được để trống")
    String bodyText;
}
