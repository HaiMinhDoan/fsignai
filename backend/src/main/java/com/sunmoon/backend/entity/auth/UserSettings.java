package com.sunmoon.backend.entity.auth;

import com.sunmoon.backend.constant.enums.ThemeMode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Size(max = 10)
    @NotNull
    @ColumnDefault("'vi'")
    @Column(name = "locale", nullable = false, length = 10)
    @Builder.Default
    private String locale = "vi";

    @Size(max = 64)
    @NotNull
    @ColumnDefault("'Asia/Ho_Chi_Minh'")
    @Column(name = "timezone", nullable = false, length = 64)
    @Builder.Default
    private String timezone = "Asia/Ho_Chi_Minh";

    @NotNull
    @ColumnDefault("10")
    @Column(name = "daily_goal_minutes", nullable = false)
    @Builder.Default
    private Integer dailyGoalMinutes = 10;

    @NotNull
    @ColumnDefault("'20:00:00'")
    @Column(name = "reminder_time", nullable = false)
    @Builder.Default
    private LocalTime reminderTime = LocalTime.of(20, 0);

    @NotNull
    @ColumnDefault("true")
    @Column(name = "notify_email", nullable = false)
    @Builder.Default
    private Boolean notifyEmail = true;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "notify_push", nullable = false)
    @Builder.Default
    private Boolean notifyPush = true;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "notify_streak", nullable = false)
    @Builder.Default
    private Boolean notifyStreak = true;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "notify_new_course", nullable = false)
    @Builder.Default
    private Boolean notifyNewCourse = true;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "notify_forum_reply", nullable = false)
    @Builder.Default
    private Boolean notifyForumReply = true;

    // Người khiếm thính thường cần video chậm; mặc định phát tốc độ bình thường
    @NotNull
    @ColumnDefault("1.00")
    @Column(name = "default_playback_rate", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal defaultPlaybackRate = new BigDecimal("1.00");

    @NotNull
    @ColumnDefault("false")
    @Column(name = "high_contrast", nullable = false)
    @Builder.Default
    private Boolean highContrast = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'SYSTEM'")
    @Column(name = "theme", nullable = false, length = 20)
    @Builder.Default
    private ThemeMode theme = ThemeMode.SYSTEM;

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
