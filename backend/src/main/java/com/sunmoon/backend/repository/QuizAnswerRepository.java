package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.practice.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, UUID> {

    List<QuizAnswer> findAllByAttemptIdOrderByQuestionIndexAsc(UUID attemptId);

    /**
     * Từ người dùng đã được hỏi gần đây (qua mọi lượt thi), dùng để tránh hỏi
     * lại khi đề trộn rút câu mới — tham số avoidRecentDays của QuizBlueprint.
     */
    @Query("""
            SELECT DISTINCT a.sign.id FROM QuizAnswer a
             WHERE a.attempt.user.id = :userId
               AND a.attempt.startedAt >= :since
               AND a.sign IS NOT NULL
            """)
    List<UUID> findRecentlyAskedSignIds(@Param("userId") UUID userId,
                                        @Param("since") OffsetDateTime since);
}
