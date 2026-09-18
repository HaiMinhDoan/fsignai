package com.sunmoon.backend.entity.practice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sunmoon.backend.constant.enums.DistractorStrategy;
import com.sunmoon.backend.entity.auth.User;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.UUID;

// ĐỀ SINH TỰ ĐỘNG: khai báo luật, không soạn từng câu. Người dùng chọn
// chủ đề + vùng miền, hệ thống rút ngẫu nhiên từ kho signs.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "quiz_blueprints")
public class QuizBlueprint {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 100)
    @NotNull
    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Size(max = 200)
    @NotNull
    @Column(name = "title_vi", nullable = false, length = 200)
    private String titleVi;

    @Column(name = "description_vi", length = Integer.MAX_VALUE)
    private String descriptionVi;

    // Mảng UUID của topics
    @NotNull
    @Type(JsonType.class)
    @ColumnDefault("'[]'")
    @Column(name = "topic_ids", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode topicIds = JsonNodeFactory.instance.arrayNode();

    // Mảng SignLevel dạng string, ví dụ ["BEGINNER","BASIC"]
    @NotNull
    @Type(JsonType.class)
    @ColumnDefault("'[]'")
    @Column(name = "levels", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode levels = JsonNodeFactory.instance.arrayNode();

    // Mảng UnitType dạng string
    @NotNull
    @Type(JsonType.class)
    @ColumnDefault("'[]'")
    @Column(name = "unit_types", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode unitTypes = JsonNodeFactory.instance.arrayNode();

    // NULL = không giới hạn từ loại
    @Type(JsonType.class)
    @Column(name = "word_types", columnDefinition = "jsonb")
    private JsonNode wordTypes;

    // {"VIDEO_TO_WORD":5,"WORD_TO_VIDEO":3,"MATCHING":2}
    @NotNull
    @Type(JsonType.class)
    @ColumnDefault("'{\"VIDEO_TO_WORD\":5,\"WORD_TO_VIDEO\":3,\"MATCHING\":2}'")
    @Column(name = "question_type_mix", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode questionTypeMix = buildDefaultQuestionTypeMix();

    @NotNull
    @ColumnDefault("10")
    @Column(name = "question_count", nullable = false)
    @Builder.Default
    private Integer questionCount = 10;

    @NotNull
    @ColumnDefault("4")
    @Column(name = "option_count", nullable = false)
    @Builder.Default
    private Integer optionCount = 4;

    @NotNull
    @ColumnDefault("70")
    @Column(name = "pass_score", nullable = false)
    @Builder.Default
    private Integer passScore = 70;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'EASILY_CONFUSED'")
    @Column(name = "distractor_strategy", nullable = false, length = 30)
    @Builder.Default
    private DistractorStrategy distractorStrategy = DistractorStrategy.EASILY_CONFUSED;

    // Tránh gặp lại cùng nhóm từ khi thi nhiều lần
    @NotNull
    @ColumnDefault("30")
    @Column(name = "avoid_recent_days", nullable = false)
    @Builder.Default
    private Integer avoidRecentDays = 30;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

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

    // Giá trị JSON mặc định khớp cột DB, để @Builder.Default có nguồn dữ liệu hợp lệ
    private static JsonNode buildDefaultQuestionTypeMix() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("VIDEO_TO_WORD", 5);
        node.put("WORD_TO_VIDEO", 3);
        node.put("MATCHING", 2);
        return node;
    }
}
