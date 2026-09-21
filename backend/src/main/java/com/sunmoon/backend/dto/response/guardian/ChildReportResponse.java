package com.sunmoon.backend.dto.response.guardian;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Báo cáo học tập của một người học — đúng bốn ô chỉ số và biểu đồ tuần của
 * Figma 1:2 "Bảng Đồng Hành".
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChildReportResponse {

    UUID childUserId;
    String fullName;

    /** Ô 1 — tổng phút học 7 ngày gần nhất */
    Integer weekMinutes;
    Integer prevWeekMinutes;

    /** Ô 2 — số ký hiệu đã thuộc (ôn đúng ít nhất một lần) */
    Integer masteredSigns;

    /**
     * Ô 3 — độ chính xác chấm bằng camera. null khi chưa dựng service AI;
     * giao diện phải hiện "chưa bật" chứ KHÔNG được hiện số 0 như thể người học sai hết.
     */
    Integer aiAccuracyPercent;

    /** Ô 4 — chuỗi ngày học liên tiếp */
    Integer streakDays;

    /** Biểu đồ tuần: đúng 7 mục, từ thứ Hai */
    List<DayPoint> week;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DayPoint {
        LocalDate date;
        String label;      // T2..CN
        Integer minutes;
        Boolean goalMet;
    }
}
