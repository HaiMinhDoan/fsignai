package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.ReportStatus;
import com.sunmoon.backend.entity.forum.ForumReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumReportRepository extends JpaRepository<ForumReport, java.util.UUID> {

    Page<ForumReport> findByStatusInOrderByCreatedAtAsc(java.util.List<ReportStatus> statuses, Pageable pageable);

    Page<ForumReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
