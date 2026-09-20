package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.practice.SubmitAttemptRequest;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.practice.QuizAttemptResponse;
import com.sunmoon.backend.service.QuizAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.UUID;

@Tag(name = "Học tập - Lượt thi", description = "Bắt đầu, xem lại và nộp bài trắc nghiệm")
@RestController
@RequestMapping("/api/v1/learn/quiz-attempts")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptService attemptService;

    @Operation(summary = "Bắt đầu lượt thi từ một đề soạn tay")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/from-quiz/{quizId}")
    public ResponseEntity<ResponseData<QuizAttemptResponse>> startFromQuiz(
            @PathVariable UUID quizId,
            @RequestParam(defaultValue = "COMMON") Region region) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        return ok(attemptService.startFromQuiz(userId, quizId, region), "QUIZ_ATTEMPT_STARTED");
    }

    @Operation(summary = "Bắt đầu lượt thi từ một cấu hình đề trộn",
            description = "Rút ngẫu nhiên thật — mỗi lần gọi ra một đề khác nhau")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/from-blueprint/{blueprintId}")
    public ResponseEntity<ResponseData<QuizAttemptResponse>> startFromBlueprint(
            @PathVariable UUID blueprintId,
            @RequestParam(defaultValue = "COMMON") Region region) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        return ok(attemptService.startFromBlueprint(userId, blueprintId, region), "QUIZ_ATTEMPT_STARTED");
    }

    @Operation(summary = "Xem lại một lượt thi của chính mình")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/{attemptId}")
    public ResponseEntity<ResponseData<QuizAttemptResponse>> get(@PathVariable UUID attemptId) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        return ok(attemptService.getAttempt(userId, attemptId), "QUIZ_ATTEMPT_DETAIL_SUCCESS");
    }

    @Operation(summary = "Nộp bài",
            description = "Chấm điểm và cập nhật nhật ký hoạt động, chuỗi ngày học, huy hiệu liên quan")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<ResponseData<QuizAttemptResponse>> submit(
            @PathVariable UUID attemptId, @Valid @RequestBody SubmitAttemptRequest request) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        return ok(attemptService.submit(userId, attemptId, request), "QUIZ_ATTEMPT_SUBMITTED");
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
