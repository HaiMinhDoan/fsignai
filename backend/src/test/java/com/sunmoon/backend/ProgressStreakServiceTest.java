package com.sunmoon.backend;

import com.sunmoon.backend.constant.enums.LessonItemType;
import com.sunmoon.backend.constant.enums.ProgressStatus;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.auth.UserSettings;
import com.sunmoon.backend.entity.catalog.Course;
import com.sunmoon.backend.entity.catalog.Lesson;
import com.sunmoon.backend.entity.catalog.LessonItem;
import com.sunmoon.backend.entity.progress.DailyActivity;
import com.sunmoon.backend.entity.progress.UserCourseProgress;
import com.sunmoon.backend.entity.progress.UserLessonProgress;
import com.sunmoon.backend.entity.progress.UserStreak;
import com.sunmoon.backend.repository.CourseRepository;
import com.sunmoon.backend.repository.DailyActivityRepository;
import com.sunmoon.backend.repository.LessonItemRepository;
import com.sunmoon.backend.repository.LessonRepository;
import com.sunmoon.backend.repository.StreakFreezeRepository;
import com.sunmoon.backend.repository.UserCourseProgressRepository;
import com.sunmoon.backend.repository.UserLessonProgressRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.repository.UserSettingsRepository;
import com.sunmoon.backend.repository.UserStreakRepository;
import com.sunmoon.backend.service.ActivityDelta;
import com.sunmoon.backend.service.ProgressService;
import com.sunmoon.backend.service.StreakService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chạy như một người học thật trong nhiều ngày, trên CSDL thật — không phải
 * test giả với dữ liệu rỗng. Vì chưa có API HTTP cho mục Tiến độ & chuỗi ngày
 * học (đó là việc của mục sau), test này gọi thẳng Service — người gọi thật
 * đầu tiên của ProgressService/StreakService.
 *
 * @Transactional trên lớp test: toàn bộ test chạy trong MỘT giao dịch và
 * Spring tự rollback khi kết thúc — không cần dọn dẹp tay, CSDL không đổi
 * dù test có ghi hàng chục dòng.
 */
@SpringBootTest
@Transactional
class ProgressStreakServiceTest {

    @Autowired
    private ProgressService progressService;
    @Autowired
    private StreakService streakService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSettingsRepository userSettingsRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private LessonItemRepository lessonItemRepository;
    @Autowired
    private DailyActivityRepository dailyActivityRepository;
    @Autowired
    private UserLessonProgressRepository lessonProgressRepository;
    @Autowired
    private UserCourseProgressRepository courseProgressRepository;
    @Autowired
    private UserStreakRepository userStreakRepository;
    @Autowired
    private StreakFreezeRepository streakFreezeRepository;

    @Test
    void simulatesMultiDayLearner() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User user = userRepository.save(User.builder()
                .email("progress-test-" + suffix + "@example.com")
                .fullName("Nguoi kiem thu tien do")
                .build());
        UUID userId = user.getId();

        userSettingsRepository.save(UserSettings.builder()
                .user(user)
                .timezone("Asia/Ho_Chi_Minh")
                .dailyGoalMinutes(10)
                .build());

        Course course = courseRepository.save(Course.builder()
                .slug("khoa-kiem-thu-" + suffix)
                .titleVi("Khoá kiểm thử tiến độ")
                .isPublished(true)
                .build());

        Lesson lesson = lessonRepository.save(Lesson.builder()
                .course(course)
                .titleVi("Bài kiểm thử tiến độ")
                .isPublished(true)
                .build());

        LessonItem item0 = lessonItemRepository.save(LessonItem.builder()
                .lesson(lesson).itemType(LessonItemType.TEXT).displayOrder(0).build());
        LessonItem item1 = lessonItemRepository.save(LessonItem.builder()
                .lesson(lesson).itemType(LessonItemType.TEXT).displayOrder(1).build());
        LessonItem item2 = lessonItemRepository.save(LessonItem.builder()
                .lesson(lesson).itemType(LessonItemType.TEXT).displayOrder(2).build());

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        // ============ NGÀY 1: học bài, làm xong bài học ============
        UserLessonProgress p = progressService.recomputeLessonProgress(userId, lesson.getId(), item0.getId(), 30);
        assertEquals(33, p.getProgressPercent(), "1/3 item -> 33%");
        assertEquals(ProgressStatus.IN_PROGRESS, p.getStatus());

        p = progressService.recomputeLessonProgress(userId, lesson.getId(), item1.getId(), 30);
        assertEquals(67, p.getProgressPercent(), "2/3 item -> 67%");

        p = progressService.recomputeLessonProgress(userId, lesson.getId(), item2.getId(), 30);
        assertEquals(100, p.getProgressPercent(), "3/3 item -> 100%");
        assertEquals(ProgressStatus.COMPLETED, p.getStatus());
        assertTrue(p.getCompletedAt() != null, "COMPLETED phải có completedAt (ràng buộc CSDL)");
        assertEquals(90, p.getTimeSpentSeconds(), "Cộng dồn 30s x 3 lần");

        UserCourseProgress cp = courseProgressRepository.findByUserIdAndCourseId(userId, course.getId())
                .orElseThrow();
        assertEquals(1, cp.getLessonsCompleted());
        assertEquals(1, cp.getLessonsTotal());
        assertEquals(100, cp.getProgressPercent());
        assertEquals(ProgressStatus.COMPLETED, cp.getStatus());

        DailyActivity activity = progressService.recordActivity(userId, ActivityDelta.builder()
                .minutesStudied(12).lessonsCompleted(1).build());
        assertEquals(today, activity.getActivityDate());
        assertEquals(12, activity.getMinutesStudied());
        assertTrue(activity.getGoalMet(), "12 phút >= mục tiêu 10 phút");

        // Cộng dồn thêm trong cùng ngày — không ghi đè
        activity = progressService.recordActivity(userId, ActivityDelta.builder().signsReviewed(5).build());
        assertEquals(12, activity.getMinutesStudied(), "Lần gọi sau không đụng tới minutesStudied");
        assertEquals(5, activity.getSignsReviewed());

        UserStreak streak = streakService.recordStudyDay(userId);
        assertEquals(1, streak.getCurrentStreak());
        assertEquals(1, streak.getLongestStreak());
        assertEquals(today, streak.getStreakStartDate());
        assertEquals(2, streak.getFreezeAvailable(), "Chưa dùng khiên nào");

        // Gọi lại trong cùng ngày phải idempotent
        streak = streakService.recordStudyDay(userId);
        assertEquals(1, streak.getCurrentStreak(), "Gọi lại cùng ngày không tăng thêm");

        // ============ MÔ PHỎNG NGÀY 3: đứt quãng 1 ngày, đủ khiên che ============
        // Không có bộ đồng hồ giả lập nên lùi lastActivityDate về 2 ngày trước,
        // để bước tính "hôm nay - lastActivityDate" ra đúng khoảng cách cần test.
        streak.setLastActivityDate(today.minusDays(2));
        userStreakRepository.save(streak);

        streak = streakService.recordStudyDay(userId);
        assertEquals(2, streak.getCurrentStreak(), "1 ngày đứt quãng, đủ khiên -> chuỗi vẫn nối");
        assertEquals(1, streak.getFreezeAvailable(), "Dùng mất 1 khiên");
        assertEquals(1, streak.getFreezeUsedTotal());
        assertEquals(2, streak.getLongestStreak());
        assertEquals(today, streak.getLastActivityDate());

        List<com.sunmoon.backend.entity.progress.StreakFreeze> freezes =
                streakFreezeRepository.findAllByUserIdOrderByUsedForDateDesc(userId);
        assertEquals(1, freezes.size());
        assertEquals(today.minusDays(1), freezes.get(0).getUsedForDate(), "Khiên che đúng ngày bị lỡ");

        // ============ MÔ PHỎNG NGÀY XA: đứt quãng dài, không đủ khiên -> reset ============
        streak.setLastActivityDate(today.minusDays(10));
        userStreakRepository.save(streak);

        streak = streakService.recordStudyDay(userId);
        assertEquals(1, streak.getCurrentStreak(), "Đứt quãng 9 ngày, chỉ còn 1 khiên -> reset về 1");
        assertEquals(today, streak.getStreakStartDate());
        assertEquals(1, streak.getFreezeAvailable(), "Không đủ khiên che thì không tiêu khiên");
        assertEquals(2, streak.getLongestStreak(), "longestStreak giữ nguyên kỷ lục cũ (ràng buộc >= currentStreak)");
    }
}
