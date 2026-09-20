package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.catalog.CourseRequest;
import com.sunmoon.backend.dto.request.catalog.GenerateCoursesRequest;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.catalog.CourseResponse;
import com.sunmoon.backend.dto.response.catalog.GenerateCoursesResult;
import com.sunmoon.backend.dto.response.catalog.LessonResponse;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.catalog.Course;
import com.sunmoon.backend.entity.catalog.Lesson;
import com.sunmoon.backend.entity.catalog.LessonItem;
import com.sunmoon.backend.constant.enums.LessonItemType;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.entity.dictionary.Topic;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.mapper.CourseMapper;
import com.sunmoon.backend.mapper.LessonMapper;
import com.sunmoon.backend.repository.*;
import com.sunmoon.backend.service.CourseService;
import com.sunmoon.backend.service.impl.util.VietnameseTextUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CourseServiceImpl extends BaseServiceImpl<Course, UUID> implements CourseService {

    @PersistenceContext
    private EntityManager entityManager;

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonItemRepository lessonItemRepository;
    private final TopicRepository topicRepository;
    private final SignRepository signRepository;
    private final SignTopicRepository signTopicRepository;
    private final FileAttachmentRepository fileRepository;
    private final UserCourseProgressRepository courseProgressRepository;
    private final UserLessonProgressRepository lessonProgressRepository;
    private final CourseMapper courseMapper;
    private final LessonMapper lessonMapper;

    public CourseServiceImpl(CourseRepository courseRepository,
                             LessonRepository lessonRepository,
                             LessonItemRepository lessonItemRepository,
                             TopicRepository topicRepository,
                             SignRepository signRepository,
                             SignTopicRepository signTopicRepository,
                             FileAttachmentRepository fileRepository,
                             UserCourseProgressRepository courseProgressRepository,
                             UserLessonProgressRepository lessonProgressRepository,
                             CourseMapper courseMapper,
                             LessonMapper lessonMapper) {
        // Course không có trường "status". Truyền "level" để changeStatus() của
        // lớp cha không ném lỗi reflection nếu bị gọi nhầm.
        super(courseRepository, "level");
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.lessonItemRepository = lessonItemRepository;
        this.topicRepository = topicRepository;
        this.signRepository = signRepository;
        this.signTopicRepository = signTopicRepository;
        this.fileRepository = fileRepository;
        this.courseProgressRepository = courseProgressRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.courseMapper = courseMapper;
        this.lessonMapper = lessonMapper;
    }

    @Override
    protected EntityManager getEntityManager() {
        return entityManager;
    }

    // ==================== ĐỌC ====================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> search(BaseFilterRequest request) {
        Page<Course> page = filter(request);
        List<CourseResponse> items = page.getContent().stream().map(courseMapper::toResponse).toList();
        fillLessonCounts(items);
        return PageResponse.<CourseResponse>builder()
                .items(items)
                .total(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getDetail(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khoá học: " + id));

        CourseResponse response = courseMapper.toResponse(course);

        List<Lesson> lessons = lessonRepository.findAllByCourseIdOrderByDisplayOrderAsc(id);
        List<LessonResponse> lessonResponses = lessons.stream().map(lessonMapper::toResponse).toList();
        fillItemCounts(lessonResponses);

        response.setLessons(lessonResponses);
        response.setLessonCount((long) lessons.size());
        return response;
    }

    /**
     * Chi tiết khoá học cho NGƯỜI HỌC — khác getDetail() (dùng cho admin sửa
     * bài) ở hai điểm: 404 nếu khoá chưa xuất bản, và danh sách bài học chỉ
     * gồm bài ĐÃ xuất bản — một khoá công khai vẫn có thể chứa bài đang soạn
     * dở, người học không được thấy trước khi admin bấm xuất bản.
     *
     * userId null (khách chưa đăng nhập) thì bỏ qua phần tiến độ.
     */
    @Override
    @Transactional(readOnly = true)
    public CourseResponse getPublishedDetail(UUID id, UUID userId) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khoá học: " + id));
        if (!Boolean.TRUE.equals(course.getIsPublished())) {
            throw new NotFoundException("Không tìm thấy khoá học: " + id);
        }

        CourseResponse response = courseMapper.toResponse(course);

        List<Lesson> lessons = lessonRepository.findAllByCourseIdOrderByDisplayOrderAsc(id).stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsPublished()))
                .toList();
        List<LessonResponse> lessonResponses = new ArrayList<>(
                lessons.stream().map(lessonMapper::toResponse).toList());
        fillItemCounts(lessonResponses);
        response.setLessons(lessonResponses);
        response.setLessonCount((long) lessons.size());

        if (userId != null) {
            courseProgressRepository.findByUserIdAndCourseId(userId, id).ifPresent(p -> {
                response.setMyStatus(p.getStatus());
                response.setMyProgressPercent(p.getProgressPercent());
            });
            if (!lessons.isEmpty()) {
                List<UUID> lessonIds = lessons.stream().map(Lesson::getId).toList();
                Map<UUID, com.sunmoon.backend.entity.progress.UserLessonProgress> byLesson =
                        new HashMap<>();
                lessonProgressRepository.findAllByUserIdAndLessonIdIn(userId, lessonIds)
                        .forEach(p -> byLesson.put(p.getLesson().getId(), p));
                for (LessonResponse lr : lessonResponses) {
                    var p = byLesson.get(lr.getId());
                    if (p != null) {
                        lr.setMyStatus(p.getStatus());
                        lr.setMyProgressPercent(p.getProgressPercent());
                    }
                }
            }
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getOptions() {
        return courseRepository.findAllByOrderByDisplayOrderAscTitleViAsc()
                .stream().map(courseMapper::toResponse).toList();
    }

    // ==================== GHI ====================

    @Override
    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        Course entity = courseMapper.toEntity(request);
        entity.setSlug(resolveSlug(request, null));
        applyRelations(entity, request);
        Course saved = courseRepository.save(entity);

        CourseResponse response = courseMapper.toResponse(saved);
        response.setLessonCount(0L);
        return response;
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(UUID id, CourseRequest request) {
        Course entity = courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khoá học: " + id));

        courseMapper.updateEntity(entity, request);
        entity.setSlug(resolveSlug(request, id));
        applyRelations(entity, request);
        Course saved = courseRepository.save(entity);

        CourseResponse response = courseMapper.toResponse(saved);
        response.setLessonCount(lessonRepository.countByCourseId(id));
        return response;
    }

    @Override
    @Transactional
    public void deleteCourse(UUID id) {
        if (!courseRepository.existsById(id)) {
            throw new NotFoundException("Không tìm thấy khoá học: " + id);
        }
        // lessons và lesson_items đã có ON DELETE CASCADE ở CSDL,
        // nhưng xoá tường minh để Hibernate không giữ lại bản ghi cũ trong phiên
        lessonRepository.findAllByCourseIdOrderByDisplayOrderAsc(id)
                .forEach(lesson -> lessonItemRepository.deleteAllByLessonId(lesson.getId()));
        lessonRepository.deleteAllByCourseId(id);
        courseRepository.deleteById(id);
    }

    @Override
    @Transactional
    public int setPublished(List<UUID> ids, boolean published) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<Course> courses = courseRepository.findAllById(ids);

        if (published) {
            // Không cho xuất bản khoá rỗng: học viên bấm vào sẽ thấy trang trắng
            // và không hiểu mình làm sai gì.
            List<String> empty = courses.stream()
                    .filter(c -> lessonRepository.countByCourseId(c.getId()) == 0)
                    .map(Course::getTitleVi)
                    .toList();
            if (!empty.isEmpty()) {
                throw new ConflictException(
                        "Không thể xuất bản khoá chưa có bài học nào: " + String.join(", ", empty));
            }
        }

        courses.forEach(c -> c.setIsPublished(published));
        courseRepository.saveAll(courses);
        return courses.size();
    }

    @Override
    @Transactional
    public void reorderCourses(ReorderRequest request) {
        List<UUID> ordered = request.getOrderedIds();
        Map<UUID, Course> byId = new HashMap<>();
        courseRepository.findAllById(ordered).forEach(c -> byId.put(c.getId(), c));

        List<Course> toSave = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            Course course = byId.get(ordered.get(i));
            if (course == null) {
                throw new NotFoundException("Không tìm thấy khoá học: " + ordered.get(i));
            }
            course.setDisplayOrder(i);
            toSave.add(course);
        }
        courseRepository.saveAll(toSave);
    }

    // ==================== SINH TỰ ĐỘNG ====================

    /**
     * Gom từ vựng theo chủ đề thành khoá học, cắt đều thành các bài.
     *
     * Ba nguyên tắc:
     *
     * 1. Không phá dữ liệu có sẵn. Chủ đề nào đã có khoá sinh tự động KÈM bài học
     *    thì bỏ qua hẳn — người biên tập có thể đã sửa tay, chạy lại không được
     *    xoá công sức đó.
     *
     * 2. Kết quả ổn định. Từ vựng sắp theo tên đã bỏ dấu nên chạy lại bao nhiêu
     *    lần cũng ra cùng một cách chia bài.
     *
     * 3. Luôn ở trạng thái chưa xuất bản. Máy chia bài chỉ là bản nháp.
     */
    @Override
    @Transactional
    public GenerateCoursesResult generate(GenerateCoursesRequest request) {
        boolean dryRun = Boolean.TRUE.equals(request.getDryRun());
        int perLesson = request.getSignsPerLesson() == null ? 8 : request.getSignsPerLesson();
        boolean onlyPublished = Boolean.TRUE.equals(request.getOnlyPublishedSigns());

        List<Topic> topics = resolveTopics(request.getTopicIds());

        GenerateCoursesResult result = GenerateCoursesResult.builder()
                .dryRun(dryRun)
                .plans(new ArrayList<>())
                .build();

        for (Topic topic : topics) {
            List<UUID> signIds = signTopicRepository.findSignIdsByTopicId(topic.getId(), onlyPublished);

            if (signIds.isEmpty()) {
                result.setTopicsSkipped(result.getTopicsSkipped() + 1);
                result.getPlans().add(GenerateCoursesResult.TopicPlan.builder()
                        .topicNameVi(topic.getNameVi())
                        .signCount(0)
                        .lessonCount(0)
                        .skippedReason(onlyPublished
                                ? "Chưa có từ vựng nào đã xuất bản"
                                : "Chưa có từ vựng nào")
                        .build());
                continue;
            }

            Optional<Course> existing = courseRepository.findByTopicIdAndGeneratedIsTrue(topic.getId());
            if (existing.isPresent() && lessonRepository.countByCourseId(existing.get().getId()) > 0) {
                result.setTopicsSkipped(result.getTopicsSkipped() + 1);
                result.getPlans().add(GenerateCoursesResult.TopicPlan.builder()
                        .topicNameVi(topic.getNameVi())
                        .signCount(signIds.size())
                        .lessonCount(0)
                        .skippedReason("Đã có khoá sinh tự động kèm bài học — không ghi đè")
                        .build());
                continue;
            }

            int lessonCount = (int) Math.ceil(signIds.size() / (double) perLesson);
            result.getPlans().add(GenerateCoursesResult.TopicPlan.builder()
                    .topicNameVi(topic.getNameVi())
                    .signCount(signIds.size())
                    .lessonCount(lessonCount)
                    .build());

            if (dryRun) {
                if (existing.isPresent()) {
                    result.setCoursesUpdated(result.getCoursesUpdated() + 1);
                } else {
                    result.setCoursesCreated(result.getCoursesCreated() + 1);
                }
                result.setLessonsCreated(result.getLessonsCreated() + lessonCount);
                result.setItemsCreated(result.getItemsCreated() + signIds.size());
                continue;
            }

            Course course = existing.orElse(null);
            if (course == null) {
                course = Course.builder()
                        .slug(uniqueSlug(VietnameseTextUtil.toSlug(topic.getNameVi()), null))
                        .titleVi(topic.getNameVi())
                        .descriptionVi(topic.getDescriptionVi())
                        .topic(topic)
                        .displayOrder(topic.getDisplayOrder() == null ? 0 : topic.getDisplayOrder())
                        .generated(true)
                        .isPublished(false)
                        .build();
                course = courseRepository.save(course);
                result.setCoursesCreated(result.getCoursesCreated() + 1);
            } else {
                result.setCoursesUpdated(result.getCoursesUpdated() + 1);
            }

            int created = buildLessons(course, signIds, perLesson);
            result.setLessonsCreated(result.getLessonsCreated() + created);
            result.setItemsCreated(result.getItemsCreated() + signIds.size());
        }

        return result;
    }

    /** Cắt danh sách từ thành từng bài và tạo lesson_items tương ứng */
    private int buildLessons(Course course, List<UUID> signIds, int perLesson) {
        int lessonIndex = 0;
        for (int start = 0; start < signIds.size(); start += perLesson) {
            List<UUID> chunk = signIds.subList(start, Math.min(start + perLesson, signIds.size()));

            Lesson lesson = lessonRepository.save(Lesson.builder()
                    .course(course)
                    .titleVi(course.getTitleVi() + " — phần " + (lessonIndex + 1))
                    .displayOrder(lessonIndex)
                    // Ước lượng: mỗi từ khoảng một phút xem video và tập theo
                    .estimatedMinutes(Math.max(1, chunk.size()))
                    .generated(true)
                    .isPublished(false)
                    .build());

            List<LessonItem> items = new ArrayList<>();
            int order = 0;
            for (UUID signId : chunk) {
                Sign sign = signRepository.getReferenceById(signId);
                items.add(LessonItem.builder()
                        .lesson(lesson)
                        .itemType(LessonItemType.SIGN)
                        .sign(sign)
                        .displayOrder(order++)
                        .build());
            }
            lessonItemRepository.saveAll(items);
            lessonIndex++;
        }
        return lessonIndex;
    }

    private List<Topic> resolveTopics(List<UUID> topicIds) {
        if (topicIds != null && !topicIds.isEmpty()) {
            List<Topic> found = topicRepository.findAllById(topicIds);
            if (found.size() != topicIds.size()) {
                throw new NotFoundException("Có chủ đề không tồn tại trong danh sách đã chọn");
            }
            return found;
        }
        // Không chọn chủ đề nào -> lấy mọi chủ đề đang có từ vựng
        List<UUID> withSigns = signTopicRepository.countSignsPerTopic().stream()
                .map(row -> (UUID) row[0])
                .toList();
        return topicRepository.findAllById(withSigns);
    }

    // ==================== TIỆN ÍCH ====================

    /**
     * Đếm bài học cho cả trang trong MỘT truy vấn.
     * Gọi countByCourseId() trong vòng lặp sẽ thành 20 truy vấn cho 20 dòng.
     */
    private void fillLessonCounts(List<CourseResponse> items) {
        if (items.isEmpty()) return;
        List<UUID> ids = items.stream().map(CourseResponse::getId).toList();
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : courseRepository.countLessonsByCourseIds(ids)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        items.forEach(item -> item.setLessonCount(counts.getOrDefault(item.getId(), 0L)));
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

    private void applyRelations(Course entity, CourseRequest request) {
        if (request.getTopicId() != null) {
            Topic topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề: " + request.getTopicId()));
            entity.setTopic(topic);
        }
        if (request.getCoverFileId() != null) {
            FileAttachment cover = fileRepository.findById(request.getCoverFileId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy tệp ảnh bìa"));
            entity.setCoverFile(cover);
        }
    }

    private String resolveSlug(CourseRequest request, UUID currentId) {
        String base = (request.getSlug() == null || request.getSlug().isBlank())
                ? VietnameseTextUtil.toSlug(request.getTitleVi())
                : VietnameseTextUtil.toSlug(request.getSlug());
        if (base.isEmpty()) {
            throw new ConflictException("Không sinh được slug từ tên khoá học");
        }
        return uniqueSlug(base, currentId);
    }

    /** Thêm hậu tố -2, -3... cho tới khi slug không đụng bản ghi nào khác */
    private String uniqueSlug(String base, UUID currentId) {
        String candidate = base.length() > 120 ? base.substring(0, 120) : base;
        int suffix = 2;
        while (taken(candidate, currentId)) {
            String tail = "-" + suffix++;
            int keep = Math.min(base.length(), 120 - tail.length());
            candidate = base.substring(0, keep) + tail;
        }
        return candidate;
    }

    private boolean taken(String slug, UUID currentId) {
        return currentId == null
                ? courseRepository.existsBySlug(slug)
                : courseRepository.existsBySlugAndIdNot(slug, currentId);
    }
}
