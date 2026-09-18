package com.sunmoon.backend.entity.progress;

import com.sunmoon.backend.constant.enums.StreakFreezeReason;
import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

// Nhat ky dung bang, de giai thich duoc khi user thac mac
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "streak_freezes")
public class StreakFreeze {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "used_for_date", nullable = false)
    private LocalDate usedForDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'MONTHLY_GRANT'")
    @Column(name = "granted_reason", nullable = false, length = 30)
    @Builder.Default
    private StreakFreezeReason grantedReason = StreakFreezeReason.MONTHLY_GRANT;

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
