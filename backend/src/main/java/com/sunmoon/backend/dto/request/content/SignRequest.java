package com.sunmoon.backend.dto.request.content;

import com.sunmoon.backend.constant.enums.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignRequest {

    // De trong thi service tu sinh tu wordVi qua VietnameseTextUtil.toGloss()
    @Size(max = 150)
    String gloss;

    @NotBlank(message = "Từ tiếng Việt không được để trống")
    @Size(max = 255)
    String wordVi;

    @Size(max = 255)
    String wordEn;

    // Mo ta cach lam ky hieu bang chu - bat buoc cho a11y nhung khong ep o day
    // vi noi dung nhap hang loat tu Bo GD&DT chua co san
    String descriptionVi;

    String noteVi;

    @Builder.Default
    SignLevel level = SignLevel.BEGINNER;

    @Builder.Default
    UnitType unitType = UnitType.WORD;

    @Builder.Default
    WordType wordType = WordType.KHONG_XAC_DINH;

    WordSubtype wordSubtype;

    SignDomain domain;

    UUID primaryTopicId;

    // Danh sach chu de (nhieu-nhieu). Null = giu nguyen, list rong = xoa het.
    List<UUID> topicIds;

    @Min(1) @Max(2)
    @Builder.Default
    Short handCount = 1;

    @Builder.Default
    SignSource source = SignSource.MANUAL;

    @Size(max = 255)
    String sourceRef;

    @Builder.Default
    Boolean isPublished = false;
}
