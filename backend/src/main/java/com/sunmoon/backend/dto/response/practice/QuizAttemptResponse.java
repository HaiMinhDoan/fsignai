package com.sunmoon.backend.dto.response.practice;

import com.sunmoon.backend.constant.enums.QuizAttemptStatus;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.dto.response.progress.AchievementResponse;
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
public class QuizAttemptResponse {

    UUID id;

    UUID quizId;
    String quizTitleVi;

    UUID blueprintId;
    String blueprintTitleVi;

    Region region;
    QuizAttemptStatus status;

    Integer score;
    Integer maxScore;
    Boolean passed;
    /** Ngưỡng % để đạt, lấy từ đề/cấu hình nguồn — để giao diện hiện "cần X% để đạt" */
    Integer passScoreRequired;

    OffsetDateTime startedAt;
    OffsetDateTime submittedAt;
    Integer durationSeconds;

    /**
     * Danh sách câu hỏi của LƯỢT THI NÀY — cố định ngay từ lúc bắt đầu, không
     * đổi dù đề nguồn có sửa sau đó (xem ghi chú ở QuizAttemptServiceImpl về
     * giới hạn khi thi từ một Quiz soạn tay).
     *
     * correctOptionIndex CHỈ có khi status = SUBMITTED — đang làm bài mà lộ
     * đáp án qua tab Network của trình duyệt thì coi như hỏng bài kiểm tra.
     */
    List<QuizQuestionResponse> questions;

    /** Câu trả lời đã chọn cho từng câu — song song với questions, null = bỏ trống. Chỉ có khi đã nộp */
    List<Integer> selectedOptionIndexes;

    /** Huy hiệu vừa đạt được NGAY sau lượt thi này — rỗng nếu không có gì mới */
    List<AchievementResponse> newAchievements;
}
