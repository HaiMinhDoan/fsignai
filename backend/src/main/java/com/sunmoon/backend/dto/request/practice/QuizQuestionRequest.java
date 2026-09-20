package com.sunmoon.backend.dto.request.practice;

import com.sunmoon.backend.constant.enums.QuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class QuizQuestionRequest {

    @NotNull(message = "Phải chọn dạng câu hỏi")
    QuestionType questionType;

    /** Từ vựng là đáp án đúng của câu hỏi này */
    @NotNull(message = "Phải chọn từ vựng làm đáp án đúng")
    UUID signId;

    /** Để trống thì service tự sinh câu dẫn theo dạng câu hỏi */
    String promptVi;

    /**
     * Danh sách lựa chọn, kể cả đáp án đúng.
     *
     * Bắt buộc với mọi dạng TRỪ AI_PERFORM — dạng đó người học tự làm ký hiệu
     * trước webcam nên không có lựa chọn nào để chọn.
     */
    List<QuizOptionRequest> options;

    /**
     * Vị trí đáp án đúng trong mảng options (đếm từ 0).
     *
     * CSDL có ràng buộc chk_quiz_question_answer: AI_PERFORM bắt buộc để trống,
     * các dạng còn lại bắt buộc có giá trị.
     */
    Integer correctOptionIndex;

    @Min(value = 1, message = "Điểm của câu hỏi phải lớn hơn 0")
    @Builder.Default
    Integer points = 1;

    /** Để trống thì service xếp xuống cuối đề */
    Integer displayOrder;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class QuizOptionRequest {
        /** Lựa chọn trỏ tới một từ vựng — giao diện tự lấy video từ id này */
        UUID signId;
        /** Nhãn hiển thị; để trống thì lấy wordVi của từ */
        String label;
    }
}
