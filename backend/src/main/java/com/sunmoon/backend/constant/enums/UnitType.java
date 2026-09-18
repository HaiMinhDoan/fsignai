package com.sunmoon.backend.constant.enums;

// Đơn vị ngôn ngữ của một mục từ vựng VSL - dùng để chọn ngưỡng DTW mặc định
// và lọc bài học (bài đánh vần chỉ lấy LETTER, bài hội thoại lấy SENTENCE).
public enum UnitType {
    LETTER,
    NUMBER,
    WORD,
    PHRASE,
    SENTENCE
}
