package com.sunmoon.backend.dto.response.notification;

import com.sunmoon.backend.constant.enums.BlogCategory;
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
public class BlogPostResponse {

    UUID id;
    String slug;
    String titleVi;
    String excerptVi;
    String contentMd;
    String coverUrl;
    String authorName;
    BlogCategory category;
    Integer viewCount;
    Boolean isPublished;
    OffsetDateTime publishedAt;
    OffsetDateTime createdAt;
}
