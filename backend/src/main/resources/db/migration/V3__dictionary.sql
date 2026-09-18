-- =====================================================================
-- V3 — Từ điển VSL: chủ đề, từ vựng, video, quan hệ từ
-- Đây là trung tâm của hệ thống: bài học, flashcard, quiz, AI checking
-- đều trỏ về bảng signs.
-- =====================================================================

-- ---------------------------------------------------------------------
-- topics — cây chủ đề, cho phép chủ đề con
-- ---------------------------------------------------------------------
CREATE TABLE topics
(
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id      UUID REFERENCES topics (id) ON DELETE SET NULL,
    slug           VARCHAR(120) NOT NULL UNIQUE,
    name_vi        VARCHAR(150) NOT NULL,
    description_vi TEXT,
    icon_name      VARCHAR(100),   -- tên icon, đổi bộ icon không phải sửa dữ liệu
    icon_file_id   UUID REFERENCES file_attachments (id) ON DELETE SET NULL,
    cover_file_id  UUID REFERENCES file_attachments (id) ON DELETE SET NULL,
    category       VARCHAR(30)  NOT NULL DEFAULT 'SIMPLE_SIGN'
        CHECK (category IN ('SIMPLE_SIGN','COMPLEX_SIGN','SITUATION')),
    display_order  INT          NOT NULL DEFAULT 0,
    is_published   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_topic_not_self_parent CHECK (parent_id IS DISTINCT FROM id)
);

CREATE INDEX idx_topics_parent ON topics (parent_id, display_order);


-- ---------------------------------------------------------------------
-- signs — một mục từ vựng VSL
--
-- Bốn trục phân loại độc lập, phục vụ bốn việc khác nhau:
--   unit_type  → lọc bài học (bài đánh vần chỉ lấy LETTER)
--   word_type  → từ loại tiếng Việt, dạy ngữ pháp và lọc bài tập
--   domain     → gom khoá chuyên ngành (VSL y tế, VSL trường học)
--   sign_topics→ chủ đề ngữ nghĩa, là điều hướng chính của người dùng
-- ---------------------------------------------------------------------
CREATE TABLE signs
(
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    gloss              VARCHAR(150) NOT NULL UNIQUE,   -- 'ME', 'CHA', 'GIA_DINH'
    word_vi            VARCHAR(255) NOT NULL,
    word_en            VARCHAR(255),
    description_vi     TEXT,        -- mô tả cách làm ký hiệu bằng chữ (bắt buộc cho a11y)
    note_vi            TEXT,

    level              VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER'
        CHECK (level IN ('BEGINNER','BASIC','INTERMEDIATE','ADVANCED')),

    unit_type          VARCHAR(20)  NOT NULL DEFAULT 'WORD'
        CHECK (unit_type IN ('LETTER','NUMBER','WORD','PHRASE','SENTENCE')),

    -- Từ loại theo ngữ pháp tiếng Việt phổ thông
    word_type          VARCHAR(30)  NOT NULL DEFAULT 'KHONG_XAC_DINH'
        CHECK (word_type IN (
            'DANH_TU','DONG_TU','TINH_TU','SO_TU','DAI_TU',
            'PHO_TU','QUAN_HE_TU','LUONG_TU','CHI_TU','TRO_TU',
            'TINH_THAI_TU','THAN_TU','KHONG_XAC_DINH')),
    word_subtype       VARCHAR(30)
        CHECK (word_subtype IS NULL OR word_subtype IN ('CHUNG','RIENG','DON_VI')),

    domain             VARCHAR(30)
        CHECK (domain IS NULL OR domain IN
            ('MATH','GEOGRAPHY','COUNTRY','MEDICAL','LEGAL','SCHOOL','IT','SPORT','RELIGION')),

    primary_topic_id   UUID REFERENCES topics (id) ON DELETE SET NULL,

    -- Số tay dùng khi thực hiện ký hiệu; dùng để chọn ngưỡng DTW mặc định
    hand_count         SMALLINT     NOT NULL DEFAULT 1 CHECK (hand_count IN (1, 2)),

    -- Nguồn gốc dữ liệu, phục vụ ghi công và truy vết bản quyền
    source             VARCHAR(30)  NOT NULL DEFAULT 'MANUAL'
        CHECK (source IN ('MOET_QIPEDC','SELF_RECORDED','IMPORTED','MANUAL')),
    source_ref         VARCHAR(255),   -- mã video gốc, ví dụ 'W00665'

    is_published       BOOLEAN      NOT NULL DEFAULT FALSE,
    review_status      VARCHAR(30)  NOT NULL DEFAULT 'UNREVIEWED'
        CHECK (review_status IN ('UNREVIEWED','APPROVED','NEEDS_FIX')),
    reviewed_by        UUID REFERENCES users (id) ON DELETE SET NULL,
    reviewed_at        TIMESTAMPTZ,

    created_by         UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Cột tìm kiếm: dựng sẵn ở DB nên không bao giờ lệch với dữ liệu
    word_vi_unaccent   TEXT GENERATED ALWAYS AS (lower(f_unaccent(word_vi))) STORED,
    search_vector      TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('simple',
            f_unaccent(coalesce(word_vi, '')) || ' ' ||
            coalesce(word_vi, '')            || ' ' ||
            coalesce(word_en, '')            || ' ' ||
            f_unaccent(coalesce(description_vi, ''))
        )
    ) STORED
);

-- Người dùng gõ "me" phải ra "mẹ"
CREATE INDEX idx_signs_search      ON signs USING GIN (search_vector);
CREATE INDEX idx_signs_unaccent_trgm ON signs USING GIN (word_vi_unaccent gin_trgm_ops);
CREATE INDEX idx_signs_level_topic ON signs (level, primary_topic_id) WHERE is_published;
CREATE INDEX idx_signs_unit_type   ON signs (unit_type, level)        WHERE is_published;
CREATE INDEX idx_signs_word_type   ON signs (word_type)               WHERE is_published;
CREATE INDEX idx_signs_domain      ON signs (domain)                  WHERE domain IS NOT NULL;
CREATE INDEX idx_signs_source_ref  ON signs (source_ref)              WHERE source_ref IS NOT NULL;

COMMENT ON COLUMN signs.description_vi IS
    'Mô tả cách làm ký hiệu bằng chữ. Bắt buộc cho người khiếm thị và cho SEO.';


-- ---------------------------------------------------------------------
-- sign_topics — một từ thuộc nhiều chủ đề
-- ---------------------------------------------------------------------
CREATE TABLE sign_topics
(
    sign_id  UUID NOT NULL REFERENCES signs (id)  ON DELETE CASCADE,
    topic_id UUID NOT NULL REFERENCES topics (id) ON DELETE CASCADE,
    PRIMARY KEY (sign_id, topic_id)
);

CREATE INDEX idx_sign_topics_topic ON sign_topics (topic_id);


-- ---------------------------------------------------------------------
-- sign_videos — metadata nghiệp vụ của video; file thật nằm ở file_attachments
--
-- region KHÔNG phải cột phụ: từ điển Bộ GD&ĐT mã hoá vùng miền ngay trong
-- tên file (W00665B / W00665T / W00665N = Bắc / Trung / Nam) và có hàng
-- trăm từ mang ba biến thể khác nhau. Người học Hà Nội được dạy ký hiệu
-- miền Nam sẽ không giao tiếp được với người điếc quanh mình.
-- ---------------------------------------------------------------------
CREATE TABLE sign_videos
(
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    sign_id       UUID        NOT NULL REFERENCES signs (id) ON DELETE CASCADE,
    file_id       UUID        REFERENCES file_attachments (id) ON DELETE SET NULL,
    thumbnail_file_id UUID    REFERENCES file_attachments (id) ON DELETE SET NULL,

    region        VARCHAR(20) NOT NULL DEFAULT 'COMMON'
        CHECK (region IN ('NORTH','CENTRAL','SOUTH','COMMON')),
    view_angle    VARCHAR(20) NOT NULL DEFAULT 'FRONT'
        CHECK (view_angle IN ('FRONT','LEFT','RIGHT')),

    signer_label  VARCHAR(50),        -- ẩn danh: 'signer_01'
    duration_ms   INT CHECK (duration_ms IS NULL OR duration_ms > 0),
    width         INT,
    height        INT,
    caption_vi    TEXT,
    is_primary    BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Nạp hàng loạt từ URL: theo dõi tiến trình tải về
    ingest_status VARCHAR(20) NOT NULL DEFAULT 'READY'
        CHECK (ingest_status IN ('PENDING','DOWNLOADING','READY','FAILED')),
    source_url    TEXT,
    ingest_error  TEXT,

    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_sign_videos_sign   ON sign_videos (sign_id, region);
CREATE INDEX idx_sign_videos_region ON sign_videos (region) WHERE ingest_status = 'READY';
CREATE INDEX idx_sign_videos_ingest ON sign_videos (ingest_status)
    WHERE ingest_status IN ('PENDING','FAILED');

-- Mỗi từ chỉ có MỘT video chính cho mỗi vùng miền
CREATE UNIQUE INDEX uq_sign_videos_primary_per_region
    ON sign_videos (sign_id, region) WHERE is_primary;

-- Không nạp trùng cùng một video nguồn
CREATE UNIQUE INDEX uq_sign_videos_source_url
    ON sign_videos (source_url) WHERE source_url IS NOT NULL;


-- ---------------------------------------------------------------------
-- sign_relations — từ liên quan
--
-- EASILY_CONFUSED phục vụ hai việc: cảnh báo người học, và sinh đáp án
-- nhiễu chất lượng cao cho quiz. Quiz với 3 đáp án ngẫu nhiên thì quá dễ
-- và không đo được năng lực thật.
-- ---------------------------------------------------------------------
CREATE TABLE sign_relations
(
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    sign_id         UUID        NOT NULL REFERENCES signs (id) ON DELETE CASCADE,
    related_sign_id UUID        NOT NULL REFERENCES signs (id) ON DELETE CASCADE,
    relation_type   VARCHAR(30) NOT NULL DEFAULT 'RELATED'
        CHECK (relation_type IN ('RELATED','SYNONYM','ANTONYM','EASILY_CONFUSED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_sign_relation UNIQUE (sign_id, related_sign_id, relation_type),
    CONSTRAINT chk_sign_relation_not_self CHECK (sign_id <> related_sign_id)
);

CREATE INDEX idx_sign_relations_confused
    ON sign_relations (sign_id) WHERE relation_type = 'EASILY_CONFUSED';


-- ---------------------------------------------------------------------
-- saved_signs — "Từ vựng đã lưu" trong Thư viện của tôi
-- ---------------------------------------------------------------------
CREATE TABLE saved_signs
(
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sign_id    UUID        NOT NULL REFERENCES signs (id) ON DELETE CASCADE,
    note       TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_saved_sign UNIQUE (user_id, sign_id)
);

CREATE INDEX idx_saved_signs_user ON saved_signs (user_id, created_at DESC);


-- ---------------------------------------------------------------------
-- sign_import_batches — nhật ký nhập Excel và nạp video hàng loạt
-- ---------------------------------------------------------------------
CREATE TABLE sign_import_batches
(
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    import_type    VARCHAR(30) NOT NULL
        CHECK (import_type IN ('EXCEL_VOCAB','BULK_VIDEO_URL','BULK_VIDEO_UPLOAD')),
    file_id        UUID REFERENCES file_attachments (id) ON DELETE SET NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED','CANCELLED')),
    total_rows     INT NOT NULL DEFAULT 0,
    processed_rows INT NOT NULL DEFAULT 0,
    created_count  INT NOT NULL DEFAULT 0,
    updated_count  INT NOT NULL DEFAULT 0,
    failed_count   INT NOT NULL DEFAULT 0,
    duplicate_strategy VARCHAR(20) NOT NULL DEFAULT 'SKIP'
        CHECK (duplicate_strategy IN ('SKIP','OVERWRITE','CREATE_DRAFT')),
    error_report_file_id UUID REFERENCES file_attachments (id) ON DELETE SET NULL,
    started_by     UUID REFERENCES users (id) ON DELETE SET NULL,
    started_at     TIMESTAMPTZ,
    finished_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sign_import_rows
(
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id    UUID        NOT NULL REFERENCES sign_import_batches (id) ON DELETE CASCADE,
    row_number  INT         NOT NULL,
    raw_data    JSONB       NOT NULL,
    sign_id     UUID REFERENCES signs (id) ON DELETE SET NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','CREATED','UPDATED','SKIPPED','FAILED')),
    error_message TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_sign_import_rows_batch ON sign_import_rows (batch_id, status);


SELECT attach_updated_at(t) FROM (VALUES
    ('topics'), ('signs'), ('sign_videos'), ('sign_relations'),
    ('saved_signs'), ('sign_import_batches'), ('sign_import_rows')
) AS x(t);
