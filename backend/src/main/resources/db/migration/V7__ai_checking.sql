-- =====================================================================
-- V7 — AI checking: exemplar, ngưỡng DTW, kết quả chấm, góp ý
--
-- Giai đoạn đầu chạy ở MỨC A: chỉ luyện tập, KHÔNG tính điểm.
-- FastAPI là service stateless — nó chỉ trả `score`; Spring Boot mới là
-- nơi so ngưỡng và quyết định `passed`, rồi ghi vào tiến độ học.
-- =====================================================================

-- ---------------------------------------------------------------------
-- sign_exemplars — mẫu chuẩn để so khớp DTW
-- Sinh tự động bằng cách chạy MediaPipe trên chính video đã nạp,
-- không phải quay thêm clip nào.
-- ---------------------------------------------------------------------
CREATE TABLE sign_exemplars
(
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    sign_id        UUID         NOT NULL REFERENCES signs (id)       ON DELETE CASCADE,
    sign_video_id  UUID         REFERENCES sign_videos (id)          ON DELETE CASCADE,
    region         VARCHAR(20)  NOT NULL DEFAULT 'COMMON'
        CHECK (region IN ('NORTH','CENTRAL','SOUTH','COMMON')),

    -- File .npz chứa chuỗi landmark đã chuẩn hoá, nằm trên MinIO
    landmark_file_id UUID REFERENCES file_attachments (id) ON DELETE SET NULL,
    frame_count    INT          CHECK (frame_count IS NULL OR frame_count > 0),
    landmark_dim   INT,
    -- Vector nhúng cho M1 (encoder). M0 dùng DTW thuần nên để NULL.
    embedding      BYTEA,
    embedding_dim  INT,

    quality_score  NUMERIC(5,4) CHECK (quality_score IS NULL OR quality_score BETWEEN 0 AND 1),
    model_version  VARCHAR(50)  NOT NULL DEFAULT 'mediapipe-holistic-v1',
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    build_status   VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        CHECK (build_status IN ('PENDING','PROCESSING','READY','FAILED')),
    build_error    TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_exemplars_sign   ON sign_exemplars (sign_id, region)
    WHERE is_active AND build_status = 'READY';
CREATE INDEX idx_exemplars_queue  ON sign_exemplars (build_status)
    WHERE build_status IN ('PENDING','FAILED');
CREATE UNIQUE INDEX uq_exemplar_per_video
    ON sign_exemplars (sign_video_id, model_version) WHERE sign_video_id IS NOT NULL;


-- ---------------------------------------------------------------------
-- Ngưỡng chấm điểm — ba tầng, tra theo thứ tự ưu tiên:
--   1. verify_thresholds.source = 'FEEDBACK_TUNED'    (sau MVP)
--   2. verify_thresholds.source = 'EXEMPLAR_DERIVED'  (job batch)
--   3. verify_threshold_groups                        (mặc định nhóm)
--
-- Tầng 3 PHẢI có sẵn từ ngày đầu: hiệu chỉnh từ góp ý giáo viên chỉ chạy
-- được khi đã có người dùng và có giáo viên đã duyệt — ngày mở thì cả hai
-- đều bằng không.
-- ---------------------------------------------------------------------
CREATE TABLE verify_threshold_groups
(
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_type  VARCHAR(20)  NOT NULL
        CHECK (unit_type IN ('LETTER','NUMBER','WORD','PHRASE','SENTENCE')),
    hand_count SMALLINT     NOT NULL CHECK (hand_count IN (1, 2)),
    threshold  NUMERIC(6,4) NOT NULL CHECK (threshold > 0),
    note       TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_threshold_group UNIQUE (unit_type, hand_count)
);

CREATE TABLE verify_thresholds
(
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    sign_id      UUID         NOT NULL UNIQUE REFERENCES signs (id) ON DELETE CASCADE,
    threshold    NUMERIC(6,4) NOT NULL CHECK (threshold > 0),
    source       VARCHAR(30)  NOT NULL DEFAULT 'GROUP_DEFAULT'
        CHECK (source IN ('EXEMPLAR_DERIVED','GROUP_DEFAULT','FEEDBACK_TUNED','MANUAL')),
    sample_count INT          NOT NULL DEFAULT 0,   -- tổng TRỌNG SỐ góp ý đã dùng
    updated_by   UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);


-- ---------------------------------------------------------------------
-- ai_check_results
--
-- consent_to_store mặc định FALSE: dữ liệu chuyển động của người khiếm
-- thính là dữ liệu sinh trắc. Chỉ lưu landmark khi người dùng chủ động
-- đồng ý — và đó cũng là cách hợp lệ duy nhất để có thêm dữ liệu train.
-- ---------------------------------------------------------------------
CREATE TABLE ai_check_results
(
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sign_id          UUID         NOT NULL REFERENCES signs (id) ON DELETE CASCADE,
    region           VARCHAR(20)  NOT NULL DEFAULT 'COMMON'
        CHECK (region IN ('NORTH','CENTRAL','SOUTH','COMMON')),

    score            NUMERIC(5,2) NOT NULL CHECK (score BETWEEN 0 AND 100),
    passed           BOOLEAN      NOT NULL DEFAULT FALSE,
    dtw_distance     NUMERIC(8,5),
    threshold_used   NUMERIC(6,4),
    matched_exemplar_id UUID REFERENCES sign_exemplars (id) ON DELETE SET NULL,

    -- Điểm theo từng tham số ngôn ngữ ký hiệu — đây mới là thứ người học sửa được.
    -- Một con số tổng "72/100" không cho biết phải sửa gì.
    score_handshape  NUMERIC(5,2),
    score_location   NUMERIC(5,2),
    score_movement   NUMERIC(5,2),
    -- Mã gợi ý, ví dụ ["HAND_SHAPE_OK","LOCATION_TOO_LOW"].
    -- Lưu MÃ chứ không lưu câu tiếng Việt: đổi cách diễn đạt hoặc thêm
    -- ngôn ngữ thì không phải deploy lại service Python.
    hint_codes       JSONB        NOT NULL DEFAULT '[]'::jsonb,
    tracking_quality NUMERIC(4,3) CHECK (tracking_quality IS NULL OR tracking_quality BETWEEN 0 AND 1),

    model_version    VARCHAR(50)  NOT NULL DEFAULT 'verify-dtw-v1',
    processing_ms    INT,
    context          VARCHAR(20)  NOT NULL DEFAULT 'PRACTICE'
        CHECK (context IN ('PRACTICE','LESSON','QUIZ')),

    landmark_file_id UUID REFERENCES file_attachments (id) ON DELETE SET NULL,
    consent_to_store BOOLEAN      NOT NULL DEFAULT FALSE,

    checked_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Không được giữ landmark nếu người dùng chưa đồng ý
    CONSTRAINT chk_landmark_consent
        CHECK (landmark_file_id IS NULL OR consent_to_store)
);

CREATE INDEX idx_ai_check_user ON ai_check_results (user_id, checked_at DESC);
CREATE INDEX idx_ai_check_sign ON ai_check_results (sign_id, checked_at DESC);

-- Bổ sung FK đã treo từ V5
ALTER TABLE quiz_answers
    ADD CONSTRAINT fk_quiz_answers_ai_check
        FOREIGN KEY (ai_check_result_id) REFERENCES ai_check_results (id) ON DELETE SET NULL;


-- ---------------------------------------------------------------------
-- ai_check_feedback — "Chấm như vậy có đúng không?" 👍 / 👎
--
-- weight_snapshot phải CHỤP LẠI tại thời điểm góp ý. Nếu tính trọng số
-- động theo vai trò hiện tại, thì một người được duyệt hoặc bị gỡ vai trò
-- sẽ làm mọi ngưỡng đã hiệu chỉnh trong quá khứ âm thầm đổi theo, và
-- không ai giải thích nổi vì sao điểm hôm nay khác hôm qua.
-- ---------------------------------------------------------------------
CREATE TABLE ai_check_feedback
(
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID        NOT NULL REFERENCES users (id)            ON DELETE CASCADE,
    ai_check_result_id UUID        NOT NULL REFERENCES ai_check_results (id) ON DELETE CASCADE,
    verdict            VARCHAR(20) NOT NULL CHECK (verdict IN ('AGREE','DISAGREE')),
    note               TEXT,
    weight_snapshot    SMALLINT    NOT NULL DEFAULT 1 CHECK (weight_snapshot >= 0),
    role_snapshot      VARCHAR(30) NOT NULL DEFAULT 'LEARNER',
    role_status_snapshot VARCHAR(30) NOT NULL DEFAULT 'SELF_DECLARED',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_ai_feedback UNIQUE (user_id, ai_check_result_id)
);

-- Lọc nhanh "từ đang bị chê nhiều nhất" — chính là đặc tả cho M1 sau này
CREATE INDEX idx_ai_feedback_disagree
    ON ai_check_feedback (ai_check_result_id) WHERE verdict = 'DISAGREE';
CREATE INDEX idx_ai_feedback_weighted
    ON ai_check_feedback (created_at DESC) WHERE weight_snapshot > 1;


SELECT attach_updated_at(t) FROM (VALUES
    ('sign_exemplars'), ('verify_threshold_groups'), ('verify_thresholds'),
    ('ai_check_results'), ('ai_check_feedback')
) AS x(t);
