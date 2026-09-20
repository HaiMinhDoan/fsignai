package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.progress.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AchievementRepository
        extends JpaRepository<Achievement, UUID>, JpaSpecificationExecutor<Achievement> {

    Optional<Achievement> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    List<Achievement> findAllByIsActiveTrueOrderByDisplayOrderAsc();
}
