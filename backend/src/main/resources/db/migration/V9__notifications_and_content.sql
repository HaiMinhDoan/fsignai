-- =====================================================================
-- V9 — Thông báo, Web Push, nội dung tĩnh (blog / our mission)
-- =====================================================================

CREATE TABLE notification_templates
(
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(100) NOT NULL UNIQUE,
    type          VARCHAR(30)  NOT NULL
        CHECK (type IN ('LESSON_REMINDER','STREAK_WARNING','STREAK_LOST','NEW_COURSE',
                        'ACHIEVEMENT','FORUM_REPLY','MODERATION_RESULT','ROLE_VERIFIED','SYSTEM')),
    channel       VARCHAR(20)  NOT NULL
        CHECK (channel IN ('IN_APP','EMAIL','PUSH')),
    subject_vi    VARCHAR(255),
    body_template TEXT         NOT NULL,   -- placeholder dạng {{fullName}}, {{streak}}
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_notification_template UNIQUE (type, channel)
);


CREATE TABLE notifications
(
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        VARCHAR(30)  NOT NULL
        CHECK (type IN ('LESSON_REMINDER','STREAK_WARNING','STREAK_LOST','NEW_COURSE',
                        'ACHIEVEMENT','FORUM_REPLY','MODERATION_RESULT','ROLE_VERIFIED','SYSTEM')),
    channel     VARCHAR(20)  NOT NULL DEFAULT 'IN_APP'
        CHECK (channel IN ('IN_APP','EMAIL','PUSH')),
    title_vi    VARCHAR(255) NOT NULL,
    body_vi     TEXT,
    action_url  VARCHAR(500),
    payload     JSONB,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','SENT','FAILED','SKIPPED')),
    error_message TEXT,
    sent_at     TIMESTAMPTZ,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_unread ON notifications (user_id, created_at DESC)
    WHERE read_at IS NULL;
CREATE INDEX idx_notifications_queue  ON notifications (status, created_at)
    WHERE status = 'PENDING';


CREATE TABLE push_subscriptions
(
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    endpoint   TEXT        NOT NULL,
    p256dh     VARCHAR(255) NOT NULL,
    auth       VARCHAR(255) NOT NULL,
    user_agent VARCHAR(255),
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_push_endpoint UNIQUE (endpoint)
);

CREATE INDEX idx_push_user ON push_subscriptions (user_id) WHERE is_active;


-- ---------------------------------------------------------------------
-- blog_posts — "blog info VSL", "our mission" ở trang chủ
-- ---------------------------------------------------------------------
CREATE TABLE blog_posts
(
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    slug          VARCHAR(160) NOT NULL UNIQUE,
    title_vi      VARCHAR(255) NOT NULL,
    excerpt_vi    TEXT,
    content_md    TEXT         NOT NULL,
    cover_file_id UUID REFERENCES file_attachments (id) ON DELETE SET NULL,
    author_id     UUID REFERENCES users (id) ON DELETE SET NULL,
    category      VARCHAR(30)  NOT NULL DEFAULT 'BLOG'
        CHECK (category IN ('BLOG','MISSION','GUIDE','NEWS')),
    view_count    INT          NOT NULL DEFAULT 0,
    is_published  BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('simple',
            f_unaccent(coalesce(title_vi, ''))   || ' ' ||
            coalesce(title_vi, '')               || ' ' ||
            f_unaccent(coalesce(excerpt_vi, ''))
        )
    ) STORED
);

CREATE INDEX idx_blog_published ON blog_posts (category, published_at DESC)
    WHERE is_published;
CREATE INDEX idx_blog_search    ON blog_posts USING GIN (search_vector);


-- ---------------------------------------------------------------------
-- system_settings — cấu hình chỉnh được mà không cần deploy lại
-- ---------------------------------------------------------------------
CREATE TABLE system_settings
(
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    key         VARCHAR(120) NOT NULL UNIQUE,
    value       JSONB        NOT NULL,
    description TEXT,
    updated_by  UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);


-- ---------------------------------------------------------------------
-- job_executions — theo dõi job nền (ShedLock chống chạy trùng khi scale)
-- ---------------------------------------------------------------------
CREATE TABLE job_executions
(
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    job_name     VARCHAR(120) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'RUNNING'
        CHECK (status IN ('RUNNING','SUCCESS','FAILED')),
    processed    INT         NOT NULL DEFAULT 0,
    failed       INT         NOT NULL DEFAULT 0,
    detail       JSONB,
    error_message TEXT,
    started_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_job_executions_name ON job_executions (job_name, started_at DESC);

-- Bảng khoá của ShedLock (schema do thư viện quy định, không đổi tên cột)
CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);


SELECT attach_updated_at(t) FROM (VALUES
    ('notification_templates'), ('notifications'), ('push_subscriptions'),
    ('blog_posts'), ('system_settings'), ('job_executions')
) AS x(t);
