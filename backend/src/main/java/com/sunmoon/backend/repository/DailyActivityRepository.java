package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.progress.DailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyActivityRepository extends JpaRepository<DailyActivity, UUID> {

    Optional<DailyActivity> findByUserIdAndActivityDate(UUID userId, LocalDate activityDate);

    /** Lịch sử gần đây, mới nhất trước — phục vụ trang tiến độ và tính huy hiệu */
    List<DailyActivity> findAllByUserIdAndActivityDateBetweenOrderByActivityDateDesc(
            UUID userId, LocalDate from, LocalDate to);
}
