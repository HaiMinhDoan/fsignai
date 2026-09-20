package com.sunmoon.backend.entity.progress;

import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.catalog.WordPack;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tiến độ của một người học trong một gói từ. Xem V12__word_packs.sql —
 * itemsTotal được CHỤP LẠI lúc bắt đầu, không đếm sống từ word_pack_items.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "user_pack_progress")
public class UserPackProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pack_id", nullable = false)
    private WordPack pack;

    @NotNull
    @ColumnDefault("IN_PROGRESS")
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "IN_PROGRESS"; // IN_PROGRESS | COMPLETED

    @NotNull
    @ColumnDefault("0")
    @Column(name = "items_completed", nullable = false)
    @Builder.Default
    private Integer itemsCompleted = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "items_total", nullable = false)
    @Builder.Default
    private Integer itemsTotal = 0;

    @Column(name = "best_score")
    private Integer bestScore;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "stars", nullable = false)
    @Builder.Default
    private Integer stars = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "last_activity_at", nullable = false)
    private OffsetDateTime lastActivityAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (startedAt == null) startedAt = now;
        if (lastActivityAt == null) lastActivityAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
