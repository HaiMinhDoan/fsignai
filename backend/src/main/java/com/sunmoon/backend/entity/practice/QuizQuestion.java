package com.sunmoon.backend.entity.practice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sunmoon.backend.constant.enums.QuestionType;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.UUID;

// Câu hỏi của đề cố định.
// AI_PERFORM là cầu nối sang FastAPI: người dùng tự làm ký hiệu trước webcam
// thay vì chọn đáp án. Ở mức A, dạng câu này KHÔNG tính điểm.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_id", nullable = false)
    private Sign sign;

    @Column(name = "prompt_vi", length = Integer.MAX_VALUE)
    private String promptVi;

    // [{signId, label, videoKey}, ...]
    @NotNull
    @Type(JsonType.class)
    @ColumnDefault("'[]'")
    @Column(name = "options_json", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode optionsJson = JsonNodeFactory.instance.arrayNode();

    // NULL khi questionType = AI_PERFORM (ràng buộc CHECK ở DB)
    @Column(name = "correct_option_index")
    private Integer correctOptionIndex;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "points", nullable = false)
    @Builder.Default
    private Integer points = 1;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

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
