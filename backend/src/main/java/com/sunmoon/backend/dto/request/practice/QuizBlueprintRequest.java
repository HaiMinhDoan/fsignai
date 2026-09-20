package com.sunmoon.backend.dto.request.practice;

import com.sunmoon.backend.constant.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cấu hình đề trộn: khai báo LUẬT sinh đề, không soạn từng câu.
 *
 * Người học chọn chủ đề và vùng miền, hệ thống rút ngẫu nhiên từ kho theo luật
 * này. Nhờ vậy mỗi lần thi là một đề khác nhau mà không ai phải soạn thêm câu.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizBlueprintRequest {

    /** Mã định danh để gọi từ giao diện học, ví dụ "on-tap-gia-dinh" */
    @Size(max = 100)
    String code;

    @NotBlank(message = "Tên cấu hình không được để trống")
    @Size(max = 200)
    String titleVi;

    String descriptionVi;

    /** Để trống = lấy từ mọi chủ đề */
    List<UUID> topicIds;

    /** Để trống = mọi cấp độ */
    List<SignLevel> levels;

    /** Để trống = mọi đơn vị ngôn ngữ (từ, cụm từ, câu...) */
    List<UnitType> unitTypes;

    /** null = không giới hạn từ loại */
    List<WordType> wordTypes;

    /** Tỉ lệ các dạng câu, ví dụ {"VIDEO_TO_WORD": 5, "MATCHING": 2} */
    Map<QuestionType, Integer> questionTypeMix;

    @Min(value = 1, message = "Số câu phải từ 1 đến 100")
    @Max(value = 100, message = "Số câu phải từ 1 đến 100")
    @Builder.Default
    Integer questionCount = 10;

    @Min(value = 2, message = "Mỗi câu phải có từ 2 đến 8 lựa chọn")
    @Max(value = 8, message = "Mỗi câu phải có từ 2 đến 8 lựa chọn")
    @Builder.Default
    Integer optionCount = 4;

    @Min(0) @Max(100)
    @Builder.Default
    Integer passScore = 70;

    @Min(value = 1, message = "Thời gian làm bài phải lớn hơn 0")
    Integer timeLimitSeconds;

    @Builder.Default
    DistractorStrategy distractorStrategy = DistractorStrategy.EASILY_CONFUSED;

    /** Số ngày tránh hỏi lại cùng nhóm từ; 0 = không tránh */
    @Min(value = 0, message = "Số ngày tránh lặp không được âm")
    @Builder.Default
    Integer avoidRecentDays = 30;

    @Builder.Default
    Boolean isActive = true;
}
