package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.response.FileAttachmentResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.UUID;

@Tag(name = "CMS - Tệp đính kèm", description = "Tải ảnh bìa, biểu tượng và các tệp dùng chung")
@RestController
@RequestMapping("/api/v1/admin/files")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "Tải ảnh lên",
            description = "Nhận JPEG, PNG, WebP, tối đa 5MB. Trả về id để gán vào "
                    + "coverFileId / iconFileId khi lưu khoá học hoặc chủ đề.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseData<FileAttachmentResponse>> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId) {
        return ok(fileUploadService.uploadImage(file, entityType, entityId), "FILE_UPLOADED");
    }

    @Operation(summary = "Xoá tệp khỏi MinIO và CSDL")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> delete(@PathVariable UUID id) {
        fileUploadService.delete(id);
        return ok(null, "FILE_DELETED");
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
