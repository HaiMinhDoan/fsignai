package com.sunmoon.backend.entity.forum;

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
@Table(name = "forum_categories")
public class ForumCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 120)
    @NotNull
    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @Size(max = 150)
    @NotNull
    @Column(name = "name_vi", nullable = false, length = 150)
    private String nameVi;

    @Column(name = "description_vi", length = Integer.MAX_VALUE)
    private String descriptionVi;

    @Size(max = 100)
    @Column(name = "icon_name", length = 100)
    private String iconName;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = true;

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
