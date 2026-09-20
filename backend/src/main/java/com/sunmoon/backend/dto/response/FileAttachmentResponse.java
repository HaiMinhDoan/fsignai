package com.sunmoon.backend.dto.response;

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
public class FileAttachmentResponse {

    UUID id;
    String bucket;
    String objectKey;
    /** URL truy cập trực tiếp — giao diện gán thẳng vào thẻ img hoặc video */
    String url;
    String originalName;
    String mimeType;
    String extension;
    Long sizeBytes;
    String entityType;
    UUID entityId;
    OffsetDateTime createdAt;
}
