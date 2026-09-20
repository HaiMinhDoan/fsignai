package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.ai.AiFeedbackRequest;
import com.sunmoon.backend.dto.request.ai.AiVerifyRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.ai.AiCheckHistoryResponse;
import com.sunmoon.backend.dto.response.ai.AiVerifyResponse;
import com.sunmoon.backend.dto.response.ai.ReadinessResponse;
import com.sunmoon.backend.service.AiCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.UUID;

@Tag(name = "Học tập - Chấm điểm AI", description = "Tập ký hiệu trước camera và nhận nhận xét từng phần")
@RestController
@RequestMapping("/api/v1/ai-check")
@RequiredArgsConstructor
public class AiCheckController {

    private final AiCheckService aiCheckService;

    @Operation(summary = "Từ này chấm điểm được chưa?",
            description = "Giao diện ẩn nút \"Tập với camera\" nếu ready = false. Công khai, không cần đăng nhập.")
    @GetMapping("/signs/{signId}/readiness")
    public ResponseEntity<ResponseData<ReadinessResponse>> readiness(@PathVariable UUID signId) {
        return ok(aiCheckService.readiness(signId), "AI_READINESS_SUCCESS");
    }

    @Operation(summary = "Chấm ký hiệu của người học",
            description = "Nhận landmark do trình duyệt trích bằng MediaPipe (video không rời khỏi máy). "
                    + "Server so khớp với mẫu, tra ngưỡng của từ và quyết định đạt/chưa. Tối đa 30 lượt/phút/người.")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/verify")
    public ResponseEntity<ResponseData<AiVerifyResponse>> verify(@Valid @RequestBody AiVerifyRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(aiCheckService.verify(me, request), "AI_VERIFY_SUCCESS");
    }

    @Operation(summary = "Lịch sử chấm điểm của tôi")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/history")
    public ResponseEntity<ResponseData<PageResponse<AiCheckHistoryResponse>>> history(
            @RequestParam(required = false) UUID signId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(aiCheckService.history(me, signId, PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)))),
                "AI_HISTORY_SUCCESS");
    }

    @Operation(summary = "Chấm như vậy có đúng không?",
            description = "👍/👎 cho một kết quả. Gửi lại thì cập nhật. Dữ liệu này dùng để hiệu chỉnh ngưỡng chấm về sau.")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/results/{resultId}/feedback")
    public ResponseEntity<ResponseData<Void>> feedback(@PathVariable UUID resultId,
                                                       @Valid @RequestBody AiFeedbackRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        aiCheckService.feedback(me, resultId, request);
        return ok(null, "AI_FEEDBACK_SAVED");
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
