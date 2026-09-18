-- =====================================================================
-- V8 — Diễn đàn: bài viết, bình luận, BÌNH LUẬN BẰNG VIDEO, kiểm duyệt
--
-- Bình luận video không phải tính năng phụ. Với nhiều người điếc, tiếng
-- Việt viết là ngôn ngữ thứ hai — bắt họ gõ chữ để tham gia cộng đồng
-- chính là dựng lại đúng rào cản mà sản phẩm này tồn tại để gỡ.
-- =====================================================================

CREATE TABLE forum_categories
(
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    slug           VARCHAR(120) NOT NULL UNIQUE,
    name_vi        VARCHAR(150) NOT NULL,
    description_vi TEXT,
    icon_name      VARCHAR(100),
    display_order  INT          NOT NULL DEFAULT 0,
    is_locked      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_published   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);


-- ---------------------------------------------------------------------
-- media_assets — video/ảnh do NGƯỜI DÙNG tải lên.
-- Tách hẳn khỏi sign_videos (nội dung biên tập) vì vòng đời khác nhau:
-- media của người dùng cần transcode, kiểm duyệt và có thể bị gỡ.
-- File thật vẫn nằm ở file_attachments.
-- ---------------------------------------------------------------------
CREATE TABLE media_assets
(
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id          UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    kind              VARCHAR(20) NOT NULL DEFAULT 'VIDEO'
        CHECK (kind IN ('VIDEO','IMAGE')),
    source            VARCHAR(20) NOT NULL DEFAULT 'FILE_UPLOAD'
        CHECK (source IN ('WEBCAM_RECORDED','FILE_UPLOAD')),

    original_file_id  UUID REFERENCES file_attachments (id) ON DELETE SET NULL,
    -- Bản đã transcode về H.264 720p để phát được trên mọi trình duyệt
    playback_file_id  UUID REFERENCES file_attachments (id) ON DELETE SET NULL,
    thumbnail_file_id UUID REFERENCES file_attachments (id) ON DELETE SET NULL,

    duration_ms       INT CHECK (duration_ms IS NULL OR duration_ms > 0),
    width             INT,
    height            INT,
    size_bytes        BIGINT,
    mime_type         VARCHAR(100),

    transcode_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (transcode_status IN ('PENDING','PROCESSING','READY','FAILED')),
    transcode_error   TEXT,

    moderation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (moderation_status IN ('PENDING','APPROVED','REJECTED')),
    moderated_by      UUID REFERENCES users (id) ON DELETE SET NULL,
    moderated_at      TIMESTAMPTZ,
    moderation_note   TEXT,

    -- Phụ đề BẮT BUỘC khi đăng video: không có mô tả chữ thì người khiếm
    -- thị, người chưa biết ký hiệu đó, và cả công cụ tìm kiếm đều không
    -- đọc được. Đây cũng là thứ biến diễn đàn thành nguồn tra cứu được.
    caption_vi        TEXT        NOT NULL DEFAULT '',

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Phụ đề được ép ở tầng API lúc ĐĂNG, không ép bằng CHECK ở đây:
-- bản ghi media_assets phải tạo ngay khi bắt đầu tải lên để worker
-- transcode có việc mà làm, lúc đó người dùng chưa kịp nhập phụ đề.
-- Chặn ở DB sẽ làm hỏng luồng upload.
COMMENT ON COLUMN media_assets.caption_vi IS
    'Phụ đề/mô tả. API từ chối gắn media VIDEO vào bài hoặc bình luận nếu còn rỗng.';

CREATE INDEX idx_media_owner      ON media_assets (owner_id, created_at DESC);
CREATE INDEX idx_media_moderation ON media_assets (moderation_status, created_at)
    WHERE moderation_status = 'PENDING';
CREATE INDEX idx_media_transcode  ON media_assets (transcode_status)
    WHERE transcode_status IN ('PENDING','PROCESSING','FAILED');


-- ---------------------------------------------------------------------
-- forum_posts
-- ---------------------------------------------------------------------
CREATE TABLE forum_posts
(
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id    UUID         NOT NULL REFERENCES forum_categories (id) ON DELETE RESTRICT,
    author_id      UUID         NOT NULL REFERENCES users (id)            ON DELETE CASCADE,
    title_vi       VARCHAR(255) NOT NULL,
    body_md        TEXT,        -- có thể rỗng nếu bài chỉ có video
    sign_id        UUID REFERENCES signs (id) ON DELETE SET NULL,

    view_count     INT          NOT NULL DEFAULT 0,
    comment_count  INT          NOT NULL DEFAULT 0,
    reaction_count INT          NOT NULL DEFAULT 0,
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    is_pinned      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_locked      BOOLEAN      NOT NULL DEFAULT FALSE,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING_REVIEW'
        CHECK (status IN ('DRAFT','PENDING_REVIEW','PUBLISHED','HIDDEN','REMOVED')),
    published_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    search_vector  TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('simple',
            f_unaccent(coalesce(title_vi, '')) || ' ' ||
            coalesce(title_vi, '')             || ' ' ||
            f_unaccent(coalesce(body_md, ''))
        )
    ) STORED
);

CREATE INDEX idx_forum_posts_feed
    ON forum_posts (category_id, is_pinned DESC, last_activity_at DESC)
    WHERE status = 'PUBLISHED';
CREATE INDEX idx_forum_posts_author ON forum_posts (author_id, created_at DESC);
CREATE INDEX idx_forum_posts_search ON forum_posts USING GIN (search_vector);
CREATE INDEX idx_forum_posts_review ON forum_posts (status, created_at)
    WHERE status = 'PENDING_REVIEW';


-- ---------------------------------------------------------------------
-- forum_comments — trả lời lồng tối đa 2 cấp
-- ---------------------------------------------------------------------
CREATE TABLE forum_comments
(
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id        UUID        NOT NULL REFERENCES forum_posts (id)    ON DELETE CASCADE,
    author_id      UUID        NOT NULL REFERENCES users (id)          ON DELETE CASCADE,
    parent_id      UUID        REFERENCES forum_comments (id)          ON DELETE CASCADE,
    depth          SMALLINT    NOT NULL DEFAULT 0 CHECK (depth IN (0, 1)),
    body_text      TEXT,       -- có thể rỗng nếu bình luận chỉ có video
    reaction_count INT         NOT NULL DEFAULT 0,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW'
        CHECK (status IN ('PENDING_REVIEW','PUBLISHED','HIDDEN','REMOVED')),
    edited_at      TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_comment_depth_parent
        CHECK ((depth = 0 AND parent_id IS NULL) OR (depth = 1 AND parent_id IS NOT NULL))
);

CREATE INDEX idx_forum_comments_post   ON forum_comments (post_id, created_at)
    WHERE status = 'PUBLISHED';
CREATE INDEX idx_forum_comments_parent ON forum_comments (parent_id)
    WHERE parent_id IS NOT NULL;
CREATE INDEX idx_forum_comments_review ON forum_comments (status, created_at)
    WHERE status = 'PENDING_REVIEW';


CREATE TABLE forum_post_media
(
    post_id       UUID NOT NULL REFERENCES forum_posts (id)   ON DELETE CASCADE,
    media_id      UUID NOT NULL REFERENCES media_assets (id)  ON DELETE CASCADE,
    display_order INT  NOT NULL DEFAULT 0,
    PRIMARY KEY (post_id, media_id)
);

CREATE TABLE forum_comment_media
(
    comment_id    UUID NOT NULL REFERENCES forum_comments (id) ON DELETE CASCADE,
    media_id      UUID NOT NULL REFERENCES media_assets (id)   ON DELETE CASCADE,
    display_order INT  NOT NULL DEFAULT 0,
    PRIMARY KEY (comment_id, media_id)
);


-- ---------------------------------------------------------------------
-- forum_reactions — dùng bảng chung cho cả bài và bình luận
-- ---------------------------------------------------------------------
CREATE TABLE forum_reactions
(
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('POST','COMMENT')),
    target_id   UUID        NOT NULL,
    reaction    VARCHAR(20) NOT NULL DEFAULT 'LIKE'
        CHECK (reaction IN ('LIKE','HELPFUL','THANKS')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_forum_reaction UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_forum_reactions_target ON forum_reactions (target_type, target_id);


-- ---------------------------------------------------------------------
-- forum_reports — báo cáo vi phạm.
-- Bắt buộc phải có: nội dung video do người dùng tạo, trong sản phẩm
-- phục vụ cả học sinh tiểu học điếc.
-- ---------------------------------------------------------------------
CREATE TABLE forum_reports
(
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('POST','COMMENT','MEDIA','USER')),
    target_id   UUID        NOT NULL,
    reason      VARCHAR(30) NOT NULL
        CHECK (reason IN ('SPAM','ABUSE','WRONG_SIGN','OFF_TOPIC','SENSITIVE','OTHER')),
    note        TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN','REVIEWING','RESOLVED','DISMISSED')),
    handled_by  UUID REFERENCES users (id) ON DELETE SET NULL,
    handled_at  TIMESTAMPTZ,
    handler_note TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_forum_reports_open   ON forum_reports (status, created_at)
    WHERE status IN ('OPEN','REVIEWING');
CREATE INDEX idx_forum_reports_target ON forum_reports (target_type, target_id);


CREATE TABLE forum_subscriptions
(
    user_id    UUID        NOT NULL REFERENCES users (id)       ON DELETE CASCADE,
    post_id    UUID        NOT NULL REFERENCES forum_posts (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, post_id)
);


SELECT attach_updated_at(t) FROM (VALUES
    ('forum_categories'), ('media_assets'), ('forum_posts'), ('forum_comments'),
    ('forum_reactions'), ('forum_reports')
) AS x(t);
