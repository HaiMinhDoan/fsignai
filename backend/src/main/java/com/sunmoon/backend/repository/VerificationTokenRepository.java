package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.VerificationPurpose;
import com.sunmoon.backend.entity.auth.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    // Cap token moi thi vo hieu token cu cung muc dich, tranh nhieu link cung song
    @Modifying
    @Query("""
            UPDATE VerificationToken vt SET vt.usedAt = :now
            WHERE vt.user.id = :userId AND vt.purpose = :purpose AND vt.usedAt IS NULL
            """)
    int invalidateAll(@Param("userId") UUID userId,
                      @Param("purpose") VerificationPurpose purpose,
                      @Param("now") OffsetDateTime now);
}
