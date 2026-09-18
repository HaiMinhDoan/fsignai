package com.sunmoon.backend.dto.response.content;

import com.sunmoon.backend.constant.enums.TopicCategory;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicResponse {

    UUID id;
    UUID parentId;
    String parentNameVi;
    String slug;
    String nameVi;
    String descriptionVi;
    String iconName;
    String iconUrl;
    String coverUrl;
    TopicCategory category;
    Integer displayOrder;
    Boolean isPublished;

    // So tu vung thuoc chu de nay - admin can biet chu de nao con rong
    Long signCount;

    // Chu de con, dung cho component Tree cua vben
    List<TopicResponse> children;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
