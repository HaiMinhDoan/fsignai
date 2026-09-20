package com.sunmoon.backend.service;

import com.sunmoon.backend.constant.enums.NotificationChannel;
import com.sunmoon.backend.constant.enums.NotificationStatus;
import com.sunmoon.backend.constant.enums.NotificationType;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.notification.NotificationResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    /**
     * Tạo thông báo trong app, và nếu alsoEmail=true thì GỬI EMAIL THẬT ngay
     * qua JavaMailSender đã cấu hình sẵn (không phải hàng đợi nền - gửi đồng
     * bộ, chấp nhận request chậm hơn một chút để đổi lấy đơn giản).
     * Không bao giờ throw ra ngoài - thông báo là phụ, không được làm hỏng
     * thao tác chính gọi tới nó.
     */
    void notify(UUID userId, NotificationType type, String titleVi, String bodyVi, String actionUrl, boolean alsoEmail);

    PageResponse<NotificationResponse> listMine(UUID userId, Pageable pageable);

    long unreadCount(UUID userId);

    void markRead(UUID userId, UUID id);

    void markAllRead(UUID userId);

    // ===== CMS =====
    PageResponse<NotificationResponse> adminFilter(NotificationType type, NotificationChannel channel,
                                                     NotificationStatus status, Pageable pageable);
}
