package com.sunmoon.backend.dto.response.auth;

import com.sunmoon.backend.constant.enums.CurrentLevel;
import com.sunmoon.backend.constant.enums.LearnReason;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OnboardingResultResponse {

    LearnReason learnReason;
    CurrentLevel currentLevel;
    Integer dailyMinutes;
    List<UUID> interestedTopics;

    boolean completed;
    OffsetDateTime completedAt;
}
