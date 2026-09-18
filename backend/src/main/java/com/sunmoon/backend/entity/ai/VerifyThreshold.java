package com.sunmoon.backend.entity.ai;

import com.sunmoon.backend.constant.enums.ThresholdSource;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.dictionary.Sign;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// Ba tang nguong, tra theo thu tu uu tien: FEEDBACK_TUNED > EXEMPLAR_DERIVED
// > VerifyThresholdGroup (mac dinh nhom).
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "verify_thresholds")
public class VerifyThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_id", nullable = false, unique = true)
    private Sign sign;

    @NotNull
    @Column(name = "threshold", nullable = false, precision = 6, scale = 4)
    private BigDecimal threshold;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'GROUP_DEFAULT'")
    @Column(name = "source", nullable = false, length = 30)
    @Builder.Default
    private ThresholdSource source = ThresholdSource.GROUP_DEFAULT;

    // Tong TRONG SO gop y da dung de hieu chinh
    @NotNull
    @ColumnDefault("0")
    @Column(name = "sample_count", nullable = false)
    @Builder.Default
    private Integer sampleCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

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
