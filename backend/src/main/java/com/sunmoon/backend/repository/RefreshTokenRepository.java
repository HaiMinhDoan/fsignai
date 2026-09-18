package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // Chi tim token CHUA bi thu hoi - token da revoke coi nhu khong ton tai
    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    // Dang xuat khoi moi thiet bi: thu hoi tat ca refresh token cua user
    @Modifying
    @Query("""
            UPDATE RefreshToken rt SET rt.revokedAt = :now
            WHERE rt.user.id = :userId AND rt.revokedAt IS NULL
            """)
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    // Don rac dinh ky: xoa token da het han hoac da thu hoi tu lau
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :threshold OR rt.revokedAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") OffsetDateTime threshold);
}
