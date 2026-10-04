package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.dto.request.analytics.PageViewRequest;
import com.sunmoon.backend.dto.response.analytics.DashboardResponse;
import com.sunmoon.backend.dto.response.analytics.DashboardResponse.DailyPoint;
import com.sunmoon.backend.dto.response.analytics.DashboardResponse.Kpi;
import com.sunmoon.backend.dto.response.analytics.DashboardResponse.NamedValue;
import com.sunmoon.backend.repository.AnalyticsQueryRepository;
import com.sunmoon.backend.service.AnalyticsService;
import com.sunmoon.backend.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.sunmoon.backend.repository.AnalyticsQueryRepository.num;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final ZoneId VN = ZoneId.of(AnalyticsQueryRepository.TZ);

    private static final Pattern BOT = Pattern.compile(
            "bot|crawl|spider|slurp|headless|lighthouse|preview|facebookexternalhit|curl|wget|python-requests",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLET = Pattern.compile("ipad|tablet|kindle|silk|(android(?!.*mobile))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MOBILE = Pattern.compile("mobi|iphone|ipod|android|windows phone",
            Pattern.CASE_INSENSITIVE);

    private final AnalyticsQueryRepository analytics;
    private final RedisService redisService;

    // ==================== GHI LƯỢT XEM ====================

    @Override
    public void trackPageView(PageViewRequest r, UUID userId, String userAgent) {
        try {
            if (userAgent == null || BOT.matcher(userAgent).find()) {
                return;     // bot, trình xem trước link, công cụ đo tốc độ: không phải người học
            }
            String path = r.getPath().split("[?#]", 2)[0];
            // Vue-router đôi khi báo hai lần cho một lần điều hướng (redirect, replace): gộp lại
            String dedupKey = redisService.buildKey("pv", r.getVisitorId() + ":" + path);
            if (redisService.exists(dedupKey)) {
                return;
            }
            redisService.set(dedupKey, 1, 2);

            analytics.insertPageView(r.getVisitorId(), r.getSessionId(), userId, path,
                    blankToNull(r.getRouteName()), referrerHost(r.getReferrer()), deviceOf(userAgent));
        } catch (Exception e) {
            log.debug("Bỏ qua một lượt xem trang: {}", e.getMessage());
        }
    }

    // ==================== DASHBOARD ====================

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(int days) {
        int n = Math.max(1, Math.min(days, 365));
        OffsetDateTime now = OffsetDateTime.now();
        LocalDate today = LocalDate.now(VN);
        LocalDate firstDay = today.minusDays(n - 1L);

        OffsetDateTime from = startOf(firstDay);
        OffsetDateTime to = startOf(today.plusDays(1));
        OffsetDateTime prevFrom = startOf(firstDay.minusDays(n));

        // ----- Truy cập -----
        Object[] t = analytics.trafficTotals(from, to);
        Object[] tp = analytics.trafficTotals(prevFrom, from);
        long views = num(t[0]), visitors = num(t[1]), sessions = num(t[2]);
        long returning = analytics.returningVisitors(from, to);

        long dau = analytics.activeUsers(startOf(today), to);
        long wau = analytics.activeUsers(startOf(today.minusDays(6)), to);
        long mau = analytics.activeUsers(startOf(today.minusDays(29)), to);

        // ----- Người dùng -----
        long totalUsers = analytics.totalUsers();
        long premiumActive = analytics.premiumActive(now);

        // ----- Doanh thu -----
        Object[] rev = analytics.revenueTotals(from, to);
        Object[] revPrev = analytics.revenueTotals(prevFrom, from);
        long revenue = num(rev[0]), paying = num(rev[2]);
        long created = analytics.ordersCreated(from, to);
        long createdPaid = analytics.ordersCreatedAndPaid(from, to);

        // ----- Học tập -----
        Object[] l = analytics.learningTotals(from, to);
        long[] forum = analytics.forumTotals(from, to);

        return DashboardResponse.builder()
                .days(n)
                .fromDate(firstDay.toString())
                .toDate(today.toString())
                .pageViews(Kpi.of(views, num(tp[0])))
                .visitors(Kpi.of(visitors, num(tp[1])))
                .sessions(Kpi.of(sessions, num(tp[2])))
                .activeUsers(Kpi.of(analytics.activeUsers(from, to), analytics.activeUsers(prevFrom, from)))
                .pagesPerSession(ratio(views, sessions))
                .returningVisitorRate(percent(returning, visitors))
                .dau(dau)
                .wau(wau)
                .mau(mau)
                .stickiness(percent(dau, mau))
                .totalUsers(totalUsers)
                .newUsers(Kpi.of(analytics.newUsers(from, to), analytics.newUsers(prevFrom, from)))
                .premiumActive(premiumActive)
                .premiumRate(percent(premiumActive, totalUsers))
                .revenue(Kpi.of(revenue, num(revPrev[0])))
                .paidOrders(Kpi.of(num(rev[1]), num(revPrev[1])))
                .payingUsers(Kpi.of(paying, num(revPrev[2])))
                .revenueAllTime(analytics.revenueAllTime())
                .ordersCreated(created)
                .ordersPending(analytics.ordersPending(now))
                .ordersExpired(analytics.ordersExpired(from, to, now))
                .conversionRate(percent(createdPaid, created))
                .arppu(paying == 0 ? 0 : revenue / paying)
                .unmatchedTransactions(analytics.unmatchedTransactions(from, to))
                .learning(DashboardResponse.Learning.builder()
                        .minutesStudied(num(l[0]))
                        .lessonsCompleted(num(l[1]))
                        .signsReviewed(num(l[2]))
                        .quizzesTaken(num(l[3]))
                        .aiChecksDone(num(l[4]))
                        .learners(num(l[5]))
                        .build())
                .forumPosts(forum[0])
                .forumComments(forum[1])
                .daily(daily(firstDay, n, from, to))
                .hourly(hourly(from, to))
                .topRoutes(analytics.topRoutes(from, to, 12).stream()
                        .map(r -> new NamedValue((String) r[0], num(r[1]), num(r[2]))).toList())
                .devices(named(analytics.devices(from, to)))
                .referrers(named(analytics.referrers(from, to, 8)))
                .accountKinds(named(analytics.accountKinds()))
                .recentPayments(analytics.recentPaid(8).stream()
                        .map(r -> DashboardResponse.RecentPayment.builder()
                                .fullName((String) r[0])
                                .email((String) r[1])
                                .amount(num(r[2]))
                                .paidAt(toOffset(r[3]))
                                .paymentCode((String) r[4])
                                .build())
                        .toList())
                .build();
    }

    // ==================== riêng ====================

    /** Đủ N ngày kể cả ngày không có dữ liệu — biểu đồ đường mà thiếu ngày là vẽ sai xu hướng */
    private List<DailyPoint> daily(LocalDate firstDay, int n, OffsetDateTime from, OffsetDateTime to) {
        Map<String, DailyPoint> theoNgay = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            String d = firstDay.plusDays(i).toString();
            theoNgay.put(d, DailyPoint.builder().date(d).build());
        }
        for (Object[] r : analytics.trafficDaily(from, to)) {
            DailyPoint p = theoNgay.get((String) r[0]);
            if (p == null) continue;
            p.setPageViews(num(r[1]));
            p.setVisitors(num(r[2]));
            p.setSignedInVisitors(num(r[3]));
        }
        for (Object[] r : analytics.newUsersDaily(from, to)) {
            DailyPoint p = theoNgay.get((String) r[0]);
            if (p != null) p.setNewUsers(num(r[1]));
        }
        for (Object[] r : analytics.revenueDaily(from, to)) {
            DailyPoint p = theoNgay.get((String) r[0]);
            if (p == null) continue;
            p.setRevenue(num(r[1]));
            p.setPaidOrders(num(r[2]));
        }
        for (Object[] r : analytics.learningDaily(from, to)) {
            DailyPoint p = theoNgay.get((String) r[0]);
            if (p == null) continue;
            p.setLearners(num(r[1]));
            p.setMinutesStudied(num(r[2]));
        }
        return new ArrayList<>(theoNgay.values());
    }

    private List<Long> hourly(OffsetDateTime from, OffsetDateTime to) {
        long[] gio = new long[24];
        for (Object[] r : analytics.trafficByHour(from, to)) {
            int h = (int) num(r[0]);
            if (h >= 0 && h < 24) gio[h] = num(r[1]);
        }
        List<Long> ds = new ArrayList<>(24);
        for (long v : gio) ds.add(v);
        return ds;
    }

    private static List<NamedValue> named(List<Object[]> rows) {
        return rows.stream().map(r -> new NamedValue((String) r[0], num(r[1]), null)).toList();
    }

    private static OffsetDateTime startOf(LocalDate d) {
        return d.atStartOfDay(VN).toOffsetDateTime();
    }

    private static double percent(long part, long whole) {
        return whole == 0 ? 0 : Math.round(part * 1000.0 / whole) / 10.0;
    }

    private static double ratio(long a, long b) {
        return b == 0 ? 0 : Math.round(a * 100.0 / b) / 100.0;
    }

    /** Hibernate trả cột timestamptz của truy vấn thuần thành Timestamp, Instant hoặc OffsetDateTime tuỳ phiên bản */
    private static OffsetDateTime toOffset(Object o) {
        if (o == null) return null;
        if (o instanceof OffsetDateTime odt) return odt;
        if (o instanceof Instant i) return i.atOffset(ZoneOffset.UTC);
        if (o instanceof Timestamp ts) return ts.toInstant().atOffset(ZoneOffset.UTC);
        return null;
    }

    static String deviceOf(String ua) {
        if (TABLET.matcher(ua).find()) return "TABLET";
        if (MOBILE.matcher(ua).find()) return "MOBILE";
        return "DESKTOP";
    }

    /** Chỉ giữ tên miền nguồn — đường dẫn đầy đủ có thể chứa từ khoá tìm kiếm, mã phiên... */
    static String referrerHost(String referrer) {
        if (referrer == null || referrer.isBlank()) return null;
        try {
            String host = URI.create(referrer.trim()).getHost();
            if (host == null) return null;
            host = host.toLowerCase(Locale.ROOT);
            host = host.startsWith("www.") ? host.substring(4) : host;
            return host.length() > 200 ? host.substring(0, 200) : host;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
