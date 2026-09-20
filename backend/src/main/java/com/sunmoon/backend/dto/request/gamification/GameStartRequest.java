package com.sunmoon.backend.dto.request.gamification;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/** Cả hai đều tuỳ chọn; cả hai null = rút từ cả kho từ. packId thắng topicId nếu gửi cả hai. */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameStartRequest {

    UUID topicId;
    UUID packId;
}
