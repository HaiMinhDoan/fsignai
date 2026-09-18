package com.sunmoon.backend.entity.practice;

import com.sunmoon.backend.constant.enums.FlashcardResult;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.dictionary.Sign;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// Thuật toán SM-2. Nút "Cần luyện thêm" phải thực sự đổi lịch ôn (dueAt),
// nếu không nó chỉ là nút trang trí và người học không được lợi gì.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "flashcard_reviews")
public class FlashcardReview {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_id", nullable = false)
    private Sign sign;

    @NotNull
    @ColumnDefault("2.50")
    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal easeFactor = new BigDecimal("2.50");

    @NotNull
    @ColumnDefault("0")
    @Column(name = "interval_days", nullable = false)
    @Builder.Default
    private Integer intervalDays = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "repetitions", nullable = false)
    @Builder.Default
    private Integer repetitions = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "lapses", nullable = false)
    @Builder.Default
    private Integer lapses = 0;

    // Index quan trọng nhất về hiệu năng: chạy mỗi lần người dùng mở mục luyện tập
    @NotNull
    @ColumnDefault("now()")
    @Column(name = "due_at", nullable = false)
    @Builder.Default
    private OffsetDateTime dueAt = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "last_result", length = 20)
    private FlashcardResult lastResult;

    @Column(name = "last_reviewed_at")
    private OffsetDateTime lastReviewedAt;

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
