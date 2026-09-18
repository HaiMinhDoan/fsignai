package com.sunmoon.backend.entity.ai;

import com.sunmoon.backend.constant.enums.ExemplarBuildStatus;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.entity.dictionary.SignVideo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// Mau chuan de so khop DTW. Sinh tu dong bang cach chay MediaPipe tren
// chinh video da nap, khong phai quay them clip nao.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "sign_exemplars")
public class SignExemplar {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_id", nullable = false)
    private Sign sign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_video_id")
    private SignVideo signVideo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'COMMON'")
    @Column(name = "region", nullable = false, length = 20)
    @Builder.Default
    private Region region = Region.COMMON;

    // File .npz chua chuoi landmark da chuan hoa, nam tren MinIO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landmark_file_id")
    private FileAttachment landmarkFile;

    @Column(name = "frame_count")
    private Integer frameCount;

    @Column(name = "landmark_dim")
    private Integer landmarkDim;

    // Vector nhung cho M1 (encoder). M0 dung DTW thuan nen de NULL.
    // Khong dung @Lob: tren PostgreSQL no anh xa sang OID (Large Object),
    // trong khi cot DB la bytea. columnDefinition ep dung kieu.
    @Column(name = "embedding", columnDefinition = "bytea")
    private byte[] embedding;

    @Column(name = "embedding_dim")
    private Integer embeddingDim;

    @Column(name = "quality_score", precision = 5, scale = 4)
    private BigDecimal qualityScore;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'mediapipe-holistic-v1'")
    @Column(name = "model_version", nullable = false, length = 50)
    @Builder.Default
    private String modelVersion = "mediapipe-holistic-v1";

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    @Column(name = "build_status", nullable = false, length = 20)
    @Builder.Default
    private ExemplarBuildStatus buildStatus = ExemplarBuildStatus.PENDING;

    @Column(name = "build_error", length = Integer.MAX_VALUE)
    private String buildError;

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
