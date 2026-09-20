package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.practice.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizQuestionRepository
        extends JpaRepository<QuizQuestion, UUID>, JpaSpecificationExecutor<QuizQuestion> {

    List<QuizQuestion> findAllByQuizIdOrderByDisplayOrderAsc(UUID quizId);

    long countByQuizId(UUID quizId);

    void deleteAllByQuizId(UUID quizId);

    @Query("SELECT COALESCE(MAX(q.displayOrder), -1) + 1 FROM QuizQuestion q WHERE q.quiz.id = :quizId")
    int nextDisplayOrder(UUID quizId);

    /** Từ vựng đã được ra đề trong đề này — tránh hỏi trùng một từ hai lần */
    @Query("SELECT q.sign.id FROM QuizQuestion q WHERE q.quiz.id = :quizId")
    List<UUID> findSignIdsByQuizId(UUID quizId);
}
