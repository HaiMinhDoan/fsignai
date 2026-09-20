package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.forum.ForumReportHandleRequest;
import com.sunmoon.backend.dto.request.forum.ForumReportRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.forum.ForumReportResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ForumReportService {

    ForumReportResponse create(UUID reporterId, ForumReportRequest request);

    PageResponse<ForumReportResponse> filter(boolean openOnly, Pageable pageable);

    ForumReportResponse handle(UUID handlerId, UUID id, ForumReportHandleRequest request);
}
