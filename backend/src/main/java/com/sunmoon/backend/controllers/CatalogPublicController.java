package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.FilterLogicType;
import com.sunmoon.backend.constant.enums.FilterOperation;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.constant.enums.SignLevel;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.AuthInfo;
import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.FilterCriteria;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.catalog.CourseResponse;
import com.sunmoon.backend.dto.response.catalog.LessonResponse;
import com.sunmoon.backend.dto.response.catalog.WordPackResponse;
import com.sunmoon.backend.dto.response.content.TopicResponse;
import com.sunmoon.backend.dto.response.practice.QuizBlueprintResponse;
import com.sunmoon.backend.dto.response.practice.QuizResponse;
import com.sunmoon.backend.service.CourseService;
import com.sunmoon.backend.service.LessonService;
import com.sunmoon.backend.service.ProgressService;
import com.sunmoon.backend.service.QuizBlueprintService;
import com.sunmoon.backend.service.QuizService;
import com.sunmoon.backend.service.TopicService;
import com.sunmoon.backend.service.WordPackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Chủ đề, khoá học, bài học — phần duyệt nội dung công khai của web học tập.
 * Xem chi tiết bài học và ghi nhận tiến độ thì bắt buộc đăng nhập (tiến độ là
 * dữ liệu riêng của từng người học); duyệt danh mục thì không.
 */
@Tag(name = "Danh mục học tập", description = "Chủ đề, khoá học, bài học")
@RestController
@RequiredArgsConstructor
public class CatalogPublicController {

    private final TopicService topicService;
    private final CourseService courseService;
    private final LessonService lessonService;
    private final ProgressService progressService;
    private final WordPackService wordPackService;
    private final QuizService quizService;
    private final QuizBlueprintService quizBlueprintService;

    @Operation(summary = "Cây chủ đề đã xuất bản")
    @GetMapping("/api/v1/topics")
    public ResponseEntity<ResponseData<List<TopicResponse>>> topics() {
        List<TopicResponse> published = filterPublished(topicService.getTree());
        return ok(published, "TOPIC_TREE_SUCCESS");
    }

    @Operation(summary = "Danh sách khoá học đã xuất bản")
    @GetMapping("/api/v1/courses")
    public ResponseEntity<ResponseData<PageResponse<CourseResponse>>> courses(
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) SignLevel level,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        List<FilterCriteria> filters = new ArrayList<>();
        filters.add(eq("isPublished", true));
        if (topicId != null) filters.add(eq("topic.id", topicId));
        if (level != null) filters.add(eq("level", level));

        PageResponse<CourseResponse> result = courseService.search(BaseFilterRequest.builder()
                .filters(filters).page(page).size(size).build());
        return ok(result, "COURSE_LIST_SUCCESS");
    }

    @Operation(summary = "Chi tiết khoá học đã xuất bản",
            description = "Kèm tiến độ của người học nếu đã đăng nhập")
    @GetMapping("/api/v1/courses/{id}")
    public ResponseEntity<ResponseData<CourseResponse>> courseDetail(@PathVariable UUID id) {
        return ok(courseService.getPublishedDetail(id, currentUserIdOrNull()), "COURSE_DETAIL_SUCCESS");
    }

    @Operation(summary = "Chi tiết bài học",
            description = "Phải đăng nhập — bài học gắn với tiến độ riêng của từng người")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/api/v1/lessons/{id}")
    public ResponseEntity<ResponseData<LessonResponse>> lessonDetail(@PathVariable UUID id) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        return ok(lessonService.getPublishedDetail(id, userId), "LESSON_DETAIL_SUCCESS");
    }

    @Operation(summary = "Ghi nhận đã xem một nội dung trong bài học",
            description = "Cập nhật % tiến độ bài học (và khoá học chứa nó) theo vị trí nội dung vừa xem")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/api/v1/lessons/{id}/progress")
    public ResponseEntity<ResponseData<LessonResponse>> trackProgress(
            @PathVariable UUID id, @RequestBody TrackProgressRequest request) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        progressService.recomputeLessonProgress(userId, id, request.itemId(),
                request.timeSpentSeconds() == null ? 0 : request.timeSpentSeconds());
        // Trả lại chi tiết bài học đã cập nhật để giao diện khỏi gọi thêm một API
        return ok(lessonService.getPublishedDetail(id, userId), "LESSON_PROGRESS_UPDATED");
    }

    public record TrackProgressRequest(@NotNull UUID itemId, Integer timeSpentSeconds) {}

    // ==================== Gói từ (bản đồ đảo) ====================

    @Operation(summary = "Danh sách gói từ cho bản đồ đảo",
            description = "Kèm trạng thái mở khoá và tiến độ nếu đã đăng nhập; khách chưa đăng nhập "
                    + "thấy mọi gói có điều kiện mở khoá đều đang khoá, vì chưa biết đã hoàn thành gói nào.")
    @GetMapping("/api/v1/word-packs")
    public ResponseEntity<ResponseData<List<WordPackResponse>>> wordPacks() {
        return ok(wordPackService.listPublishedForLearner(currentUserIdOrNull()), "WORD_PACK_LIST_SUCCESS");
    }

    @Operation(summary = "Chi tiết một gói từ đã xuất bản",
            description = "Xem trước được dù gói đang khoá — chỉ ghi tiến độ mới bị chặn lúc khoá")
    @GetMapping("/api/v1/word-packs/{id}")
    public ResponseEntity<ResponseData<WordPackResponse>> wordPackDetail(@PathVariable UUID id) {
        return ok(wordPackService.getPublishedDetail(id, currentUserIdOrNull()), "WORD_PACK_DETAIL_SUCCESS");
    }

    @Operation(summary = "Bắt đầu học một gói từ",
            description = "Tạo bản ghi tiến độ nếu chưa có (chụp lại số từ hiện tại làm mốc); "
                    + "gọi lại nhiều lần không tạo trùng. Từ chối nếu gói đang khoá.")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/api/v1/word-packs/{id}/start")
    public ResponseEntity<ResponseData<WordPackResponse>> startWordPack(@PathVariable UUID id) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        return ok(wordPackService.startPack(id, userId), "WORD_PACK_STARTED");
    }

    @Operation(summary = "Cập nhật số từ đã học trong gói",
            description = "Chỉ tăng, không lùi — học lại các từ đã qua không làm tụt tiến độ. "
                    + "Tự chuyển sang hoàn thành khi đủ số từ.")
    @RequireAuth(roles = {RoleType.ALL})
    @PutMapping("/api/v1/word-packs/{id}/progress")
    public ResponseEntity<ResponseData<WordPackResponse>> updateWordPackProgress(
            @PathVariable UUID id, @RequestBody UpdatePackProgressRequest request) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        return ok(wordPackService.updateProgress(id, userId, request.itemsCompleted()),
                "WORD_PACK_PROGRESS_UPDATED");
    }

    public record UpdatePackProgressRequest(@NotNull Integer itemsCompleted) {}

    // ==================== Bài kiểm tra ====================

    @Operation(summary = "Danh sách đề đã xuất bản (đề cố định do admin soạn tay)",
            description = "Không kèm câu hỏi - chỉ để duyệt danh sách rồi bắt đầu lượt thi riêng")
    @GetMapping("/api/v1/quizzes")
    public ResponseEntity<ResponseData<PageResponse<QuizResponse>>> quizzes(
            @RequestParam(required = false) UUID topicId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        List<FilterCriteria> filters = new ArrayList<>();
        filters.add(eq("isPublished", true));
        if (topicId != null) filters.add(eq("topic.id", topicId));

        PageResponse<QuizResponse> result = quizService.search(BaseFilterRequest.builder()
                .filters(filters).page(page).size(size).build());
        return ok(result, "QUIZ_LIST_SUCCESS");
    }

    @Operation(summary = "Danh sách cấu hình đề trộn đang bật",
            description = "Mỗi lần bắt đầu lượt thi từ một cấu hình sẽ rút ngẫu nhiên một đề khác nhau")
    @GetMapping("/api/v1/quiz-blueprints")
    public ResponseEntity<ResponseData<PageResponse<QuizBlueprintResponse>>> quizBlueprints(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        PageResponse<QuizBlueprintResponse> result = quizBlueprintService.search(BaseFilterRequest.builder()
                .filters(List.of(eq("isActive", true))).page(page).size(size).build());
        return ok(result, "QUIZ_BLUEPRINT_LIST_SUCCESS");
    }

    // ==================== TIỆN ÍCH ====================

    private UUID currentUserIdOrNull() {
        AuthInfo auth = SecurityContextHolder.getAuthInfo();
        return auth == null ? null : auth.getId();
    }

    private FilterCriteria eq(String field, Object value) {
        return FilterCriteria.builder()
                .fieldName(field)
                .operation(FilterOperation.EQUALS)
                .value(value)
                .logicType(FilterLogicType.AND)
                .build();
    }

    /** Giữ lại chủ đề đã xuất bản; con của một chủ đề cũng được lọc lại đệ quy */
    private List<TopicResponse> filterPublished(List<TopicResponse> nodes) {
        List<TopicResponse> result = new ArrayList<>();
        for (TopicResponse node : nodes) {
            if (!Boolean.TRUE.equals(node.getIsPublished())) continue;
            if (node.getChildren() != null) {
                node.setChildren(filterPublished(node.getChildren()));
            }
            result.add(node);
        }
        return result;
    }

    private <T> ResponseEntity<ResponseData<T>> ok(T data, String messageCode) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseData.<T>builder()
                .status(HttpStatus.OK.value())
                .messageCode(messageCode)
                .data(data)
                .lang(SecurityContextHolder.getLang())
                .path(SecurityContextHolder.getPath())
                .timestamp(new Date())
                .build());
    }
}
