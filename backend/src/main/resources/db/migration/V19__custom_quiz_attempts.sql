-- Người học tự tạo đề kiểm tra: chọn "tất cả chủ đề" hoặc vài chủ đề muốn ôn.
--
-- Lượt thi trước đây bắt buộc đến từ đúng một nguồn: đề soạn tay (quiz_id) hoặc
-- cấu hình đề trộn của quản trị (blueprint_id). Đề tự tạo không thuộc nguồn nào,
-- nên thêm nguồn thứ ba: custom_config ghi lại chính lựa chọn của người học.
--
-- Không tạo blueprint ẩn cho mỗi lần tự tạo: sẽ làm phình bảng cấu hình mà quản
-- trị viên đang dùng để soạn đề thật.

ALTER TABLE quiz_attempts ADD COLUMN custom_config jsonb;

ALTER TABLE quiz_attempts DROP CONSTRAINT chk_attempt_source;
ALTER TABLE quiz_attempts ADD CONSTRAINT chk_attempt_source CHECK (
    (quiz_id IS NOT NULL AND blueprint_id IS NULL     AND custom_config IS NULL) OR
    (quiz_id IS NULL     AND blueprint_id IS NOT NULL AND custom_config IS NULL) OR
    (quiz_id IS NULL     AND blueprint_id IS NULL     AND custom_config IS NOT NULL)
);

COMMENT ON COLUMN quiz_attempts.custom_config IS
    'Đề người học tự tạo: {"topicIds": [...] (rỗng = tất cả), "topicNamesVi": [...], "questionCount": n, "passScore": n}';
