package com.sunmoon.backend.entity.progress;

import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "user_streaks")
public class UserStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "longest_streak", nullable = false)
    @Builder.Default
    private Integer longestStreak = 0;

    @Column(name = "streak_start_date")
    private LocalDate streakStartDate;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    // So bang cuu chuoi con lai
    @NotNull
    @ColumnDefault("2")
    @Column(name = "freeze_available", nullable = false)
    @Builder.Default
    private Integer freezeAvailable = 2;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "freeze_used_total", nullable = false)
    @Builder.Default
    private Integer freezeUsedTotal = 0;

    // Tranh cap bang hai lan trong cung thang
    @Column(name = "last_freeze_grant_month")
    private LocalDate lastFreezeGrantMonth;

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
