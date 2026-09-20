package com.sunmoon.backend.dto.response.practice;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả sinh câu hỏi tự động.
 *
 * Trả về cả danh sách câu xem trước chứ không chỉ con số: người soạn cần đọc
 * thử vài câu để biết đáp án nhiễu có hợp lý không, trước khi ghi thật.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateQuestionsResult {

    @Builder.Default
    Boolean dryRun = true;

    @Builder.Default
    int created = 0;

    /** Số từ bị loại vì không đủ điều kiện ra đề */
    @Builder.Default
    int signsSkipped = 0;

    /** Lý do loại, gộp theo nhóm để không in ra hàng nghìn dòng giống nhau */
    @Builder.Default
    List<String> warnings = new ArrayList<>();

    /** Câu hỏi đã sinh — khi chạy thử thì các câu này chưa có id */
    @Builder.Default
    List<QuizQuestionResponse> questions = new ArrayList<>();
}
