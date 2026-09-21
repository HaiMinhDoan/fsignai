package com.sunmoon.backend.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Đọc/ghi liên kết phụ huynh ↔ con và số liệu báo cáo.
 *
 * Dùng SQL thuần: bảng guardian_links chỉ phục vụ đúng một màn hình và không
 * cần vòng đời entity; dựng thêm entity JPA ở đây chỉ làm nặng mô hình.
 */
@Repository
@RequiredArgsConstructor
public class GuardianQueryRepository {

    private final EntityManager em;

    /** Danh sách người học mà người này đang theo dõi (chỉ liên kết đã xác nhận) */
    @SuppressWarnings("unchecked")
    public List<Object[]> childrenOf(UUID guardianId) {
        return em.createNativeQuery("""
                        SELECT u.id, u.full_name, u.age_range, g.relationship
                          FROM guardian_links g
                          JOIN users u ON u.id = g.child_user_id
                         WHERE g.guardian_user_id = :gid
                           AND g.status = 'ACTIVE'
                         ORDER BY u.full_name
                        """)
                .setParameter("gid", guardianId)
                .getResultList();
    }

    /** Người này có thực sự được phép xem người học kia không */
    public boolean canView(UUID guardianId, UUID childId) {
        Number n = (Number) em.createNativeQuery("""
                        SELECT count(*) FROM guardian_links
                         WHERE guardian_user_id = :gid AND child_user_id = :cid
                           AND status = 'ACTIVE' AND can_view_progress = TRUE
                        """)
                .setParameter("gid", guardianId)
                .setParameter("cid", childId)
                .getSingleResult();
        return n.intValue() > 0;
    }

    /** Số phút học theo từng ngày trong khoảng, để vẽ biểu đồ tuần */
    @SuppressWarnings("unchecked")
    public List<Object[]> dailyMinutes(UUID childId, LocalDate from, LocalDate to) {
        return em.createNativeQuery("""
                        SELECT activity_date, minutes_studied, goal_met
                          FROM daily_activity
                         WHERE user_id = :cid AND activity_date BETWEEN :from AND :to
                         ORDER BY activity_date
                        """)
                .setParameter("cid", childId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public int minutesBetween(UUID childId, LocalDate from, LocalDate to) {
        Number n = (Number) em.createNativeQuery("""
                        SELECT COALESCE(sum(minutes_studied), 0) FROM daily_activity
                         WHERE user_id = :cid AND activity_date BETWEEN :from AND :to
                        """)
                .setParameter("cid", childId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return n.intValue();
    }

    /** Số ký hiệu người học đã ôn đúng ít nhất một lần */
    public int masteredSigns(UUID childId) {
        Number n = (Number) em.createNativeQuery("""
                        SELECT count(*) FROM flashcard_reviews
                         WHERE user_id = :cid AND repetitions >= 1
                        """)
                .setParameter("cid", childId)
                .getSingleResult();
        return n.intValue();
    }

    public int streakOf(UUID childId) {
        Number n = (Number) em.createNativeQuery("""
                        SELECT COALESCE(max(current_streak), 0) FROM user_streaks WHERE user_id = :cid
                        """)
                .setParameter("cid", childId)
                .getSingleResult();
        return n.intValue();
    }

    public String fullNameOf(UUID userId) {
        List<?> r = em.createNativeQuery("SELECT full_name FROM users WHERE id = :id")
                .setParameter("id", userId).getResultList();
        return r.isEmpty() ? null : (String) r.get(0);
    }

    /**
     * Tạo (hoặc làm mới) mã mời của người học. Một người học chỉ giữ MỘT mã đang chờ; gọi
     * lại thì mã cũ bị thay, để mã đã đọc cho người lạ không dùng được nữa.
     */
    @Transactional
    public void putInvite(UUID childId, String code) {
        em.createNativeQuery("""
                        DELETE FROM guardian_links
                         WHERE child_user_id = :cid AND status = 'PENDING'
                        """)
                .setParameter("cid", childId).executeUpdate();

        em.createNativeQuery("""
                        INSERT INTO guardian_links
                            (guardian_user_id, child_user_id, relationship, status,
                             invite_code, invite_expires_at, created_by)
                        VALUES (NULL, :cid, 'PARENT', 'PENDING', :code, now() + interval '24 hours', :cid)
                        """)
                .setParameter("cid", childId)
                .setParameter("code", code)
                .executeUpdate();
    }

    /** Tìm người học theo mã mời còn hạn; trả null nếu mã sai hoặc đã hết hạn */
    public UUID childByInvite(String code) {
        List<?> r = em.createNativeQuery("""
                        SELECT child_user_id FROM guardian_links
                         WHERE invite_code = :code AND status = 'PENDING'
                           AND (invite_expires_at IS NULL OR invite_expires_at > now())
                        """)
                .setParameter("code", code).getResultList();
        return r.isEmpty() ? null : (UUID) r.get(0);
    }

    /** Nhận người học: xoá bản ghi chờ rồi tạo liên kết thật đã xác nhận */
    @Transactional
    public void claim(UUID guardianId, UUID childId, String relationship) {
        em.createNativeQuery("DELETE FROM guardian_links WHERE child_user_id = :cid AND status = 'PENDING'")
                .setParameter("cid", childId).executeUpdate();

        em.createNativeQuery("""
                        INSERT INTO guardian_links
                            (guardian_user_id, child_user_id, relationship, status, accepted_at, created_by)
                        VALUES (:gid, :cid, :rel, 'ACTIVE', now(), :gid)
                        ON CONFLICT (guardian_user_id, child_user_id)
                        DO UPDATE SET status = 'ACTIVE', accepted_at = now(), revoked_at = NULL
                        """)
                .setParameter("gid", guardianId)
                .setParameter("cid", childId)
                .setParameter("rel", relationship)
                .executeUpdate();
    }
}
