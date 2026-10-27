package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.analytics.PageViewRequest;
import com.sunmoon.backend.dto.response.analytics.DashboardResponse;
import com.sunmoon.backend.dto.response.analytics.UserStatResponse;

import java.util.List;

import java.util.UUID;

public interface AnalyticsService {

    /**
     * Ghi một lượt xem trang. Không bao giờ ném lỗi ra ngoài: đo đếm hỏng thì bỏ qua lượt đó,
     * chứ không được làm người học thấy thông báo lỗi.
     */
    void trackPageView(PageViewRequest request, UUID userId, String userAgent);

    /** Toàn bộ số liệu Dashboard cho N ngày gần nhất (tính cả hôm nay), so với N ngày liền trước */
    DashboardResponse dashboard(int days);

    /** Thống kê từng người dùng (từ trước tới nay) để xuất Excel; ghi nhật ký audit vì có thông tin cá nhân */
    List<UserStatResponse> userStats(UUID exportedBy);
}
