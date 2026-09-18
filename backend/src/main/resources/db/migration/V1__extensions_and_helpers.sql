-- =====================================================================
-- V1 — Extension, hàm tiện ích và trigger dùng chung
-- =====================================================================
-- Toàn bộ khoá chính dùng UUID (khớp với entity FileAttachment sẵn có).
-- Mốc thời gian dùng TIMESTAMPTZ để ánh xạ sang OffsetDateTime của Java.
-- Không dùng kiểu ENUM của PostgreSQL: dự án đang biểu diễn enum bằng
-- String (xem constant/enums/RoleType.java), nên dùng VARCHAR + CHECK
-- để thêm giá trị mới không phải ALTER TYPE.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS unaccent;   -- bỏ dấu tiếng Việt khi tìm kiếm
CREATE EXTENSION IF NOT EXISTS pg_trgm;    -- tìm gần đúng, gõ sai chính tả


-- ---------------------------------------------------------------------
-- f_unaccent: bọc unaccent() thành IMMUTABLE.
-- unaccent() nguyên bản là STABLE nên không dùng được trong generated
-- column hay index biểu thức. Chỉ định thẳng từ điển 'public.unaccent'
-- khiến kết quả không phụ thuộc search_path, nên đánh IMMUTABLE là an toàn.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION f_unaccent(text)
    RETURNS text
    LANGUAGE sql
    IMMUTABLE
    PARALLEL SAFE
    STRICT
AS $$
SELECT public.unaccent('public.unaccent', $1)
$$;


-- ---------------------------------------------------------------------
-- Tự cập nhật updated_at mỗi lần UPDATE.
-- Entity cũng gán giá trị này ở tầng Java, nhưng trigger bảo đảm
-- cả những thao tác SQL trực tiếp (job batch, sửa tay) cũng đúng.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;


-- ---------------------------------------------------------------------
-- Gắn trigger set_updated_at cho một bảng (gọi ở cuối mỗi migration).
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION attach_updated_at(p_table text)
    RETURNS void
    LANGUAGE plpgsql
AS $$
BEGIN
    EXECUTE format(
        'DROP TRIGGER IF EXISTS trg_%1$s_updated_at ON %1$I;
         CREATE TRIGGER trg_%1$s_updated_at
             BEFORE UPDATE ON %1$I
             FOR EACH ROW EXECUTE FUNCTION set_updated_at();',
        p_table);
END;
$$;


-- ---------------------------------------------------------------------
-- file_attachments — bảng lưu file đa hình đã có sẵn trong dự án
-- (entity com.sunmoon.backend.entity.FileAttachment).
-- Tạo bằng IF NOT EXISTS để migration chạy được trên cả CSDL trống
-- lẫn CSDL đã có bảng này.
--
-- QUY ƯỚC: mọi file (video ký hiệu, ảnh bìa, avatar, video bình luận)
-- đều nằm ở đây. Các bảng nghiệp vụ chỉ giữ metadata riêng của mình và
-- trỏ tới file_attachments.id — không bảng nào tự lưu object_key.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS file_attachments
(
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    bucket        VARCHAR(100) NOT NULL,
    object_key    TEXT         NOT NULL,
    original_name VARCHAR(255),
    mime_type     VARCHAR(100),
    extension     VARCHAR(100),
    size_bytes    BIGINT,
    entity_type   VARCHAR(100),
    entity_id     UUID,
    uploaded_by   UUID,
    status        VARCHAR(50)  NOT NULL DEFAULT 'active',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_file_attachments_entity
    ON file_attachments (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_file_attachments_uploader
    ON file_attachments (uploaded_by, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_file_attachments_status
    ON file_attachments (status) WHERE status <> 'active';

SELECT attach_updated_at('file_attachments');

COMMENT ON TABLE file_attachments IS
    'Kho file đa hình dùng chung. entity_type khớp hằng số trong constant/enums/EntityType.java';
