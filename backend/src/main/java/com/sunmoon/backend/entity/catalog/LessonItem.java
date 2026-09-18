package com.sunmoon.backend.entity.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.sunmoon.backend.constant.enums.LessonItemType;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.entity.practice.Quiz;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.UUID;

// Nội dung bên trong bài học, sắp thứ tự kéo-thả (sortablejs ở FE)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "lesson_items")
public class LessonItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private LessonItemType itemType;

    // Bắt buộc khi itemType = SIGN (ràng buộc CHECK ở DB)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_id")
    private Sign sign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    // Nội dung tự do cho TEXT / VIDEO
    @Type(JsonType.class)
    @Column(name = "content_json", columnDefinition = "jsonb")
    private JsonNode contentJson;

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
