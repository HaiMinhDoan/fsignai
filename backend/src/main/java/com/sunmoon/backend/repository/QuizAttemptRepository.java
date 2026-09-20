package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.practice.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository
        extends JpaRepository<QuizAttempt, UUID>, JpaSpecificationExecutor<QuizAttempt> {

    List<QuizAttempt> findAllByUserIdOrderByStartedAtDesc(UUID userId);

    /** Đạt điểm tuyệt đối ít nhất một lần — dùng cho huy hiệu "quiz_perfect" */
    @Query("""
            SELECT COUNT(a) FROM QuizAttempt a
             WHERE a.user.id = :userId AND a.status = 'SUBMITTED'
               AND a.maxScore > 0 AND a.score = a.maxScore
            """)
    long countPerfectAttempts(@Param("userId") UUID userId);
}
