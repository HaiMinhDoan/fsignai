package com.sunmoon.backend.dto.response.analytics;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Một dòng trong file Excel "Thống kê người dùng" — số liệu tính từ trước tới nay */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserStatResponse {

    // ===== Thông tin tài khoản =====
    UUID userId;
    String fullName;
    String email;
    String accountKind;
    String roles;
    Boolean premium;
    OffsetDateTime createdAt;
    OffsetDateTime lastLoginAt;

    // ===== Học tập =====
    long minutesStudied;
    long studyDays;
    String lastStudyDate;
    long lessonsCompleted;
    long signsReviewed;
    long quizzesTaken;
    long aiChecksDone;

    // ===== Khoá học =====
    /** Khoá đã hoàn thành MỌI bài học đang xuất bản */
    long coursesCompleted;
    /** Khoá đã học ít nhất một bài nhưng chưa xong */
    long coursesInProgress;

    // ===== Truy cập =====
    long pageViews;
    long sessions;
    long gameCornerVisits;
    OffsetDateTime lastVisitAt;
}
