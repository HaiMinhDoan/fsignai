package com.sunmoon.backend.entity.catalog;

import com.sunmoon.backend.constant.enums.SignLevel;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.dictionary.Topic;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Gói từ — một chặng học ngắn gồm một nhúm từ vựng, dùng cho bản đồ đảo
 * phiêu lưu. Xem V12__word_packs.sql cho lý do không tái dùng courses/lessons.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "word_packs")
public class WordPack {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 100)
    @NotNull
    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Size(max = 255)
    @NotNull
    @Column(name = "title_vi", nullable = false)
    private String titleVi;

    @Column(name = "description_vi", columnDefinition = "TEXT")
    private String descriptionVi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_file_id")
    private FileAttachment coverFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Enumerated(EnumType.STRING)
    @NotNull
    @ColumnDefault("BEGINNER")
    @Column(name = "level", nullable = false, length = 20)
    @Builder.Default
    private SignLevel level = SignLevel.BEGINNER;

    @Size(max = 20)
    @Column(name = "island_color", length = 20)
    private String islandColor;

    @Size(max = 50)
    @Column(name = "icon_name", length = 50)
    private String iconName;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /** Chặng này chỉ mở khi đã hoàn thành chặng kia. NULL = mở sẵn từ đầu. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unlock_after_pack_id")
    private WordPack unlockAfterPack;

    @NotNull
    @ColumnDefault("80")
    @Column(name = "pass_score", nullable = false)
    @Builder.Default
    private Integer passScore = 80;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

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
