package com.sunmoon.backend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Số liệu truy cập, người dùng, doanh thu cho Dashboard CMS.
 *
 * Dùng SQL thuần như GuardianQueryRepository: toàn là phép gom nhóm theo ngày/giờ trên nhiều bảng,
 * JPQL không diễn đạt được gọn mà cũng không cần vòng đời entity.
 *
 * Mọi mốc "ngày" tính theo giờ Việt Nam: server có thể chạy UTC, mà người học vào lúc 6 giờ sáng
 * ở VN thì vẫn phải tính là hôm nay chứ không phải 23 giờ hôm qua. Ngày trả về dạng chuỗi
 * 'YYYY-MM-DD' để khỏi phụ thuộc kiểu Hibernate chọn cho cột DATE.
 */
@Repository
@RequiredArgsConstructor
public class AnalyticsQueryRepository {

    public static final String TZ = "Asia/Ho_Chi_Minh";

    private final EntityManager em;

    // ==================== GHI LƯỢT XEM ====================

    @Transactional
    public void insertPageView(String visitorId, String sessionId, UUID userId, String path, String routeName,
                               String referrerHost, String deviceType) {
        // Khai kiểu Java cho từng tham số: giá trị null không kèm kiểu thì Hibernate gửi đi dạng bytea
        // và Postgres từ chối ghi vào cột uuid/varchar
        @SuppressWarnings("unchecked")
        NativeQuery<Object> q = em.createNativeQuery("""
                        INSERT INTO page_views (visitor_id, session_id, user_id, path, route_name, referrer_host, device_type)
                        VALUES (:v, :s, :u, :p, :r, :ref, :d)
                        """)
                .unwrap(NativeQuery.class);
        q.setParameter("v", visitorId, String.class)
                .setParameter("s", sessionId, String.class)
                .setParameter("u", userId, UUID.class)
                .setParameter("p", path, String.class)
                .setParameter("r", routeName, String.class)
                .setParameter("ref", referrerHost, String.class)
                .setParameter("d", deviceType, String.class)
                .executeUpdate();
    }

    // ==================== TRUY CẬP ====================

    /** [lượt xem, khách (visitor), phiên, người dùng đã đăng nhập] trong [from, to) */
    public Object[] trafficTotals(OffsetDateTime from, OffsetDateTime to) {
        return (Object[]) range("""
                SELECT count(*), count(DISTINCT visitor_id), count(DISTINCT session_id), count(DISTINCT user_id)
                  FROM page_views WHERE created_at >= :from AND created_at < :to
                """, from, to).getSingleResult();
    }

    /** Khách trong khoảng mà ĐÃ từng ghé trước khoảng đó — đo độ quay lại */
    public long returningVisitors(OffsetDateTime from, OffsetDateTime to) {
        return num(range("""
                SELECT count(DISTINCT pv.visitor_id) FROM page_views pv
                 WHERE pv.created_at >= :from AND pv.created_at < :to
                   AND EXISTS (SELECT 1 FROM page_views old
                                WHERE old.visitor_id = pv.visitor_id AND old.created_at < :from)
                """, from, to).getSingleResult());
    }

    /** Mỗi ngày: [ngày, lượt xem, khách, người dùng đăng nhập] */
    @SuppressWarnings("unchecked")
    public List<Object[]> trafficDaily(OffsetDateTime from, OffsetDateTime to) {
        return range("""
                SELECT to_char((created_at AT TIME ZONE '%s')::date, 'YYYY-MM-DD') d,
                       count(*), count(DISTINCT visitor_id), count(DISTINCT user_id)
                  FROM page_views WHERE created_at >= :from AND created_at < :to
                 GROUP BY 1 ORDER BY 1
                """.formatted(TZ), from, to).getResultList();
    }

    /** Lượt xem theo giờ trong ngày (0..23, giờ VN): [giờ, lượt xem] */
    @SuppressWarnings("unchecked")
    public List<Object[]> trafficByHour(OffsetDateTime from, OffsetDateTime to) {
        return range("""
                SELECT extract(hour FROM created_at AT TIME ZONE '%s')::int h, count(*)
                  FROM page_views WHERE created_at >= :from AND created_at < :to
                 GROUP BY 1 ORDER BY 1
                """.formatted(TZ), from, to).getResultList();
    }

    /** Lượt xem theo tính năng (tên route): [route, lượt xem, khách] */
    @SuppressWarnings("unchecked")
    public List<Object[]> topRoutes(OffsetDateTime from, OffsetDateTime to, int limit) {
        return range("""
                SELECT COALESCE(route_name, 'khac'), count(*), count(DISTINCT visitor_id)
                  FROM page_views WHERE created_at >= :from AND created_at < :to
                 GROUP BY 1 ORDER BY 2 DESC LIMIT %d
                """.formatted(limit), from, to).getResultList();
    }

    /** [thiết bị, khách] */
    @SuppressWarnings("unchecked")
    public List<Object[]> devices(OffsetDateTime from, OffsetDateTime to) {
        return range("""
                SELECT device_type, count(DISTINCT visitor_id)
                  FROM page_views WHERE created_at >= :from AND created_at < :to
                 GROUP BY 1 ORDER BY 2 DESC
                """, from, to).getResultList();
    }

    /** Nguồn truy cập theo phiên — chỉ tính lượt xem đầu tiên của phiên. null = vào thẳng */
    @SuppressWarnings("unchecked")
    public List<Object[]> referrers(OffsetDateTime from, OffsetDateTime to, int limit) {
        return range("""
                SELECT ref, count(*) FROM (
                    SELECT DISTINCT ON (session_id) session_id, referrer_host ref
                      FROM page_views WHERE created_at >= :from AND created_at < :to
                     ORDER BY session_id, created_at
                ) s GROUP BY ref ORDER BY 2 DESC LIMIT %d
                """.formatted(limit), from, to).getResultList();
    }

    /** Người dùng hoạt động (có lượt xem khi đã đăng nhập HOẶC có học) trong khoảng */
    public long activeUsers(OffsetDateTime from, OffsetDateTime to) {
        return num(em.createNativeQuery("""
                        SELECT count(*) FROM (
                            SELECT user_id FROM page_views
                             WHERE user_id IS NOT NULL AND created_at >= :from AND created_at < :to
                            UNION
                            SELECT user_id FROM daily_activity
                             WHERE activity_date >= :fromDate AND activity_date < :toDate
                        ) a
                        """)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("fromDate", vnDate(from))
                .setParameter("toDate", vnDate(to))
                .getSingleResult());
    }

    // ==================== NGƯỜI DÙNG ====================

    public long totalUsers() {
        return num(em.createNativeQuery("SELECT count(*) FROM users").getSingleResult());
    }

    public long newUsers(OffsetDateTime from, OffsetDateTime to) {
        return num(range("SELECT count(*) FROM users WHERE created_at >= :from AND created_at < :to", from, to)
                .getSingleResult());
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> newUsersDaily(OffsetDateTime from, OffsetDateTime to) {
        return range("""
                SELECT to_char((created_at AT TIME ZONE '%s')::date, 'YYYY-MM-DD'), count(*)
                  FROM users WHERE created_at >= :from AND created_at < :to
                 GROUP BY 1 ORDER BY 1
                """.formatted(TZ), from, to).getResultList();
    }

    public long premiumActive(OffsetDateTime now) {
        return num(em.createNativeQuery("SELECT count(*) FROM users WHERE premium_until > :now")
                .setParameter("now", now).getSingleResult());
    }

    /** [loại tài khoản, số người] */
    @SuppressWarnings("unchecked")
    public List<Object[]> accountKinds() {
        return em.createNativeQuery("SELECT account_kind, count(*) FROM users GROUP BY 1 ORDER BY 2 DESC")
                .getResultList();
    }

    // ==================== HỌC TẬP ====================

    /** [phút học, bài hoàn thành, từ đã ôn, bài kiểm tra, lượt chấm AI, số người học] */
    public Object[] learningTotals(OffsetDateTime from, OffsetDateTime to) {
        return (Object[]) em.createNativeQuery("""
                        SELECT COALESCE(sum(minutes_studied), 0), COALESCE(sum(lessons_completed), 0),
                               COALESCE(sum(signs_reviewed), 0), COALESCE(sum(quizzes_taken), 0),
                               COALESCE(sum(ai_checks_done), 0), count(DISTINCT user_id)
                          FROM daily_activity
                         WHERE activity_date >= :fromDate AND activity_date < :toDate
                        """)
                .setParameter("fromDate", vnDate(from))
                .setParameter("toDate", vnDate(to))
                .getSingleResult();
    }

    /** Mỗi ngày: [ngày, số người học, phút học] */
    @SuppressWarnings("unchecked")
    public List<Object[]> learningDaily(OffsetDateTime from, OffsetDateTime to) {
        return em.createNativeQuery("""
                        SELECT to_char(activity_date, 'YYYY-MM-DD'), count(DISTINCT user_id), COALESCE(sum(minutes_studied), 0)
                          FROM daily_activity
                         WHERE activity_date >= :fromDate AND activity_date < :toDate
                         GROUP BY 1 ORDER BY 1
                        """)
                .setParameter("fromDate", vnDate(from))
                .setParameter("toDate", vnDate(to))
                .getResultList();
    }

    /** [bài viết mới, bình luận mới] */
    public long[] forumTotals(OffsetDateTime from, OffsetDateTime to) {
        long posts = num(range("SELECT count(*) FROM forum_posts WHERE created_at >= :from AND created_at < :to",
                from, to).getSingleResult());
        long comments = num(range("SELECT count(*) FROM forum_comments WHERE created_at >= :from AND created_at < :to",
                from, to).getSingleResult());
        return new long[]{posts, comments};
    }

    // ==================== DOANH THU ====================

    /** [doanh thu, số đơn đã trả, số người trả tiền] theo NGÀY THANH TOÁN */
    public Object[] revenueTotals(OffsetDateTime from, OffsetDateTime to) {
        return (Object[]) range("""
                SELECT COALESCE(sum(COALESCE(paid_amount, amount)), 0), count(*), count(DISTINCT user_id)
                  FROM payment_orders WHERE status = 'PAID' AND paid_at >= :from AND paid_at < :to
                """, from, to).getSingleResult();
    }

    public long revenueAllTime() {
        return num(em.createNativeQuery(
                "SELECT COALESCE(sum(COALESCE(paid_amount, amount)), 0) FROM payment_orders WHERE status = 'PAID'")
                .getSingleResult());
    }

    /** Số đơn được TẠO trong khoảng — mẫu số của tỉ lệ chuyển đổi */
    public long ordersCreated(OffsetDateTime from, OffsetDateTime to) {
        return num(range("SELECT count(*) FROM payment_orders WHERE created_at >= :from AND created_at < :to",
                from, to).getSingleResult());
    }

    /** Trong các đơn tạo trong khoảng, bao nhiêu đơn đã được trả */
    public long ordersCreatedAndPaid(OffsetDateTime from, OffsetDateTime to) {
        return num(range("""
                SELECT count(*) FROM payment_orders
                 WHERE created_at >= :from AND created_at < :to AND status = 'PAID'
                """, from, to).getSingleResult());
    }

    /** Đơn còn chờ chuyển khoản và chưa hết hạn */
    public long ordersPending(OffsetDateTime now) {
        return num(em.createNativeQuery(
                        "SELECT count(*) FROM payment_orders WHERE status = 'PENDING' AND expires_at > :now")
                .setParameter("now", now).getSingleResult());
    }

    /** Đơn đã hết hạn mà không trả (gồm cả đơn còn PENDING trong CSDL nhưng quá hạn) */
    public long ordersExpired(OffsetDateTime from, OffsetDateTime to, OffsetDateTime now) {
        return num(range("""
                SELECT count(*) FROM payment_orders
                 WHERE created_at >= :from AND created_at < :to
                   AND (status = 'EXPIRED' OR (status = 'PENDING' AND expires_at <= :now))
                """, from, to).setParameter("now", now).getSingleResult());
    }

    /** Mỗi ngày: [ngày, doanh thu, số đơn đã trả] */
    @SuppressWarnings("unchecked")
    public List<Object[]> revenueDaily(OffsetDateTime from, OffsetDateTime to) {
        return range("""
                SELECT to_char((paid_at AT TIME ZONE '%s')::date, 'YYYY-MM-DD'),
                       COALESCE(sum(COALESCE(paid_amount, amount)), 0), count(*)
                  FROM payment_orders WHERE status = 'PAID' AND paid_at >= :from AND paid_at < :to
                 GROUP BY 1 ORDER BY 1
                """.formatted(TZ), from, to).getResultList();
    }

    /** Giao dịch tiền vào không khớp đơn nào trong khoảng */
    public long unmatchedTransactions(OffsetDateTime from, OffsetDateTime to) {
        return num(range("""
                SELECT count(*) FROM sepay_transactions
                 WHERE matched_order_id IS NULL AND lower(COALESCE(transfer_type, '')) = 'in'
                   AND created_at >= :from AND created_at < :to
                """, from, to).getSingleResult());
    }

    /** Đơn vừa thanh toán gần nhất: [họ tên, email, số tiền, lúc trả, mã] */
    @SuppressWarnings("unchecked")
    public List<Object[]> recentPaid(int limit) {
        return em.createNativeQuery("""
                        SELECT u.full_name, u.email, COALESCE(o.paid_amount, o.amount), o.paid_at, o.payment_code
                          FROM payment_orders o JOIN users u ON u.id = o.user_id
                         WHERE o.status = 'PAID'
                         ORDER BY o.paid_at DESC LIMIT %d
                        """.formatted(limit))
                .getResultList();
    }

    // ==================== riêng ====================

    /**
     * daily_activity.activity_date đã là ngày theo múi giờ người học; so với ngày VN của mốc.
     * Tính ở Java rồi truyền kiểu DATE — "tham số AT TIME ZONE" để Postgres tự đoán kiểu thì
     * dễ dính lỗi "function timezone(unknown, unknown) is not unique".
     */
    private static LocalDate vnDate(OffsetDateTime t) {
        return t.atZoneSameInstant(ZoneId.of(TZ)).toLocalDate();
    }

    private Query range(String sql, OffsetDateTime from, OffsetDateTime to) {
        return em.createNativeQuery(sql).setParameter("from", from).setParameter("to", to);
    }

    public static long num(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }
}
