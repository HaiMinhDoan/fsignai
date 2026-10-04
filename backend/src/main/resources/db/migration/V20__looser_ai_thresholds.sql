-- Nới ngưỡng chấm điểm AI thêm 20% (2026-10-04).
--
-- 19 lượt chấm của người học thật: điểm trung bình 32,8 và KHÔNG lượt nào đạt. Đo lại trên
-- mẫu trong kho (ai-service/tools/eval_exemplars.py, eval_cohort.py) với cặp "cùng từ, người
-- khác làm" thay cho người học giả lập cũ:
--
--   ngưỡng 0,30 (cũ, từ 2 tay)  người làm đúng đạt 63,9%   từ khác lọt 1,5%
--   ngưỡng 0,36 (mới, +20%)     người làm đúng đạt 73,2%   từ khác lọt 5,3%
--   ngưỡng 0,40                 người làm đúng đạt 81,5%   từ khác lọt 18,1%  ← vách đá
--
-- +20% là điểm cân bằng: thêm ~10 điểm phần trăm người làm đúng được công nhận mà tỉ lệ chấm
-- nhầm từ khác vẫn thấp. Đi kèm thay đổi ở ai-service (bù độ lệch khi người học ngồi lệch so
-- với khung) và đường điểm mới ở AiCheckServiceImpl (chạm ngưỡng = 72 điểm thay vì 60).
UPDATE verify_threshold_groups
SET threshold  = ROUND(threshold * 1.2, 4),
    updated_at = now();
