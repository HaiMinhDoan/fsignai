package com.sunmoon.backend.dto.response.catalog;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/** Một từ vựng trong gói, kèm sẵn video/ảnh để màn học không phải gọi thêm API cho từng từ */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WordPackItemResponse {

    UUID id;
    Integer displayOrder;

    UUID signId;
    String signWordVi;
    String signGloss;
    String signPrimaryVideoUrl;
    String signThumbnailUrl;
}
