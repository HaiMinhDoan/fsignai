package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.ai.AiCheckFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiCheckFeedbackRepository extends JpaRepository<AiCheckFeedback, UUID> {
    Optional<AiCheckFeedback> findByUserIdAndAiCheckResultId(UUID userId, UUID resultId);
}
