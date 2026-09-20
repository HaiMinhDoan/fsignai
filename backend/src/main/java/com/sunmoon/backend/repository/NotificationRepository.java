package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.NotificationChannel;
import com.sunmoon.backend.constant.enums.NotificationStatus;
import com.sunmoon.backend.constant.enums.NotificationType;
import com.sunmoon.backend.entity.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUser_IdAndChannelOrderByCreatedAtDesc(
            UUID userId, NotificationChannel channel, Pageable pageable);

    long countByUser_IdAndChannelAndReadAtIsNull(UUID userId, NotificationChannel channel);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.id = :id AND n.user.id = :userId AND n.readAt IS NULL")
    int markRead(@Param("id") UUID id, @Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now " +
            "WHERE n.user.id = :userId AND n.channel = 'IN_APP' AND n.readAt IS NULL")
    int markAllRead(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    @Query("""
            SELECT n FROM Notification n
            WHERE (:type IS NULL OR n.type = :type)
              AND (:channel IS NULL OR n.channel = :channel)
              AND (:status IS NULL OR n.status = :status)
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> adminFilter(@Param("type") NotificationType type,
                                    @Param("channel") NotificationChannel channel,
                                    @Param("status") NotificationStatus status,
                                    Pageable pageable);
}
