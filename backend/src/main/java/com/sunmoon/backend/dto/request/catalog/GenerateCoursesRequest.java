package com.sunmoon.backend.dto.request.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

/**
 * Sinh khoá học và bài học tự động từ từ vựng đã có theo chủ đề.
 *
 * Vì sao cần: từ điển đã nạp hơn 3.300 từ. Nếu bắt biên tập viên tự tay tạo
 * từng khoá, từng bài, rồi kéo từng từ vào bài thì sẽ mất hàng tuần. Việc gom
 * từ theo chủ đề và cắt thành bài đều nhau là việc máy làm được.
 *
 * Kết quả luôn ở trạng thái CHƯA xuất bản: máy chia bài chỉ là bản nháp, người
 * biên tập vẫn phải xem lại rồi mới cho học viên thấy.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateCoursesRequest {

    /** Để trống nghĩa là sinh cho tất cả chủ đề đang có từ vựng */
    List<UUID> topicIds;

    @Min(value = 3, message = "Mỗi bài phải có ít nhất 3 từ")
    @Max(value = 30, message = "Mỗi bài không nên quá 30 từ")
    @Builder.Default
    Integer signsPerLesson = 8;

    /**
     * Chỉ lấy từ đã xuất bản. Mặc định false vì sau khi nạp từ điển, toàn bộ
     * 3.322 từ đều đang ở trạng thái chưa xuất bản — bật lên sẽ không sinh được gì.
     */
    @Builder.Default
    Boolean onlyPublishedSigns = false;

    /**
     * Chỉ tính toán và trả về kết quả dự kiến, không ghi vào CSDL.
     * Luôn nên chạy thử trước khi sinh thật.
     */
    @Builder.Default
    Boolean dryRun = true;
}
