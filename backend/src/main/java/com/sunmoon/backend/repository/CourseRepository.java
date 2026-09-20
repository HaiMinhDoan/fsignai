package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.catalog.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** Dùng khi đổi slug: bỏ qua chính bản ghi đang sửa */
    boolean existsBySlugAndIdNot(String slug, UUID id);

    List<Course> findAllByOrderByDisplayOrderAscTitleViAsc();

    /** Khoá đã sinh tự động từ một chủ đề — dùng để chạy lại việc sinh mà không tạo trùng */
    Optional<Course> findByTopicIdAndGeneratedIsTrue(UUID topicId);

    /**
     * Đếm bài học của nhiều khoá trong MỘT truy vấn.
     *
     * Nếu để mapper gọi lesson.size() cho từng dòng thì danh sách 20 khoá sẽ bắn
     * 20 truy vấn con (bài toán N+1). Ở đây trả về [courseId, count] rồi service
     * tự ghép vào response.
     */
    @Query("""
            SELECT l.course.id, COUNT(l.id)
              FROM Lesson l
             WHERE l.course.id IN :courseIds
             GROUP BY l.course.id
            """)
    List<Object[]> countLessonsByCourseIds(List<UUID> courseIds);
}
