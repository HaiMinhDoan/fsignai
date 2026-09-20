package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.progress.StreakFreeze;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StreakFreezeRepository extends JpaRepository<StreakFreeze, UUID> {

    List<StreakFreeze> findAllByUserIdOrderByUsedForDateDesc(UUID userId);

    boolean existsByUserIdAndUsedForDate(UUID userId, LocalDate usedForDate);
}
