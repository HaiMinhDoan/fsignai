package com.sunmoon.backend.dto.response.ai;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ExemplarJobStatusResponse {
    boolean running;
    int total;
    int done;
    int failed;
    String lastMessage;
    OffsetDateTime startedAt;
    OffsetDateTime finishedAt;

    // Toàn cảnh, đọc thẳng từ CSDL — không phụ thuộc lần chạy nền nào
    String featureVersion;
    long totalVideos;
    long readyExemplars;
    long failedExemplars;
    long signsReady;
    long totalSigns;
}
