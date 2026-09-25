-- Diễn đàn nhận bài bằng NGÔN NGỮ KÝ HIỆU, không chỉ bằng chữ.
--
-- Bảng media_assets, forum_post_media, forum_comment_media đã dựng từ V8 nhưng
-- chưa có dòng nào: API và giao diện chỉ làm phần chữ. Lần này nối chúng vào.
--
-- Với nhiều người điếc, tiếng Việt viết là ngôn ngữ THỨ HAI. Bắt gõ chữ mới được
-- tham gia cộng đồng chính là dựng lại đúng rào cản mà sản phẩm này sinh ra để gỡ.

-- 1. Tiêu đề không còn bắt buộc: có thể gõ tay, để trống, hoặc thay bằng video ký hiệu.
ALTER TABLE forum_posts ALTER COLUMN title_vi DROP NOT NULL;

-- 2. Video làm tiêu đề. Tách riêng khỏi forum_post_media vì nó đóng vai trò khác:
--    một cái là "tên bài" hiện ngoài danh sách, cái kia là nội dung bên trong bài.
--    ON DELETE SET NULL để gỡ media không làm bay cả bài viết.
ALTER TABLE forum_posts ADD COLUMN title_media_id uuid REFERENCES media_assets (id) ON DELETE SET NULL;

CREATE INDEX idx_forum_posts_title_media ON forum_posts (title_media_id);

-- 3. Dọn rác: media người dùng tải lên nhưng bấm huỷ, không gắn vào bài/bình luận nào.
--    Có cột này thì quét dọn định kỳ mới biết đâu là tệp mồ côi, đâu là tệp vừa tải
--    lên và đang chờ người dùng bấm Đăng.
CREATE INDEX idx_media_assets_owner_created ON media_assets (owner_id, created_at);

COMMENT ON COLUMN forum_posts.title_media_id IS
    'Video ký hiệu dùng làm tiêu đề. Bài có thể có title_vi, title_media_id, cả hai, hoặc không cái nào (khi đó nội dung phải có chữ hoặc media).';
