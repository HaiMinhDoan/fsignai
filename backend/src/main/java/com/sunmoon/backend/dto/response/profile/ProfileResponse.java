package com.sunmoon.backend.dto.response.profile;

import com.sunmoon.backend.constant.enums.AccountKind;
import com.sunmoon.backend.constant.enums.VslRole;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Trang cá nhân — ai cũng xem được. Không có email, địa chỉ, độ tuổi hay gói dịch vụ:
 * nhiều tài khoản là trẻ em, trang này chỉ nên nói những gì người dùng tự muốn khoe.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileResponse {

    UUID userId;
    String fullName;
    String bio;

    /** Ảnh tĩnh — với ảnh đại diện video thì đây là khung hình đầu */
    String avatarUrl;
    /** Video ≤ 5 giây, phát lặp không tiếng; null nếu ảnh đại diện là ảnh tĩnh */
    String avatarVideoUrl;

    AccountKind accountKind;
    /** Chỉ trả khi chuyên môn VSL ĐÃ ĐƯỢC XÁC MINH — tự khai chưa duyệt thì không gắn huy hiệu */
    VslRole verifiedVslRole;

    OffsetDateTime joinedAt;

    /** Người đang xem chính là chủ trang */
    Boolean isMe;

    Stats stats;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Stats {
        long posts;
        long comments;
        long reactionsReceived;
        int streakDays;
        int longestStreakDays;
        int stars;
        int level;
    }
}
