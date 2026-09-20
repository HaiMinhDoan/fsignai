package com.sunmoon.backend.dto.response.notification;

import com.sunmoon.backend.constant.enums.NotificationChannel;
import com.sunmoon.backend.constant.enums.NotificationStatus;
import com.sunmoon.backend.constant.enums.NotificationType;
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
public class NotificationResponse {

    UUID id;
    /** Chỉ có giá trị ở màn CMS - người học tự xem thông báo của mình thì không cần */
    UUID userId;
    String userName;

    NotificationType type;
    NotificationChannel channel;
    String titleVi;
    String bodyVi;
    String actionUrl;
    NotificationStatus status;
    String errorMessage;
    OffsetDateTime sentAt;
    OffsetDateTime readAt;
    OffsetDateTime createdAt;
}
