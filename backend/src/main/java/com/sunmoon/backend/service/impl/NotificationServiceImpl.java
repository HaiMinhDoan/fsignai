package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.NotificationChannel;
import com.sunmoon.backend.constant.enums.NotificationStatus;
import com.sunmoon.backend.constant.enums.NotificationType;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.notification.NotificationResponse;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.notification.Notification;
import com.sunmoon.backend.repository.NotificationRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;

    @Override
    @Transactional
    public void notify(UUID userId, NotificationType type, String titleVi, String bodyVi, String actionUrl,
                        boolean alsoEmail) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            OffsetDateTime now = OffsetDateTime.now();
            notificationRepository.save(Notification.builder()
                    .user(user)
                    .type(type)
                    .channel(NotificationChannel.IN_APP)
                    .titleVi(titleVi)
                    .bodyVi(bodyVi)
                    .actionUrl(actionUrl)
                    .status(NotificationStatus.SENT)
                    .sentAt(now)
                    .build());

            if (alsoEmail) {
                sendEmailNotification(user, type, titleVi, bodyVi);
            }
        } catch (Exception e) {
            // Thong bao la phu - loi o day khong duoc lam hong thao tac chinh
            log.warn("Không tạo được thông báo cho user={}, type={}", userId, type, e);
        }
    }

    private void sendEmailNotification(User user, NotificationType type, String titleVi, String bodyVi) {
        Notification emailNotification = Notification.builder()
                .user(user)
                .type(type)
                .channel(NotificationChannel.EMAIL)
                .titleVi(titleVi)
                .bodyVi(bodyVi)
                .status(NotificationStatus.PENDING)
                .build();

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("[SignAI] " + titleVi);
            message.setText(bodyVi == null ? titleVi : bodyVi);
            javaMailSender.send(message);

            emailNotification.setStatus(NotificationStatus.SENT);
            emailNotification.setSentAt(OffsetDateTime.now());
        } catch (Exception e) {
            log.warn("Gửi email thông báo thất bại cho {}", user.getEmail(), e);
            emailNotification.setStatus(NotificationStatus.FAILED);
            emailNotification.setErrorMessage(e.getMessage());
        }
        notificationRepository.save(emailNotification);
    }

    @Override
    public PageResponse<NotificationResponse> listMine(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository
                .findByUser_IdAndChannelOrderByCreatedAtDesc(userId, NotificationChannel.IN_APP, pageable);
        return PageResponse.of(page, n -> toResponse(n, false));
    }

    @Override
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUser_IdAndChannelAndReadAtIsNull(userId, NotificationChannel.IN_APP);
    }

    @Override
    @Transactional
    public void markRead(UUID userId, UUID id) {
        notificationRepository.markRead(id, userId, OffsetDateTime.now());
    }

    @Override
    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId, OffsetDateTime.now());
    }

    @Override
    public PageResponse<NotificationResponse> adminFilter(NotificationType type, NotificationChannel channel,
                                                            NotificationStatus status, Pageable pageable) {
        Page<Notification> page = notificationRepository.adminFilter(type, channel, status, pageable);
        return PageResponse.of(page, n -> toResponse(n, true));
    }

    private NotificationResponse toResponse(Notification n, boolean includeUser) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(includeUser ? n.getUser().getId() : null)
                .userName(includeUser ? n.getUser().getFullName() : null)
                .type(n.getType())
                .channel(n.getChannel())
                .titleVi(n.getTitleVi())
                .bodyVi(n.getBodyVi())
                .actionUrl(n.getActionUrl())
                .status(n.getStatus())
                .errorMessage(n.getErrorMessage())
                .sentAt(n.getSentAt())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
