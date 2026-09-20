package com.sunmoon.backend.dto.response.catalog;

import com.sunmoon.backend.constant.enums.ProgressStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonResponse {

    UUID id;
    UUID courseId;
    String courseTitleVi;

    String titleVi;
    String descriptionVi;
    Integer displayOrder;
    Integer estimatedMinutes;
    Boolean generated;
    Boolean isPublished;

    /** Số nội dung trong bài — service điền, tránh truy vấn N+1 */
    Long itemCount;

    /** Chỉ có ở API chi tiết bài học */
    List<LessonItemResponse> items;

    /** Ba trường dưới CHỈ có ở API học tập của người học đã đăng nhập */
    ProgressStatus myStatus;
    Integer myProgressPercent;
    UUID myLastItemId;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
