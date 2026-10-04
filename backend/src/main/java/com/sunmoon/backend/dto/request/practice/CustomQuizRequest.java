package com.sunmoon.backend.dto.request.practice;

import com.sunmoon.backend.constant.enums.Region;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

/** Người học tự tạo đề: chọn chủ đề muốn ôn và số câu */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomQuizRequest {

    /** Rỗng hoặc null = kiểm tra trên TẤT CẢ chủ đề */
    List<UUID> topicIds;

    @Min(value = 5, message = "Đề cần ít nhất 5 câu")
    @Max(value = 30, message = "Đề tối đa 30 câu")
    @Builder.Default
    Integer questionCount = 10;

    @Builder.Default
    Region region = Region.COMMON;
}
