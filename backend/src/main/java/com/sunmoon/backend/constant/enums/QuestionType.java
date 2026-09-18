package com.sunmoon.backend.constant.enums;

// AI_PERFORM là cầu nối sang FastAPI: người dùng tự làm ký hiệu trước webcam
// thay vì chọn đáp án. Ở mức A, dạng câu này KHÔNG tính điểm.
public enum QuestionType {
    VIDEO_TO_WORD,
    WORD_TO_VIDEO,
    MULTIPLE_CHOICE,
    MATCHING,
    AI_PERFORM
}
