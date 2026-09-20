package com.sunmoon.backend.dto.request.progress;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Điều kiện đạt huy hiệu.
 *
 * criteriaJson đi thẳng vào cột jsonb, KHÔNG qua chuyển đổi thủ công như
 * QuizBlueprint — vì đây là một object tự do (không phải List/Map enum) nên
 * Jackson tự bind vào JsonNode được, không cần @Named converter.
 *
 * Lược đồ hiện có (khớp dữ liệu seed sẵn trong CSDL, AchievementServiceImpl
 * chỉ biết đánh giá đúng 5 dạng này):
 *   {"type":"lessons_completed","value":N}
 *   {"type":"streak","value":N}            (dùng longestStreak, huy hiệu không mất khi chuỗi đứt)
 *   {"type":"signs_learned","value":N}
 *   {"type":"quiz_perfect","value":N}
 *   {"type":"ai_checks","value":N}
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AchievementRequest {

    @NotBlank(message = "Mã huy hiệu không được để trống")
    @Size(max = 100)
    String code;

    @NotBlank(message = "Tên huy hiệu không được để trống")
    @Size(max = 150)
    String nameVi;

    String descriptionVi;

    @Size(max = 100)
    String iconName;

    UUID iconFileId;

    @NotNull(message = "Phải khai điều kiện đạt huy hiệu")
    JsonNode criteriaJson;

    @Builder.Default
    Integer displayOrder = 0;

    @Builder.Default
    Boolean isActive = true;
}
