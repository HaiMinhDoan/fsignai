package com.sunmoon.backend.dto.response.progress;

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
public class AchievementResponse {

    UUID id;
    String code;
    String nameVi;
    String descriptionVi;
    String iconName;
    UUID iconFileId;
    String iconUrl;
    JsonNode criteriaJson;
    Integer displayOrder;
    Boolean isActive;

    /** Chỉ có ở API của người học — huy hiệu này người học hiện tại đã đạt chưa */
    Boolean earned;
    OffsetDateTime earnedAt;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
