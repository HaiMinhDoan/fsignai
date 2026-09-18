package com.sunmoon.backend.entity.notification;

import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "push_subscriptions")
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "endpoint", nullable = false, unique = true, length = Integer.MAX_VALUE)
    private String endpoint;

    @Size(max = 255)
    @NotNull
    @Column(name = "p256dh", nullable = false, length = 255)
    private String p256dh;

    @Size(max = 255)
    @NotNull
    @Column(name = "auth", nullable = false, length = 255)
    private String auth;

    @Size(max = 255)
    @Column(name = "user_agent", length = 255)
    private String userAgent;

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
