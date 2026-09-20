package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.progress.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    List<UserAchievement> findAllByUserIdOrderByEarnedAtDesc(UUID userId);

    boolean existsByUserIdAndAchievementId(UUID userId, UUID achievementId);

    /** Huy hiệu người dùng đã có — dùng để loại khỏi danh sách cần soát điều kiện */
    @Query("SELECT ua.achievement.id FROM UserAchievement ua WHERE ua.user.id = :userId")
    List<UUID> findAchievementIdsByUserId(@Param("userId") UUID userId);
}
