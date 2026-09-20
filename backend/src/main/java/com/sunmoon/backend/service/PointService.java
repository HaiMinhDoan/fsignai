package com.sunmoon.backend.service;

import com.sunmoon.backend.constant.enums.PointSource;

import java.util.UUID;

public interface PointService {

    /**
     * Cộng điểm (sao) cho người học: ghi point_events rồi cập nhật bộ nhớ đệm
     * user_points (tổng / tuần / tháng / cấp). Có sourceId thì mỗi (user, source,
     * sourceId) chỉ được cộng MỘT lần — gọi lại là bỏ qua, nên nộp bài/hoàn thành
     * lặp không cộng trùng.
     *
     * @return số điểm thực sự được cộng (0 nếu bỏ qua)
     */
    int award(UUID userId, PointSource source, UUID sourceId, int points, String noteVi);
}
