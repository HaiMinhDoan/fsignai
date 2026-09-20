package com.sunmoon.backend.service;

import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.dto.request.practice.SubmitAttemptRequest;
import com.sunmoon.backend.dto.response.practice.QuizAttemptResponse;

import java.util.UUID;

/**
 * Một lượt thi của người học — nơi mục "Ngân hàng câu hỏi + đề trộn" và mục
 * "Tiến độ & chuỗi ngày học" thật sự gặp nhau: bắt đầu lượt thi thì rút câu
 * hỏi (QuestionGenerator), nộp bài thì cập nhật nhật ký hoạt động
 * (ProgressService), chuỗi ngày (StreakService) và huy hiệu (AchievementService).
 */
public interface QuizAttemptService {

    /** Bắt đầu lượt thi từ một đề soạn tay (Quiz). Đề phải đã xuất bản và có ít nhất một câu hỏi */
    QuizAttemptResponse startFromQuiz(UUID userId, UUID quizId, Region region);

    /**
     * Bắt đầu lượt thi từ một cấu hình đề trộn (QuizBlueprint). Rút ngẫu nhiên
     * thật — KHÔNG dùng hạt cố định như lúc admin rút thử — nên mỗi lượt thi
     * là một đề khác nhau. Bộ câu hỏi được chốt và lưu lại ngay lúc bắt đầu để
     * xem lại/nộp bài sau vẫn ra đúng đề đó.
     */
    QuizAttemptResponse startFromBlueprint(UUID userId, UUID blueprintId, Region region);

    /** Xem lại một lượt thi — của chính mình, dùng để tiếp tục làm dở hoặc xem lại kết quả */
    QuizAttemptResponse getAttempt(UUID userId, UUID attemptId);

    /** Nộp bài, chấm điểm, và cập nhật tiến độ/chuỗi ngày/huy hiệu liên quan */
    QuizAttemptResponse submit(UUID userId, UUID attemptId, SubmitAttemptRequest request);
}
