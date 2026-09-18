package com.sunmoon.backend.entity.ai;

import com.sunmoon.backend.constant.enums.UnitType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// TANG 3 cua nguong cham diem - PHAI co san tu ngay dau. Khong co bang nay
// thi ngay mo khong cham duoc tu nao, vi ca hieu chinh tu exemplar lan tu
// gop y giao vien deu chua co du lieu.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "verify_threshold_groups")
public class VerifyThresholdGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false, length = 20)
    private UnitType unitType;

    @NotNull
    @Column(name = "hand_count", nullable = false)
    private Short handCount;

    @NotNull
    @Column(name = "threshold", nullable = false, precision = 6, scale = 4)
    private BigDecimal threshold;

    @Column(name = "note", length = Integer.MAX_VALUE)
    private String note;

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
