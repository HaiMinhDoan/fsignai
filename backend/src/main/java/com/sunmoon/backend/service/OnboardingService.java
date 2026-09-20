package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.auth.OnboardingRequest;
import com.sunmoon.backend.dto.response.auth.OnboardingResultResponse;

import java.util.UUID;

public interface OnboardingService {

    OnboardingResultResponse getMine(UUID userId);

    OnboardingResultResponse save(UUID userId, OnboardingRequest request);
}
