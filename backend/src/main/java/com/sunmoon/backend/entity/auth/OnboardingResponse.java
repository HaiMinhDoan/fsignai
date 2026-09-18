package com.sunmoon.backend.entity.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sunmoon.backend.constant.enums.CurrentLevel;
import com.sunmoon.backend.constant.enums.LearnReason;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.UUID;

// 4 câu hỏi sau khi đăng ký, dùng để sinh learning path cá nhân hoá
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "onboarding_responses")
public class OnboardingResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "learn_reason", length = 30)
    private LearnReason learnReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_level", length = 20)
    private CurrentLevel currentLevel;

    // Chỉ nhận 5 / 10 / 15 / 30 (ràng buộc CHECK ở DB)
    @Column(name = "daily_minutes")
    private Integer dailyMinutes;

    // jsonb vì danh sách chủ đề sẽ đổi; đây là dữ liệu lịch sử, không ràng buộc FK
    @NotNull
    @Type(JsonType.class)
    @ColumnDefault("'[]'")
    @Column(name = "interested_topics", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode interestedTopics = JsonNodeFactory.instance.arrayNode();

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
