package com.sunmoon.backend.dto.response.practice;

import com.sunmoon.backend.constant.enums.QuizType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizResponse {

    UUID id;

    UUID lessonId;
    String lessonTitleVi;
    UUID courseId;
    String courseTitleVi;

    UUID topicId;
    String topicNameVi;

    String titleVi;
    String descriptionVi;
    QuizType quizType;
    Integer passScore;
    Integer timeLimitSeconds;
    Boolean isPublished;

    /** Service điền, gom trong một truy vấn để tránh N+1 */
    Long questionCount;

    /** Chỉ có ở API chi tiết */
    List<QuizQuestionResponse> questions;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
