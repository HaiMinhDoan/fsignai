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

    /** Đề này có nằm trong một bài học không — bài kiểm tra trong khoá học thuộc gói Free */
    boolean existsByQuizId(UUID quizId);

    void deleteAllByLessonId(UUID lessonId);

    @Query("SELECT COALESCE(MAX(i.displayOrder), -1) + 1 FROM LessonItem i WHERE i.lesson.id = :lessonId")
    int nextDisplayOrder(UUID lessonId);

    /** Các từ vựng đã nằm trong bài học — dùng để loại khỏi danh sách chọn thêm */
    @Query("SELECT i.sign.id FROM LessonItem i WHERE i.lesson.id = :lessonId AND i.sign.id IS NOT NULL")
    List<UUID> findSignIdsByLessonId(UUID lessonId);

    /** Từ đã nằm trong một khoá KHÁC khoá đang soạn */
    @Query("""
            SELECT DISTINCT i.sign.id FROM LessonItem i
            WHERE i.sign.id IS NOT NULL AND i.lesson.course.id <> :courseId
            """)
    List<UUID> findSignIdsUsedOutsideCourse(UUID courseId);

    /**
     * Từ đã nằm ở một bài KHÁC (trong cùng khoá hay khoá khác đều tính).
     *
     * Một từ ĐƯỢC PHÉP nằm ở nhiều bài (2026-10-05, người dùng chốt) — CMS chỉ gắn cảnh báo
     * vàng để người soạn biết là đang lặp lại, không chặn.
     */
    @Query("""
            SELECT DISTINCT i.sign.id FROM LessonItem i
            WHERE i.sign.id IS NOT NULL AND i.lesson.id <> :lessonId
            """)
    List<UUID> findSignIdsUsedOutsideLesson(UUID lessonId);

    /**
     * Bài học khác đang dùng các từ này: [signId, courseId, tên khoá, lessonId, tên bài].
     * Một truy vấn cho cả trang kết quả tìm kiếm, không tra từng từ.
     */
    @Query("""
            SELECT i.sign.id, c.id, c.titleVi, l.id, l.titleVi
              FROM LessonItem i JOIN i.lesson l JOIN l.course c
             WHERE i.sign.id IN :signIds AND l.id <> :lessonId
             ORDER BY c.displayOrder, c.titleVi, l.displayOrder, l.titleVi
            """)
    List<Object[]> findLessonUsages(java.util.Collection<UUID> signIds, UUID lessonId);

    /** Mọi từ đã có mặt trong bất kỳ khoá nào - dùng khi sinh khoá tự động */
    @Query("SELECT DISTINCT i.sign.id FROM LessonItem i WHERE i.sign.id IS NOT NULL")
    List<UUID> findAllUsedSignIds();
}
