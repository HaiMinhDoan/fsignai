package com.sunmoon.backend.dto.response.gamification;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Một rương thưởng, kèm việc người học đã mở được chưa */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RewardChestResponse {

    UUID id;
    String code;
    String nameVi;
    String descriptionVi;
    String iconName;
    Integer requiredPoints;

    /** Đã mở rồi hay chưa */
    Boolean opened;
    OffsetDateTime openedAt;
    /** Đủ sao để mở chưa — khác "đã mở": đủ rồi nhưng bé chưa bấm mở */
    Boolean unlockable;
    /** Còn thiếu bao nhiêu sao; 0 khi đã đủ */
    Integer pointsShort;
}
