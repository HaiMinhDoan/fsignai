package com.sunmoon.backend.entity;

import com.sunmoon.backend.constant.ConstantVariables;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "file_attachments")
public class FileAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 100)
    @NotNull
    @Column(name = "bucket", nullable = false, length = 100)
    private String bucket;

    @NotNull
    @Column(name = "object_key", nullable = false, length = Integer.MAX_VALUE)
    private String objectKey;

    @Size(max = 255)
    @Column(name = "original_name")
    private String originalName;

    @Size(max = 100)
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Size(max = 100)
    @Column(name = "extension", length = 100)
    private String extension;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Size(max = 100)
    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'active'")
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "active";

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();



    /**
     * Địa chỉ TRÌNH DUYỆT dùng để tải video/ảnh — khác với địa chỉ server dùng để nói chuyện với MinIO.
     *
     * Ưu tiên `minio.domain` (tên miền công khai, có HTTPS), chỉ khi bỏ trống mới quay về
     * `minio.endpoint`. Tách hai thứ này vì chúng phục vụ hai mục đích trái ngược nhau:
     *  • server nên đi thẳng vào MinIO (nhanh, không qua proxy nên không dính giới hạn dung lượng tải lên)
     *  • trình duyệt bắt buộc phải dùng HTTPS, nếu không trang HTTPS sẽ chặn sạch video vì nội dung hỗn hợp
     */
    public String getPublicUrl(){
        String base = ConstantVariables.MINIO_DOMAIN == null || ConstantVariables.MINIO_DOMAIN.isBlank()
                ? ConstantVariables.MINIO_ENDPOINT
                : ConstantVariables.MINIO_DOMAIN;
        return base.endsWith("/")
                ? base + getBucket() + "/" + getObjectKey()
                : base + "/" + getBucket() + "/" + getObjectKey();
    }

}
