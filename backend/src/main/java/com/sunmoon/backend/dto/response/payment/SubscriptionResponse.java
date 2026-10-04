package com.sunmoon.backend.dto.response.payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriptionResponse {
    String plan;                    // FREE | PREMIUM
    boolean premium;
    OffsetDateTime premiumUntil;
    /** Nhân sự nội bộ (quản trị, biên tập, kiểm duyệt, thẩm định) dùng mọi tính năng, không cần mua */
    boolean staff;
}
