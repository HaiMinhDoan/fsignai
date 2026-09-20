package com.sunmoon.backend.dto.request.auth;

import com.sunmoon.backend.constant.enums.AccountKind;
import com.sunmoon.backend.constant.enums.AgeRange;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.constant.enums.UserType;
import com.sunmoon.backend.constant.enums.VslRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255)
    String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
    String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150)
    String fullName;

    /**
     * Chọn ngay trên màn đăng ký: học sinh / phụ huynh / giáo viên. Bắt buộc vì
     * đây là câu hỏi phân luồng đầu tiên - không phải câu hỏi tuỳ chọn như age_range.
     */
    @NotNull(message = "Vui lòng chọn bạn là học sinh, phụ huynh hay giáo viên")
    AccountKind accountKind;

    AgeRange ageRange;

    /**
     * Quan hệ với cộng đồng khiếm thính - câu hỏi bắt buộc trong tài liệu yêu cầu.
     */
    @Builder.Default
    UserType userType = UserType.OTHER;

    /**
     * Vùng miền của người học. Quyết định video ký hiệu nào được phục vụ:
     * người học Hà Nội được dạy ký hiệu miền Nam sẽ không giao tiếp được
     * với người điếc quanh mình.
     */
    @Builder.Default
    Region region = Region.COMMON;

    /**
     * Khai báo chuyên môn VSL. Chỉ ảnh hưởng TRỌNG SỐ góp ý hiệu chỉnh ngưỡng
     * chấm điểm, KHÔNG cấp quyền quản trị nào.
     * Khai khác LEARNER sẽ vào hàng đợi chờ admin xác minh.
     */
    @Builder.Default
    VslRole vslRole = VslRole.LEARNER;

    /** Nơi công tác, số chứng chỉ... để admin xác minh vai trò chuyên môn */
    String vslRoleEvidence;
}
