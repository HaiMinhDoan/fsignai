package com.sunmoon.backend.dto.request.practice;

import com.sunmoon.backend.constant.enums.DistractorStrategy;
import com.sunmoon.backend.constant.enums.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sinh câu hỏi tự động từ kho từ vựng.
 *
 * Vì sao cần: soạn tay một câu trắc nghiệm nghĩa là chọn từ, viết câu dẫn, rồi
 * tự nghĩ ra ba đáp án nhiễu. Với vài nghìn từ thì đó là việc không làm xuể.
 * Máy có sẵn toàn bộ kho từ và biết từ nào cùng chủ đề, nên phần cơ học này
 * để máy làm, người chỉ xem lại và sửa những câu chưa ổn.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateQuestionsRequest {

    /** Lấy từ vựng trong các chủ đề này. Để trống thì dùng signIds. */
    List<UUID> topicIds;

    /** Chỉ định thẳng danh sách từ. Ưu tiên hơn topicIds nếu cả hai cùng có. */
    List<UUID> signIds;

    @Min(value = 1, message = "Số câu phải từ 1 đến 100")
    @Max(value = 100, message = "Số câu phải từ 1 đến 100")
    @Builder.Default
    Integer questionCount = 10;

    /**
     * Tỉ lệ các dạng câu, ví dụ {"VIDEO_TO_WORD": 5, "WORD_TO_VIDEO": 3}.
     * Để trống thì chia đều giữa VIDEO_TO_WORD và WORD_TO_VIDEO.
     */
    Map<QuestionType, Integer> questionTypeMix;

    @Min(value = 2, message = "Mỗi câu phải có từ 2 đến 8 lựa chọn")
    @Max(value = 8, message = "Mỗi câu phải có từ 2 đến 8 lựa chọn")
    @Builder.Default
    Integer optionCount = 4;

    @Builder.Default
    DistractorStrategy distractorStrategy = DistractorStrategy.EASILY_CONFUSED;

    /**
     * Chỉ lấy từ đã có video. Mặc định bật: câu hỏi VIDEO_TO_WORD mà từ không có
     * video thì người học nhìn vào ô trống, không có cách nào trả lời đúng.
     */
    @Builder.Default
    Boolean requireVideo = true;

    /** Chỉ tính toán và trả về xem trước, không ghi vào CSDL */
    @Builder.Default
    Boolean dryRun = true;
}
