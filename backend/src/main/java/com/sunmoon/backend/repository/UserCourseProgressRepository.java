package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.ProgressStatus;
import com.sunmoon.backend.entity.progress.UserCourseProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCourseProgressRepository extends JpaRepository<UserCourseProgress, UUID> {

    Optional<UserCourseProgress> findByUserIdAndCourseId(UUID userId, UUID courseId);

    List<UserCourseProgress> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    long countByUserIdAndStatus(UUID userId, ProgressStatus status);
}
