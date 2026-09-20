package com.sunmoon.backend.dto.response.ai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReadinessResponse {
    /** Từ này đã có mẫu để chấm chưa — giao diện ẩn nút "Tập với camera" nếu chưa */
    boolean ready;
    int exemplarCount;
}
