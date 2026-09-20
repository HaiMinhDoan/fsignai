package com.sunmoon.backend.dto.response.gamification;

import com.sunmoon.backend.constant.enums.GameCode;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameStartResponse {

    UUID sessionId;
    GameCode gameCode;
    /** Số sao tối đa của ván này */
    Integer maxScore;
    /** Số vòng/cặp - đúng hết thì đủ điểm tối đa */
    Integer totalRounds;

    /** MATCH_PAIR, MEMORY_FLIP: các cặp (video ↔ chữ) để bé nối/lật */
    List<GameSign> pairs;
    /** SPEED_GUESS, FINGER_DANCE: từng câu đoán */
    List<GameQuestion> questions;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GameSign {
        UUID signId;
        String wordVi;
        String videoUrl;
        String thumbnailUrl;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GameQuestion {
        /** Từ đúng - video của từ này là đề bài */
        UUID signId;
        String videoUrl;
        String thumbnailUrl;
        /** Đã trộn sẵn; đáp án đúng là dòng có signId trùng signId của câu */
        List<Option> options;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Option {
        UUID signId;
        String label;
    }
}
