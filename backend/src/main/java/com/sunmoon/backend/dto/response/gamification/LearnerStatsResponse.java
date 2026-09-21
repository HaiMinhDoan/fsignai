package com.sunmoon.backend.dto.response.gamification;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Số liệu hiện lên thanh đầu trang của web học tập: chuỗi ngày, sao, cấp độ.
 *
 * Gộp ba thứ vào MỘT lần gọi vì chúng luôn hiện cùng nhau ở header — tách
 * thành ba API thì mỗi lần chuyển trang người học phải chờ ba vòng mạng.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearnerStatsResponse {

    /** Chuỗi ngày học liên tiếp hiện tại */
    Integer streakDays;
    Integer longestStreakDays;

    /** Tổng "sao" đã thu thập — chính là tổng điểm */
    Integer stars;
    Integer weeklyStars;

    Integer level;
    /** Điểm đã có trong cấp hiện tại và mốc cần để lên cấp sau */
    Integer levelPoints;
    Integer levelTarget;
    /** Phần trăm đã đi được trong cấp hiện tại, làm tròn xuống */
    Integer levelPercent;
}
