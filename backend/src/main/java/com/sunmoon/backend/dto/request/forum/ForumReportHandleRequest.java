package com.sunmoon.backend.dto.request.forum;

import com.sunmoon.backend.constant.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumReportHandleRequest {

    @NotNull(message = "Thiếu trạng thái xử lý")
    ReportStatus status;

    String handlerNote;
}
