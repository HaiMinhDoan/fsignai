package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.ReportStatus;
import com.sunmoon.backend.constant.enums.ReportTargetType;
import com.sunmoon.backend.dto.request.forum.ForumReportHandleRequest;
import com.sunmoon.backend.dto.request.forum.ForumReportRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.forum.ForumReportResponse;
import com.sunmoon.backend.entity.forum.ForumComment;
import com.sunmoon.backend.entity.forum.ForumPost;
import com.sunmoon.backend.entity.forum.ForumReport;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.ForumCommentRepository;
import com.sunmoon.backend.repository.ForumPostRepository;
import com.sunmoon.backend.repository.ForumReportRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.AuditLogService;
import com.sunmoon.backend.service.ForumReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForumReportServiceImpl implements ForumReportService {

    private static final int PREVIEW_LENGTH = 140;

    private final ForumReportRepository forumReportRepository;
    private final ForumPostRepository forumPostRepository;
    private final ForumCommentRepository forumCommentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public ForumReportResponse create(UUID reporterId, ForumReportRequest request) {
        ForumReport report = ForumReport.builder()
                .reporter(userRepository.getReferenceById(reporterId))
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .note(request.getNote())
                .status(ReportStatus.OPEN)
                .build();
        return toResponse(forumReportRepository.save(report));
    }

    @Override
    public PageResponse<ForumReportResponse> filter(boolean openOnly, Pageable pageable) {
        Page<ForumReport> page = openOnly
                ? forumReportRepository.findByStatusInOrderByCreatedAtAsc(List.of(ReportStatus.OPEN, ReportStatus.REVIEWING), pageable)
                : forumReportRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Override
    @Transactional
    public ForumReportResponse handle(UUID handlerId, UUID id, ForumReportHandleRequest request) {
        ForumReport report = forumReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy báo cáo"));
        var before = report.getStatus();
        report.setStatus(request.getStatus());
        report.setHandlerNote(request.getHandlerNote());
        report.setHandledBy(userRepository.getReferenceById(handlerId));
        report.setHandledAt(OffsetDateTime.now());
        ForumReport saved = forumReportRepository.save(report);
        auditLogService.record(handlerId, "forum.report.handle", "forum_reports", id,
                Map.of("status", before), Map.of("status", request.getStatus()));
        return toResponse(saved);
    }

    private ForumReportResponse toResponse(ForumReport r) {
        String preview = null;
        boolean stillExists = true;

        if (r.getTargetType() == ReportTargetType.POST) {
            Optional<ForumPost> post = forumPostRepository.findById(r.getTargetId());
            stillExists = post.isPresent();
            preview = post.map(ForumPost::getTitleVi).orElse(null);
        } else if (r.getTargetType() == ReportTargetType.COMMENT) {
            Optional<ForumComment> comment = forumCommentRepository.findById(r.getTargetId());
            stillExists = comment.isPresent();
            preview = comment.map(ForumComment::getBodyText)
                    .map(t -> t == null ? "" : t)
                    .map(t -> t.length() > PREVIEW_LENGTH ? t.substring(0, PREVIEW_LENGTH) + "…" : t)
                    .orElse(null);
        }

        return ForumReportResponse.builder()
                .id(r.getId())
                .reporterId(r.getReporter().getId())
                .reporterName(r.getReporter().getFullName())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .targetPreview(preview)
                .targetStillExists(stillExists)
                .reason(r.getReason())
                .note(r.getNote())
                .status(r.getStatus())
                .handledById(r.getHandledBy() == null ? null : r.getHandledBy().getId())
                .handledByName(r.getHandledBy() == null ? null : r.getHandledBy().getFullName())
                .handledAt(r.getHandledAt())
                .handlerNote(r.getHandlerNote())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
