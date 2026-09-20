package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.ProgressStatus;
import com.sunmoon.backend.entity.progress.UserLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, UUID> {

    Optional<UserLessonProgress> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    /** Dùng cho huy hiệu "lessons_completed" — không phân biệt khoá nào */
    long countByUserIdAndStatus(UUID userId, ProgressStatus status);

    List<UserLessonProgress> findAllByUserIdAndLessonIdIn(UUID userId, List<UUID> lessonIds);

    /** Số bài đã hoàn thành trong MỘT khoá — dùng để dựng lại % tiến độ khoá */
    @Query("""
            SELECT COUNT(p) FROM UserLessonProgress p
             WHERE p.user.id = :userId
               AND p.lesson.course.id = :courseId
               AND p.status = :status
            """)
    long countByUserIdAndCourseIdAndStatus(@Param("userId") UUID userId,
                                           @Param("courseId") UUID courseId,
                                           @Param("status") ProgressStatus status);
}
