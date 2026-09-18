package com.sunmoon.backend.entity.practice;

import com.sunmoon.backend.constant.enums.QuizType;
import com.sunmoon.backend.entity.catalog.Lesson;
import com.sunmoon.backend.entity.dictionary.Topic;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

// ĐỀ CỐ ĐỊNH do admin soạn tay (khác quiz_blueprints - đề sinh tự động)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Size(max = 200)
    @NotNull
    @Column(name = "title_vi", nullable = false, length = 200)
    private String titleVi;

    @Column(name = "description_vi", length = Integer.MAX_VALUE)
    private String descriptionVi;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'LESSON_QUIZ'")
    @Column(name = "quiz_type", nullable = false, length = 20)
    @Builder.Default
    private QuizType quizType = QuizType.LESSON_QUIZ;

    @NotNull
    @ColumnDefault("70")
    @Column(name = "pass_score", nullable = false)
    @Builder.Default
    private Integer passScore = 70;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

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
