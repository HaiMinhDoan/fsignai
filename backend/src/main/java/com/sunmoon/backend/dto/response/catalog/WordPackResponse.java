package com.sunmoon.backend.dto.response.catalog;

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
public class WordPackResponse {

    UUID id;
    String code;
    String titleVi;
    String descriptionVi;

    UUID coverFileId;
    String coverUrl;

    UUID topicId;
    String topicNameVi;

    SignLevel level;
    String islandColor;
    String iconName;
    Integer displayOrder;

    UUID unlockAfterPackId;
    /** Tên gói cần hoàn thành trước — để CMS hiện được luôn, khỏi phải tra ngược */
    String unlockAfterPackTitleVi;

    Integer passScore;
    Boolean isPublished;

    /** Service tự điền để gom thành một truy vấn, mapper không đụng vào */
    Long itemCount;

    /** Chỉ có ở API chi tiết, danh sách để trống cho nhẹ */
    List<WordPackItemResponse> items;

    /**
     * Bốn trường dưới CHỈ có ở API học tập của người học — API quản trị
     * không điền, nên luôn vắng mặt trong JSON trả cho admin
     * (spring.jackson.default-property-inclusion=non_null).
     */
    Boolean unlocked;
    String myStatus; // IN_PROGRESS | COMPLETED
    Integer myItemsCompleted;
    Integer myStars;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
