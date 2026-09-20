-- =====================================================================
-- V16 — Mã mời đang chờ thì CHƯA có phụ huynh
-- =====================================================================
-- V13 bắt guardian_user_id NOT NULL. Nhưng lúc bé vừa tạo mã, chưa biết ai
-- sẽ nhận — tôi đã lách bằng cách điền tạm chính bé vào, và đâm thẳng vào
-- ràng buộc chk_guardian_not_self của chính mình.
--
-- Sửa cho đúng bản chất: dòng PENDING là MỘT LỜI MỜI, chưa phải một liên
-- kết, nên chưa có phụ huynh. Chỉ khi chuyển sang ACTIVE mới bắt buộc có.
-- =====================================================================

-- Dọn các lời mời hỏng do bản cũ (nếu có)
DELETE FROM guardian_links WHERE status = 'PENDING' AND guardian_user_id = child_user_id;

ALTER TABLE guardian_links ALTER COLUMN guardian_user_id DROP NOT NULL;

ALTER TABLE guardian_links DROP CONSTRAINT IF EXISTS chk_guardian_not_self;
ALTER TABLE guardian_links
    ADD CONSTRAINT chk_guardian_not_self
        CHECK (guardian_user_id IS NULL OR guardian_user_id <> child_user_id);

-- Liên kết đã xác nhận thì BẮT BUỘC phải biết phụ huynh là ai
ALTER TABLE guardian_links
    ADD CONSTRAINT chk_guardian_active_has_guardian
        CHECK (status <> 'ACTIVE' OR guardian_user_id IS NOT NULL);

COMMENT ON COLUMN guardian_links.guardian_user_id IS
    'NULL khi dòng này mới chỉ là lời mời đang chờ (status = PENDING)';
