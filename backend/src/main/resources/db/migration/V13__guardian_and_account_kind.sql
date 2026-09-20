-- =====================================================================
-- V13 — Loại tài khoản và liên kết phụ huynh ↔ con
-- =====================================================================
-- "Đăng ký là học sinh thì có tài khoản bố mẹ theo dõi tiến độ"
-- (màn Bảng Đồng Hành, Figma 1:2).
--
-- users đã có age_range nhưng cột đó CHO PHÉP NULL và là câu hỏi tuỳ
-- chọn lúc onboarding — không đủ chắc để quyết định quyền. account_kind
-- dưới đây là cột dứt khoát: mọi tài khoản đều có một giá trị.
-- =====================================================================

ALTER TABLE users
    ADD COLUMN account_kind VARCHAR(20) NOT NULL DEFAULT 'ADULT'
        CHECK (account_kind IN ('CHILD','ADULT','PARENT','TEACHER'));

COMMENT ON COLUMN users.account_kind IS
    'CHILD = trẻ em, bị ẩn diễn đàn và cần phụ huynh đi kèm. Khác vsl_role (chuyên môn) và roles (quyền quản trị).';

-- Người đã tự khai dưới 12 hoặc 12–17 thì chuyển sang CHILD ngay, khỏi
-- phải đợi họ đăng nhập lại
UPDATE users SET account_kind = 'CHILD'
 WHERE age_range IN ('UNDER_12','AGE_12_17');

-- Lọc nhanh khi dựng danh sách trẻ cho phụ huynh và khi chặn diễn đàn
CREATE INDEX idx_users_account_kind ON users (account_kind);


-- ---------------------------------------------------------------------
-- guardian_links — một phụ huynh/thầy cô theo dõi một bé
--
-- Quan hệ nhiều–nhiều có chủ đích: một bé có thể có cả bố, mẹ và cô giáo
-- cùng theo dõi; một phụ huynh có nhiều con (màn 1:2 có nút đổi con).
--
-- Không tự động nối bằng email: phải có bước bé/người lớn xác nhận bằng
-- invite_code, nếu không thì ai biết email của trẻ cũng xem được toàn bộ
-- lịch sử học của trẻ đó.
-- ---------------------------------------------------------------------
CREATE TABLE guardian_links
(
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    guardian_user_id  UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    child_user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    relationship      VARCHAR(20) NOT NULL DEFAULT 'PARENT'
        CHECK (relationship IN ('PARENT','GUARDIAN','TEACHER','RELATIVE')),

    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','ACTIVE','REVOKED')),

    -- Mã ngắn để ghép cặp. UNIQUE trên cả bảng; đặt NULL sau khi đã dùng
    -- để mã cũ không ghép lại được lần hai.
    invite_code       VARCHAR(20) UNIQUE,
    invite_expires_at TIMESTAMPTZ,

    -- Xem tiến độ là mặc định; đổi cài đặt của bé thì phải bật riêng
    can_view_progress  BOOLEAN    NOT NULL DEFAULT TRUE,
    can_manage_settings BOOLEAN   NOT NULL DEFAULT FALSE,

    created_by        UUID        REFERENCES users (id) ON DELETE SET NULL,
    accepted_at       TIMESTAMPTZ,
    revoked_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_guardian_child UNIQUE (guardian_user_id, child_user_id),
    CONSTRAINT chk_guardian_not_self CHECK (guardian_user_id <> child_user_id),
    CONSTRAINT chk_guardian_active_has_time CHECK (status <> 'ACTIVE' OR accepted_at IS NOT NULL)
);

-- Phụ huynh mở bảng đồng hành: lấy danh sách con
CREATE INDEX idx_guardian_by_guardian ON guardian_links (guardian_user_id, status);
-- Bé xem ai đang theo dõi mình, và job kiểm tra quyền truy cập
CREATE INDEX idx_guardian_by_child ON guardian_links (child_user_id, status);

SELECT attach_updated_at('guardian_links');

COMMENT ON TABLE guardian_links IS
    'Liên kết phụ huynh/thầy cô ↔ trẻ. Phải được xác nhận bằng invite_code, không tự nối theo email.';
COMMENT ON COLUMN guardian_links.invite_code IS
    'Đặt NULL sau khi ghép xong để mã không dùng lại được lần hai';
