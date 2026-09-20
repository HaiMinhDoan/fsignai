-- =====================================================================
-- V15 — Đổi mã trò chơi cho khớp bản thiết kế
-- =====================================================================
-- V14 đặt bốn mã theo phỏng đoán của tôi. Đọc kỹ Figma 1:611 thì bốn trò
-- thực tế là:
--   1. Bàn Tay Vui Nhộn   — nối hình với ký hiệu   -> MATCH_PAIR   (giữ)
--   2. Thám Tử Ký Hiệu    — đoán trong 10 giây     -> SPEED_GUESS  (giữ)
--   3. Vũ Điệu Ngón Tay   — bấm theo nhịp          -> FINGER_DANCE (MỚI)
--   4. Truy Tìm Ký Hiệu   — lật thẻ trí nhớ        -> MEMORY_FLIP  (giữ)
-- 'PICK_SIGN' tôi tự nghĩ ra, không có trong thiết kế -> bỏ.
--
-- Bảng đang rỗng nên đổi ràng buộc là an toàn; vẫn chuyển dữ liệu cũ trước
-- cho chắc, phòng khi chạy trên môi trường đã có bản ghi.
-- =====================================================================

UPDATE game_sessions SET game_code = 'FINGER_DANCE' WHERE game_code = 'PICK_SIGN';

ALTER TABLE game_sessions DROP CONSTRAINT IF EXISTS game_sessions_game_code_check;

ALTER TABLE game_sessions
    ADD CONSTRAINT game_sessions_game_code_check
        CHECK (game_code IN ('MATCH_PAIR','SPEED_GUESS','FINGER_DANCE','MEMORY_FLIP'));

COMMENT ON COLUMN game_sessions.game_code IS
    'Bốn trò ở Góc Trò Chơi, khớp Figma 1:611: MATCH_PAIR, SPEED_GUESS, FINGER_DANCE, MEMORY_FLIP';
