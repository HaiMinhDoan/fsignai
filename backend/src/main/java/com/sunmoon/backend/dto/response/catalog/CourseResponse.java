package com.sunmoon.backend.dto.response.catalog;

import com.sunmoon.backend.constant.enums.ProgressStatus;
import com.sunmoon.backend.constant.enums.SignLevel;
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
public class CourseResponse {

    UUID id;
    String slug;
    String titleVi;
    String descriptionVi;

    UUID coverFileId;
    String coverUrl;

    UUID topicId;
    String topicNameVi;

    SignLevel level;
    Integer displayOrder;
    Boolean generated;
    Boolean isPublished;

    /** Số bài học — service điền, mapper bỏ qua để tránh truy vấn N+1 */
    Long lessonCount;

    /** Chỉ có ở API chi tiết, danh sách để trống cho nhẹ */
    List<LessonResponse> lessons;

    /**
     * Hai trường dưới CHỈ có ở API học tập của người học đã đăng nhập — API
     * quản trị (CourseServiceImpl.getDetail) không điền, nên luôn vắng mặt
     * trong JSON trả cho admin (default_property_inclusion=non_null).
     */
    ProgressStatus myStatus;
    Integer myProgressPercent;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
