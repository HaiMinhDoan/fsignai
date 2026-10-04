package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.payment.PaymentOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
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
}
