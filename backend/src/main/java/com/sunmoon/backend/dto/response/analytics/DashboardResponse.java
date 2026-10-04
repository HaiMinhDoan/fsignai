package com.sunmoon.backend.dto.response.analytics;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Số liệu Dashboard CMS cho N ngày gần nhất (giờ Việt Nam), mỗi chỉ số chính kèm số của
 * N ngày liền trước để giao diện tự tính tăng/giảm.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardResponse {

    int days;
    /** Ngày đầu / ngày cuối của khoảng, dạng YYYY-MM-DD */
    String fromDate;
    String toDate;

    // ===== Truy cập =====
    Kpi pageViews;
    Kpi visitors;
    Kpi sessions;
    /** Người dùng có đăng nhập vào web hoặc có học trong khoảng */
    Kpi activeUsers;
    double pagesPerSession;
    /** % khách trong khoảng đã từng ghé trước đó */
    double returningVisitorRate;
    long dau;
    long wau;
    long mau;
    /** DAU / MAU (%) — độ "dính" của sản phẩm */
    double stickiness;

    // ===== Người dùng =====
    long totalUsers;
    Kpi newUsers;
    long premiumActive;
    /** % người dùng đang có Premium */
    double premiumRate;

    // ===== Doanh thu =====
    Kpi revenue;
    Kpi paidOrders;
    Kpi payingUsers;
    long revenueAllTime;
    long ordersCreated;
    long ordersPending;
    long ordersExpired;
    /** % đơn tạo trong khoảng đã được trả */
    double conversionRate;
    /** Doanh thu trung bình trên mỗi người trả tiền */
    long arppu;
    long unmatchedTransactions;

    // ===== Học tập & cộng đồng =====
    Learning learning;
    long forumPosts;
    long forumComments;

    // ===== Xu hướng =====
    List<DailyPoint> daily;
    /** Lượt xem theo giờ 0..23 (giờ VN) */
    List<Long> hourly;
    /** name = tên route vue-router; value = lượt xem; extra = số khách */
    List<NamedValue> topRoutes;
    List<NamedValue> devices;
    /** name = tên miền nguồn, null = vào thẳng; value = số phiên */
    List<NamedValue> referrers;
    List<NamedValue> accountKinds;
    List<RecentPayment> recentPayments;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Kpi {
        long current;
        long previous;

        public static Kpi of(long current, long previous) {
            return new Kpi(current, previous);
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Learning {
        long minutesStudied;
        long lessonsCompleted;
        long signsReviewed;
        long quizzesTaken;
        long aiChecksDone;
        long learners;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DailyPoint {
        String date;
        long pageViews;
        long visitors;
        /** Khách có đăng nhập */
        long signedInVisitors;
        long newUsers;
        long revenue;
        long paidOrders;
        long learners;
        long minutesStudied;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class NamedValue {
        String name;
        long value;
        Long extra;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RecentPayment {
        String fullName;
        String email;
        long amount;
        OffsetDateTime paidAt;
        String paymentCode;
    }
}
