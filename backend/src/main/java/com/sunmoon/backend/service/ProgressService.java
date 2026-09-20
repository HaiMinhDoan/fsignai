package com.sunmoon.backend.service;

import com.sunmoon.backend.entity.progress.DailyActivity;
import com.sunmoon.backend.entity.progress.UserCourseProgress;
import com.sunmoon.backend.entity.progress.UserLessonProgress;

import java.util.UUID;

/**
 * Nhật ký hoạt động trong ngày và tiến độ bài học / khoá học.
 *
 * KHÔNG tự cập nhật chuỗi ngày học (xem StreakService) — hai việc này tách
 * riêng để mỗi service dễ kiểm chứng độc lập. Nơi gọi (API làm bài, học bài
 * của người học — sẽ xây ở mục tiến độ & luyện tập) tự quyết định khi nào một
 * hành động vừa tính là "có học hôm nay" và gọi StreakService riêng.
 */
public interface ProgressService {

    /**
     * Cộng dồn hoạt động vào bản ghi của NGÀY HÔM NAY theo múi giờ người học.
     * Tạo mới nếu ngày đó chưa có bản ghi. Có thể gọi nhiều lần trong ngày —
     * mỗi lần cộng thêm, không ghi đè.
     */
    DailyActivity recordActivity(UUID userId, ActivityDelta delta);

    /**
     * Dựng lại % tiến độ một bài học dựa trên vị trí của item vừa xem trong
     * danh sách item của bài (item xếp theo displayOrder). Luôn dựng lại tiến
     * độ khoá học chứa bài này ngay sau đó, để hai con số không bao giờ lệch
     * nhau — không phụ thuộc nơi gọi phải nhớ gọi thêm một hàm nữa.
     */
    UserLessonProgress recomputeLessonProgress(UUID userId, UUID lessonId, UUID lastItemId,
                                               int timeSpentSecondsDelta);

    /** Dựng lại % tiến độ một khoá học từ số bài đã hoàn thành / tổng số bài đã xuất bản */
    UserCourseProgress recomputeCourseProgress(UUID userId, UUID courseId);
}
