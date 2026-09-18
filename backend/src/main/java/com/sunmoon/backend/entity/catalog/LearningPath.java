package com.sunmoon.backend.entity.catalog;

import com.sunmoon.backend.constant.enums.LearningPathSource;
import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

// Sinh từ câu trả lời onboarding
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "learning_paths")
public class LearningPath {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 200)
    @NotNull
    @ColumnDefault("'Lộ trình của tôi'")
    @Column(name = "name", nullable = false, length = 200)
    @Builder.Default
    private String name = "Lộ trình của tôi";

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ONBOARDING'")
    @Column(name = "generated_from", nullable = false, length = 20)
    @Builder.Default
    private LearningPathSource generatedFrom = LearningPathSource.ONBOARDING;

    // Mỗi người chỉ có một lộ trình đang hoạt động (unique index có điều kiện ở DB)
    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
