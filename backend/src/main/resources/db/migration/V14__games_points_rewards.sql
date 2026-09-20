-- =====================================================================
-- V14 — Góc trò chơi, điểm, bảng xếp hạng và rương thưởng
-- =====================================================================
-- Màn "Góc Trò Chơi Ôn Tập Ký Hiệu" (Figma 1:611): 4 module trò chơi,
-- bảng xếp hạng và rương thưởng.
--
-- Bốn game đều chạy trên dữ liệu đã có (3322 từ + ảnh + video), không
-- cần soạn thêm nội dung — nên bảng dưới đây chỉ ghi KẾT QUẢ chơi.
-- =====================================================================

CREATE TABLE game_sessions
(
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    game_code      VARCHAR(30) NOT NULL
        CHECK (game_code IN ('MATCH_PAIR','SPEED_GUESS','MEMORY_FLIP','PICK_SIGN')),

    -- Chơi trong phạm vi nào: một gói từ, một chủ đề, hay cả kho
    pack_id        UUID        REFERENCES word_packs (id) ON DELETE SET NULL,
    topic_id       UUID        REFERENCES topics (id) ON DELETE SET NULL,

    score          INT         NOT NULL DEFAULT 0 CHECK (score >= 0),
    max_score      INT         NOT NULL DEFAULT 0 CHECK (max_score >= 0),
    correct_count  INT         NOT NULL DEFAULT 0 CHECK (correct_count >= 0),
    wrong_count    INT         NOT NULL DEFAULT 0 CHECK (wrong_count >= 0),
    duration_seconds INT       NOT NULL DEFAULT 0 CHECK (duration_seconds >= 0),

    started_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_game_score_le_max CHECK (score <= max_score)
);

CREATE INDEX idx_game_sessions_user ON game_sessions (user_id, created_at DESC);
CREATE INDEX idx_game_sessions_game ON game_sessions (game_code, score DESC);

SELECT attach_updated_at('game_sessions');


-- ---------------------------------------------------------------------
-- point_events — NGUỒN GỐC của mọi điểm
--
-- Cùng triết lý với daily_activity ở V6: điểm là thứ SUY RA từ nhật ký
-- này, không phải một biến đếm tự tăng. Sai sót, gian lận hay đổi công
-- thức tính điểm đều xử lý được bằng cách tính lại từ đây.
-- ---------------------------------------------------------------------
CREATE TABLE point_events
(
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    source      VARCHAR(30) NOT NULL
        CHECK (source IN ('GAME','QUIZ','PACK','LESSON','STREAK','DAILY_QUEST','AI_CHECK','MANUAL')),
    source_id   UUID,       -- id của phiên chơi / lượt thi / gói từ tương ứng

    points      INT         NOT NULL,   -- cho phép ÂM để trừ điểm khi cần sửa sai
    note_vi     VARCHAR(255),

    -- Ngày theo múi giờ người dùng, giống daily_activity.activity_date —
    -- có cột này thì tổng điểm tuần/tháng tính được mà không phải đổi
    -- múi giờ lại từ đầu mỗi lần truy vấn
    earned_date DATE        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_point_events_user ON point_events (user_id, earned_date DESC);
CREATE INDEX idx_point_events_source ON point_events (source, source_id);


-- ---------------------------------------------------------------------
-- user_points — bản tổng hợp để xếp hạng cho nhanh
--
-- Là BỘ NHỚ ĐỆM của point_events, không phải sự thật gốc. Lệch thì tính
-- lại từ point_events, đúng như cách user_streaks dựa vào daily_activity.
-- ---------------------------------------------------------------------
CREATE TABLE user_points
(
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,

    total_points   INT         NOT NULL DEFAULT 0,
    weekly_points  INT         NOT NULL DEFAULT 0,
    monthly_points INT         NOT NULL DEFAULT 0,

    -- Mốc của kỳ đang cộng dồn; sang kỳ mới thì reset điểm kỳ về 0
    week_start_date  DATE,
    month_start_date DATE,

    level          INT         NOT NULL DEFAULT 1 CHECK (level >= 1),

    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Bảng xếp hạng: xếp theo điểm, không cần bảng riêng cho thứ hạng
CREATE INDEX idx_user_points_total ON user_points (total_points DESC);
CREATE INDEX idx_user_points_weekly ON user_points (weekly_points DESC);

SELECT attach_updated_at('user_points');


-- ---------------------------------------------------------------------
-- reward_chests — rương thưởng mở theo mốc điểm
-- ---------------------------------------------------------------------
CREATE TABLE reward_chests
(
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(50)  NOT NULL UNIQUE,
    name_vi         VARCHAR(150) NOT NULL,
    description_vi  TEXT,
    icon_file_id    UUID         REFERENCES file_attachments (id) ON DELETE SET NULL,
    icon_name       VARCHAR(50),

    required_points INT          NOT NULL CHECK (required_points >= 0),
    display_order   INT          NOT NULL DEFAULT 0,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

SELECT attach_updated_at('reward_chests');


CREATE TABLE user_reward_chests
(
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    chest_id   UUID        NOT NULL REFERENCES reward_chests (id) ON DELETE CASCADE,
    opened_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_user_chest UNIQUE (user_id, chest_id)
);

CREATE INDEX idx_user_chests_user ON user_reward_chests (user_id, opened_at DESC);


-- ---------------------------------------------------------------------
-- Rương mặc định. Mốc điểm thưa dần: rương đầu phải mở được ngay trong
-- buổi học đầu tiên, nếu không bé sẽ không tin là mình mở được cái nào.
-- ---------------------------------------------------------------------
INSERT INTO reward_chests (code, name_vi, description_vi, icon_name, required_points, display_order)
VALUES
    ('CHEST_FIRST',  'Rương Khởi Đầu',   'Mở ngay khi bé chơi xong ván đầu tiên.', 'gift',    50,   1),
    ('CHEST_BRONZE', 'Rương Đồng',       'Bé đã chăm chỉ được một tuần rồi!',      'medal',   300,  2),
    ('CHEST_SILVER', 'Rương Bạc',        'Kho từ của bé đang lớn lên mỗi ngày.',   'star',    1000, 3),
    ('CHEST_GOLD',   'Rương Vàng',       'Bé đã là bạn thân của ngôn ngữ ký hiệu.', 'trophy', 3000, 4);

COMMENT ON TABLE point_events IS
    'Nhật ký gốc của điểm. user_points chỉ là bộ nhớ đệm, tính lại được từ bảng này.';
COMMENT ON TABLE game_sessions IS
    'Kết quả từng ván của 4 trò chơi ôn tập. Bốn game chạy trên kho từ sẵn có, không cần soạn nội dung riêng.';
