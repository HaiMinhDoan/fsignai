package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.LessonItemType;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.dto.request.catalog.LessonItemRequest;
import com.sunmoon.backend.dto.request.catalog.LessonRequest;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.response.catalog.LessonItemResponse;
import com.sunmoon.backend.dto.response.catalog.LessonResponse;
import com.sunmoon.backend.entity.catalog.Course;
import com.sunmoon.backend.entity.catalog.Lesson;
import com.sunmoon.backend.entity.catalog.LessonItem;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.entity.dictionary.SignVideo;
import com.sunmoon.backend.entity.practice.Quiz;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.mapper.LessonItemMapper;
import com.sunmoon.backend.mapper.LessonMapper;
import com.sunmoon.backend.repository.*;
import com.sunmoon.backend.service.LessonService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class LessonServiceImpl extends BaseServiceImpl<Lesson, UUID> implements LessonService {

    @PersistenceContext
    private EntityManager entityManager;

    private final LessonRepository lessonRepository;
    private final LessonItemRepository lessonItemRepository;
    private final CourseRepository courseRepository;
    private final SignRepository signRepository;
    private final SignVideoRepository signVideoRepository;
    private final UserLessonProgressRepository lessonProgressRepository;
    private final LessonMapper lessonMapper;
    private final LessonItemMapper lessonItemMapper;

    public LessonServiceImpl(LessonRepository lessonRepository,
                             LessonItemRepository lessonItemRepository,
                             CourseRepository courseRepository,
                             SignRepository signRepository,
                             SignVideoRepository signVideoRepository,
                             UserLessonProgressRepository lessonProgressRepository,
                             LessonMapper lessonMapper,
                             LessonItemMapper lessonItemMapper) {
        // Lesson không có trường "status" — xem ghi chú ở CourseServiceImpl
        super(lessonRepository, "titleVi");
        this.lessonRepository = lessonRepository;
        this.lessonItemRepository = lessonItemRepository;
        this.courseRepository = courseRepository;
        this.signRepository = signRepository;
        this.signVideoRepository = signVideoRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.lessonMapper = lessonMapper;
        this.lessonItemMapper = lessonItemMapper;
    }

    @Override
    protected EntityManager getEntityManager() {
        return entityManager;
    }

    // ==================== ĐỌC ====================

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> listByCourse(UUID courseId) {
        List<LessonResponse> lessons = lessonRepository.findAllByCourseIdOrderByDisplayOrderAsc(courseId)
                .stream().map(lessonMapper::toResponse).toList();
        fillItemCounts(lessons);
        return lessons;
    }

    @Override
    @Transactional(readOnly = true)
    public LessonResponse getDetail(UUID lessonId) {
        Lesson lesson = findLesson(lessonId);
        LessonResponse response = lessonMapper.toResponse(lesson);

        List<LessonItem> items = lessonItemRepository.findAllByLessonIdOrderByDisplayOrderAsc(lessonId);
        List<LessonItemResponse> itemResponses =
                new ArrayList<>(items.stream().map(lessonItemMapper::toResponse).toList());
        fillPrimaryVideoUrls(itemResponses);

        response.setItems(itemResponses);
        response.setItemCount((long) itemResponses.size());
        return response;
    }

    /**
     * Chi tiết bài học cho NGƯỜI HỌC — 404 nếu bài hoặc chính khoá chứa nó
     * chưa xuất bản. Một khoá đã xuất bản vẫn có thể có bài đang soạn dở, nên
     * phải kiểm CẢ HAI cấp, không chỉ riêng bài học.
     */
    @Override
    @Transactional(readOnly = true)
    public LessonResponse getPublishedDetail(UUID lessonId, UUID userId) {
        Lesson lesson = findLesson(lessonId);
        if (!Boolean.TRUE.equals(lesson.getIsPublished())
                || !Boolean.TRUE.equals(lesson.getCourse().getIsPublished())) {
            throw new NotFoundException("Không tìm thấy bài học: " + lessonId);
        }

        LessonResponse response = lessonMapper.toResponse(lesson);
        List<LessonItem> items = lessonItemRepository.findAllByLessonIdOrderByDisplayOrderAsc(lessonId);
        List<LessonItemResponse> itemResponses =
                new ArrayList<>(items.stream().map(lessonItemMapper::toResponse).toList());
        fillPrimaryVideoUrls(itemResponses);
        response.setItems(itemResponses);
        response.setItemCount((long) itemResponses.size());

        if (userId != null) {
            lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId).ifPresent(p -> {
                response.setMyStatus(p.getStatus());
                response.setMyProgressPercent(p.getProgressPercent());
                response.setMyLastItemId(p.getLastItem() != null ? p.getLastItem().getId() : null);
            });
        }
        return response;
    }

    // ==================== BÀI HỌC ====================

    @Override
    @Transactional
    public LessonResponse createLesson(UUID courseId, LessonRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khoá học: " + courseId));

        Lesson lesson = lessonMapper.toEntity(request);
        lesson.setCourse(course);
        lesson.setGenerated(false);
        if (request.getDisplayOrder() == null) {
            lesson.setDisplayOrder(lessonRepository.nextDisplayOrder(courseId));
        }

        LessonResponse response = lessonMapper.toResponse(lessonRepository.save(lesson));
        response.setItemCount(0L);
        return response;
    }

    @Override
    @Transactional
    public LessonResponse updateLesson(UUID lessonId, LessonRequest request) {
        Lesson lesson = findLesson(lessonId);

        if (Boolean.TRUE.equals(request.getIsPublished())
                && lessonItemRepository.findAllByLessonIdOrderByDisplayOrderAsc(lessonId).isEmpty()) {
            // Cùng lý do như khoá học rỗng: học viên mở bài trống sẽ tưởng hệ thống lỗi
            throw new ConflictException("Không thể xuất bản bài học chưa có nội dung nào");
        }

        lessonMapper.updateEntity(lesson, request);
        LessonResponse response = lessonMapper.toResponse(lessonRepository.save(lesson));
        response.setItemCount((long) lessonItemRepository
                .findAllByLessonIdOrderByDisplayOrderAsc(lessonId).size());
        return response;
    }

    @Override
    @Transactional
    public void deleteLesson(UUID lessonId) {
        Lesson lesson = findLesson(lessonId);
        lessonItemRepository.deleteAllByLessonId(lessonId);
        lessonRepository.delete(lesson);
    }

    @Override
    @Transactional
    public void reorderLessons(UUID courseId, ReorderRequest request) {
        List<Lesson> lessons = lessonRepository.findAllByCourseIdOrderByDisplayOrderAsc(courseId);
        Map<UUID, Lesson> byId = new HashMap<>();
        lessons.forEach(l -> byId.put(l.getId(), l));

        List<UUID> ordered = request.getOrderedIds();
        if (ordered.size() != lessons.size()) {
            // Thiếu id nghĩa là giao diện và CSDL đang lệch nhau (ai đó vừa thêm
            // hoặc xoá bài ở tab khác). Ghi nửa vời sẽ làm thứ tự loạn hẳn.
            throw new ConflictException(
                    "Danh sách sắp xếp không khớp số bài học hiện có. Hãy tải lại trang rồi thử lại.");
        }

        for (int i = 0; i < ordered.size(); i++) {
            Lesson lesson = byId.get(ordered.get(i));
            if (lesson == null) {
                throw new NotFoundException("Bài học không thuộc khoá này: " + ordered.get(i));
            }
            lesson.setDisplayOrder(i);
        }
        lessonRepository.saveAll(lessons);
    }

    // ==================== NỘI DUNG BÀI HỌC ====================

    @Override
    @Transactional
    public LessonItemResponse addItem(UUID lessonId, LessonItemRequest request) {
        Lesson lesson = findLesson(lessonId);

        LessonItem item = lessonItemMapper.toEntity(request);
        item.setLesson(lesson);

        if (request.getItemType() == LessonItemType.SIGN) {
            if (request.getSignId() == null) {
                throw new ConflictException("Nội dung kiểu từ vựng bắt buộc phải chọn một từ");
            }
            Sign sign = signRepository.findById(request.getSignId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy từ vựng: " + request.getSignId()));
            item.setSign(sign);
        }

        if (request.getQuizId() != null) {
            item.setQuiz(entityManager.getReference(Quiz.class, request.getQuizId()));
        }

        if (request.getDisplayOrder() == null) {
            item.setDisplayOrder(lessonItemRepository.nextDisplayOrder(lessonId));
        }

        LessonItemResponse response = lessonItemMapper.toResponse(lessonItemRepository.save(item));
        fillPrimaryVideoUrls(List.of(response));
        return response;
    }

    @Override
    @Transactional
    public int addSigns(UUID lessonId, List<UUID> signIds) {
        if (signIds == null || signIds.isEmpty()) {
            return 0;
        }
        Lesson lesson = findLesson(lessonId);

        // Bỏ từ đã có sẵn trong bài. Chọn trùng là chuyện thường khi tích chọn
        // hàng loạt trên danh sách dài, không đáng để báo lỗi và bắt làm lại.
        Set<UUID> already = new HashSet<>(lessonItemRepository.findSignIdsByLessonId(lessonId));
        List<UUID> toAdd = signIds.stream().distinct().filter(id -> !already.contains(id)).toList();
        if (toAdd.isEmpty()) {
            return 0;
        }

        List<Sign> signs = signRepository.findAllById(toAdd);
        if (signs.size() != toAdd.size()) {
            throw new NotFoundException("Có từ vựng không tồn tại trong danh sách đã chọn");
        }

        int order = lessonItemRepository.nextDisplayOrder(lessonId);
        List<LessonItem> items = new ArrayList<>();
        for (Sign sign : signs) {
            items.add(LessonItem.builder()
                    .lesson(lesson)
                    .itemType(LessonItemType.SIGN)
                    .sign(sign)
                    .displayOrder(order++)
                    .build());
        }
        lessonItemRepository.saveAll(items);
        return items.size();
    }

    @Override
    @Transactional
    public void removeItem(UUID lessonId, UUID itemId) {
        LessonItem item = lessonItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nội dung: " + itemId));
        if (!item.getLesson().getId().equals(lessonId)) {
            throw new ConflictException("Nội dung này không thuộc bài học đã chỉ định");
        }
        lessonItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void reorderItems(UUID lessonId, ReorderRequest request) {
        List<LessonItem> items = lessonItemRepository.findAllByLessonIdOrderByDisplayOrderAsc(lessonId);
        Map<UUID, LessonItem> byId = new HashMap<>();
        items.forEach(i -> byId.put(i.getId(), i));

        List<UUID> ordered = request.getOrderedIds();
        if (ordered.size() != items.size()) {
            throw new ConflictException(
                    "Danh sách sắp xếp không khớp nội dung hiện có. Hãy tải lại trang rồi thử lại.");
        }

        for (int i = 0; i < ordered.size(); i++) {
            LessonItem item = byId.get(ordered.get(i));
            if (item == null) {
                throw new NotFoundException("Nội dung không thuộc bài học này: " + ordered.get(i));
            }
            item.setDisplayOrder(i);
        }
        lessonItemRepository.saveAll(items);
    }

    // ==================== TIỆN ÍCH ====================

    private Lesson findLesson(UUID lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài học: " + lessonId));
    }

    private void fillItemCounts(List<LessonResponse> lessons) {
        if (lessons.isEmpty()) return;
        List<UUID> ids = lessons.stream().map(LessonResponse::getId).toList();
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : lessonRepository.countItemsByLessonIds(ids)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        lessons.forEach(l -> l.setItemCount(counts.getOrDefault(l.getId(), 0L)));
    }

    /**
     * Gắn URL video chính cho các item kiểu từ vựng, bằng MỘT truy vấn cho cả bài.
     *
     * Mỗi từ có thể có nhiều video chính — một cho mỗi vùng miền. Ưu tiên bản
     * dùng chung (COMMON), không có thì lấy bản đầu tiên, để biên tập viên luôn
     * xem được thay vì thấy ô trống.
     */
    private void fillPrimaryVideoUrls(List<LessonItemResponse> items) {
        List<UUID> signIds = items.stream()
                .map(LessonItemResponse::getSignId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (signIds.isEmpty()) return;

        Map<UUID, SignVideo> best = new HashMap<>();
        for (SignVideo video : signVideoRepository.findPrimaryVideosBySignIds(signIds)) {
            UUID signId = video.getSign().getId();
            SignVideo current = best.get(signId);
            if (current == null || (video.getRegion() == Region.COMMON && current.getRegion() != Region.COMMON)) {
                best.put(signId, video);
            }
        }

        items.forEach(item -> {
            SignVideo video = item.getSignId() == null ? null : best.get(item.getSignId());
            if (video != null && video.getFile() != null) {
                item.setSignPrimaryVideoUrl(video.getFile().getPublicUrl());
                if (video.getThumbnailFile() != null) {
                    item.setSignThumbnailUrl(video.getThumbnailFile().getPublicUrl());
                }
            }
        });
    }
}
