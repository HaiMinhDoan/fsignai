package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.ai.VerifyThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerifyThresholdRepository extends JpaRepository<VerifyThreshold, UUID> {
    Optional<VerifyThreshold> findBySignId(UUID signId);
}
