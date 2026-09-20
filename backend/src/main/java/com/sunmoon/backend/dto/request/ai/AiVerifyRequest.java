package com.sunmoon.backend.dto.request.ai;

import com.sunmoon.backend.constant.enums.AiCheckContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Landmark do TRÌNH DUYỆT trích bằng MediaPipe — video của người học không rời khỏi máy.
 * Định dạng khung hình khớp với ai-service (xem ai-service/README.md).
 */
@Getter
@Setter
public class AiVerifyRequest {

    @NotNull(message = "Thiếu từ cần chấm")
    private UUID signId;

    @NotNull(message = "Thiếu dữ liệu landmark")
    @Valid
    private LandmarkClip landmarks;

    /** Mặc định PRACTICE. Mức A chỉ luyện tập, chưa tính vào bài học/bài kiểm tra. */
    private AiCheckContext context;

    @Getter
    @Setter
    public static class LandmarkClip {

        /** Rộng / cao của khung hình đã quay, cần để đưa toạ độ chuẩn hoá về dạng đẳng hướng */
        @NotNull
        @DecimalMin(value = "0.2", message = "Tỉ lệ khung hình không hợp lệ")
        @DecimalMax(value = "5", message = "Tỉ lệ khung hình không hợp lệ")
        private Double aspect;

        @NotNull
        @Size(min = 8, max = 400, message = "Cần từ 8 đến 400 khung hình")
        @Valid
        private List<Frame> frames;
    }

    @Getter
    @Setter
    public static class Frame {
        /** 33 điểm × [x, y, z, visibility]; null khi khung này không thấy người */
        private List<List<Double>> pose;

        /** 0-2 bàn tay, mỗi tay 21 điểm × [x, y, z] */
        @Size(max = 2)
        private List<List<List<Double>>> hands;
    }
}
