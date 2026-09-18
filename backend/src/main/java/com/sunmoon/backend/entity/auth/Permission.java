package com.sunmoon.backend.entity.auth;

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
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    // 'sign:write', 'forum:moderate'
    @Size(max = 100)
    @NotNull
    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Size(max = 150)
    @NotNull
    @Column(name = "name_vi", nullable = false, length = 150)
    private String nameVi;

    @Size(max = 50)
    @NotNull
    @Column(name = "module", nullable = false, length = 50)
    private String module;

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
