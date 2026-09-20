package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.request.content.SignStepRequest;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.content.SignStepResponse;
import com.sunmoon.backend.service.SignStepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * CMS — soạn hướng dẫn thực hiện ký hiệu theo từng bước.
 *
 * Tách khỏi SignController như video ký hiệu vốn đã có endpoint riêng
 * ({@code /admin/signs/{id}/videos}) — cùng một từ nhưng hai khía cạnh nội
 * dung độc lập nhau.
 */
@Tag(name = "CMS - Hướng dẫn từng bước", description = "Soạn ảnh + mô tả từng bước thực hiện ký hiệu")
@RestController
@RequestMapping("/api/v1/admin/signs/{signId}/steps")
@RequiredArgsConstructor
public class SignStepController {

    private final SignStepService signStepService;

    @Operation(summary = "Danh sách bước theo đúng thứ tự")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR, RoleType.VSL_REVIEWER})
    @GetMapping
    public ResponseEntity<ResponseData<List<SignStepResponse>>> list(@PathVariable UUID signId) {
        return ok(signStepService.list(signId), "SIGN_STEP_LIST_SUCCESS");
    }

    @Operation(summary = "Thêm một bước mới",
            description = "stepOrder là vị trí muốn chèn vào — các bước từ vị trí đó trở đi tự lùi lại một bậc.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping
    public ResponseEntity<ResponseData<SignStepResponse>> create(
            @PathVariable UUID signId, @Valid @RequestBody SignStepRequest request) {
        return ok(signStepService.create(signId, request), "SIGN_STEP_CREATED");
    }

    @Operation(summary = "Sửa một bước")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/{stepId}")
    public ResponseEntity<ResponseData<SignStepResponse>> update(
            @PathVariable UUID signId, @PathVariable UUID stepId,
            @Valid @RequestBody SignStepRequest request) {
        return ok(signStepService.update(signId, stepId, request), "SIGN_STEP_UPDATED");
    }

    @Operation(summary = "Xoá một bước", description = "Các bước sau tự dồn lên để không để lại khoảng trống số thứ tự.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/{stepId}")
    public ResponseEntity<ResponseData<Void>> delete(@PathVariable UUID signId, @PathVariable UUID stepId) {
        signStepService.delete(signId, stepId);
        return ok(null, "SIGN_STEP_DELETED");
    }

    @Operation(summary = "Sắp xếp lại toàn bộ bước",
            description = "Gửi nguyên mảng id theo thứ tự mới, giống cách sắp xếp câu hỏi đề thi và nội dung bài học.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/reorder")
    public ResponseEntity<ResponseData<List<SignStepResponse>>> reorder(
            @PathVariable UUID signId, @Valid @RequestBody ReorderRequest request) {
        return ok(signStepService.reorder(signId, request), "SIGN_STEP_REORDERED");
    }

    @Operation(summary = "Tải ảnh thế tay cho một bước",
            description = "Ảnh cũ (nếu có) bị thay thế — mỗi bước chỉ giữ đúng một ảnh.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping(value = "/{stepId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseData<SignStepResponse>> uploadImage(
            @PathVariable UUID signId, @PathVariable UUID stepId,
            @RequestPart("file") MultipartFile file) {
        return ok(signStepService.uploadImage(signId, stepId, file), "SIGN_STEP_IMAGE_UPLOADED");
    }

    @Operation(summary = "Xoá ảnh của một bước", description = "Giữ lại chữ mô tả — bước không còn ảnh vẫn dùng được, chỉ thiếu minh hoạ.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/{stepId}/image")
    public ResponseEntity<ResponseData<Void>> deleteImage(
            @PathVariable UUID signId, @PathVariable UUID stepId) {
        signStepService.deleteImage(signId, stepId);
        return ok(null, "SIGN_STEP_IMAGE_DELETED");
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
