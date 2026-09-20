package com.sunmoon.backend.entity.gamification;

import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tổng điểm của một người học — BỘ NHỚ ĐỆM của point_events.
 *
 * Sự thật gốc nằm ở bảng point_events; bảng này chỉ để xếp hạng cho nhanh
 * mà không phải cộng dồn hàng nghìn dòng mỗi lần mở bảng xếp hạng. Lệch thì
 * tính lại được từ point_events (xem V14__games_points_rewards.sql).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "user_points")
public class UserPoints {

    /** Mỗi cấp cần đúng 200 điểm — khớp con số "150 / 200 XP" trong thiết kế Figma */
    public static final int POINTS_PER_LEVEL = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ColumnDefault("0")
    @Column(name = "total_points", nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;

    @ColumnDefault("0")
    @Column(name = "weekly_points", nullable = false)
    @Builder.Default
    private Integer weeklyPoints = 0;

    @ColumnDefault("0")
    @Column(name = "monthly_points", nullable = false)
    @Builder.Default
    private Integer monthlyPoints = 0;

    @Column(name = "week_start_date")
    private LocalDate weekStartDate;

    @Column(name = "month_start_date")
    private LocalDate monthStartDate;

    @ColumnDefault("1")
    @Column(name = "level", nullable = false)
    @Builder.Default
    private Integer level = 1;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
