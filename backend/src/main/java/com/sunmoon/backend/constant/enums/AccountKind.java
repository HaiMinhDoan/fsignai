package com.sunmoon.backend.constant.enums;

// Loại tài khoản chọn lúc đăng ký. Giá trị PHẢI khớp CHECK constraint của
// cột users.account_kind trong V13__guardian_and_account_kind.sql.
// Khác hoàn toàn roles (quyền quản trị) và vsl_role (chuyên môn VSL).
public enum AccountKind {
    CHILD,
    ADULT,
    PARENT,
    TEACHER
}
