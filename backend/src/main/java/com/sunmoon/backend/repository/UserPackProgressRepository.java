package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.progress.UserPackProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPackProgressRepository extends JpaRepository<UserPackProgress, UUID> {

    Optional<UserPackProgress> findByUserIdAndPackId(UUID userId, UUID packId);

    List<UserPackProgress> findAllByUserId(UUID userId);

    boolean existsByUserIdAndPackIdAndStatus(UUID userId, UUID packId, String status);
}
