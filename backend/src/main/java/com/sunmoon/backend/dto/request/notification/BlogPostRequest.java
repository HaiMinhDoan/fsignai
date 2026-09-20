package com.sunmoon.backend.dto.request.notification;

import com.sunmoon.backend.constant.enums.BlogCategory;
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
public class BlogPostRequest {

    @Size(max = 160)
    String slug;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255)
    String titleVi;

    String excerptVi;

    @NotBlank(message = "Nội dung không được để trống")
    String contentMd;

    UUID coverFileId;

    @Builder.Default
    BlogCategory category = BlogCategory.BLOG;

    @Builder.Default
    Boolean isPublished = false;
}
