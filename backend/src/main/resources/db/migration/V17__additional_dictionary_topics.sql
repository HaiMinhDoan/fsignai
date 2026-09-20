-- =====================================================================
-- V17 — Mở rộng danh mục chủ đề cho việc phân loại toàn bộ từ vựng
-- =====================================================================
-- 12 chủ đề có sẵn (V10) được đặt ra trước khi kho từ MOET_QIPEDC được nạp
-- vào. Sau khi nạp 3322 từ (V3, nạp bằng tools/ingest_qipedc.py) mới lộ ra
-- kho từ này là MỘT TỪ ĐIỂN TỔNG QUÁT — bao trùm cả trăm lĩnh vực chứ không
-- riêng 12 nhóm chủ đề ban đầu (địa danh, lễ hội, tôn giáo, pháp luật, công
-- nghệ, khoa học, toán học, thể thao, giao thông, mua sắm, nghệ thuật, quần
-- áo, màu sắc, động vật, thiên nhiên, cơ thể người, đồ vật, cộng đồng người
-- Điếc, văn hoá & lịch sử, đại từ & ngữ pháp, cùng hai nhóm "bắt phần còn
-- lại": Hành động & tính chất thông dụng, và Từ vựng khác).
--
-- Việc GÁN từng từ vào các chủ đề này (bảng sign_topics + signs.primary_
-- topic_id) được thực hiện bằng tools/classify_signs.py — MỘT SCRIPT, không
-- phải migration: gán chủ đề là quyết định biên tập dựa trên từ khoá, có
-- thể cần chỉnh lại sau này qua CMS, không phải một phép biến đổi lược đồ
-- một-chiều-mãi-mãi như migration. Xem chú thích ở đầu script đó để biết
-- logic phân loại và cách chạy lại an toàn (idempotent).
-- =====================================================================

INSERT INTO topics (slug, name_vi, description_vi, icon_name, category, display_order, is_published)
VALUES
    ('van-hoa-lich-su', 'Văn hoá & Lịch sử', 'Nhân vật lịch sử, truyền thuyết và di sản văn hoá', 'book', 'COMPLEX_SIGN', 19, TRUE),
    ('le-hoi-ngay-ky-niem', 'Lễ hội & Ngày kỷ niệm', 'Các ngày lễ, tết và ngày kỷ niệm trong năm', 'sparkles', 'COMPLEX_SIGN', 20, TRUE),
    ('ton-giao-tin-nguong', 'Tôn giáo & Tín ngưỡng', 'Các khái niệm tôn giáo, tín ngưỡng và tâm linh', 'star', 'COMPLEX_SIGN', 21, TRUE),
    ('phap-luat-hanh-chinh', 'Pháp luật & Hành chính', 'Luật pháp, giấy tờ và thủ tục hành chính', 'grid', 'COMPLEX_SIGN', 22, TRUE),
    ('cong-nghe-thong-tin', 'Công nghệ thông tin', 'Máy tính, điện thoại và internet', 'grid', 'COMPLEX_SIGN', 23, TRUE),
    ('khoa-hoc-tu-nhien', 'Khoa học tự nhiên', 'Vật lý, hoá học và các khái niệm khoa học cơ bản', 'sparkles', 'COMPLEX_SIGN', 24, TRUE),
    ('toan-hoc-so-lieu', 'Toán học & Số liệu', 'Các khái niệm số học, hình học và đơn vị đo lường', 'grid', 'SIMPLE_SIGN', 25, TRUE),
    ('the-thao', 'Thể thao', 'Các môn thể thao và hoạt động vận động', 'flame', 'SIMPLE_SIGN', 26, TRUE),
    ('giao-thong-phuong-tien', 'Giao thông & Phương tiện', 'Các phương tiện và hoạt động giao thông', 'map', 'SIMPLE_SIGN', 27, TRUE),
    ('mua-sam-tien-te', 'Mua sắm & Tiền tệ', 'Tiền bạc, mua bán và cửa hàng', 'grid', 'SITUATION', 28, TRUE),
    ('nghe-thuat-giai-tri', 'Nghệ thuật & Giải trí', 'Âm nhạc, phim ảnh và các hoạt động giải trí', 'sparkles', 'SIMPLE_SIGN', 29, TRUE),
    ('quan-ao-phu-kien', 'Quần áo & Phụ kiện', 'Trang phục và các đồ dùng cá nhân', 'grid', 'SIMPLE_SIGN', 30, TRUE),
    ('mau-sac', 'Màu sắc', 'Tên các màu sắc cơ bản', 'sparkles', 'SIMPLE_SIGN', 31, TRUE),
    ('dong-vat', 'Động vật', 'Các loài vật nuôi và động vật hoang dã', 'hand', 'SIMPLE_SIGN', 32, TRUE),
    ('thien-nhien-thoi-tiet', 'Thiên nhiên & Thời tiết', 'Hiện tượng thiên nhiên, cây cối và thời tiết', 'star', 'SIMPLE_SIGN', 33, TRUE),
    ('co-the-nguoi', 'Cơ thể người', 'Các bộ phận trên cơ thể người', 'hand', 'SIMPLE_SIGN', 34, TRUE),
    ('nha-cua-do-vat', 'Nhà cửa & Đồ vật', 'Nhà ở, nội thất và các vật dụng hằng ngày', 'grid', 'SIMPLE_SIGN', 35, TRUE),
    ('cong-dong-nguoi-diec', 'Cộng đồng Người Điếc & Khuyết tật', 'Người khiếm thính, khuyết tật và ngôn ngữ ký hiệu', 'hand', 'SITUATION', 36, TRUE),
    ('dai-tu-lien-tu', 'Đại từ & Từ ngữ pháp', 'Đại từ, liên từ và các từ nối câu', 'grid', 'SIMPLE_SIGN', 37, TRUE),
    -- Hai chủ đề "bắt phần còn lại" — CỐ Ý tồn tại thay vì ép mọi từ vào một
    -- chủ đề chuyên biệt không phù hợp. Một động từ như "theo dõi" hay "đối
    -- diện" không thuộc lĩnh vực nào cả; nhét bừa vào "Công việc" hay "Toán
    -- học" chỉ vì tình cờ có điểm chung sẽ làm bẩn dữ liệu hơn là để đúng ở
    -- đây. Biên tập viên xem tại CMS và tách dần khi cần.
    ('hanh-dong-tinh-chat-chung', 'Hành động & Tính chất thông dụng', 'Các động từ và tính từ thường gặp trong đời sống', 'sparkles', 'SIMPLE_SIGN', 38, TRUE),
    ('tu-vung-khac', 'Từ vựng khác', 'Các từ vựng chưa xếp vào chủ đề cụ thể', 'grid', 'SIMPLE_SIGN', 39, TRUE)
ON CONFLICT (slug) DO NOTHING;

COMMENT ON TABLE topics IS
    'Chủ đề ngữ nghĩa của từ vựng — điều hướng chính cho người học (bản đồ đảo, bộ lọc tra cứu). '
    '33 chủ đề tính đến V17: 12 chủ đề gốc + 21 chủ đề mở rộng để phủ hết một từ điển tổng quát.';
