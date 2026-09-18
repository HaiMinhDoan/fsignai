package com.sunmoon.backend.entity.dictionary;

import com.sunmoon.backend.constant.enums.TopicCategory;
import com.sunmoon.backend.entity.FileAttachment;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

// Cây chủ đề, cho phép chủ đề con qua parent
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Topic parent;

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

    // Tên icon logic, đổi bộ icon không phải sửa dữ liệu
    @Size(max = 100)
    @Column(name = "icon_name", length = 100)
    private String iconName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icon_file_id")
    private FileAttachment iconFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_file_id")
    private FileAttachment coverFile;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'SIMPLE_SIGN'")
    @Column(name = "category", nullable = false, length = 30)
    @Builder.Default
    private TopicCategory category = TopicCategory.SIMPLE_SIGN;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

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
