package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.UnitType;
import com.sunmoon.backend.entity.ai.VerifyThresholdGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerifyThresholdGroupRepository extends JpaRepository<VerifyThresholdGroup, UUID> {
    Optional<VerifyThresholdGroup> findByUnitTypeAndHandCount(UnitType unitType, Short handCount);
}
