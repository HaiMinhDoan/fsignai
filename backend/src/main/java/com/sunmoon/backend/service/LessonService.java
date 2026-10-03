package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.catalog.LessonItemRequest;
import com.sunmoon.backend.dto.request.catalog.LessonRequest;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.response.catalog.LessonItemResponse;
import com.sunmoon.backend.dto.response.catalog.LessonResponse;
import com.sunmoon.backend.entity.catalog.Lesson;

import java.util.List;
import java.util.UUID;

public interface LessonService extends BaseService<Lesson, UUID> {

    List<LessonResponse> listByCourse(UUID courseId);

    /** Kèm toàn bộ nội dung bên trong bài học */
    LessonResponse getDetail(UUID lessonId);

    /**
     * Chi tiết bài học cho người học: 404 nếu bài hoặc khoá chứa nó chưa xuất
     * bản, kèm tiến độ của userId nếu có (null = khách chưa đăng nhập).
     */
    LessonResponse getPublishedDetail(UUID lessonId, UUID userId);

    LessonResponse createLesson(UUID courseId, LessonRequest request);

    LessonResponse updateLesson(UUID lessonId, LessonRequest request);

    void deleteLesson(UUID lessonId);

    void reorderLessons(UUID courseId, ReorderRequest request);

    /**
     * Xuất bản / gỡ xuất bản mọi bài của một khoá trong một lần.
     * Bài chưa có nội dung bị bỏ qua khi xuất bản. Trả về {affected, skipped}.
     */
    java.util.Map<String, Integer> publishAllLessons(UUID courseId, boolean published);

    LessonItemResponse addItem(UUID lessonId, LessonItemRequest request);

    /**
     * Thêm nhiều từ vựng vào bài học cùng lúc.
     *
     * Đây là thao tác chính khi soạn bài: biên tập viên lọc từ vựng theo chủ đề
     * rồi tích chọn hàng loạt. Bỏ qua từ đã có trong bài thay vì báo lỗi, vì
     * chọn trùng là chuyện bình thường khi thao tác trên danh sách dài.
     *
     * @return số từ thực sự được thêm
     */
    /**
     * Thêm từ vào bài. Trả về {added, skipped, usedElsewhere}: skipped là từ đã có
     * sẵn trong chính bài này, usedElsewhere là từ đã thuộc một khoá học khác.
     */
    java.util.Map<String, Integer> addSigns(UUID lessonId, List<UUID> signIds);

    void removeItem(UUID lessonId, UUID itemId);

    void reorderItems(UUID lessonId, ReorderRequest request);
}
