package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.payment.SepayTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface SepayTransactionRepository extends JpaRepository<SepayTransaction, UUID> {

    boolean existsBySepayId(Long sepayId);

    /** matched: true = đã khớp đơn, false = chưa khớp (cần xử lý tay), null = tất cả */
    @Query("""
            SELECT t FROM SepayTransaction t
            WHERE (:matched IS NULL
                   OR (:matched = TRUE AND t.matchedOrderId IS NOT NULL)
                   OR (:matched = FALSE AND t.matchedOrderId IS NULL))
              AND (:keyword IS NULL OR LOWER(t.content) LIKE :keyword OR LOWER(t.code) LIKE :keyword
                   OR LOWER(t.referenceCode) LIKE :keyword)
              AND t.createdAt >= :from AND t.createdAt < :to
            ORDER BY t.createdAt DESC
            """)
    Page<SepayTransaction> filter(@Param("matched") Boolean matched,
                                  @Param("keyword") String keyword,
                                  @Param("from") OffsetDateTime from,
                                  @Param("to") OffsetDateTime to,
                                  Pageable pageable);
}
