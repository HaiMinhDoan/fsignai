package com.sunmoon.backend.dto.request.practice;

import com.sunmoon.backend.constant.enums.QuizType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizRequest {

    /** Đề gắn với một bài học cụ thể; để trống nếu là đề ôn theo chủ đề */
    UUID lessonId;

    UUID topicId;

    @NotBlank(message = "Tên đề không được để trống")
    @Size(max = 200)
    String titleVi;

    String descriptionVi;

    @Builder.Default
    QuizType quizType = QuizType.LESSON_QUIZ;

    @Min(value = 0, message = "Điểm đạt phải từ 0 đến 100")
    @Max(value = 100, message = "Điểm đạt phải từ 0 đến 100")
    @Builder.Default
    Integer passScore = 70;

    /** Để trống nghĩa là không giới hạn thời gian */
    @Min(value = 1, message = "Thời gian làm bài phải lớn hơn 0")
    Integer timeLimitSeconds;

    @Builder.Default
    Boolean isPublished = false;
}
