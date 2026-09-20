package com.sunmoon.backend.dto.request.forum;

import com.sunmoon.backend.constant.enums.ForumPostStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Dùng chung cho duyệt bài lẫn bình luận - ForumCommentStatus là tập con của ForumPostStatus (bỏ DRAFT) */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumModerateRequest {

    @NotNull(message = "Thiếu trạng thái")
    ForumPostStatus status;
}
