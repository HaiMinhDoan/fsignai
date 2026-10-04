package com.sunmoon.backend.dto.response.payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanResponse {
    String code;            // FREE | PREMIUM
    String nameVi;
    String taglineVi;
    String descriptionVi;
    long price;             // VND, 0 với gói Free
    Integer durationDays;   // null với gói Free
    /** Quyền lợi riêng của gói (gói Premium kèm "toàn bộ quyền lợi của gói Free") */
    List<String> benefits;
    boolean includesFree;
}
