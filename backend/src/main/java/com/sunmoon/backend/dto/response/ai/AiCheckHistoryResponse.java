package com.sunmoon.backend.dto.response.ai;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class AiCheckHistoryResponse {
    UUID id;
    UUID signId;
    String wordVi;
    double score;
    boolean passed;
    Integer handshape;
    Integer location;
    Integer movement;
    OffsetDateTime checkedAt;
}
