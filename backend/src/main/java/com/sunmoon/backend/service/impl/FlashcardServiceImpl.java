package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.FlashcardResult;
import com.sunmoon.backend.dto.request.practice.FlashcardReviewRequest;
import com.sunmoon.backend.dto.response.practice.FlashcardResponse;
import com.sunmoon.backend.dto.response.practice.FlashcardReviewResultResponse;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.entity.dictionary.SignVideo;
import com.sunmoon.backend.entity.practice.FlashcardReview;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.FlashcardReviewRepository;
import com.sunmoon.backend.repository.SignRepository;
import com.sunmoon.backend.repository.SignVideoRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.ActivityDelta;
import com.sunmoon.backend.service.FlashcardService;
import com.sunmoon.backend.service.ProgressService;
import com.sunmoon.backend.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Thuat toan SM-2 co dien, dieu chinh cho tin hieu nhi phan cua UI (chi co 2
// nut: Da thuoc / Can luyen them) bang cach anh xa sang chat luong SM-2 goc:
// Da thuoc -> quality 5 (nho hoan hao), Can luyen them -> quality 2 (giong
// "sai, nhung con nho lo mo" - dat lai chuoi lap nhung khong coi la quen sach).
@Service
@RequiredArgsConstructor
public class FlashcardServiceImpl implements FlashcardService {

    private static final BigDecimal MIN_EASE_FACTOR = new BigDecimal("1.30");

    private final FlashcardReviewRepository flashcardReviewRepository;
    private final SignRepository signRepository;
    private final SignVideoRepository signVideoRepository;
    private final UserRepository userRepository;
    private final ProgressService progressService;
    private final StreakService streakService;

    @Override
    public List<FlashcardResponse> getDue(UUID userId, UUID topicId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));

        List<FlashcardReview> dueReviews = flashcardReviewRepository
                .findDue(userId, topicId, OffsetDateTime.now(), PageRequest.of(0, safeLimit));

        List<FlashcardResponse> result = new ArrayList<>(dueReviews.stream()
                .map(fr -> toResponse(fr.getSign(), false, fr.getDueAt()))
                .toList());

        if (result.size() < safeLimit) {
            List<Sign> newSigns = signRepository.findNewForFlashcards(
                    userId, topicId, PageRequest.of(0, safeLimit - result.size()));
            newSigns.forEach(s -> result.add(toResponse(s, true, null)));
        }

        return result;
    }

    @Override
    @Transactional
    public FlashcardReviewResultResponse review(UUID userId, UUID signId, FlashcardReviewRequest request) {
        Sign sign = signRepository.findById(signId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy từ này"));

        FlashcardReview review = flashcardReviewRepository.findByUserIdAndSignId(userId, signId)
                .orElseGet(() -> FlashcardReview.builder()
                        .user(userRepository.getReferenceById(userId))
                        .sign(sign)
                        .build());

        applySm2(review, request.getResult());
        flashcardReviewRepository.save(review);

        progressService.recordActivity(userId, ActivityDelta.builder().signsReviewed(1).build());
        streakService.recordStudyDay(userId);

        return FlashcardReviewResultResponse.builder()
                .nextDueAt(review.getDueAt())
                .intervalDays(review.getIntervalDays())
                .easeFactor(review.getEaseFactor().doubleValue())
                .repetitions(review.getRepetitions())
                .build();
    }

    /** SM-2 kinh dien. Cong thuc va nguong 1.3 lay dung nguyen ban cua thuat toan gio. */
    private void applySm2(FlashcardReview review, FlashcardResult result) {
        int quality = result == FlashcardResult.KNOWN ? 5 : 2;

        // QUAN TRONG: cong thuc interval = I(truoc) * EF dung EF CU (truoc lan on
        // nay), roi MOI cap nhat EF cho lan sau - dung thu tu ban goc thuat toan
        // SM-2 cua Wozniak. Cap nhat EF truoc se lam sai moi interval tu lan lap
        // thu 3 tro di.
        BigDecimal oldEase = review.getEaseFactor();

        if (quality < 3) {
            // Tra loi "Can luyen them" - dat lai chuoi lap, ngay mai on lai ngay
            review.setRepetitions(0);
            review.setIntervalDays(1);
            review.setLapses(review.getLapses() + 1);
        } else {
            int repetitions = review.getRepetitions() + 1;
            review.setRepetitions(repetitions);
            int interval;
            if (repetitions == 1) interval = 1;
            else if (repetitions == 2) interval = 6;
            else interval = (int) Math.round(review.getIntervalDays() * oldEase.doubleValue());
            review.setIntervalDays(Math.max(1, interval));
        }

        double delta = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02);
        BigDecimal newEase = oldEase.add(BigDecimal.valueOf(delta)).setScale(2, RoundingMode.HALF_UP);
        if (newEase.compareTo(MIN_EASE_FACTOR) < 0) newEase = MIN_EASE_FACTOR;
        review.setEaseFactor(newEase);

        review.setLastResult(result);
        review.setLastReviewedAt(OffsetDateTime.now());
        review.setDueAt(OffsetDateTime.now().plusDays(review.getIntervalDays()));
    }

    private FlashcardResponse toResponse(Sign sign, boolean isNew, OffsetDateTime dueAt) {
        List<SignVideo> videos = signVideoRepository.findAllBySignIdOrderByRegionAscViewAngleAsc(sign.getId());
        SignVideo primary = videos.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsPrimary()))
                .findFirst()
                .orElse(videos.isEmpty() ? null : videos.get(0));

        return FlashcardResponse.builder()
                .signId(sign.getId())
                .wordVi(sign.getWordVi())
                .gloss(sign.getGloss())
                .descriptionVi(sign.getDescriptionVi())
                .videoUrl(primary != null && primary.getFile() != null
                        ? primary.getFile().getPublicUrl() : null)
                .thumbnailUrl(primary != null && primary.getThumbnailFile() != null
                        ? primary.getThumbnailFile().getPublicUrl() : null)
                .isNew(isNew)
                .dueAt(dueAt)
                .build();
    }
}
