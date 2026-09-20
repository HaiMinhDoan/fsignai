package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.gamification.UserPoints;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Truy vấn đọc cho bảng xếp hạng và rương thưởng.
 *
 * Viết bằng SQL thuần thay vì dựng thêm bốn entity JPA: đây là hai màn hình
 * CHỈ ĐỌC, không sửa gì, mà ánh xạ entity cho reward_chests/game_sessions chỉ
 * để chạy một câu đếm thì lợi bất cập hại.
 */
@Repository
@RequiredArgsConstructor
public class GamificationQueryRepository {

    private final EntityManager em;

    /**
     * Top người học theo điểm. weekly=true thì xếp theo điểm tuần.
     * LEFT JOIN game_sessions để người chưa chơi ván nào vẫn có mặt với số 0.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> topLearners(boolean weekly, int limit) {
        String column = weekly ? "p.weekly_points" : "p.total_points";
        return em.createNativeQuery("""
                        SELECT u.id, u.full_name, %s AS pts,
                               (SELECT count(*) FROM game_sessions g
                                 WHERE g.user_id = u.id AND g.finished_at IS NOT NULL) AS games
                          FROM user_points p
                          JOIN users u ON u.id = p.user_id
                         WHERE u.status = 'ACTIVE'
                         ORDER BY pts DESC, u.full_name ASC
                         LIMIT :lim
                        """.formatted(column))
                .setParameter("lim", limit)
                .getResultList();
    }

    /** Rương thưởng đang bật, kèm mốc điểm và thời điểm người này đã mở (nếu có) */
    @SuppressWarnings("unchecked")
    public List<Object[]> chestsFor(UUID userId) {
        return em.createNativeQuery("""
                        SELECT c.id, c.code, c.name_vi, c.description_vi, c.icon_name,
                               c.required_points, uc.opened_at
                          FROM reward_chests c
                          LEFT JOIN user_reward_chests uc
                                 ON uc.chest_id = c.id AND uc.user_id = :uid
                         WHERE c.is_active = TRUE
                         ORDER BY c.display_order, c.required_points
                        """)
                .setParameter("uid", userId)
                .getResultList();
    }

    /** Tổng sao hiện có; 0 nếu người học chưa từng được cộng điểm */
    public int starsOf(UUID userId) {
        return em.createQuery("SELECT p FROM UserPoints p WHERE p.user.id = :uid", UserPoints.class)
                .setParameter("uid", userId)
                .getResultStream()
                .findFirst()
                .map(UserPoints::getTotalPoints)
                .orElse(0);
    }
}
