package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.PointSource;
import com.sunmoon.backend.entity.gamification.PointEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PointEventRepository extends JpaRepository<PointEvent, UUID> {

    boolean existsByUserIdAndSourceAndSourceId(UUID userId, PointSource source, UUID sourceId);
}
