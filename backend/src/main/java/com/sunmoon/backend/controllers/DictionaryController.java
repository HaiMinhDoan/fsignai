package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.SignLevel;
import com.sunmoon.backend.constant.enums.UnitType;
import com.sunmoon.backend.constant.enums.WordType;
import com.sunmoon.backend.dto.request.content.SignSearchRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.content.SignResponse;
import com.sunmoon.backend.dto.response.content.SignStepResponse;
import com.sunmoon.backend.dto.response.content.SignVideoResponse;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.entity.dictionary.SignStep;
import com.sunmoon.backend.repository.SignStepRepository;
import com.sunmoon.backend.service.SignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Từ điển VSL — CÔNG KHAI, không cần đăng nhập. Chỉ trả từ đã xuất bản: một
 * khách ghé xem thử trang phải tra được từ điển trước khi quyết định đăng ký.
 *
 * Tái dùng thẳng SignService.search()/getDetail()/getVideos() của CMS —
 * KHÔNG viết lại logic tìm kiếm — chỉ ép thêm điều kiện isPublished=true và
 * kiểm lại ở getDetail()/getVideos() vì hai hàm đó vốn cho admin xem cả bản
 * nháp, không tự lọc.
 */
@Tag(name = "Từ điển VSL", description = "Tra cứu từ vựng — công khai")
@RestController
@RequestMapping("/api/v1/dictionary")
@RequiredArgsConstructor
public class DictionaryController {

    private final SignService signService;
    private final SignStepRepository signStepRepository;

    @Operation(summary = "Tìm kiếm từ vựng đã xuất bản")
    @GetMapping("/search")
    public ResponseEntity<ResponseData<PageResponse<SignResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) SignLevel level,
            @RequestParam(required = false) UnitType unitType,
            @RequestParam(required = false) WordType wordType,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "24") Integer size) {
        SignSearchRequest request = SignSearchRequest.builder()
                .keyword(keyword)
                .topicId(topicId)
                .level(level)
                .unitType(unitType)
                .wordType(wordType)
                .isPublished(true)
                .page(page)
                .size(size)
                .build();
        return ok(signService.search(request), "DICTIONARY_SEARCH_SUCCESS");
    }

    @Operation(summary = "Chi tiết một từ đã xuất bản")
    @GetMapping("/signs/{id}")
    public ResponseEntity<ResponseData<SignResponse>> detail(@PathVariable UUID id) {
        SignResponse sign = requirePublished(id);
        return ok(sign, "DICTIONARY_SIGN_DETAIL_SUCCESS");
    }

    @Operation(summary = "Video của một từ, theo vùng miền và góc quay")
    @GetMapping("/signs/{id}/videos")
    public ResponseEntity<ResponseData<List<SignVideoResponse>>> videos(@PathVariable UUID id) {
        requirePublished(id); // 404 nếu từ chưa xuất bản, trước khi lộ danh sách video
        return ok(signService.getVideos(id), "DICTIONARY_SIGN_VIDEOS_SUCCESS");
    }

    @Operation(summary = "Các bước thực hiện một ký hiệu",
            description = "Ảnh thế tay kèm mô tả từng bước. Trả mảng rỗng nếu từ này chưa được soạn hướng dẫn.")
    @GetMapping("/signs/{id}/steps")
    public ResponseEntity<ResponseData<List<SignStepResponse>>> steps(@PathVariable UUID id) {
        requirePublished(id); // 404 trước, giống hai endpoint trên
        List<SignStepResponse> steps = signStepRepository.findBySignIdOrdered(id).stream()
                .map(DictionaryController::toStepResponse)
                .toList();
        return ok(steps, "DICTIONARY_SIGN_STEPS_SUCCESS");
    }

    private static SignStepResponse toStepResponse(SignStep step) {
        return SignStepResponse.builder()
                .id(step.getId())
                .signId(step.getSign().getId())
                .stepOrder(step.getStepOrder())
                .titleVi(step.getTitleVi())
                .descriptionVi(step.getDescriptionVi())
                .bodyFocus(step.getBodyFocus())
                .holdSeconds(step.getHoldSeconds())
                // Ảnh có thể chưa gắn — để null, giao diện tự hiện khung chờ
                .imageUrl(step.getImageFile() == null ? null : step.getImageFile().getPublicUrl())
                .build();
    }

    private SignResponse requirePublished(UUID id) {
        SignResponse sign = signService.getDetail(id);
        if (!Boolean.TRUE.equals(sign.getIsPublished())) {
            // Cùng thông điệp với "không tồn tại" — từ chưa xuất bản coi như
            // không có trong từ điển công khai, không tiết lộ nó đang được soạn
            throw new NotFoundException("Không tìm thấy từ: " + id);
        }
        return sign;
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
