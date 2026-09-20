package com.sunmoon.backend.dto.request.forum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumCategoryRequest {

    @Size(max = 120)
    String slug;

    @NotBlank(message = "Tên chuyên mục không được để trống")
    @Size(max = 150)
    String nameVi;

    String descriptionVi;

    @Size(max = 100)
    String iconName;

    Integer displayOrder;

    Boolean isLocked;

    Boolean isPublished;
}
