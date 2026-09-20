package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.AccountKind;
import com.sunmoon.backend.constant.enums.UserStatus;
import com.sunmoon.backend.constant.enums.VslRoleStatus;
import com.sunmoon.backend.entity.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    /**
     * Tra cuu KHONG phan biet hoa thuong.
     * users.email_normalized la cot sinh o DB (lower(email)) va co unique index,
     * nen tim theo no vua dung vua dung duoc index - khong can lower() luc chay.
     */
    Optional<User> findByEmailNormalized(String emailNormalized);

    boolean existsByEmailNormalized(String emailNormalized);

    Optional<User> findByGoogleId(String googleId);

    @Query("""
            SELECT u FROM User u
            WHERE (:keyword IS NULL
                    OR LOWER(u.fullName) LIKE :keyword
                    OR LOWER(u.email) LIKE :keyword)
              AND (:status IS NULL OR u.status = :status)
              AND (:accountKind IS NULL OR u.accountKind = :accountKind)
              AND (:vslRoleStatus IS NULL OR u.vslRoleStatus = :vslRoleStatus)
            ORDER BY u.createdAt DESC
            """)
    Page<User> adminFilter(@Param("keyword") String keyword,
                            @Param("status") UserStatus status,
                            @Param("accountKind") AccountKind accountKind,
                            @Param("vslRoleStatus") VslRoleStatus vslRoleStatus,
                            Pageable pageable);
}
