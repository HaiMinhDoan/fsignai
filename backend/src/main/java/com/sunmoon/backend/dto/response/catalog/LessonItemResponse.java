package com.sunmoon.backend.dto.response.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.sunmoon.backend.constant.enums.LessonItemType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonItemResponse {

    UUID id;
    UUID lessonId;
    LessonItemType itemType;
    Integer displayOrder;

    UUID signId;
    /** Kèm sẵn thông tin từ vựng để giao diện không phải gọi thêm API cho từng dòng */
    String signWordVi;
    String signGloss;
    /** Video chính của từ — để biên tập viên xem ngay trong bài học */
    String signPrimaryVideoUrl;
    /** Ảnh đại diện của chính video đó — dùng làm poster, khung video khỏi đen sì */
    String signThumbnailUrl;

    UUID quizId;
    String quizTitleVi;

    JsonNode contentJson;
}
