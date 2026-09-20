package com.sunmoon.backend.dto.response.gamification;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/** Một dòng trên bảng xếp hạng tuần */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeaderboardRowResponse {

    Integer rank;
    UUID userId;
    String fullName;
    Integer points;
    /** Số ván chơi đã hoàn thành — lấy từ game_sessions */
    Integer gamesWon;
    /** Dòng này có phải chính người đang xem không, để giao diện tô sáng */
    Boolean isMe;
}
