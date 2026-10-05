-- Câu hỏi trắc nghiệm chỉ còn HAI cơ chế (2026-10-05, người dùng chốt):
--   VIDEO_TO_WORD — xem video: "Đây là ký hiệu của từ gì?"  → chọn trong các TỪ
--   WORD_TO_VIDEO — cho từ:   "Đâu là ký hiệu của từ "X"?"  → chọn trong các VIDEO
--
-- MULTIPLE_CHOICE và MATCHING không có giao diện riêng: trang làm bài hiện chúng như một lưới
-- vừa video vừa chữ, câu "Đâu là ký hiệu của từ X?" vừa có video đáp án ở trên, vừa có chữ
-- ngay trong lựa chọn — lộ đáp án. Gộp về cơ chế gần nhất. Giữ hai giá trị trong ràng buộc
-- CHECK vì lượt thi cũ (generated_questions) vẫn còn nhắc tới chúng.

-- 1. Câu đã soạn trong đề. Câu đổi cơ chế thì viết lại câu dẫn luôn: câu dẫn cũ (kể cả câu
--    biên tập viên tự gõ) viết cho dạng khác, có khi chứa chính từ đáp án.
UPDATE quiz_questions
   SET question_type = 'VIDEO_TO_WORD',
       prompt_vi     = 'Đây là ký hiệu của từ gì?',
       updated_at    = now()
 WHERE question_type = 'MULTIPLE_CHOICE';

UPDATE quiz_questions q
   SET question_type = 'WORD_TO_VIDEO',
       prompt_vi     = 'Đâu là ký hiệu của từ "' || s.word_vi || '"?',
       updated_at    = now()
  FROM signs s
 WHERE s.id = q.sign_id
   AND q.question_type = 'MATCHING';

-- Câu dẫn mặc định cũ của dạng xem video → câu dẫn mới. Câu biên tập viên tự gõ thì giữ.
UPDATE quiz_questions
   SET prompt_vi = 'Đây là ký hiệu của từ gì?', updated_at = now()
 WHERE question_type = 'VIDEO_TO_WORD'
   AND (prompt_vi IS NULL OR prompt_vi = 'Ký hiệu trong video có nghĩa là gì?');

-- 2. Tỉ lệ dạng câu của đề trộn: cộng dồn MULTIPLE_CHOICE vào VIDEO_TO_WORD, MATCHING vào WORD_TO_VIDEO
UPDATE quiz_blueprints
   SET question_type_mix =
           jsonb_build_object(
               'VIDEO_TO_WORD', COALESCE((question_type_mix ->> 'VIDEO_TO_WORD')::int, 0)
                              + COALESCE((question_type_mix ->> 'MULTIPLE_CHOICE')::int, 0),
               'WORD_TO_VIDEO', COALESCE((question_type_mix ->> 'WORD_TO_VIDEO')::int, 0)
                              + COALESCE((question_type_mix ->> 'MATCHING')::int, 0))
           || CASE WHEN question_type_mix ? 'AI_PERFORM'
                   THEN jsonb_build_object('AI_PERFORM', question_type_mix -> 'AI_PERFORM')
                   ELSE '{}'::jsonb END,
       updated_at = now()
 WHERE question_type_mix ? 'MULTIPLE_CHOICE' OR question_type_mix ? 'MATCHING';

ALTER TABLE quiz_blueprints
    ALTER COLUMN question_type_mix SET DEFAULT '{"VIDEO_TO_WORD":5,"WORD_TO_VIDEO":5}'::jsonb;
