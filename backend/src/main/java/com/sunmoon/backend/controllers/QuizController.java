package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.request.practice.GenerateQuestionsRequest;
import com.sunmoon.backend.dto.request.practice.QuizQuestionRequest;
import com.sunmoon.backend.dto.request.practice.QuizRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.practice.GenerateQuestionsResult;
import com.sunmoon.backend.dto.response.practice.QuizQuestionResponse;
import com.sunmoon.backend.dto.response.practice.QuizResponse;
import com.sunmoon.backend.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "CMS - Ngân hàng câu hỏi", description = "Quản lý đề và câu hỏi luyện tập")
@RestController
@RequestMapping("/api/v1/admin/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "Lọc đề có phân trang")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/filter")
    public ResponseEntity<ResponseData<PageResponse<QuizResponse>>> filter(
            @RequestBody BaseFilterRequest request) {
        return ok(quizService.search(request), "QUIZ_FILTER_SUCCESS");
    }

    @Operation(summary = "Danh sách đề của một bài học")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @GetMapping("/by-lesson/{lessonId}")
    public ResponseEntity<ResponseData<List<QuizResponse>>> byLesson(@PathVariable UUID lessonId) {
        return ok(quizService.listByLesson(lessonId), "QUIZ_LIST_SUCCESS");
    }

    @Operation(summary = "Chi tiết đề kèm toàn bộ câu hỏi và đáp án đúng",
            description = "CHỈ dùng cho màn hình quản trị. API làm bài của người học "
                    + "phải dùng endpoint riêng, không trả về correctOptionIndex.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<QuizResponse>> detail(@PathVariable UUID id) {
        return ok(quizService.getDetail(id), "QUIZ_DETAIL_SUCCESS");
    }

    @Operation(summary = "Tạo đề mới")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping
    public ResponseEntity<ResponseData<QuizResponse>> create(@Valid @RequestBody QuizRequest request) {
        return ok(quizService.createQuiz(request), "QUIZ_CREATED");
    }

    @Operation(summary = "Cập nhật đề")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/{id}")
    public ResponseEntity<ResponseData<QuizResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody QuizRequest request) {
        return ok(quizService.updateQuiz(id, request), "QUIZ_UPDATED");
    }

    @Operation(summary = "Xoá đề cùng toàn bộ câu hỏi")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> delete(@PathVariable UUID id) {
        quizService.deleteQuiz(id);
        return ok(null, "QUIZ_DELETED");
    }

    @Operation(summary = "Xuất bản hoặc gỡ xuất bản hàng loạt")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/publish")
    public ResponseEntity<ResponseData<Map<String, Integer>>> publish(
            @RequestBody PublishRequest request) {
        int affected = quizService.setPublished(request.ids(), request.published());
        return ok(Map.of("affected", affected),
                request.published() ? "QUIZ_PUBLISHED" : "QUIZ_UNPUBLISHED");
    }

    // ==================== CÂU HỎI ====================

    @Operation(summary = "Thêm câu hỏi vào đề")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/{quizId}/questions")
    public ResponseEntity<ResponseData<QuizQuestionResponse>> addQuestion(
            @PathVariable UUID quizId, @Valid @RequestBody QuizQuestionRequest request) {
        return ok(quizService.addQuestion(quizId, request), "QUIZ_QUESTION_ADDED");
    }

    @Operation(summary = "Sửa câu hỏi")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/{quizId}/questions/{questionId}")
    public ResponseEntity<ResponseData<QuizQuestionResponse>> updateQuestion(
            @PathVariable UUID quizId, @PathVariable UUID questionId,
            @Valid @RequestBody QuizQuestionRequest request) {
        return ok(quizService.updateQuestion(quizId, questionId, request), "QUIZ_QUESTION_UPDATED");
    }

    @Operation(summary = "Xoá câu hỏi")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/{quizId}/questions/{questionId}")
    public ResponseEntity<ResponseData<Void>> deleteQuestion(
            @PathVariable UUID quizId, @PathVariable UUID questionId) {
        quizService.deleteQuestion(quizId, questionId);
        return ok(null, "QUIZ_QUESTION_DELETED");
    }

    @Operation(summary = "Sắp xếp lại thứ tự câu hỏi")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/{quizId}/questions/reorder")
    public ResponseEntity<ResponseData<Void>> reorderQuestions(
            @PathVariable UUID quizId, @Valid @RequestBody ReorderRequest request) {
        quizService.reorderQuestions(quizId, request);
        return ok(null, "QUIZ_QUESTION_REORDERED");
    }

    @Operation(summary = "Sinh câu hỏi tự động từ kho từ vựng",
            description = "Mặc định chạy thử (dryRun=true). Chạy thử và ghi thật cho ra "
                    + "cùng một bộ câu hỏi, nên bản xem trước đúng là thứ sẽ được ghi.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/{quizId}/questions/generate")
    public ResponseEntity<ResponseData<GenerateQuestionsResult>> generate(
            @PathVariable UUID quizId, @Valid @RequestBody GenerateQuestionsRequest request) {
        GenerateQuestionsResult result = quizService.generateQuestions(quizId, request);
        return ok(result, Boolean.TRUE.equals(result.getDryRun())
                ? "QUIZ_QUESTION_GENERATE_PREVIEW" : "QUIZ_QUESTION_GENERATED");
    }

    public record PublishRequest(List<UUID> ids, boolean published) {}

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
