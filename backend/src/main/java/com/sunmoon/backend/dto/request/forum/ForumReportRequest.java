package com.sunmoon.backend.dto.request.forum;

import com.sunmoon.backend.constant.enums.ReportReason;
import com.sunmoon.backend.constant.enums.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumReportRequest {

    @NotNull(message = "Thiếu loại nội dung bị báo cáo")
    ReportTargetType targetType;

    @NotNull(message = "Thiếu nội dung bị báo cáo")
    UUID targetId;

    @NotNull(message = "Vui lòng chọn lý do báo cáo")
    ReportReason reason;

    String note;
}
