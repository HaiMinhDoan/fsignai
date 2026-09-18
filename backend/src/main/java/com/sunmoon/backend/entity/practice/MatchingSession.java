package com.sunmoon.backend.entity.practice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.dictionary.Topic;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "matching_sessions")
public class MatchingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'COMMON'")
    @Column(name = "region", nullable = false, length = 20)
    @Builder.Default
    private Region region = Region.COMMON;

    @NotNull
    @ColumnDefault("8")
    @Column(name = "pair_count", nullable = false)
    @Builder.Default
    private Integer pairCount = 8;

    @NotNull
    @Type(JsonType.class)
    @ColumnDefault("'[]'")
    @Column(name = "pairs_json", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode pairsJson = JsonNodeFactory.instance.arrayNode();

    @NotNull
    @ColumnDefault("0")
    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private Integer correctCount = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

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
