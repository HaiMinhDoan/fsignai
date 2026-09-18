package com.sunmoon.backend.constant.enums;

// Danh sách phải khớp với dữ liệu seed trong V10__seed_reference_data.sql.
// Lệch một tên ở đây là API mở nhầm quyền mà không có lỗi biên dịch nào cảnh báo.
public interface RoleType {
    String ALL             = "ALL";              // Bất cứ role nào cũng có thể dùng api này
    String SYSTEM_ADMIN    = "SYSTEM_ADMIN";     // Quản trị hệ thống
    String CONTENT_EDITOR  = "CONTENT_EDITOR";   // Biên tập nội dung: từ vựng, video, chủ đề, bài học
    String MODERATOR       = "MODERATOR";        // Kiểm duyệt diễn đàn
    String VSL_REVIEWER    = "VSL_REVIEWER";     // Thẩm định tính đúng đắn của ký hiệu
    String STUDENT         = "STUDENT";          // Khách hàng
    String TEACHER         = "TEACHER";          // Giáo viên
}
