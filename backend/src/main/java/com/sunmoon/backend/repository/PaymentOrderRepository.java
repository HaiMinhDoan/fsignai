package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.payment.PaymentOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {

    /**
     * Khoá dòng khi khớp tiền: hai lượt webhook (SePay gửi lại) chạy song song không được
     * cùng thấy đơn còn PENDING rồi cùng cộng thêm ngày Premium.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PaymentOrder o WHERE upper(o.paymentCode) = upper(:code)")
    Optional<PaymentOrder> findByCodeForUpdate(String code);

    /** Đơn đang chờ còn hạn của người này cho gói đó — bấm Nâng cấp nhiều lần thì dùng lại, không sinh mã mới */
    @Query("""
            SELECT o FROM PaymentOrder o
            WHERE o.user.id = :userId AND o.planCode = :planCode AND o.status = 'PENDING' AND o.expiresAt > :now
            ORDER BY o.createdAt DESC LIMIT 1
            """)
    Optional<PaymentOrder> findReusable(UUID userId, String planCode, OffsetDateTime now);

    boolean existsByPaymentCode(String paymentCode);

    /**
     * Lịch sử đơn cho trang cá nhân và CMS.
     *
     * Trạng thái lọc theo trạng thái HIỆU LỰC: đơn PENDING đã quá hạn được tính là EXPIRED, vì
     * không có job nào chuyển trạng thái — chỉ đổi khi có người mở đơn ra xem.
     * from/to không bao giờ null (service thay bằng mốc rất xa) để Postgres khỏi phải đoán kiểu tham số.
     */
    @Query(value = """
            SELECT o FROM PaymentOrder o JOIN FETCH o.user u
            WHERE (:userId IS NULL OR u.id = :userId)
              AND (:keyword IS NULL OR LOWER(u.fullName) LIKE :keyword OR LOWER(u.email) LIKE :keyword
                   OR LOWER(o.paymentCode) LIKE :keyword)
              AND o.createdAt >= :from AND o.createdAt < :to
              AND (:status IS NULL
                   OR (:status = 'EXPIRED' AND (o.status = 'EXPIRED' OR (o.status = 'PENDING' AND o.expiresAt <= :now)))
                   OR (:status = 'PENDING' AND o.status = 'PENDING' AND o.expiresAt > :now)
                   OR (:status <> 'EXPIRED' AND :status <> 'PENDING' AND o.status = :status))
            ORDER BY o.createdAt DESC
            """,
            countQuery = """
            SELECT count(o) FROM PaymentOrder o JOIN o.user u
            WHERE (:userId IS NULL OR u.id = :userId)
              AND (:keyword IS NULL OR LOWER(u.fullName) LIKE :keyword OR LOWER(u.email) LIKE :keyword
                   OR LOWER(o.paymentCode) LIKE :keyword)
              AND o.createdAt >= :from AND o.createdAt < :to
              AND (:status IS NULL
                   OR (:status = 'EXPIRED' AND (o.status = 'EXPIRED' OR (o.status = 'PENDING' AND o.expiresAt <= :now)))
                   OR (:status = 'PENDING' AND o.status = 'PENDING' AND o.expiresAt > :now)
                   OR (:status <> 'EXPIRED' AND :status <> 'PENDING' AND o.status = :status))
            """)
    Page<PaymentOrder> history(
            @Param("userId") UUID userId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("now") OffsetDateTime now,
            Pageable pageable);
}
