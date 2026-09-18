package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.constant.enums.ViewAngle;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.content.SignImportRequest;
import com.sunmoon.backend.dto.request.content.SignRequest;
import com.sunmoon.backend.dto.request.content.SignSearchRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.content.SignImportResultResponse;
import com.sunmoon.backend.dto.response.content.SignResponse;
import com.sunmoon.backend.dto.response.content.SignVideoResponse;
import com.sunmoon.backend.service.SignService;
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
import java.util.Map;
import java.util.UUID;

@Tag(name = "CMS - Từ vựng", description = "Quản lý từ vựng VSL, video ký hiệu và nhập liệu hàng loạt")
@RestController
@RequestMapping("/api/v1/admin/signs")
@RequiredArgsConstructor
public class SignController {

    private final SignService signService;

    @Operation(summary = "Tìm kiếm từ vựng (tự bỏ dấu tiếng Việt)",
            description = "Gõ 'dia chi' vẫn ra 'địa chỉ'. Hỗ trợ lọc theo chủ đề, cấp độ, "
                    + "đơn vị, từ loại, vùng miền còn thiếu video và tình trạng exemplar.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR, RoleType.VSL_REVIEWER})
    @PostMapping("/search")
    public ResponseEntity<ResponseData<PageResponse<SignResponse>>> search(
            @RequestBody SignSearchRequest request) {
        return ok(signService.search(request), "SIGN_SEARCH_SUCCESS");
    }

    @Operation(summary = "Chi tiết một từ vựng kèm video")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR, RoleType.VSL_REVIEWER})
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<SignResponse>> detail(@PathVariable UUID id) {
        return ok(signService.getDetail(id), "SIGN_DETAIL_SUCCESS");
    }

    @Operation(summary = "Tạo từ vựng mới",
            description = "Bỏ trống gloss thì hệ thống tự sinh từ wordVi: 'gia đình' -> 'GIA_DINH'.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping
    public ResponseEntity<ResponseData<SignResponse>> create(@Valid @RequestBody SignRequest request) {
        return ok(signService.createSign(request), "SIGN_CREATED");
    }

    @Operation(summary = "Cập nhật từ vựng")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/{id}")
    public ResponseEntity<ResponseData<SignResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody SignRequest request) {
        return ok(signService.updateSign(id, request), "SIGN_UPDATED");
    }

    @Operation(summary = "Xoá từ vựng")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> delete(@PathVariable UUID id) {
        signService.deleteSign(id);
        return ok(null, "SIGN_DELETED");
    }

    @Operation(summary = "Xuất bản / gỡ xuất bản hàng loạt")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/publish")
    public ResponseEntity<ResponseData<Map<String, Integer>>> togglePublish(
            @RequestBody PublishRequest request) {
        int affected = signService.togglePublish(request.ids(), request.published());
        return ok(Map.of("affected", affected),
                request.published() ? "SIGN_PUBLISHED" : "SIGN_UNPUBLISHED");
    }

    public record PublishRequest(List<UUID> ids, boolean published) {
    }

    // ==================== NHAP HANG LOAT ====================

    @Operation(summary = "Nhập từ vựng hàng loạt từ Excel",
            description = "File .xlsx được phân tích ở frontend bằng thư viện xlsx của vben, "
                    + "gửi lên đây dưới dạng JSON. Đặt dryRun=true để xem trước kết quả "
                    + "mà không ghi vào CSDL.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/import")
    public ResponseEntity<ResponseData<SignImportResultResponse>> importSigns(
            @Valid @RequestBody SignImportRequest request) {
        SignImportResultResponse result = signService.importSigns(request);
        return ok(result, Boolean.TRUE.equals(request.getDryRun())
                ? "SIGN_IMPORT_PREVIEW" : "SIGN_IMPORT_DONE");
    }

    // ==================== VIDEO ====================

    @Operation(summary = "Danh sách video của một từ")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR, RoleType.VSL_REVIEWER})
    @GetMapping("/{id}/videos")
    public ResponseEntity<ResponseData<List<SignVideoResponse>>> videos(@PathVariable UUID id) {
        return ok(signService.getVideos(id), "SIGN_VIDEO_LIST_SUCCESS");
    }

    @Operation(summary = "Tải video ký hiệu lên",
            description = "Video được đẩy lên MinIO và ghi vào bảng file_attachments dùng chung. "
                    + "Video đầu tiên của một vùng miền tự động thành video chính.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping(value = "/{id}/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseData<SignVideoResponse>> uploadVideo(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Region region,
            @RequestParam(required = false) ViewAngle viewAngle,
            @RequestParam(required = false) String signerLabel,
            @RequestParam(required = false) String captionVi) {
        return ok(signService.uploadVideo(id, file, region, viewAngle, signerLabel, captionVi),
                "SIGN_VIDEO_UPLOADED");
    }

    @Operation(summary = "Đặt video chính cho một vùng miền")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/{id}/videos/{videoId}/primary")
    public ResponseEntity<ResponseData<SignVideoResponse>> setPrimary(
            @PathVariable UUID id, @PathVariable UUID videoId) {
        return ok(signService.setPrimaryVideo(id, videoId), "SIGN_VIDEO_PRIMARY_SET");
    }

    @Operation(summary = "Xoá video ký hiệu")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/{id}/videos/{videoId}")
    public ResponseEntity<ResponseData<Void>> deleteVideo(
            @PathVariable UUID id, @PathVariable UUID videoId) {
        signService.deleteVideo(id, videoId);
        return ok(null, "SIGN_VIDEO_DELETED");
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
