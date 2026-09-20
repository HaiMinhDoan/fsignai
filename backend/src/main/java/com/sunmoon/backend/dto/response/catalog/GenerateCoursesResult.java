package com.sunmoon.backend.dto.response.catalog;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả của việc sinh khoá học tự động.
 *
 * Luôn liệt kê chi tiết từng chủ đề chứ không chỉ trả về con số tổng: người
 * biên tập cần thấy chủ đề nào bị bỏ qua và vì sao, trước khi chạy thật.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateCoursesResult {

    @Builder.Default
    Boolean dryRun = true;

    @Builder.Default
    int coursesCreated = 0;

    @Builder.Default
    int coursesUpdated = 0;

    @Builder.Default
    int lessonsCreated = 0;

    @Builder.Default
    int itemsCreated = 0;

    @Builder.Default
    int topicsSkipped = 0;

    @Builder.Default
    List<TopicPlan> plans = new ArrayList<>();

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TopicPlan {
        String topicNameVi;
        int signCount;
        int lessonCount;
        /** Có giá trị nghĩa là chủ đề này bị bỏ qua, và đây là lý do */
        String skippedReason;
    }
}
