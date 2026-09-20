package com.sunmoon.backend.dto.response.forum;

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
public class ForumCategoryResponse {

    UUID id;
    String slug;
    String nameVi;
    String descriptionVi;
    String iconName;
    Integer displayOrder;
    Boolean isLocked;
    Boolean isPublished;
    Long postCount;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
