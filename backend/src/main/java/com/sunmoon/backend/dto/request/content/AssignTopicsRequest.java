package com.sunmoon.backend.dto.request.content;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

/**
 * Gán chủ đề cho nhiều từ vựng cùng lúc.
 *
 * Vì sao cần: từ điển đã nạp 3.322 từ nhưng bảng sign_topics đang rỗng. Gán tay
 * từng từ là bất khả thi; mà chưa gán chủ đề thì không sinh được khoá học, không
 * trộn được đề theo chủ đề, và người học cũng không duyệt từ vựng theo chủ đề được.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssignTopicsRequest {

    @NotEmpty(message = "Phải chọn ít nhất một từ vựng")
    List<UUID> signIds;

    @NotEmpty(message = "Phải chọn ít nhất một chủ đề")
    List<UUID> topicIds;

    /**
     * true  = thay thế toàn bộ chủ đề cũ của các từ này
     * false = thêm vào, giữ nguyên chủ đề đã có (mặc định)
     *
     * Mặc định là thêm chứ không thay, vì thao tác hàng loạt mà lỡ tay thay thế
     * sẽ xoá sạch công phân loại trước đó mà không có cách hoàn tác.
     */
    @Builder.Default
    Boolean replace = false;

    /**
     * Đặt luôn làm chủ đề chính của từ (cột signs.primary_topic_id).
     * Chỉ có tác dụng khi topicIds chỉ có đúng một chủ đề.
     */
    @Builder.Default
    Boolean setPrimary = false;
}
