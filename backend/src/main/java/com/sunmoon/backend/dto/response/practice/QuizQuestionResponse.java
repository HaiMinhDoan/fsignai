package com.sunmoon.backend.dto.response.practice;

import com.sunmoon.backend.constant.enums.QuestionType;
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
public class QuizQuestionResponse {

    UUID id;
    UUID quizId;
    QuestionType questionType;
    Integer displayOrder;
    Integer points;

    /** Từ vựng là đáp án đúng */
    UUID signId;
    String signWordVi;
    String signGloss;
    String signVideoUrl;
    /** Ảnh đại diện của video — dùng làm poster cho thẻ video */
    String signThumbnailUrl;

    String promptVi;

    List<QuizOptionResponse> options;

    /**
     * Vị trí đáp án đúng.
     *
     * CHỈ trả về ở API quản trị. API làm bài của người học phải bỏ trường này,
     * nếu không người học mở tab Network của trình duyệt là thấy hết đáp án.
     */
    Integer correctOptionIndex;
}
