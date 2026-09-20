package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.response.gamification.LearnerStatsResponse;

import java.util.UUID;

public interface LearnerStatsService {

    /**
     * Số liệu tổng hợp cho thanh đầu trang. Người học chưa từng hoạt động vẫn
     * nhận về một bản ghi đầy đủ với số 0 — giao diện không phải xử lý null,
     * và bé mới vào vẫn thấy ô "0 ngày" để biết chỗ đó rồi sẽ có gì.
     */
    LearnerStatsResponse getStats(UUID userId);
}
