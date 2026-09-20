package com.sunmoon.backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunmoon.backend.dto.request.auth.OnboardingRequest;
import com.sunmoon.backend.dto.response.auth.OnboardingResultResponse;
import com.sunmoon.backend.entity.auth.OnboardingResponse;
import com.sunmoon.backend.exception.customize.InvalidFieldException;
import com.sunmoon.backend.repository.OnboardingResponseRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private static final Set<Integer> ALLOWED_DAILY_MINUTES = Set.of(5, 10, 15, 30);

    private final OnboardingResponseRepository onboardingResponseRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public OnboardingResultResponse getMine(UUID userId) {
        return onboardingResponseRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> OnboardingResultResponse.builder()
                        .interestedTopics(List.of())
                        .completed(false)
                        .build());
    }

    @Override
    @Transactional
    public OnboardingResultResponse save(UUID userId, OnboardingRequest request) {
        if (request.getDailyMinutes() != null && !ALLOWED_DAILY_MINUTES.contains(request.getDailyMinutes())) {
            throw new InvalidFieldException("Số phút học mỗi ngày chỉ nhận 5, 10, 15 hoặc 30");
        }

        OnboardingResponse entity = onboardingResponseRepository.findByUserId(userId)
                .orElseGet(() -> OnboardingResponse.builder()
                        .user(userRepository.getReferenceById(userId))
                        .build());

        // Luu tung cau mot: field nao null trong request thi giu nguyen gia tri cu,
        // khong ghi de ve null - be dong app giua chung khong mat cau da tra loi.
        if (request.getLearnReason() != null) entity.setLearnReason(request.getLearnReason());
        if (request.getCurrentLevel() != null) entity.setCurrentLevel(request.getCurrentLevel());
        if (request.getDailyMinutes() != null) entity.setDailyMinutes(request.getDailyMinutes());
        if (request.getInterestedTopics() != null) {
            entity.setInterestedTopics(objectMapper.valueToTree(request.getInterestedTopics()));
        }

        if (request.isComplete()) {
            if (entity.getLearnReason() == null || entity.getCurrentLevel() == null
                    || entity.getDailyMinutes() == null
                    || entity.getInterestedTopics() == null || entity.getInterestedTopics().isEmpty()) {
                throw new InvalidFieldException("Cần trả lời đủ 4 câu hỏi trước khi hoàn tất");
            }
            entity.setCompletedAt(OffsetDateTime.now());
        }

        OnboardingResponse saved = onboardingResponseRepository.save(entity);
        return toResponse(saved);
    }

    private OnboardingResultResponse toResponse(OnboardingResponse entity) {
        JsonNode topicsNode = entity.getInterestedTopics();
        List<UUID> topics = (topicsNode == null || topicsNode.isEmpty())
                ? List.of()
                : objectMapper.convertValue(topicsNode, new TypeReference<List<UUID>>() {});

        return OnboardingResultResponse.builder()
                .learnReason(entity.getLearnReason())
                .currentLevel(entity.getCurrentLevel())
                .dailyMinutes(entity.getDailyMinutes())
                .interestedTopics(topics)
                .completed(entity.getCompletedAt() != null)
                .completedAt(entity.getCompletedAt())
                .build();
    }
}
