package com.sunmoon.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.auth.AuditLogResponse;
import com.sunmoon.backend.entity.auth.AuditLog;
import com.sunmoon.backend.repository.AuditLogRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void record(UUID actorId, String action, String entityType, UUID entityId, Object before, Object after) {
        try {
            AuditLog log = AuditLog.builder()
                    .actor(actorId == null ? null : userRepository.getReferenceById(actorId))
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .beforeData(before == null ? null : objectMapper.valueToTree(before))
                    .afterData(after == null ? null : objectMapper.valueToTree(after))
                    .ipAddress(currentClientIp())
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Ghi log la phu - loi o day khong duoc lam hong thao tac chinh cua admin
            log.warn("Không ghi được audit log cho action={}", action, e);
        }
    }

    @Override
    public PageResponse<AuditLogResponse> filter(UUID actorId, String action, String entityType, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.adminFilter(actorId, action, entityType, pageable);
        return PageResponse.of(page, this::toResponse);
    }

    private String currentClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest http = attrs.getRequest();
            String forwarded = http.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return http.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .actorId(a.getActor() == null ? null : a.getActor().getId())
                .actorName(a.getActor() == null ? null : a.getActor().getFullName())
                .action(a.getAction())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .beforeData(a.getBeforeData())
                .afterData(a.getAfterData())
                .ipAddress(a.getIpAddress())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
