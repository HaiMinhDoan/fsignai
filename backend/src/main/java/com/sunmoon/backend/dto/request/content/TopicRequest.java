package com.sunmoon.backend.dto.request.content;

import com.sunmoon.backend.constant.enums.TopicCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicRequest {

    UUID parentId;

    // De trong thi service tu sinh tu nameVi qua VietnameseTextUtil.toSlug()
    @Size(max = 120)
    String slug;

    @NotBlank(message = "Tên chủ đề không được để trống")
    @Size(max = 150)
    String nameVi;

    String descriptionVi;

    @Size(max = 100)
    String iconName;

    UUID iconFileId;
    UUID coverFileId;

    @Builder.Default
    TopicCategory category = TopicCategory.SIMPLE_SIGN;

    @Builder.Default
    Integer displayOrder = 0;

    @Builder.Default
    Boolean isPublished = false;
}
