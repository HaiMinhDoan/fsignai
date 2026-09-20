package com.sunmoon.backend.dto.request.catalog;

import com.sunmoon.backend.constant.enums.SignLevel;
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
public class CourseRequest {

    // Để trống thì service tự sinh từ titleVi qua VietnameseTextUtil.toSlug()
    @Size(max = 120)
    String slug;

    @NotBlank(message = "Tên khoá học không được để trống")
    @Size(max = 200)
    String titleVi;

    String descriptionVi;

    UUID coverFileId;

    UUID topicId;

    @Builder.Default
    SignLevel level = SignLevel.BEGINNER;

    @Builder.Default
    Integer displayOrder = 0;

    @Builder.Default
    Boolean isPublished = false;
}
