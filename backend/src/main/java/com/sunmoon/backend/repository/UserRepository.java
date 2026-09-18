package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
