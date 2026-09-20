package com.sunmoon.backend.dto.response.forum;

import com.sunmoon.backend.constant.enums.ReportReason;
import com.sunmoon.backend.constant.enums.ReportStatus;
import com.sunmoon.backend.constant.enums.ReportTargetType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumReportResponse {

    UUID id;
    UUID reporterId;
    String reporterName;

    ReportTargetType targetType;
    UUID targetId;
    /** Xem trước nội dung bị báo cáo - tiêu đề bài hoặc đoạn đầu bình luận, để CMS không phải mở từng cái */
    String targetPreview;
    /** Bài/bình luận bị báo cáo đã bị gỡ trước đó chưa (dữ liệu có thể đã REMOVED bởi người khác) */
    Boolean targetStillExists;

    ReportReason reason;
    String note;
    ReportStatus status;

    UUID handledById;
    String handledByName;
    OffsetDateTime handledAt;
    String handlerNote;

    OffsetDateTime createdAt;
}
