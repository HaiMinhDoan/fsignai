package com.sunmoon.backend.dto.response.content;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

/** Một bước trong hướng dẫn làm ký hiệu — ảnh thế tay kèm mô tả */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignStepResponse {

    UUID id;
    UUID signId;
    Integer stepOrder;
    String titleVi;
    String descriptionVi;
    /** Bộ phận cần chú ý ở bước này: LEFT_HAND, RIGHT_HAND, BOTH_HANDS, FACE... */
    String bodyFocus;
    BigDecimal holdSeconds;
    /** URL ảnh trên MinIO; null khi biên tập viên mới soạn chữ, chưa gắn ảnh */
    String imageUrl;
}
