package com.sunmoon.backend.dto.request.catalog;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonRequest {

    @NotBlank(message = "Tên bài học không được để trống")
    @Size(max = 200)
    String titleVi;

    String descriptionVi;

    /**
     * Để trống khi tạo mới thì service tự xếp xuống cuối danh sách.
     * Người dùng sắp lại thứ tự bằng API reorder chứ không sửa tay số này.
     */
    Integer displayOrder;

    @Min(value = 1, message = "Thời lượng ước tính phải lớn hơn 0")
    @Builder.Default
    Integer estimatedMinutes = 5;

    @Builder.Default
    Boolean isPublished = false;
}
