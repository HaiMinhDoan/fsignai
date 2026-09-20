package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.notification.SystemSettingRequest;
import com.sunmoon.backend.dto.response.notification.SystemSettingResponse;

import java.util.List;
import java.util.UUID;

public interface SystemSettingService {

    List<SystemSettingResponse> listAll();

    SystemSettingResponse create(UUID actingAdminId, SystemSettingRequest request);

    SystemSettingResponse update(UUID actingAdminId, UUID id, SystemSettingRequest request);

    void delete(UUID id);
}
