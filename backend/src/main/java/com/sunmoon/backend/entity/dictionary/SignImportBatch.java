package com.sunmoon.backend.entity.dictionary;

import com.sunmoon.backend.constant.enums.DuplicateStrategy;
import com.sunmoon.backend.constant.enums.ImportBatchStatus;
import com.sunmoon.backend.constant.enums.ImportType;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

// Nhật ký nhập Excel và nạp video hàng loạt
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "sign_import_batches")
public class SignImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "import_type", nullable = false, length = 30)
    private ImportType importType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileAttachment file;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ImportBatchStatus status = ImportBatchStatus.PENDING;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "total_rows", nullable = false)
    @Builder.Default
    private Integer totalRows = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "processed_rows", nullable = false)
    @Builder.Default
    private Integer processedRows = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "created_count", nullable = false)
    @Builder.Default
    private Integer createdCount = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "updated_count", nullable = false)
    @Builder.Default
    private Integer updatedCount = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    private Integer failedCount = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'SKIP'")
    @Column(name = "duplicate_strategy", nullable = false, length = 20)
    @Builder.Default
    private DuplicateStrategy duplicateStrategy = DuplicateStrategy.SKIP;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_report_file_id")
    private FileAttachment errorReportFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by")
    private User startedBy;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

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
