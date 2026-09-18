package com.sunmoon.backend.entity.progress;

import com.sunmoon.backend.constant.enums.ProgressStatus;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.catalog.Course;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "user_course_progress")
public class UserCourseProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "lessons_completed", nullable = false)
    @Builder.Default
    private Integer lessonsCompleted = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "lessons_total", nullable = false)
    @Builder.Default
    private Integer lessonsTotal = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "progress_percent", nullable = false)
    @Builder.Default
    private Integer progressPercent = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'NOT_STARTED'")
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProgressStatus status = ProgressStatus.NOT_STARTED;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

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
