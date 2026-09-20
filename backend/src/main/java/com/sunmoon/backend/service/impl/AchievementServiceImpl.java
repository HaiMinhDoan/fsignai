package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.ProgressStatus;
import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.progress.AchievementRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.progress.AchievementResponse;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.progress.Achievement;
import com.sunmoon.backend.entity.progress.UserAchievement;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.mapper.AchievementMapper;
import com.sunmoon.backend.repository.AchievementRepository;
import com.sunmoon.backend.repository.AiCheckResultRepository;
import com.sunmoon.backend.repository.FileAttachmentRepository;
import com.sunmoon.backend.repository.FlashcardReviewRepository;
import com.sunmoon.backend.repository.QuizAttemptRepository;
import com.sunmoon.backend.repository.UserAchievementRepository;
import com.sunmoon.backend.repository.UserLessonProgressRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.repository.UserStreakRepository;
import com.sunmoon.backend.service.AchievementService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Huy hiệu và điều kiện đạt được.
 *
 * evaluateAndAward() chỉ biết đánh giá đúng 5 dạng điều kiện khớp dữ liệu đã
 * seed sẵn (xem AchievementRequest). Một criteriaJson với "type" lạ bị BỎ QUA
 * (coi như không thể đạt) thay vì ném lỗi — admin có thể đang soạn một dạng
 * điều kiện chưa được lập trình hỗ trợ, không nên làm hỏng việc soát các huy
 * hiệu khác.
 */
@Service
public class AchievementServiceImpl extends BaseServiceImpl<Achievement, UUID>
        implements AchievementService {

    @PersistenceContext
    private EntityManager entityManager;

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final UserRepository userRepository;
    private final UserLessonProgressRepository lessonProgressRepository;
    private final UserStreakRepository userStreakRepository;
    private final FlashcardReviewRepository flashcardReviewRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final AiCheckResultRepository aiCheckResultRepository;
    private final AchievementMapper achievementMapper;

    public AchievementServiceImpl(AchievementRepository achievementRepository,
                                  UserAchievementRepository userAchievementRepository,
                                  FileAttachmentRepository fileAttachmentRepository,
                                  UserRepository userRepository,
                                  UserLessonProgressRepository lessonProgressRepository,
                                  UserStreakRepository userStreakRepository,
                                  FlashcardReviewRepository flashcardReviewRepository,
                                  QuizAttemptRepository quizAttemptRepository,
                                  AiCheckResultRepository aiCheckResultRepository,
                                  AchievementMapper achievementMapper) {
        // Achievement không dùng trường "status" chung — xem ghi chú ở CourseServiceImpl
        super(achievementRepository, "nameVi");
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.fileAttachmentRepository = fileAttachmentRepository;
        this.userRepository = userRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.userStreakRepository = userStreakRepository;
        this.flashcardReviewRepository = flashcardReviewRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.aiCheckResultRepository = aiCheckResultRepository;
        this.achievementMapper = achievementMapper;
    }

    @Override
    protected EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AchievementResponse> search(BaseFilterRequest request) {
        Page<Achievement> page = filter(request);
        List<AchievementResponse> items =
                page.getContent().stream().map(achievementMapper::toResponse).toList();
        return PageResponse.<AchievementResponse>builder()
                .items(items)
                .total(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AchievementResponse getDetail(UUID id) {
        return achievementMapper.toResponse(findAchievement(id));
    }

    @Override
    @Transactional
    public AchievementResponse create(AchievementRequest request) {
        validateCode(request.getCode(), null);
        Achievement entity = achievementMapper.toEntity(request);
        applyIconFile(entity, request.getIconFileId());
        return achievementMapper.toResponse(achievementRepository.save(entity));
    }

    @Override
    @Transactional
    public AchievementResponse update(UUID id, AchievementRequest request) {
        Achievement entity = findAchievement(id);
        validateCode(request.getCode(), id);
        achievementMapper.updateEntity(entity, request);
        applyIconFile(entity, request.getIconFileId());
        return achievementMapper.toResponse(achievementRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        achievementRepository.delete(findAchievement(id));
    }

    @Override
    @Transactional
    public int setActive(List<UUID> ids, boolean active) {
        if (ids == null || ids.isEmpty()) return 0;
        List<Achievement> items = achievementRepository.findAllById(ids);
        items.forEach(a -> a.setIsActive(active));
        achievementRepository.saveAll(items);
        return items.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchievementResponse> listForLearner(UUID userId) {
        List<Achievement> achievements = achievementRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
        List<UserAchievement> earned = userAchievementRepository.findAllByUserIdOrderByEarnedAtDesc(userId);

        return achievements.stream().map(a -> {
            AchievementResponse response = achievementMapper.toResponse(a);
            earned.stream()
                    .filter(ua -> ua.getAchievement().getId().equals(a.getId()))
                    .findFirst()
                    .ifPresentOrElse(
                            ua -> {
                                response.setEarned(true);
                                response.setEarnedAt(ua.getEarnedAt());
                            },
                            () -> response.setEarned(false));
            return response;
        }).toList();
    }

    @Override
    @Transactional
    public List<Achievement> evaluateAndAward(UUID userId) {
        List<Achievement> active = achievementRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
        if (active.isEmpty()) return List.of();

        Set<UUID> alreadyEarned = new HashSet<>(userAchievementRepository.findAchievementIdsByUserId(userId));
        List<Achievement> newlyAwarded = new ArrayList<>();

        for (Achievement achievement : active) {
            if (alreadyEarned.contains(achievement.getId())) continue;
            if (meetsCriteria(userId, achievement.getCriteriaJson())) {
                userAchievementRepository.save(UserAchievement.builder()
                        .user(userRepository.getReferenceById(userId))
                        .achievement(achievement)
                        .build());
                newlyAwarded.add(achievement);
            }
        }
        return newlyAwarded;
    }

    // ==================== ĐÁNH GIÁ ĐIỀU KIỆN ====================

    private boolean meetsCriteria(UUID userId, com.fasterxml.jackson.databind.JsonNode criteria) {
        if (criteria == null || !criteria.hasNonNull("type")) return false;
        String type = criteria.path("type").asText("");
        // Thiếu "value" thì coi như không thể đạt, KHÔNG mặc định 0 — mặc định 0
        // sẽ khiến mọi người dùng lập tức "đạt" một huy hiệu chưa cấu hình xong.
        if (!criteria.hasNonNull("value")) return false;
        long target = criteria.path("value").asLong();

        long current = switch (type) {
            case "lessons_completed" ->
                    lessonProgressRepository.countByUserIdAndStatus(userId, ProgressStatus.COMPLETED);
            case "streak" -> userStreakRepository.findByUserId(userId)
                    .map(s -> (long) s.getLongestStreak())
                    .orElse(0L);
            case "signs_learned" ->
                    flashcardReviewRepository.countByUserIdAndRepetitionsGreaterThanEqual(userId, 1);
            case "quiz_perfect" -> quizAttemptRepository.countPerfectAttempts(userId);
            case "ai_checks" -> aiCheckResultRepository.countByUserId(userId);
            default -> -1L; // dạng điều kiện chưa hỗ trợ -> không bao giờ đạt
        };
        return current >= target;
    }

    // ==================== TIỆN ÍCH ====================

    private Achievement findAchievement(UUID id) {
        return achievementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy huy hiệu: " + id));
    }

    private void validateCode(String code, UUID excludeId) {
        boolean exists = excludeId == null
                ? achievementRepository.existsByCode(code)
                : achievementRepository.existsByCodeAndIdNot(code, excludeId);
        if (exists) {
            throw new ConflictException("Mã huy hiệu đã tồn tại: " + code);
        }
    }

    private void applyIconFile(Achievement entity, UUID iconFileId) {
        if (iconFileId == null) {
            entity.setIconFile(null);
            return;
        }
        FileAttachment file = fileAttachmentRepository.findById(iconFileId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tệp ảnh: " + iconFileId));
        entity.setIconFile(file);
    }
}
