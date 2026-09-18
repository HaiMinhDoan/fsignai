package com.sunmoon.backend.entity.progress;

import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

// DU LIEU GOC de tinh streak. Streak duoc SUY RA tu bang nay, khong phai
// mot bien dem tu tang - bien dem se lech khi co loi, khi user doi mui gio,
// hoac khi can tinh bu; con daily_activity thi tinh lai duoc bat cu luc nao.
//
// activityDate la NGAY THEO MUI GIO NGUOI DUNG (user_settings.timezone),
// khong phai theo gio server. Nguoi o VN hoc luc 23h ma server chay UTC
// se bi tinh sang hom sau va mat streak oan.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "daily_activity")
public class DailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "minutes_studied", nullable = false)
    @Builder.Default
    private Integer minutesStudied = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "lessons_completed", nullable = false)
    @Builder.Default
    private Integer lessonsCompleted = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "signs_reviewed", nullable = false)
    @Builder.Default
    private Integer signsReviewed = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "quizzes_taken", nullable = false)
    @Builder.Default
    private Integer quizzesTaken = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "ai_checks_done", nullable = false)
    @Builder.Default
    private Integer aiChecksDone = 0;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "goal_met", nullable = false)
    @Builder.Default
    private Boolean goalMet = false;

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
