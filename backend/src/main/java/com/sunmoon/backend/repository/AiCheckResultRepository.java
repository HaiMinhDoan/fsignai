package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.ai.AiCheckResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiCheckResultRepository extends JpaRepository<AiCheckResult, UUID> {

    long countByUserId(UUID userId);

    Optional<AiCheckResult> findByIdAndUserId(UUID id, UUID userId);

    /** Lịch sử của một người, lọc theo từ nếu có. JOIN FETCH sign để dựng tên từ mà không N+1 */
    @Query(value = """
            SELECT r FROM AiCheckResult r JOIN FETCH r.sign
            WHERE r.user.id = :userId AND (:signId IS NULL OR r.sign.id = :signId)
            ORDER BY r.checkedAt DESC
            """,
            countQuery = """
            SELECT COUNT(r) FROM AiCheckResult r
            WHERE r.user.id = :userId AND (:signId IS NULL OR r.sign.id = :signId)
            """)
    Page<AiCheckResult> findHistory(@Param("userId") UUID userId, @Param("signId") UUID signId, Pageable pageable);
}
