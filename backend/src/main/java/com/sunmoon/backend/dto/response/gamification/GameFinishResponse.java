package com.sunmoon.backend.dto.response.gamification;

import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameFinishResponse {

    Integer score;
    Integer maxScore;
    Integer correctCount;
    Integer wrongCount;
    Integer durationSeconds;
    /** Sao thực sự được cộng lần này (bằng score; 0 nếu chốt trùng) */
    Integer starsEarned;
    /** Tổng sao sau khi cộng */
    Integer totalStars;
}
