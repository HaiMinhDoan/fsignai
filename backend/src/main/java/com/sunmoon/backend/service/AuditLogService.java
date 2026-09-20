package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.auth.AuditLogResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditLogService {

    /**
     * Ghi một dòng nhật ký. Không throw ra ngoài dù lỗi gì xảy ra khi ghi log -
     * audit log là phụ, không được làm hỏng thao tác chính của admin.
     */
    void record(UUID actorId, String action, String entityType, UUID entityId, Object before, Object after);

    PageResponse<AuditLogResponse> filter(UUID actorId, String action, String entityType, Pageable pageable);
}
