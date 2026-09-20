package com.sunmoon.backend.dto.request.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.sunmoon.backend.constant.enums.LessonItemType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonItemRequest {

    @NotNull(message = "Phải chọn loại nội dung")
    LessonItemType itemType;

    /** Bắt buộc khi itemType = SIGN — CSDL có CHECK chk_lesson_item_sign chặn */
    UUID signId;

    UUID quizId;

    /** Nội dung tự do cho item kiểu TEXT hoặc VIDEO */
    JsonNode contentJson;

    /** Để trống thì service xếp xuống cuối bài học */
    Integer displayOrder;
}
