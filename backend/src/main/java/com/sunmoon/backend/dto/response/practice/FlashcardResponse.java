package com.sunmoon.backend.dto.response.practice;

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
public class FlashcardResponse {

    UUID signId;
    String wordVi;
    String gloss;
    String descriptionVi;
    String videoUrl;
    String thumbnailUrl;

    /** true = chưa từng ôn từ này bao giờ; false = đến hạn ôn lại */
    Boolean isNew;
    /** null nếu isNew=true */
    OffsetDateTime dueAt;
}
