package com.sunmoon.backend.constant.enums;

// Ba tầng ngưỡng, tra theo thứ tự ưu tiên: FEEDBACK_TUNED > EXEMPLAR_DERIVED > GROUP_DEFAULT.
// GROUP_DEFAULT phải có sẵn từ ngày đầu, vì hai tầng kia cần dữ liệu người dùng.
public enum ThresholdSource {
    EXEMPLAR_DERIVED,
    GROUP_DEFAULT,
    FEEDBACK_TUNED,
    MANUAL
}
