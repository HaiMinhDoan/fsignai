package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.catalog.LessonItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LessonItemRepository
        extends JpaRepository<LessonItem, UUID>, JpaSpecificationExecutor<LessonItem> {

    List<LessonItem> findAllByLessonIdOrderByDisplayOrderAsc(UUID lessonId);

    long countByLessonId(UUID lessonId);

    boolean existsByLessonIdAndSignId(UUID lessonId, UUID signId);

    void deleteAllByLessonId(UUID lessonId);

    @Query("SELECT COALESCE(MAX(i.displayOrder), -1) + 1 FROM LessonItem i WHERE i.lesson.id = :lessonId")
    int nextDisplayOrder(UUID lessonId);

    /** Các từ vựng đã nằm trong bài học — dùng để loại khỏi danh sách chọn thêm */
    @Query("SELECT i.sign.id FROM LessonItem i WHERE i.lesson.id = :lessonId AND i.sign.id IS NOT NULL")
    List<UUID> findSignIdsByLessonId(UUID lessonId);

    /**
     * Từ đã nằm trong một khoá KHÁC khoá đang soạn.
     *
     * Quy ước: mỗi từ chỉ thuộc đúng một khoá học, để người học đi hết các khoá
     * không gặp lại cùng một từ ở khoá sau như thể đó là bài mới.
     */
    @Query("""
            SELECT DISTINCT i.sign.id FROM LessonItem i
            WHERE i.sign.id IS NOT NULL AND i.lesson.course.id <> :courseId
            """)
    List<UUID> findSignIdsUsedOutsideCourse(UUID courseId);

    /** Mọi từ đã có mặt trong bất kỳ khoá nào - dùng khi sinh khoá tự động */
    @Query("SELECT DISTINCT i.sign.id FROM LessonItem i WHERE i.sign.id IS NOT NULL")
    List<UUID> findAllUsedSignIds();
}
