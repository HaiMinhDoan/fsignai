package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.catalog.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID>, JpaSpecificationExecutor<Lesson> {

    List<Lesson> findAllByCourseIdOrderByDisplayOrderAsc(UUID courseId);

    long countByCourseId(UUID courseId);

    /** Chỉ đếm bài đã xuất bản — mẫu số tính % tiến độ không nên tính cả bài nháp */
    long countByCourseIdAndIsPublishedTrue(UUID courseId);

    void deleteAllByCourseId(UUID courseId);

    /** Vị trí kế tiếp khi thêm bài học mới vào cuối danh sách */
    @Query("SELECT COALESCE(MAX(l.displayOrder), -1) + 1 FROM Lesson l WHERE l.course.id = :courseId")
    int nextDisplayOrder(UUID courseId);

    /** Đếm item của nhiều bài học trong một truy vấn, tránh N+1 */
    @Query("""
            SELECT i.lesson.id, COUNT(i.id)
              FROM LessonItem i
             WHERE i.lesson.id IN :lessonIds
             GROUP BY i.lesson.id
            """)
    List<Object[]> countItemsByLessonIds(List<UUID> lessonIds);
}
