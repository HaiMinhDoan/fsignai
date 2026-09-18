package com.sunmoon.backend.dto.response.content;

import com.sunmoon.backend.constant.enums.IngestStatus;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.constant.enums.ViewAngle;
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
public class SignVideoResponse {

    UUID id;
    UUID signId;
    Region region;
    ViewAngle viewAngle;

    // URL cong khai lay tu FileAttachment.getPublicUrl()
    String videoUrl;
    String thumbnailUrl;

    String signerLabel;
    Integer durationMs;
    Integer width;
    Integer height;
    String captionVi;
    Boolean isPrimary;

    IngestStatus ingestStatus;
    String sourceUrl;
    String ingestError;

    OffsetDateTime createdAt;
}
