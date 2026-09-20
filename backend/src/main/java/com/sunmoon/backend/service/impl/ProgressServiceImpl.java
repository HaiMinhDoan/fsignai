package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.PointSource;
import com.sunmoon.backend.constant.enums.ProgressStatus;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.catalog.Course;
import com.sunmoon.backend.entity.catalog.Lesson;
import com.sunmoon.backend.entity.catalog.LessonItem;
import com.sunmoon.backend.entity.progress.DailyActivity;
import com.sunmoon.backend.entity.progress.UserCourseProgress;
import com.sunmoon.backend.entity.progress.UserLessonProgress;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.CourseRepository;
import com.sunmoon.backend.repository.DailyActivityRepository;
import com.sunmoon.backend.repository.LessonItemRepository;
import com.sunmoon.backend.repository.LessonRepository;
import com.sunmoon.backend.repository.UserCourseProgressRepository;
import com.sunmoon.backend.repository.UserLessonProgressRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.repository.UserSettingsRepository;
import com.sunmoon.backend.service.ActivityDelta;
import com.sunmoon.backend.service.PointService;
import com.sunmoon.backend.service.ProgressService;
import com.sunmoon.backend.service.support.UserTimeZoneResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private static final int DEFAULT_DAILY_GOAL_MINUTES = 10;

    private final DailyActivityRepository dailyActivityRepository;
    private final UserLessonProgressRepository lessonProgressRepository;
    private final UserCourseProgressRepository courseProgressRepository;
    private final LessonRepository lessonRepository;
    private final LessonItemRepository lessonItemRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PointService pointService;
    private final UserTimeZoneResolver timeZoneResolver;

    @Override
    @Transactional
    public DailyActivity recordActivity(UUID userId, ActivityDelta delta) {
        LocalDate today = timeZoneResolver.today(userId);

        DailyActivity activity = dailyActivityRepository.findByUserIdAndActivityDate(userId, today)
                .orElseGet(() -> DailyActivity.builder()
                        .user(getUserRef(userId))
                        .activityDate(today)
                        .build());

        activity.setMinutesStudied(activity.getMinutesStudied() + delta.getMinutesStudied());
        activity.setLessonsCompleted(activity.getLessonsCompleted() + delta.getLessonsCompleted());
        activity.setSignsReviewed(activity.getSignsReviewed() + delta.getSignsReviewed());
        activity.setQuizzesTaken(activity.getQuizzesTaken() + delta.getQuizzesTaken());
        activity.setAiChecksDone(activity.getAiChecksDone() + delta.getAiChecksDone());

        int goalMinutes = userSettingsRepository.findByUserId(userId)
                .map(s -> s.getDailyGoalMinutes())
                .orElse(DEFAULT_DAILY_GOAL_MINUTES);
        activity.setGoalMet(activity.getMinutesStudied() >= goalMinutes);

        return dailyActivityRepository.save(activity);
    }

    @Override
    @Transactional
    public UserLessonProgress recomputeLessonProgress(UUID userId, UUID lessonId, UUID lastItemId,
                                                       int timeSpentSecondsDelta) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài học: " + lessonId));

        List<LessonItem> items = lessonItemRepository.findAllByLessonIdOrderByDisplayOrderAsc(lessonId);
        int total = items.size();

        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(lastItemId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new ConflictException("Nội dung này không thuộc bài học đã chỉ định");
        }

        int percent = total == 0 ? 0 : (int) Math.round((index + 1) * 100.0 / total);
        boolean nowCompleted = percent >= 100;

        UserLessonProgress progress = lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> UserLessonProgress.builder()
                        .user(getUserRef(userId))
                        .lesson(lesson)
                        .startedAt(OffsetDateTime.now())
                        .build());

        boolean wasCompleted = progress.getStatus() == ProgressStatus.COMPLETED;
        progress.setLastItem(items.get(index));
        progress.setProgressPercent(percent);
        progress.setTimeSpentSeconds(progress.getTimeSpentSeconds() + Math.max(0, timeSpentSecondsDelta));
        progress.setStatus(nowCompleted ? ProgressStatus.COMPLETED
                : percent > 0 ? ProgressStatus.IN_PROGRESS : ProgressStatus.NOT_STARTED);
        // Ràng buộc chk_lesson_completed_has_timestamp: COMPLETED bắt buộc phải có completedAt
        if (nowCompleted && !wasCompleted) {
            progress.setCompletedAt(OffsetDateTime.now());
            pointService.award(userId, PointSource.LESSON, lessonId, 20, "Hoàn thành bài học");
        }

        UserLessonProgress saved = lessonProgressRepository.save(progress);

        // Tiến độ khoá học phải luôn khớp với tiến độ bài học vừa đổi — dựng lại
        // ngay ở đây, không để nơi gọi phải nhớ gọi thêm một hàm nữa.
        recomputeCourseProgress(userId, lesson.getCourse().getId());

        return saved;
    }

    @Override
    @Transactional
    public UserCourseProgress recomputeCourseProgress(UUID userId, UUID courseId) {
        long total = lessonRepository.countByCourseIdAndIsPublishedTrue(courseId);
        long completed = lessonProgressRepository
                .countByUserIdAndCourseIdAndStatus(userId, courseId, ProgressStatus.COMPLETED);
        // Học xong một bài rồi admin gỡ xuất bản bài đó vẫn được tính hoàn thành,
        // nên completed có thể vượt total trong trường hợp hiếm này — chặn lại
        // để % không bao giờ vượt 100.
        if (completed > total) completed = total;

        int percent = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
        ProgressStatus status = total > 0 && completed >= total ? ProgressStatus.COMPLETED
                : completed > 0 ? ProgressStatus.IN_PROGRESS : ProgressStatus.NOT_STARTED;

        UserCourseProgress progress = courseProgressRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseGet(() -> UserCourseProgress.builder()
                        .user(getUserRef(userId))
                        .course(getCourseRef(courseId))
                        .build());

        boolean wasCompleted = progress.getStatus() == ProgressStatus.COMPLETED;
        progress.setLessonsCompleted((int) completed);
        progress.setLessonsTotal((int) total);
        progress.setProgressPercent(percent);
        progress.setStatus(status);
        if (status == ProgressStatus.COMPLETED && !wasCompleted) {
            progress.setCompletedAt(OffsetDateTime.now());
        }

        return courseProgressRepository.save(progress);
    }

    private User getUserRef(UUID userId) {
        return userRepository.getReferenceById(userId);
    }

    private Course getCourseRef(UUID courseId) {
        return courseRepository.getReferenceById(courseId);
    }
}
