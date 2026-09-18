package com.sunmoon.backend.entity.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.sunmoon.backend.constant.enums.JobExecutionStatus;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.UUID;

// Theo doi job nen (ShedLock chong chay trung khi scale nhieu instance)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "job_executions")
public class JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 120)
    @NotNull
    @Column(name = "job_name", nullable = false, length = 120)
    private String jobName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'RUNNING'")
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private JobExecutionStatus status = JobExecutionStatus.RUNNING;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Integer processed = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "failed", nullable = false)
    @Builder.Default
    private Integer failed = 0;

    @Type(JsonType.class)
    @Column(name = "detail", columnDefinition = "jsonb")
    private JsonNode detail;

    @Column(name = "error_message", length = Integer.MAX_VALUE)
    private String errorMessage;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private OffsetDateTime startedAt = OffsetDateTime.now();

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
