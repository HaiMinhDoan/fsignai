package com.sunmoon.backend.dto.response.dictionary;

import com.sunmoon.backend.constant.enums.SignLevel;
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
public class SavedSignResponse {

    UUID id;
    UUID signId;
    String wordVi;
    String gloss;
    SignLevel level;
    String videoUrl;
    String thumbnailUrl;
    String note;
    OffsetDateTime savedAt;
}
