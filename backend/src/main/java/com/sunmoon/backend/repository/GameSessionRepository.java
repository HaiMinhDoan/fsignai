package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.gamification.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

    Optional<GameSession> findByIdAndUserId(UUID id, UUID userId);
}
