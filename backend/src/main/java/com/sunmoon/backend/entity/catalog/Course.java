package com.sunmoon.backend.entity.catalog;

import com.sunmoon.backend.constant.enums.SignLevel;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.dictionary.Topic;
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
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 120)
    @NotNull
    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @Size(max = 200)
    @NotNull
    @Column(name = "title_vi", nullable = false, length = 200)
    private String titleVi;

    @Column(name = "description_vi", length = Integer.MAX_VALUE)
    private String descriptionVi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_file_id")
    private FileAttachment coverFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'BEGINNER'")
    @Column(name = "level", nullable = false, length = 20)
    @Builder.Default
    private SignLevel level = SignLevel.BEGINNER;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    // Khoá sinh tự động từ việc gom từ vựng theo chủ đề + cấp độ
    @NotNull
    @ColumnDefault("false")
    @Column(name = "generated", nullable = false)
    @Builder.Default
    private Boolean generated = false;

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
