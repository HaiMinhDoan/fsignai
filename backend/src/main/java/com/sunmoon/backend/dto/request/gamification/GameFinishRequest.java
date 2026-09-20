package com.sunmoon.backend.dto.request.gamification;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameFinishRequest {

    @NotNull
    @Min(0)
    @Max(100)
    Integer correctCount;

    /** Số lần sai (lượt ghép sai, đoán sai, hết giờ...) - có thể lớn hơn số vòng */
    @NotNull
    @Min(0)
    @Max(500)
    Integer wrongCount;
}
