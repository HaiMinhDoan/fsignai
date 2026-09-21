package com.sunmoon.backend.entity.dictionary;

import com.sunmoon.backend.entity.FileAttachment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một bước trong hướng dẫn thực hiện ký hiệu.
 *
 * Khác signs.descriptionVi ở chỗ có THỨ TỰ: người học làm theo từng nhịp thay vì
 * đọc một khối chữ dài. Xem V11__sign_steps.sql.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "sign_steps")
public class SignStep {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sign_id", nullable = false)
    private Sign sign;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_file_id")
    private FileAttachment imageFile;

    @Column(name = "title_vi", length = 150)
    private String titleVi;

    /** Bắt buộc có: ảnh không được là kênh thông tin duy nhất (§2.2) */
    @Column(name = "description_vi", nullable = false, columnDefinition = "TEXT")
    private String descriptionVi;

    @Column(name = "body_focus", length = 20)
    private String bodyFocus;

    @Column(name = "hold_seconds")
    private BigDecimal holdSeconds;

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
