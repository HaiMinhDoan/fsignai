package com.sunmoon.backend.dto.request.forum;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumPostRequest {

    @NotNull(message = "Vui lòng chọn chuyên mục")
    UUID categoryId;

    /**
      * Tiêu đề bằng chữ. Được phép để trống: người điếc có thể ra hiệu tiêu đề
      * bằng video (titleMediaId) thay vì gõ tiếng Việt.
      */
    @Size(max = 255)
    String titleVi;

    /** Nội dung bằng chữ. Được phép trống nếu bài đã có video/ảnh. */
    String bodyMd;

    /** Video ký hiệu dùng làm tiêu đề - id trả về từ POST /forum/media */
    UUID titleMediaId;

    /** Video ký hiệu và ảnh của phần nội dung, theo đúng thứ tự muốn hiện */
    List<UUID> mediaIds;

    /** Gắn bài với một từ trong từ điển - không bắt buộc */
    UUID signId;
}
