package com.sunmoon.backend.entity.practice;

import com.fasterxml.jackson.databind.JsonNode;
import com.sunmoon.backend.constant.enums.QuizAttemptStatus;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.entity.auth.User;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.UUID;

// generatedQuestions luu ANH CHUP cua de da sinh, khong chi luu seed.
// Neu chi luu seed roi sinh lai khi xem lai bai, chi can mot tu bi sua hoac
// go xuat ban la de tai tao ra khac voi de nguoi dung da lam - diem so se
// khong khop cau hoi va khong ai giai thich duoc. Anh chup giu lich su bat bien.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id")
    private QuizBlueprint blueprint;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'COMMON'")
    @Column(name = "region", nullable = false, length = 20)
    @Builder.Default
    private Region region = Region.COMMON;

    @Type(JsonType.class)
    @Column(name = "generated_questions", columnDefinition = "jsonb")
    private JsonNode generatedQuestions;

    @Column(name = "seed")
    private Long seed;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "score", nullable = false)
    @Builder.Default
    private Integer score = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "max_score", nullable = false)
    @Builder.Default
    private Integer maxScore = 0;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "passed", nullable = false)
    @Builder.Default
    private Boolean passed = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'IN_PROGRESS'")
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private QuizAttemptStatus status = QuizAttemptStatus.IN_PROGRESS;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

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
