package com.sunmoon.backend.dto.response.practice;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Một lựa chọn trong câu hỏi trắc nghiệm.
 *
 * Kèm sẵn videoUrl để giao diện làm bài không phải gọi thêm API cho từng lựa
 * chọn: một đề 10 câu x 4 lựa chọn sẽ thành 40 lượt gọi nếu để giao diện tự tra.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizOptionResponse {

    UUID signId;
    String label;
    String videoUrl;
}
