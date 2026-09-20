-- =====================================================================
-- V11 — Hướng dẫn động tác tay theo từng bước
-- =====================================================================
-- Màn "Phòng Luyện Ký Hiệu Tương Tác" (Figma 1:1668) có khối
-- "Step-by-Step Tactile Visual Cues": mỗi ký hiệu được tách thành vài
-- bước, mỗi bước một tấm ảnh chụp thế tay kèm một câu mô tả.
--
-- signs.description_vi đã có sẵn nhưng là MỘT đoạn văn mô tả cả ký hiệu.
-- Bảng này khác hẳn: nó chia nhỏ theo thứ tự thực hiện, để bé làm theo
-- được từng nhịp thay vì đọc một khối chữ dài.
-- =====================================================================

CREATE TABLE sign_steps
(
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    sign_id        UUID         NOT NULL REFERENCES signs (id) ON DELETE CASCADE,

    step_order     INT          NOT NULL CHECK (step_order >= 1),

    -- Ảnh chụp thế tay ở bước này. Cho phép NULL để biên tập viên soạn
    -- chữ trước, bổ sung ảnh sau mà không phải xoá rồi nhập lại.
    image_file_id  UUID         REFERENCES file_attachments (id) ON DELETE SET NULL,

    title_vi       VARCHAR(150),

    -- BẮT BUỘC, không cho NULL. Quy tắc §2.2 của hệ thống thiết kế: không
    -- bao giờ để một kênh giác quan là kênh duy nhất. Bé khiếm thị kèm
    -- khiếm thính, hoặc ảnh tải hỏng, thì câu mô tả này là thứ còn lại.
    description_vi TEXT         NOT NULL,

    -- Gợi ý bộ phận cơ thể cần chú ý ở bước này, để giao diện tô sáng đúng
    -- chỗ thay vì bắt bé tự dò
    body_focus     VARCHAR(20)
        CHECK (body_focus IS NULL OR body_focus IN
            ('LEFT_HAND','RIGHT_HAND','BOTH_HANDS','FACE','MOUTH','SHOULDER','CHEST')),

    -- Số giây nên dừng ở bước này khi phát chậm từng bước
    hold_seconds   NUMERIC(4,1) CHECK (hold_seconds IS NULL OR hold_seconds > 0),

    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_sign_steps_order UNIQUE (sign_id, step_order)
);

CREATE INDEX idx_sign_steps_sign ON sign_steps (sign_id, step_order);

SELECT attach_updated_at('sign_steps');

COMMENT ON TABLE sign_steps IS
    'Các bước thực hiện một ký hiệu: ảnh thế tay + mô tả. Khác signs.description_vi ở chỗ có thứ tự.';
COMMENT ON COLUMN sign_steps.description_vi IS
    'Bắt buộc có: ảnh không được là kênh thông tin duy nhất (docs/05-design-system.md §2.2)';
