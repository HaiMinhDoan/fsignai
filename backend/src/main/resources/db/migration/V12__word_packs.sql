-- =====================================================================
-- V12 — Gói từ và hành trình luyện tập
-- =====================================================================
-- Bản đồ đảo phiêu lưu ở màn Trang Chủ (Figma 1:1154) cần những chặng
-- NGẮN, mở khoá dần. courses/lessons đã có nhưng là lộ trình dài có bài
-- giảng; gói từ nhẹ hơn hẳn: một nhúm từ cùng nhóm, luyện xong là xong.
--
-- Vì sao không tái dùng lessons: lesson_items đa hình (SIGN/VIDEO/TEXT/
-- QUIZ) và buộc phải thuộc một course. Gói từ thì luôn chỉ chứa từ vựng
-- và phải đứng độc lập được — nhồi vào lessons sẽ phải nới lỏng ràng
-- buộc của cả hai bên.
-- =====================================================================

CREATE TABLE word_packs
(
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(100) NOT NULL UNIQUE,   -- slug, dùng cho URL
    title_vi        VARCHAR(255) NOT NULL,
    description_vi  TEXT,

    cover_file_id   UUID         REFERENCES file_attachments (id) ON DELETE SET NULL,
    topic_id        UUID         REFERENCES topics (id) ON DELETE SET NULL,

    level           VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER'
        CHECK (level IN ('BEGINNER','BASIC','INTERMEDIATE','ADVANCED')),

    -- Màu và icon của hòn đảo trên bản đồ. Để ở CSDL chứ không hardcode
    -- theo thứ tự trong code: biên tập viên thêm gói mới giữa chừng thì
    -- màu của mọi gói phía sau sẽ không bị xáo tung.
    island_color    VARCHAR(20),
    icon_name       VARCHAR(50),

    display_order   INT          NOT NULL DEFAULT 0,

    -- Chặng này chỉ mở khi đã xong chặng kia. NULL = mở sẵn từ đầu.
    -- Tự tham chiếu nên xoá gói cha chỉ gỡ ràng buộc, không xoá lây gói con.
    unlock_after_pack_id UUID    REFERENCES word_packs (id) ON DELETE SET NULL,

    -- Số câu đúng tối thiểu (%) để tính là hoàn thành chặng
    pass_score      INT          NOT NULL DEFAULT 80 CHECK (pass_score BETWEEN 0 AND 100),

    is_published    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by      UUID         REFERENCES users (id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Một gói không thể là điều kiện mở khoá của chính nó
    CONSTRAINT chk_pack_unlock_not_self CHECK (unlock_after_pack_id IS NULL OR unlock_after_pack_id <> id)
);

CREATE INDEX idx_word_packs_published ON word_packs (is_published, display_order)
    WHERE is_published = TRUE;
CREATE INDEX idx_word_packs_topic ON word_packs (topic_id);

SELECT attach_updated_at('word_packs');


-- ---------------------------------------------------------------------
-- word_pack_items — từ vựng trong gói, có thứ tự
-- ---------------------------------------------------------------------
CREATE TABLE word_pack_items
(
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    pack_id       UUID        NOT NULL REFERENCES word_packs (id) ON DELETE CASCADE,
    sign_id       UUID        NOT NULL REFERENCES signs (id) ON DELETE CASCADE,
    display_order INT         NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Một từ chỉ nằm một lần trong cùng gói
    CONSTRAINT uq_pack_item UNIQUE (pack_id, sign_id)
);

CREATE INDEX idx_pack_items_pack ON word_pack_items (pack_id, display_order);
CREATE INDEX idx_pack_items_sign ON word_pack_items (sign_id);

SELECT attach_updated_at('word_pack_items');


-- ---------------------------------------------------------------------
-- user_pack_progress — bé đi tới đâu trong từng gói
--
-- items_total được chụp lại tại thời điểm bắt đầu chứ không đếm sống từ
-- word_pack_items: biên tập viên thêm từ vào gói giữa chừng thì thanh
-- tiến độ của bé đang học sẽ tụt ngược, rất khó hiểu với trẻ con.
-- ---------------------------------------------------------------------
CREATE TABLE user_pack_progress
(
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    pack_id          UUID        NOT NULL REFERENCES word_packs (id) ON DELETE CASCADE,

    status           VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
        CHECK (status IN ('IN_PROGRESS','COMPLETED')),

    items_completed  INT         NOT NULL DEFAULT 0 CHECK (items_completed >= 0),
    items_total      INT         NOT NULL DEFAULT 0 CHECK (items_total >= 0),

    best_score       INT         CHECK (best_score IS NULL OR best_score BETWEEN 0 AND 100),
    -- 0–3 sao như bản đồ game; suy ra từ best_score nhưng lưu lại để
    -- không phải tính lại công thức cũ khi sau này đổi thang điểm
    stars            INT         NOT NULL DEFAULT 0 CHECK (stars BETWEEN 0 AND 3),

    attempts         INT         NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at     TIMESTAMPTZ,
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_user_pack UNIQUE (user_id, pack_id),
    CONSTRAINT chk_pack_done_has_time CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL),
    CONSTRAINT chk_pack_items_le_total CHECK (items_completed <= items_total)
);

CREATE INDEX idx_user_pack_user ON user_pack_progress (user_id, last_activity_at DESC);
CREATE INDEX idx_user_pack_pack ON user_pack_progress (pack_id);

SELECT attach_updated_at('user_pack_progress');

COMMENT ON TABLE word_packs IS
    'Chặng học ngắn gồm một nhóm từ. Nhẹ hơn courses/lessons, dùng cho bản đồ đảo phiêu lưu.';
COMMENT ON COLUMN user_pack_progress.items_total IS
    'Chụp lại lúc bắt đầu, không đếm sống — thêm từ vào gói không được làm tiến độ của bé tụt ngược';
