package com.sunmoon.backend.dto.request.forum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumPostRequest {

    @NotNull(message = "Vui lòng chọn chuyên mục")
    UUID categoryId;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255)
    String titleVi;

    @NotBlank(message = "Nội dung không được để trống")
    String bodyMd;

    /** Gắn bài với một từ trong từ điển - không bắt buộc */
    UUID signId;
}
