package com.sunmoon.backend.service;

import lombok.Builder;
import lombok.Getter;

/**
 * Phần tăng thêm cho nhật ký hoạt động trong ngày — gói lại vì
 * ProgressService.recordActivity() CỘNG DỒN chứ không ghi đè, và một hành
 * động của người học thường chỉ đụng tới một hoặc hai trường trong số này.
 *
 * Ví dụ nộp một bài trắc nghiệm: quizzesTaken(1) và minutesStudied(số phút
 * làm bài); các trường còn lại để mặc định 0.
 */
@Builder
@Getter
public class ActivityDelta {

    @Builder.Default
    int minutesStudied = 0;

    @Builder.Default
    int lessonsCompleted = 0;

    @Builder.Default
    int signsReviewed = 0;

    @Builder.Default
    int quizzesTaken = 0;

    @Builder.Default
    int aiChecksDone = 0;
}
