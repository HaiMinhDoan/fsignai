package com.sunmoon.backend.dto.response.practice;

import com.sunmoon.backend.constant.enums.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizBlueprintResponse {

    UUID id;
    String code;
    String titleVi;
    String descriptionVi;

    List<UUID> topicIds;
    /** Tên chủ đề, để giao diện khỏi phải tra ngược từng id */
    List<String> topicNames;

    List<SignLevel> levels;
    List<UnitType> unitTypes;
    List<WordType> wordTypes;

    Map<QuestionType, Integer> questionTypeMix;

    Integer questionCount;
    Integer optionCount;
    Integer passScore;
    Integer timeLimitSeconds;
    DistractorStrategy distractorStrategy;
    Integer avoidRecentDays;
    Boolean isActive;

    /**
     * Số từ vựng thực sự khớp bộ lọc của cấu hình này.
     *
     * Quan trọng hơn vẻ ngoài: cấu hình đòi 20 câu mà kho chỉ khớp 6 từ thì đề
     * sinh ra sẽ thiếu câu. Cho thấy ngay lúc soạn thay vì để người học phát hiện.
     */
    Long matchingSignCount;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
