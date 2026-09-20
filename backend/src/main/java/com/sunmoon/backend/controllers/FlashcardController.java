package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.practice.FlashcardReviewRequest;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.practice.FlashcardResponse;
import com.sunmoon.backend.dto.response.practice.FlashcardReviewResultResponse;
import com.sunmoon.backend.service.FlashcardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Học tập - Flashcard", description = "Ôn từ vựng theo lịch lặp lại ngắt quãng (SM-2)")
@RestController
@RequestMapping("/api/v1/practice/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService flashcardService;

    @Operation(summary = "Thẻ đến hạn ôn (kèm từ mới nếu chưa đủ số lượng)")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping
    public ResponseEntity<ResponseData<List<FlashcardResponse>>> due(
            @RequestParam(required = false) UUID topicId,
            @RequestParam(defaultValue = "20") Integer limit) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        return ok(flashcardService.getDue(userId, topicId, limit), "FLASHCARD_DUE_SUCCESS");
    }

    @Operation(summary = "Ghi nhận kết quả ôn một thẻ",
            description = "Cập nhật lịch ôn tiếp theo theo thuật toán SM-2")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/{signId}/review")
    public ResponseEntity<ResponseData<FlashcardReviewResultResponse>> review(
            @PathVariable UUID signId, @Valid @RequestBody FlashcardReviewRequest request) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        return ok(flashcardService.review(userId, signId, request), "FLASHCARD_REVIEWED");
    }

    private <T> ResponseEntity<ResponseData<T>> ok(T data, String messageCode) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseData.<T>builder()
                .status(HttpStatus.OK.value())
                .messageCode(messageCode)
                .data(data)
                .lang(SecurityContextHolder.getLang())
                .path(SecurityContextHolder.getPath())
                .timestamp(new java.util.Date())
                .build());
    }
}
