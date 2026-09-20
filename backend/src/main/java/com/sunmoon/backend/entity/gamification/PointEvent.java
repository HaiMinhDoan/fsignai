package com.sunmoon.backend.entity.gamification;

import com.sunmoon.backend.constant.enums.PointSource;
import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Nguồn gốc của mọi điểm — user_points chỉ là bộ nhớ đệm của bảng này. */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "point_events")
public class PointEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private PointSource source;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "note_vi", length = 255)
    private String noteVi;

    @Column(name = "earned_date", nullable = false)
    private LocalDate earnedDate;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
