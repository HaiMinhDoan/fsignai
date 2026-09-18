-- =====================================================================
-- V5 — Flashcard (lặp lại ngắt quãng), Quiz cố định, Quiz sinh đề trộn
-- =====================================================================

-- ---------------------------------------------------------------------
-- flashcard_reviews — thuật toán SM-2
-- Nút "Cần luyện thêm" phải thực sự đổi lịch ôn, nếu không nó chỉ là
-- nút trang trí và người học không được lợi gì.
-- ---------------------------------------------------------------------
CREATE TABLE flashcard_reviews
(
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sign_id        UUID         NOT NULL REFERENCES signs (id) ON DELETE CASCADE,
    ease_factor    NUMERIC(4,2) NOT NULL DEFAULT 2.50 CHECK (ease_factor >= 1.30),
    interval_days  INT          NOT NULL DEFAULT 0 CHECK (interval_days >= 0),
    repetitions    INT          NOT NULL DEFAULT 0 CHECK (repetitions >= 0),
    lapses         INT          NOT NULL DEFAULT 0,
    due_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_result    VARCHAR(20)
        CHECK (last_result IS NULL OR last_result IN ('KNOWN','NEEDS_PRACTICE')),
    last_reviewed_at TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_flashcard_user_sign UNIQUE (user_id, sign_id)
);

-- Index quan trọng nhất về hiệu năng: chạy mỗi lần người dùng mở mục luyện tập
CREATE INDEX idx_flashcard_due ON flashcard_reviews (user_id, due_at);


-- ---------------------------------------------------------------------
-- quizzes — ĐỀ CỐ ĐỊNH do admin soạn tay
-- ---------------------------------------------------------------------
CREATE TABLE quizzes
(
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id         UUID REFERENCES lessons (id) ON DELETE CASCADE,
    topic_id          UUID REFERENCES topics (id)  ON DELETE SET NULL,
    title_vi          VARCHAR(200) NOT NULL,
    description_vi    TEXT,
    quiz_type         VARCHAR(20)  NOT NULL DEFAULT 'LESSON_QUIZ'
        CHECK (quiz_type IN ('LESSON_QUIZ','TOPIC_TEST','PRACTICE')),
    pass_score        INT          NOT NULL DEFAULT 70 CHECK (pass_score BETWEEN 0 AND 100),
    time_limit_seconds INT CHECK (time_limit_seconds IS NULL OR time_limit_seconds > 0),
    is_published      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_quizzes_lesson ON quizzes (lesson_id);

-- Bổ sung FK đã treo từ V4
ALTER TABLE lesson_items
    ADD CONSTRAINT fk_lesson_items_quiz
        FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE SET NULL;


-- ---------------------------------------------------------------------
-- quiz_questions — câu hỏi của đề cố định
--
-- AI_PERFORM là cầu nối sang FastAPI: người dùng tự làm ký hiệu trước
-- webcam thay vì chọn đáp án. Ở giai đoạn đầu (mức A) dạng câu này
-- KHÔNG tính điểm, chỉ dùng để luyện tập.
-- ---------------------------------------------------------------------
CREATE TABLE quiz_questions
(
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id              UUID        NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    question_type        VARCHAR(30) NOT NULL
        CHECK (question_type IN
            ('VIDEO_TO_WORD','WORD_TO_VIDEO','MULTIPLE_CHOICE','MATCHING','AI_PERFORM')),
    sign_id              UUID        NOT NULL REFERENCES signs (id) ON DELETE CASCADE,
    prompt_vi            TEXT,
    options_json         JSONB       NOT NULL DEFAULT '[]'::jsonb,
    correct_option_index INT,
    points               INT         NOT NULL DEFAULT 1 CHECK (points > 0),
    display_order        INT         NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Câu trắc nghiệm bắt buộc có đáp án đúng; câu AI_PERFORM thì không
    CONSTRAINT chk_quiz_question_answer CHECK (
        (question_type = 'AI_PERFORM' AND correct_option_index IS NULL)
        OR (question_type <> 'AI_PERFORM' AND correct_option_index IS NOT NULL)
    )
);

CREATE INDEX idx_quiz_questions_quiz ON quiz_questions (quiz_id, display_order);


-- ---------------------------------------------------------------------
-- quiz_blueprints — ĐỀ SINH TỰ ĐỘNG: khai báo luật, không soạn từng câu
-- Người dùng chọn chủ đề + vùng miền, hệ thống rút ngẫu nhiên từ kho.
-- ---------------------------------------------------------------------
CREATE TABLE quiz_blueprints
(
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(100) NOT NULL UNIQUE,
    title_vi            VARCHAR(200) NOT NULL,
    description_vi      TEXT,
    topic_ids           JSONB        NOT NULL DEFAULT '[]'::jsonb,
    levels              JSONB        NOT NULL DEFAULT '[]'::jsonb,
    unit_types          JSONB        NOT NULL DEFAULT '[]'::jsonb,
    word_types          JSONB,       -- NULL = không giới hạn từ loại
    question_type_mix   JSONB        NOT NULL
        DEFAULT '{"VIDEO_TO_WORD":5,"WORD_TO_VIDEO":3,"MATCHING":2}'::jsonb,
    question_count      INT          NOT NULL DEFAULT 10 CHECK (question_count BETWEEN 1 AND 100),
    option_count        INT          NOT NULL DEFAULT 4  CHECK (option_count BETWEEN 2 AND 8),
    pass_score          INT          NOT NULL DEFAULT 70 CHECK (pass_score BETWEEN 0 AND 100),
    time_limit_seconds  INT CHECK (time_limit_seconds IS NULL OR time_limit_seconds > 0),
    distractor_strategy VARCHAR(30)  NOT NULL DEFAULT 'EASILY_CONFUSED'
        CHECK (distractor_strategy IN ('SAME_TOPIC','EASILY_CONFUSED','MIXED')),
    -- Tránh gặp lại cùng nhóm từ khi thi nhiều lần
    avoid_recent_days   INT          NOT NULL DEFAULT 30 CHECK (avoid_recent_days >= 0),
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by          UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);


-- ---------------------------------------------------------------------
-- quiz_attempts
--
-- generated_questions lưu ẢNH CHỤP của đề đã sinh, không chỉ lưu seed.
-- Nếu chỉ lưu seed rồi sinh lại khi xem lại bài, chỉ cần một từ bị sửa
-- hoặc gỡ xuất bản là đề tái tạo ra khác với đề người dùng đã làm —
-- điểm số sẽ không khớp câu hỏi và không ai giải thích được.
-- ---------------------------------------------------------------------
CREATE TABLE quiz_attempts
(
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    quiz_id             UUID REFERENCES quizzes (id)         ON DELETE SET NULL,
    blueprint_id        UUID REFERENCES quiz_blueprints (id) ON DELETE SET NULL,
    region              VARCHAR(20) NOT NULL DEFAULT 'COMMON'
        CHECK (region IN ('NORTH','CENTRAL','SOUTH','COMMON')),
    generated_questions JSONB,
    seed                BIGINT,
    score               INT         NOT NULL DEFAULT 0,
    max_score           INT         NOT NULL DEFAULT 0,
    passed              BOOLEAN     NOT NULL DEFAULT FALSE,
    status              VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
        CHECK (status IN ('IN_PROGRESS','SUBMITTED','EXPIRED','ABANDONED')),
    started_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at        TIMESTAMPTZ,
    duration_seconds    INT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Một lần làm bài phải thuộc về đúng một nguồn đề
    CONSTRAINT chk_attempt_source CHECK (
        (quiz_id IS NOT NULL AND blueprint_id IS NULL)
        OR (quiz_id IS NULL AND blueprint_id IS NOT NULL)
    )
);

CREATE INDEX idx_quiz_attempts_user ON quiz_attempts (user_id, submitted_at DESC);
CREATE INDEX idx_quiz_attempts_open ON quiz_attempts (user_id, status)
    WHERE status = 'IN_PROGRESS';


CREATE TABLE quiz_answers
(
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id           UUID        NOT NULL REFERENCES quiz_attempts (id) ON DELETE CASCADE,
    question_id          UUID REFERENCES quiz_questions (id) ON DELETE SET NULL,
    question_index       INT,        -- vị trí trong generated_questions (đề sinh)
    sign_id              UUID REFERENCES signs (id) ON DELETE SET NULL,
    selected_option_index INT,
    is_correct           BOOLEAN     NOT NULL DEFAULT FALSE,
    ai_check_result_id   UUID,       -- FK thêm ở V7
    points_earned        INT         NOT NULL DEFAULT 0,
    answered_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_answer_ref CHECK (question_id IS NOT NULL OR question_index IS NOT NULL)
);

CREATE INDEX idx_quiz_answers_attempt ON quiz_answers (attempt_id);
-- Phục vụ luật "tránh trùng từ giữa các lần thi" của quiz_blueprints
CREATE INDEX idx_quiz_answers_sign_recent ON quiz_answers (sign_id, answered_at DESC)
    WHERE sign_id IS NOT NULL;


-- ---------------------------------------------------------------------
-- matching_sessions — bài ghép cặp
-- ---------------------------------------------------------------------
CREATE TABLE matching_sessions
(
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL REFERENCES users (id)  ON DELETE CASCADE,
    topic_id      UUID REFERENCES topics (id) ON DELETE SET NULL,
    region        VARCHAR(20) NOT NULL DEFAULT 'COMMON'
        CHECK (region IN ('NORTH','CENTRAL','SOUTH','COMMON')),
    pair_count    INT         NOT NULL DEFAULT 8 CHECK (pair_count BETWEEN 2 AND 30),
    pairs_json    JSONB       NOT NULL DEFAULT '[]'::jsonb,
    correct_count INT         NOT NULL DEFAULT 0,
    attempt_count INT         NOT NULL DEFAULT 0,
    duration_seconds INT,
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_matching_sessions_user ON matching_sessions (user_id, created_at DESC);


SELECT attach_updated_at(t) FROM (VALUES
    ('flashcard_reviews'), ('quizzes'), ('quiz_questions'), ('quiz_blueprints'),
    ('quiz_attempts'), ('quiz_answers'), ('matching_sessions')
) AS x(t);
