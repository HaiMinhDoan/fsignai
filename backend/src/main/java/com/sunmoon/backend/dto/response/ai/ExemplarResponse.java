package com.sunmoon.backend.dto.response.ai;

import com.sunmoon.backend.constant.enums.ExemplarBuildStatus;
import com.sunmoon.backend.constant.enums.Region;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class ExemplarResponse {
    UUID id;
    UUID signVideoId;
    Region region;
    ExemplarBuildStatus buildStatus;
    String buildError;
    Integer frameCount;
    BigDecimal qualityScore;
    String modelVersion;
    Boolean isActive;
    /** Mẫu này thuộc phiên bản đặc trưng hiện hành hay đã cũ và cần sinh lại */
    boolean current;
    OffsetDateTime updatedAt;
}
