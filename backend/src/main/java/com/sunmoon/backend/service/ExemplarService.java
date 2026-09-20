package com.sunmoon.backend.service;

import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.dto.response.ai.ExemplarJobStatusResponse;
import com.sunmoon.backend.dto.response.ai.ExemplarResponse;

import java.util.List;
import java.util.UUID;

/**
 * Mẫu chuẩn để chấm ký hiệu. Sinh tự động bằng cách chạy MediaPipe (qua ai-service) trên chính video
 * từ điển đã nạp — không quay thêm clip nào.
 */
public interface ExemplarService {

    /** Mẫu đã tải sẵn đặc trưng, sẵn sàng gửi sang ai-service */
    record LoadedExemplar(UUID id, Region region, double[][] features) {}

    List<ExemplarResponse> listForSign(UUID signId);

    /** Sinh lại mẫu cho mọi video của một từ (đồng bộ: vài video, vài giây) */
    List<ExemplarResponse> rebuildForSign(UUID signId);

    /** Bật/tắt một mẫu — tắt mẫu xấu (quay hỏng, ký sai) mà không xoá */
    void setActive(UUID exemplarId, boolean active);

    /** Khởi động job nền sinh mẫu cho các video còn thiếu. Chỉ một job chạy tại một thời điểm. */
    ExemplarJobStatusResponse startBuildJob(int limit, boolean retryFailed);

    ExemplarJobStatusResponse jobStatus();

    /** Các mẫu dùng được để chấm từ này (đúng phiên bản, đã dựng xong, đang bật) */
    List<LoadedExemplar> loadUsable(UUID signId);

    int usableCount(UUID signId);
}
