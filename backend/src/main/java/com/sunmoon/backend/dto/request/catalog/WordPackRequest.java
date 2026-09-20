package com.sunmoon.backend.dto.request.catalog;

import com.sunmoon.backend.constant.enums.SignLevel;
import jakarta.validation.constraints.NotBlank;
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
public class WordPackRequest {

    // Để trống thì service tự sinh từ titleVi qua VietnameseTextUtil.toSlug()
    @Size(max = 100)
    String code;

    @NotBlank(message = "Tên gói từ không được để trống")
    @Size(max = 255)
    String titleVi;

    String descriptionVi;

    UUID coverFileId;

    UUID topicId;

    @Builder.Default
    SignLevel level = SignLevel.BEGINNER;

    String islandColor;

    String iconName;

    @Builder.Default
    Integer displayOrder = 0;

    /** null = mở sẵn ngay từ đầu, không cần hoàn thành gói nào trước */
    UUID unlockAfterPackId;

    @Builder.Default
    Integer passScore = 80;

    @Builder.Default
    Boolean isPublished = false;
}
