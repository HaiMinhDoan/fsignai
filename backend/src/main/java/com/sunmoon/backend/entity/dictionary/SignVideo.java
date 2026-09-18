package com.sunmoon.backend.entity.dictionary;

import com.sunmoon.backend.constant.enums.IngestStatus;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.constant.enums.ViewAngle;
import com.sunmoon.backend.entity.FileAttachment;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

// Metadata nghiệp vụ của video; file thật nằm ở file_attachments.
//
// region KHÔNG phải cột phụ: từ điển Bộ GD&ĐT mã hoá vùng miền ngay trong
// tên file (W00665B/T/N = Bắc/Trung/Nam). Người học Hà Nội được dạy ký hiệu
// miền Nam sẽ không giao tiếp được với người điếc quanh mình.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "sign_videos")
public class SignVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_id", nullable = false)
    private Sign sign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileAttachment file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_file_id")
    private FileAttachment thumbnailFile;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'COMMON'")
    @Column(name = "region", nullable = false, length = 20)
    @Builder.Default
    private Region region = Region.COMMON;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'FRONT'")
    @Column(name = "view_angle", nullable = false, length = 20)
    @Builder.Default
    private ViewAngle viewAngle = ViewAngle.FRONT;

    // Ẩn danh: 'signer_01'
    @Size(max = 50)
    @Column(name = "signer_label", length = 50)
    private String signerLabel;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "caption_vi", length = Integer.MAX_VALUE)
    private String captionVi;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    // Nạp hàng loạt từ URL: theo dõi tiến trình tải về
    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'READY'")
    @Column(name = "ingest_status", nullable = false, length = 20)
    @Builder.Default
    private IngestStatus ingestStatus = IngestStatus.READY;

    @Column(name = "source_url", length = Integer.MAX_VALUE)
    private String sourceUrl;

    @Column(name = "ingest_error", length = Integer.MAX_VALUE)
    private String ingestError;

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
}
