package com.sunmoon.backend.dto.response.ai;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AiVerifyResponse {

    UUID resultId;
    UUID signId;
    double score;
    boolean passed;
    Feedback feedback;
    String modelVersion;
    OffsetDateTime checkedAt;

    /** Điểm từng tham số ký hiệu — thứ người học sửa được; một số tổng "72/100" thì không */
    @Getter
    @Builder
    public static class Feedback {
        int handshape;
        int location;
        int movement;
        /** Lời khuyên tiếng Việt, đã dựng từ mã gợi ý */
        List<String> hints;
        /** Mã gợi ý gốc, để giao diện chọn biểu tượng nếu cần */
        List<String> hintCodes;
    }
}
