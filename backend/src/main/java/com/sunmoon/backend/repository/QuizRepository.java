package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.practice.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID>, JpaSpecificationExecutor<Quiz> {

    List<Quiz> findAllByLessonIdOrderByTitleViAsc(UUID lessonId);

    List<Quiz> findAllByOrderByTitleViAsc();

    /** Đếm câu hỏi của nhiều đề trong một truy vấn, tránh N+1 khi hiện danh sách */
    @Query("""
            SELECT q.quiz.id, COUNT(q.id)
              FROM QuizQuestion q
             WHERE q.quiz.id IN :quizIds
             GROUP BY q.quiz.id
            """)
    List<Object[]> countQuestionsByQuizIds(List<UUID> quizIds);
}
