package com.sunmoon.backend.dto.request.practice;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Bài làm người học nộp lên. Chỉ cần gửi những câu ĐÃ trả lời — câu bị bỏ
 * trống (kể cả AI_PERFORM, dạng không có lựa chọn) không cần xuất hiện ở đây,
 * server tự hiểu là chưa trả lời và tính 0 điểm cho câu đó.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitAttemptRequest {

    @NotNull(message = "Phải gửi kèm danh sách câu trả lời (có thể rỗng)")
    List<AnswerSubmission> answers;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AnswerSubmission {
        /** Vị trí câu hỏi trong đề, tính từ 0 — khớp thứ tự trả về lúc bắt đầu lượt thi */
        @NotNull
        Integer questionIndex;

        /** null = bỏ trống câu này */
        Integer selectedOptionIndex;
    }
}
