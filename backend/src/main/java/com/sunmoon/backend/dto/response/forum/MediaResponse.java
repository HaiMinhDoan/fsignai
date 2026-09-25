package com.sunmoon.backend.dto.response.forum;

import com.sunmoon.backend.constant.enums.MediaKind;
import com.sunmoon.backend.constant.enums.MediaSource;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/** Một video ký hiệu hoặc một tấm ảnh người dùng gắn vào bài viết / bình luận */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaResponse {

    UUID id;
    MediaKind kind;
    MediaSource source;

    /** Địa chỉ phát/xem trực tiếp trong trình duyệt */
    String url;

    /** Ảnh đại diện của video; ảnh thường thì trùng với url */
    String thumbnailUrl;

    Integer durationMs;
    Integer width;
    Integer height;
    Long sizeBytes;
    String mimeType;
}
