package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.auth.OnboardingResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OnboardingResponseRepository extends JpaRepository<OnboardingResponse, UUID> {

    Optional<OnboardingResponse> findByUserId(UUID userId);

    // Da hoan tat onboarding chua - frontend dung de quyet dinh dieu huong
    // sau khi dang nhap: chua xong thi dua vao luong 4 cau hoi.
    boolean existsByUserIdAndCompletedAtIsNotNull(UUID userId);
}
