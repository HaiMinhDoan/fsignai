package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.auth.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:actorId IS NULL OR a.actor.id = :actorId)
              AND (:action IS NULL OR a.action = :action)
              AND (:entityType IS NULL OR a.entityType = :entityType)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> adminFilter(@Param("actorId") UUID actorId,
                                @Param("action") String action,
                                @Param("entityType") String entityType,
                                Pageable pageable);
}
