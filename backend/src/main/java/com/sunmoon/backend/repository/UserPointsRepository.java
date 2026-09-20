package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.gamification.UserPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPointsRepository extends JpaRepository<UserPoints, UUID> {

    Optional<UserPoints> findByUserId(UUID userId);
}
