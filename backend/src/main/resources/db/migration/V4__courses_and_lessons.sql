-- =====================================================================
-- V4 — Khoá học, bài học, lộ trình học cá nhân hoá
-- =====================================================================

CREATE TABLE courses
(
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    slug           VARCHAR(120) NOT NULL UNIQUE,
    title_vi       VARCHAR(200) NOT NULL,
    description_vi TEXT,
    cover_file_id  UUID REFERENCES file_attachments (id) ON DELETE SET NULL,
    topic_id       UUID REFERENCES topics (id) ON DELETE SET NULL,
    level          VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER'
        CHECK (level IN ('BEGINNER','BASIC','INTERMEDIATE','ADVANCED')),
    display_order  INT          NOT NULL DEFAULT 0,
    -- Khoá sinh tự động từ việc gom từ vựng theo chủ đề + cấp độ
    generated      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_published   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_courses_level ON courses (level, display_order) WHERE is_published;


CREATE TABLE lessons
(
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id         UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    title_vi          VARCHAR(200) NOT NULL,
    description_vi    TEXT,
    display_order     INT          NOT NULL DEFAULT 0,
    estimated_minutes INT          NOT NULL DEFAULT 5 CHECK (estimated_minutes > 0),
    generated         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_published      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_lessons_course ON lessons (course_id, display_order);


-- lesson_items — nội dung bên trong bài học, sắp thứ tự kéo–thả
CREATE TABLE lesson_items
(
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id     UUID        NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    item_type     VARCHAR(20) NOT NULL
        CHECK (item_type IN ('SIGN','VIDEO','TEXT','PRACTICE','QUIZ')),
    sign_id       UUID REFERENCES signs (id) ON DELETE CASCADE,
    quiz_id       UUID,   -- FK thêm ở V5, sau khi bảng quizzes tồn tại
    content_json  JSONB,  -- nội dung tự do cho TEXT / VIDEO
    display_order INT         NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Item kiểu SIGN bắt buộc phải trỏ tới một từ vựng
    CONSTRAINT chk_lesson_item_sign
        CHECK (item_type <> 'SIGN' OR sign_id IS NOT NULL)
);

CREATE INDEX idx_lesson_items_lesson ON lesson_items (lesson_id, display_order);
CREATE INDEX idx_lesson_items_sign   ON lesson_items (sign_id) WHERE sign_id IS NOT NULL;


-- ---------------------------------------------------------------------
-- learning_paths — sinh từ câu trả lời onboarding
-- ---------------------------------------------------------------------
CREATE TABLE learning_paths
(
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name           VARCHAR(200) NOT NULL DEFAULT 'Lộ trình của tôi',
    generated_from VARCHAR(20)  NOT NULL DEFAULT 'ONBOARDING'
        CHECK (generated_from IN ('ONBOARDING','MANUAL','ADMIN')),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Mỗi người chỉ có một lộ trình đang hoạt động
CREATE UNIQUE INDEX uq_learning_path_active
    ON learning_paths (user_id) WHERE is_active;


CREATE TABLE learning_path_items
(
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    learning_path_id UUID        NOT NULL REFERENCES learning_paths (id) ON DELETE CASCADE,
    course_id        UUID        NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    display_order    INT         NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_learning_path_course UNIQUE (learning_path_id, course_id)
);

CREATE INDEX idx_learning_path_items ON learning_path_items (learning_path_id, display_order);


SELECT attach_updated_at(t) FROM (VALUES
    ('courses'), ('lessons'), ('lesson_items'),
    ('learning_paths'), ('learning_path_items')
) AS x(t);
