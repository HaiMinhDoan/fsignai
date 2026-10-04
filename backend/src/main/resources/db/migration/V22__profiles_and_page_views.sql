-- Trang cá nhân + số liệu truy cập cho Dashboard (2026-10-04).
--
-- 1. Trang cá nhân: giới thiệu bản thân (bio) và ảnh đại diện ĐỘNG bằng video ≤ 5 giây.
--    avatar_file_id giữ nguyên vai trò "ảnh tĩnh": với ảnh đại diện video, đó là khung hình
--    đầu do trình duyệt cắt ra. Mọi chỗ cũ (diễn đàn, CMS) cứ đọc avatar_file_id là có ảnh,
--    chỗ nào muốn chuyển động thì đọc thêm avatar_video_file_id.
ALTER TABLE users ADD COLUMN bio varchar(500);
ALTER TABLE users ADD COLUMN avatar_video_file_id uuid REFERENCES file_attachments (id) ON DELETE SET NULL;

COMMENT ON COLUMN users.avatar_video_file_id IS
    'Video ảnh đại diện (tối đa 5 giây, phát lặp, không tiếng). avatar_file_id là khung hình đầu của nó.';

-- 2. Lượt xem trang của web học (portal). Trước đây hệ thống không đo truy cập nào:
--    last_login_at chỉ có mốc gần nhất, daily_activity chỉ ghi khi người học HỌC.
--
--    visitor_id: chuỗi ngẫu nhiên trình duyệt tự sinh và giữ trong localStorage — đếm được
--    khách chưa đăng nhập mà không cần cookie theo dõi hay địa chỉ IP. KHÔNG lưu IP.
--    session_id: sinh lại sau 30 phút không thao tác (giữ trong sessionStorage).
CREATE TABLE page_views (
    id            bigserial PRIMARY KEY,
    visitor_id    varchar(64)  NOT NULL,
    session_id    varchar(64)  NOT NULL,
    user_id       uuid REFERENCES users (id) ON DELETE SET NULL,
    path          varchar(300) NOT NULL,
    -- Tên route của vue-router: gom /tu-dien/<id> của mọi từ về một "trang chi tiết từ"
    route_name    varchar(80),
    -- Chỉ giữ tên miền nguồn (google.com, facebook.com), bỏ phần đường dẫn có thể chứa dữ liệu riêng
    referrer_host varchar(200),
    device_type   varchar(10)  NOT NULL CHECK (device_type IN ('DESKTOP', 'MOBILE', 'TABLET')),
    created_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_page_views_created ON page_views (created_at);
CREATE INDEX idx_page_views_visitor ON page_views (visitor_id, created_at);
CREATE INDEX idx_page_views_user ON page_views (user_id, created_at) WHERE user_id IS NOT NULL;

-- Dashboard lọc doanh thu theo ngày thanh toán
CREATE INDEX idx_payment_orders_paid ON payment_orders (paid_at) WHERE status = 'PAID';
CREATE INDEX idx_sepay_transactions_created ON sepay_transactions (created_at DESC);
