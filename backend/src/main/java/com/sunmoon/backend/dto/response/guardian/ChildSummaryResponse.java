package com.sunmoon.backend.dto.response.guardian;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/** Một người học mà phụ huynh/thầy cô đang theo dõi */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChildSummaryResponse {

    UUID childUserId;
    String fullName;
    String ageRange;
    String relationship;
    /** % mục tiêu tuần đã đạt, tính từ số ngày có học trong 7 ngày gần nhất */
    Integer weekPercent;
}
