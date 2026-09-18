package com.sunmoon.backend.entity.ai;

import com.sunmoon.backend.constant.enums.FeedbackVerdict;
import com.sunmoon.backend.constant.enums.VslRole;
import com.sunmoon.backend.constant.enums.VslRoleStatus;
import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

// "Cham nhu vay co dung khong?" (thumbs up/down).
//
// weightSnapshot va roleSnapshot PHAI CHUP LAI tai thoi diem gop y. Neu tinh
// trong so dong theo vai tro hien tai, thi mot nguoi duoc duyet hoac bi go
// vai tro se lam moi nguong da hieu chinh trong qua khu am tham doi theo,
// va khong ai giai thich noi vi sao diem hom nay khac hom qua.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "ai_check_feedback")
public class AiCheckFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_check_result_id", nullable = false)
    private AiCheckResult aiCheckResult;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 20)
    private FeedbackVerdict verdict;

    @Column(name = "note", length = Integer.MAX_VALUE)
    private String note;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "weight_snapshot", nullable = false)
    @Builder.Default
    private Short weightSnapshot = 1;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'LEARNER'")
    @Column(name = "role_snapshot", nullable = false, length = 30)
    @Builder.Default
    private VslRole roleSnapshot = VslRole.LEARNER;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'SELF_DECLARED'")
    @Column(name = "role_status_snapshot", nullable = false, length = 30)
    @Builder.Default
    private VslRoleStatus roleStatusSnapshot = VslRoleStatus.SELF_DECLARED;

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
