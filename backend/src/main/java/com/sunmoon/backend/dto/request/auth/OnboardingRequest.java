package com.sunmoon.backend.dto.request.auth;

import com.sunmoon.backend.constant.enums.CurrentLevel;
import com.sunmoon.backend.constant.enums.LearnReason;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

/**
 * 4 câu hỏi sau đăng ký. Mọi trường đều để trống được - cho phép lưu từng
 * câu một khi người học trả lời tới đâu lưu tới đó (đóng app giữa chừng không mất).
 * Chỉ khi complete=true mới bắt buộc đủ 4 câu và đóng dấu completed_at.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OnboardingRequest {

    LearnReason learnReason;

    CurrentLevel currentLevel;

    /** Chỉ nhận 5 / 10 / 15 / 30 - khớp CHECK constraint của cột daily_minutes */
    Integer dailyMinutes;

    List<UUID> interestedTopics;

    /** true = đây là câu trả lời cuối cùng, đóng dấu hoàn tất onboarding */
    @Builder.Default
    boolean complete = false;
}
