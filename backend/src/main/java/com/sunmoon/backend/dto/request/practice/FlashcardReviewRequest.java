package com.sunmoon.backend.dto.request.practice;

import com.sunmoon.backend.constant.enums.FlashcardResult;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlashcardReviewRequest {

    @NotNull(message = "Thiếu kết quả ôn tập")
    FlashcardResult result;
}
