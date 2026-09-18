package com.sunmoon.backend.entity.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnDefault;
import jakarta.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "user_roles")
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    private User grantedBy;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "granted_at", nullable = false)
    @Builder.Default
    private OffsetDateTime grantedAt = OffsetDateTime.now();
}
