package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.request.practice.GenerateQuestionsRequest;
import com.sunmoon.backend.dto.request.practice.QuizQuestionRequest;
import com.sunmoon.backend.dto.request.practice.QuizRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.practice.GenerateQuestionsResult;
import com.sunmoon.backend.dto.response.practice.QuizQuestionResponse;
import com.sunmoon.backend.dto.response.practice.QuizResponse;
import com.sunmoon.backend.entity.practice.Quiz;

import java.util.List;
import java.util.UUID;

public interface QuizService extends BaseService<Quiz, UUID> {

    PageResponse<QuizResponse> search(BaseFilterRequest request);

    List<QuizResponse> listByLesson(UUID lessonId);

    /** Kèm toàn bộ câu hỏi, có cả đáp án đúng — chỉ dùng cho màn hình quản trị */
    QuizResponse getDetail(UUID id);

    QuizResponse createQuiz(QuizRequest request);

    QuizResponse updateQuiz(UUID id, QuizRequest request);

    void deleteQuiz(UUID id);

    int setPublished(List<UUID> ids, boolean published);

    // ===== Câu hỏi =====

    QuizQuestionResponse addQuestion(UUID quizId, QuizQuestionRequest request);

    QuizQuestionResponse updateQuestion(UUID quizId, UUID questionId, QuizQuestionRequest request);

    void deleteQuestion(UUID quizId, UUID questionId);

    void reorderQuestions(UUID quizId, ReorderRequest request);

    /**
     * Sinh câu hỏi tự động từ kho từ vựng.
     *
     * Chạy thử và ghi thật cho ra CÙNG một bộ câu hỏi (dùng chung hạt ngẫu nhiên
     * lấy từ id của đề), nên bản xem trước đúng là thứ sẽ được ghi.
     */
    GenerateQuestionsResult generateQuestions(UUID quizId, GenerateQuestionsRequest request);
}
