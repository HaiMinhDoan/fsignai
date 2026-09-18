-- =====================================================================
-- V2 — Tài khoản, phân quyền, onboarding
-- =====================================================================

-- ---------------------------------------------------------------------
-- roles — khớp hằng số trong constant/enums/RoleType.java
-- ---------------------------------------------------------------------
CREATE TABLE roles
(
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name_vi     VARCHAR(150) NOT NULL,
    description TEXT,
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,  -- vai trò lõi, không cho xoá
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE permissions
(
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL UNIQUE,   -- 'sign:write', 'forum:moderate'
    name_vi     VARCHAR(150) NOT NULL,
    module      VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE role_permissions
(
    role_id       UUID NOT NULL REFERENCES roles (id)       ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);


-- ---------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------
CREATE TABLE users
(
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    email_normalized VARCHAR(255) GENERATED ALWAYS AS (lower(email)) STORED,
    password_hash   VARCHAR(255),                -- NULL nếu chỉ đăng nhập bằng Google
    google_id       VARCHAR(255) UNIQUE,
    full_name       VARCHAR(150) NOT NULL,
    avatar_file_id  UUID REFERENCES file_attachments (id) ON DELETE SET NULL,
    address         VARCHAR(255),

    age_range       VARCHAR(20)
        CHECK (age_range IN ('UNDER_12','AGE_12_17','AGE_18_24','AGE_25_34','AGE_35_49','AGE_50_PLUS')),

    -- Quan hệ với cộng đồng khiếm thính (câu hỏi bắt buộc lúc đăng ký)
    user_type       VARCHAR(30)  NOT NULL DEFAULT 'OTHER'
        CHECK (user_type IN ('DEAF_HOH','FAMILY','FRIEND','CAREGIVER','OTHER')),

    -- Vùng miền dùng để chọn video ký hiệu phù hợp
    region          VARCHAR(20)  NOT NULL DEFAULT 'COMMON'
        CHECK (region IN ('NORTH','CENTRAL','SOUTH','COMMON')),

    -- Chuyên môn VSL: quyết định TRỌNG SỐ góp ý hiệu chỉnh ngưỡng chấm điểm.
    -- Khác hoàn toàn với roles/permissions (quyền vào trang quản trị).
    vsl_role        VARCHAR(30)  NOT NULL DEFAULT 'LEARNER'
        CHECK (vsl_role IN ('LEARNER','DEAF_NATIVE','TEACHER','INTERPRETER')),
    vsl_role_status VARCHAR(30)  NOT NULL DEFAULT 'SELF_DECLARED'
        CHECK (vsl_role_status IN ('SELF_DECLARED','PENDING','VERIFIED','REJECTED')),
    vsl_role_evidence   TEXT,
    vsl_role_verified_by UUID REFERENCES users (id) ON DELETE SET NULL,
    vsl_role_verified_at TIMESTAMPTZ,

    status          VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE','PENDING_VERIFICATION','DISABLED','BANNED')),
    banned_until    TIMESTAMPTZ,
    ban_reason      TEXT,

    email_verified_at TIMESTAMPTZ,
    last_login_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Đã xác minh thì bắt buộc có mốc thời gian duyệt.
    -- Cố tình KHÔNG ràng buộc chiều ngược lại: khi gỡ vai trò
    -- (VERIFIED -> REJECTED) vẫn giữ lại lịch sử đã từng duyệt khi nào.
    CONSTRAINT chk_vsl_verified_has_timestamp
        CHECK (vsl_role_status <> 'VERIFIED' OR vsl_role_verified_at IS NOT NULL)
);

CREATE UNIQUE INDEX idx_users_email_normalized ON users (email_normalized);
CREATE INDEX idx_users_vsl_pending ON users (vsl_role_status, created_at)
    WHERE vsl_role_status = 'PENDING';
CREATE INDEX idx_users_status ON users (status) WHERE status <> 'ACTIVE';

COMMENT ON COLUMN users.vsl_role IS
    'Khai báo chuyên môn VSL của người dùng thường. Chỉ ảnh hưởng trọng số góp ý, KHÔNG cấp quyền admin.';


CREATE TABLE user_roles
(
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id     UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    granted_by  UUID REFERENCES users (id) ON DELETE SET NULL,
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);


-- ---------------------------------------------------------------------
-- user_settings
-- ---------------------------------------------------------------------
CREATE TABLE user_settings
(
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID        NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    locale             VARCHAR(10) NOT NULL DEFAULT 'vi',
    timezone           VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    daily_goal_minutes INT         NOT NULL DEFAULT 10 CHECK (daily_goal_minutes BETWEEN 1 AND 600),
    reminder_time      TIME        NOT NULL DEFAULT '20:00',
    notify_email       BOOLEAN     NOT NULL DEFAULT TRUE,
    notify_push        BOOLEAN     NOT NULL DEFAULT TRUE,
    notify_streak      BOOLEAN     NOT NULL DEFAULT TRUE,
    notify_new_course  BOOLEAN     NOT NULL DEFAULT TRUE,
    notify_forum_reply BOOLEAN     NOT NULL DEFAULT TRUE,
    -- Hiển thị: người khiếm thính thường cần video chậm và tương phản cao
    default_playback_rate NUMERIC(3,2) NOT NULL DEFAULT 1.00
        CHECK (default_playback_rate BETWEEN 0.25 AND 2.00),
    high_contrast      BOOLEAN     NOT NULL DEFAULT FALSE,
    theme              VARCHAR(20) NOT NULL DEFAULT 'SYSTEM'
        CHECK (theme IN ('LIGHT','DARK','SYSTEM')),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);


-- ---------------------------------------------------------------------
-- onboarding_responses — 4 câu hỏi sau khi đăng ký
-- ---------------------------------------------------------------------
CREATE TABLE onboarding_responses
(
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    learn_reason      VARCHAR(30)
        CHECK (learn_reason IN ('FAMILY','FRIENDS','PERSONAL','WORK','BASIC_COMM','OTHER')),
    current_level     VARCHAR(20)
        CHECK (current_level IN ('BEGINNER','BASIC','INTERMEDIATE','ADVANCED','UNSURE')),
    daily_minutes     INT CHECK (daily_minutes IN (5,10,15,30)),
    -- jsonb vì danh sách chủ đề sẽ đổi; đây là dữ liệu lịch sử, không ràng buộc FK
    interested_topics JSONB       NOT NULL DEFAULT '[]'::jsonb,
    completed_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);


-- ---------------------------------------------------------------------
-- refresh_tokens & xác thực email / đặt lại mật khẩu
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens
(
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,   -- lưu hash, không lưu token gốc
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    user_agent VARCHAR(255),
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id)
    WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens (expires_at)
    WHERE revoked_at IS NULL;

CREATE TABLE verification_tokens
(
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    purpose    VARCHAR(30) NOT NULL
        CHECK (purpose IN ('EMAIL_VERIFY','PASSWORD_RESET')),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_verification_tokens_user ON verification_tokens (user_id, purpose)
    WHERE used_at IS NULL;


-- ---------------------------------------------------------------------
-- audit_logs — nhật ký thao tác quản trị
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs
(
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID REFERENCES users (id) ON DELETE SET NULL,
    action      VARCHAR(100) NOT NULL,          -- 'sign.update', 'forum.hide'
    entity_type VARCHAR(100),
    entity_id   UUID,
    before_data JSONB,
    after_data  JSONB,
    ip_address  VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_actor  ON audit_logs (actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id, created_at DESC);


SELECT attach_updated_at(t) FROM (VALUES
    ('roles'), ('permissions'), ('users'), ('user_settings'),
    ('onboarding_responses'), ('refresh_tokens'), ('verification_tokens'),
    ('audit_logs')
) AS x(t);
