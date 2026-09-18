package com.sunmoon.backend.entity.practice;

import com.sunmoon.backend.entity.ai.AiCheckResult;
import com.sunmoon.backend.entity.dictionary.Sign;
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
@Table(name = "quiz_answers")
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    // Voi de co dinh
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuizQuestion question;

    // Voi de sinh: vi tri trong QuizAttempt.generatedQuestions
    @Column(name = "question_index")
    private Integer questionIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_id")
    private Sign sign;

    @Column(name = "selected_option_index")
    private Integer selectedOptionIndex;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_check_result_id")
    private AiCheckResult aiCheckResult;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "points_earned", nullable = false)
    @Builder.Default
    private Integer pointsEarned = 0;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "answered_at", nullable = false)
    @Builder.Default
    private OffsetDateTime answeredAt = OffsetDateTime.now();

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
