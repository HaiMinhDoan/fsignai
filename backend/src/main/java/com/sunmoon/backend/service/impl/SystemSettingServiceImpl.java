package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.dto.request.notification.SystemSettingRequest;
import com.sunmoon.backend.dto.response.notification.SystemSettingResponse;
import com.sunmoon.backend.entity.notification.SystemSetting;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.SystemSettingRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.AuditLogService;
import com.sunmoon.backend.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    public List<SystemSettingResponse> listAll() {
        return systemSettingRepository.findAllByOrderByKeyAsc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public SystemSettingResponse create(UUID actingAdminId, SystemSettingRequest request) {
        if (systemSettingRepository.existsByKey(request.getKey())) {
            throw new ConflictException("Khoá cấu hình này đã tồn tại: " + request.getKey());
        }
        SystemSetting entity = SystemSetting.builder()
                .key(request.getKey().trim())
                .value(request.getValue())
                .description(request.getDescription())
                .updatedBy(userRepository.getReferenceById(actingAdminId))
                .build();
        SystemSetting saved = systemSettingRepository.save(entity);
        auditLogService.record(actingAdminId, "settings.create", "system_settings", saved.getId(),
                null, Map.of("key", saved.getKey(), "value", saved.getValue()));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SystemSettingResponse update(UUID actingAdminId, UUID id, SystemSettingRequest request) {
        SystemSetting entity = systemSettingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình"));

        Object before = entity.getValue();
        entity.setValue(request.getValue());
        entity.setDescription(request.getDescription());
        entity.setUpdatedBy(userRepository.getReferenceById(actingAdminId));

        SystemSetting saved = systemSettingRepository.save(entity);
        auditLogService.record(actingAdminId, "settings.update", "system_settings", id,
                Map.of("value", before), Map.of("value", saved.getValue()));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        SystemSetting entity = systemSettingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình"));
        systemSettingRepository.delete(entity);
    }

    private SystemSettingResponse toResponse(SystemSetting s) {
        return SystemSettingResponse.builder()
                .id(s.getId())
                .key(s.getKey())
                .value(s.getValue())
                .description(s.getDescription())
                .updatedByName(s.getUpdatedBy() == null ? null : s.getUpdatedBy().getFullName())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
