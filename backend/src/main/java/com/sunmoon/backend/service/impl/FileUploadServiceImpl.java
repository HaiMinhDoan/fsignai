package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.EntityType;
import com.sunmoon.backend.constant.enums.FileType;
import com.sunmoon.backend.dto.response.FileAttachmentResponse;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.FileAttachmentRepository;
import com.sunmoon.backend.service.FileUploadService;
import com.sunmoon.backend.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    /**
     * Ảnh bìa và biểu tượng chỉ cần vài trăm KB. Chặn ở 5MB để một lần lỡ tay
     * kéo nhầm ảnh máy ảnh 20MB không chiếm chỗ và làm chậm trang của người học.
     */
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    /** Chỉ cho phép entityType thuộc danh sách đã biết, tránh rác trong CSDL */
    private static final Set<String> ALLOWED_ENTITY_TYPES = Set.of(
            EntityType.TOPIC, EntityType.SIGN, EntityType.COURSE, EntityType.LESSON,
            EntityType.QUIZ, EntityType.QUIZ_QUESTION, EntityType.USER,
            EntityType.FORUM_POST, EntityType.BLOG_POST, EntityType.FILE_ATTACHMENT);

    private final MinioService minioService;
    private final FileAttachmentRepository fileRepository;

    @Override
    @Transactional
    public FileAttachmentResponse uploadImage(MultipartFile file, String entityType, UUID entityId) {
        if (file == null || file.isEmpty()) {
            throw badRequest("Chưa chọn tệp nào");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw badRequest("Ảnh không được lớn hơn 5MB (tệp này "
                    + (file.getSize() / 1024 / 1024) + "MB)");
        }

        String mime = file.getContentType();
        if (mime == null || !FileType.IMAGE.matches(mime)) {
            throw badRequest("Chỉ nhận ảnh JPEG, PNG hoặc WebP. Tệp gửi lên có kiểu: " + mime);
        }

        String type = (entityType == null || entityType.isBlank())
                ? EntityType.FILE_ATTACHMENT : entityType;
        if (!ALLOWED_ENTITY_TYPES.contains(type)) {
            throw badRequest("entityType không hợp lệ: " + type);
        }

        // Tên object có UUID ở đầu: hai người tải cùng lúc hai tệp trùng tên
        // sẽ không đè lên nhau.
        String objectName = "images/" + type + "/" + UUID.randomUUID()
                + "_" + safeName(file.getOriginalFilename());

        try {
            minioService.upload(file, objectName);
        } catch (Exception e) {
            throw new CommonException("Tải ảnh lên MinIO thất bại: " + e.getMessage(), e);
        }

        FileAttachment saved = fileRepository.save(FileAttachment.builder()
                .bucket(minioService.getBucketName())
                .objectKey(objectName)
                .originalName(file.getOriginalFilename())
                .mimeType(mime)
                .extension(extensionOf(file.getOriginalFilename()))
                .sizeBytes(file.getSize())
                .entityType(type)
                .entityId(entityId)
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID fileId) {
        FileAttachment file = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tệp: " + fileId));

        // Xoá object trước, bản ghi sau. Nếu xoá object hỏng thì bản ghi vẫn còn
        // và còn truy vết được; làm ngược lại sẽ để lại object mồ côi trên MinIO
        // mà không ai biết đường tìm.
        boolean removed = minioService.delete(file.getBucket(), file.getObjectKey());
        if (!removed) {
            log.warn("Không xoá được object trên MinIO: {}/{}", file.getBucket(), file.getObjectKey());
        }
        fileRepository.delete(file);
    }

    private FileAttachmentResponse toResponse(FileAttachment entity) {
        return FileAttachmentResponse.builder()
                .id(entity.getId())
                .bucket(entity.getBucket())
                .objectKey(entity.getObjectKey())
                .url(entity.getPublicUrl())
                .originalName(entity.getOriginalName())
                .mimeType(entity.getMimeType())
                .extension(entity.getExtension())
                .sizeBytes(entity.getSizeBytes())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private CommonException badRequest(String message) {
        CommonException ex = new CommonException(message);
        ex.setHttpStatus(HttpStatus.BAD_REQUEST);
        return ex;
    }

    /** Bỏ ký tự có thể làm hỏng đường dẫn object trên MinIO */
    private String safeName(String original) {
        if (original == null || original.isBlank()) {
            return "file";
        }
        return original.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extensionOf(String filename) {
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? null : filename.substring(dot + 1).toLowerCase();
    }
}
