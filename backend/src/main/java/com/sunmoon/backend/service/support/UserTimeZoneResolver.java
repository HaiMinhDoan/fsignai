package com.sunmoon.backend.service.support;

import com.sunmoon.backend.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Ngày "hôm nay" cho chuỗi ngày học và nhật ký hoạt động PHẢI tính theo múi
 * giờ của người học, không phải UTC hay giờ máy chủ.
 *
 * Người học ở Việt Nam học lúc 23h30 giờ địa phương mà hệ thống tính theo UTC
 * sẽ thấy hoạt động đó rơi sang ngày hôm sau — mất chuỗi oan dù họ học đủ mỗi
 * ngày. Đây là lỗi người dùng cảm nhận rất rõ, phải làm đúng từ đầu.
 *
 * Dùng chung cho ProgressService (nhật ký hoạt động) và StreakService (chuỗi
 * ngày) để "hôm nay" luôn là CÙNG một ngày ở cả hai nơi.
 */
@Component
@RequiredArgsConstructor
public class UserTimeZoneResolver {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserSettingsRepository userSettingsRepository;

    /**
     * Múi giờ đã khai trong cài đặt của người dùng.
     *
     * timezone là cột VARCHAR tự do, không ràng buộc phải là id IANA hợp lệ ở
     * tầng CSDL — một giá trị hỏng (do nhập tay hoặc dữ liệu cũ) không được
     * phép làm sập việc tính chuỗi ngày của CẢ HỆ THỐNG, nên bắt lỗi và dùng
     * múi giờ mặc định thay vì để ném ngoại lệ.
     */
    public ZoneId resolveZone(UUID userId) {
        return userSettingsRepository.findByUserId(userId)
                .map(settings -> settings.getTimezone())
                .filter(tz -> tz != null && !tz.isBlank())
                .map(this::parseOrDefault)
                .orElse(DEFAULT_ZONE);
    }

    public LocalDate today(UUID userId) {
        return LocalDate.now(resolveZone(userId));
    }

    private ZoneId parseOrDefault(String tz) {
        try {
            return ZoneId.of(tz);
        } catch (Exception e) {
            return DEFAULT_ZONE;
        }
    }
}
