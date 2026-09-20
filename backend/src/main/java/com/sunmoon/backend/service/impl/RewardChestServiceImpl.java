package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.GamificationQueryRepository;
import com.sunmoon.backend.service.RewardChestService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardChestServiceImpl implements RewardChestService {

    private final EntityManager em;
    private final GamificationQueryRepository queryRepository;

    @Override
    @Transactional
    public boolean open(UUID userId, UUID chestId) {
        List<?> rows = em.createNativeQuery(
                        "SELECT required_points FROM reward_chests WHERE id = :id AND is_active = TRUE")
                .setParameter("id", chestId)
                .getResultList();
        if (rows.isEmpty()) {
            throw new NotFoundException("Không tìm thấy rương thưởng");
        }
        int required = ((Number) rows.get(0)).intValue();

        int stars = queryRepository.starsOf(userId);
        if (stars < required) {
            throw new ConflictException("Chưa đủ sao để mở rương này, còn thiếu " + (required - stars) + " sao");
        }

        // UNIQUE(user_id, chest_id): mở trùng không tạo dòng thứ hai
        int inserted = em.createNativeQuery("""
                        INSERT INTO user_reward_chests (user_id, chest_id)
                        VALUES (:uid, :cid)
                        ON CONFLICT (user_id, chest_id) DO NOTHING
                        """)
                .setParameter("uid", userId)
                .setParameter("cid", chestId)
                .executeUpdate();
        return inserted > 0;
    }
}
