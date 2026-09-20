package com.sunmoon.backend.dto.response.notification;

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
public class SystemSettingResponse {

    UUID id;
    String key;
    JsonNode value;
    String description;
    String updatedByName;
    OffsetDateTime updatedAt;
}
