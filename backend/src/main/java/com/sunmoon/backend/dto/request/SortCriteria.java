package com.sunmoon.backend.dto.request;

import com.sunmoon.backend.constant.enums.SortDirection;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SortCriteria {
    String fieldName;
    SortDirection direction;
}
