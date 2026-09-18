-- =====================================================================
-- V6 — Tiến độ học, streak, thành tích
-- =====================================================================

CREATE TABLE user_lesson_progress
(
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID        NOT NULL REFERENCES users (id)   ON DELETE CASCADE,
    lesson_id          UUID        NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    status             VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
        CHECK (status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED')),
    progress_percent   INT         NOT NULL DEFAULT 0 CHECK (progress_percent BETWEEN 0 AND 100),
    last_item_id       UUID REFERENCES lesson_items (id) ON DELETE SET NULL,
    time_spent_seconds INT         NOT NULL DEFAULT 0 CHECK (time_spent_seconds >= 0),
    started_at         TIMESTAMPTZ,
    completed_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_lesson UNIQUE (user_id, lesson_id),
    -- Một chiều: đã hoàn thành thì phải có mốc thời gian.
    -- Không chặn chiều ngược lại để cho phép học lại bài đã xong.
    CONSTRAINT chk_lesson_completed_has_timestamp
        CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL)
);

CREATE INDEX idx_lesson_progress_user ON user_lesson_progress (user_id, status);
CREATE INDEX idx_lesson_progress_recent
    ON user_lesson_progress (user_id, updated_at DESC);


CREATE TABLE user_course_progress
(
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL REFERENCES users (id)   ON DELETE CASCADE,
    course_id        UUID        NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    lessons_completed INT        NOT NULL DEFAULT 0,
    lessons_total    INT         NOT NULL DEFAULT 0,
    progress_percent INT         NOT NULL DEFAULT 0 CHECK (progress_percent BETWEEN 0 AND 100),
    status           VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
        CHECK (status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED')),
    completed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_course UNIQUE (user_id, course_id)
);


-- ---------------------------------------------------------------------
-- daily_activity — DỮ LIỆU GỐC để tính streak
--
-- Streak được SUY RA từ bảng này, không phải một biến đếm tự tăng.
-- Biến đếm sẽ lệch khi có lỗi, khi user đổi múi giờ, hoặc khi cần tính bù;
-- còn daily_activity thì tính lại được bất cứ lúc nào.
--
-- activity_date là NGÀY THEO MÚI GIỜ NGƯỜI DÙNG (user_settings.timezone),
-- không phải theo giờ server. Người ở VN học lúc 23h mà server chạy UTC
-- sẽ bị tính sang hôm sau và mất streak oan.
-- ---------------------------------------------------------------------
CREATE TABLE daily_activity
(
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    activity_date     DATE        NOT NULL,
    minutes_studied   INT         NOT NULL DEFAULT 0 CHECK (minutes_studied >= 0),
    lessons_completed INT         NOT NULL DEFAULT 0,
    signs_reviewed    INT         NOT NULL DEFAULT 0,
    quizzes_taken     INT         NOT NULL DEFAULT 0,
    ai_checks_done    INT         NOT NULL DEFAULT 0,
    goal_met          BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_daily_activity UNIQUE (user_id, activity_date)
);

CREATE INDEX idx_daily_activity_user ON daily_activity (user_id, activity_date DESC);


CREATE TABLE user_streaks
(
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    current_streak    INT         NOT NULL DEFAULT 0 CHECK (current_streak >= 0),
    longest_streak    INT         NOT NULL DEFAULT 0 CHECK (longest_streak >= 0),
    streak_start_date DATE,
    last_activity_date DATE,
    freeze_available  INT         NOT NULL DEFAULT 2 CHECK (freeze_available BETWEEN 0 AND 5),
    freeze_used_total INT         NOT NULL DEFAULT 0,
    last_freeze_grant_month DATE, -- tránh cấp băng hai lần trong cùng tháng
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_longest_ge_current CHECK (longest_streak >= current_streak)
);

-- Job 01:00 quét những người có nguy cơ mất chuỗi
CREATE INDEX idx_streaks_at_risk ON user_streaks (last_activity_date)
    WHERE current_streak > 0;


-- streak_freezes — nhật ký dùng băng, để giải thích được khi user thắc mắc
CREATE TABLE streak_freezes
(
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    used_for_date  DATE        NOT NULL,
    granted_reason VARCHAR(30) NOT NULL DEFAULT 'MONTHLY_GRANT'
        CHECK (granted_reason IN ('MONTHLY_GRANT','ACHIEVEMENT','ADMIN_GIFT')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_streak_freeze UNIQUE (user_id, used_for_date)
);


-- ---------------------------------------------------------------------
-- achievements
-- ---------------------------------------------------------------------
CREATE TABLE achievements
(
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code           VARCHAR(100) NOT NULL UNIQUE,
    name_vi        VARCHAR(150) NOT NULL,
    description_vi TEXT,
    icon_name      VARCHAR(100),
    icon_file_id   UUID REFERENCES file_attachments (id) ON DELETE SET NULL,
    criteria_json  JSONB        NOT NULL,   -- {"type":"streak","value":7}
    display_order  INT          NOT NULL DEFAULT 0,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_achievements
(
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users (id)        ON DELETE CASCADE,
    achievement_id UUID        NOT NULL REFERENCES achievements (id) ON DELETE CASCADE,
    earned_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_achievement UNIQUE (user_id, achievement_id)
);

CREATE INDEX idx_user_achievements_user ON user_achievements (user_id, earned_at DESC);


SELECT attach_updated_at(t) FROM (VALUES
    ('user_lesson_progress'), ('user_course_progress'), ('daily_activity'),
    ('user_streaks'), ('streak_freezes'), ('achievements'), ('user_achievements')
) AS x(t);
