package com.sunmoon.backend.dto.response.practice;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlashcardReviewResultResponse {

    OffsetDateTime nextDueAt;
    Integer intervalDays;
    Double easeFactor;
    Integer repetitions;
}
