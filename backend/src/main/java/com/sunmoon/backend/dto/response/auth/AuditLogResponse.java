package com.sunmoon.backend.dto.response.auth;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLogResponse {

    UUID id;
    UUID actorId;
    String actorName;
    String action;
    String entityType;
    UUID entityId;
    JsonNode beforeData;
    JsonNode afterData;
    String ipAddress;
    OffsetDateTime createdAt;
}
