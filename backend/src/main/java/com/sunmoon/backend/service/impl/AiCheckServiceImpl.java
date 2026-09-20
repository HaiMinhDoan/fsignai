package com.sunmoon.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunmoon.backend.constant.enums.AiCheckContext;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.constant.enums.VslRole;
import com.sunmoon.backend.constant.enums.VslRoleStatus;
import com.sunmoon.backend.dto.request.ai.AiFeedbackRequest;
import com.sunmoon.backend.dto.request.ai.AiVerifyRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ai.AiCheckHistoryResponse;
import com.sunmoon.backend.dto.response.ai.AiVerifyResponse;
import com.sunmoon.backend.dto.response.ai.ReadinessResponse;
import com.sunmoon.backend.entity.ai.AiCheckFeedback;
import com.sunmoon.backend.entity.ai.AiCheckResult;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.AiCheckFeedbackRepository;
import com.sunmoon.backend.repository.AiCheckResultRepository;
import com.sunmoon.backend.repository.SignExemplarRepository;
import com.sunmoon.backend.repository.SignRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.repository.VerifyThresholdGroupRepository;
import com.sunmoon.backend.repository.VerifyThresholdRepository;
import com.sunmoon.backend.service.ActivityDelta;
import com.sunmoon.backend.service.AiCheckService;
import com.sunmoon.backend.service.ExemplarService;
import com.sunmoon.backend.service.ExemplarService.LoadedExemplar;
import com.sunmoon.backend.service.ProgressService;
import com.sunmoon.backend.service.StreakService;
import com.sunmoon.backend.service.support.AiHintCatalog;
import com.sunmoon.backend.service.support.AiServiceClient;
import com.sunmoon.backend.service.support.AiServiceClient.VerifyResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCheckServiceImpl implements AiCheckService {

    /** Khi từ chưa có ngưỡng riêng và bảng nhóm thiếu dòng tương ứng — ngưỡng của nhóm "từ 2 tay" */
    private static final BigDecimal FALLBACK_THRESHOLD = new BigDecimal("0.30");

    /** Điểm 60 đúng tại ngưỡng: khoảng cách = ngưỡng ↔ 60 điểm, 0 ↔ 100 điểm, gấp đôi ngưỡng ↔ 36 điểm */
    private static final double SCORE_AT_THRESHOLD = 0.6;

    private static final double GOOD_PART = 80;
    private static final double BAD_PART = 60;
    private static final double LOW_TRACKING = 0.6;

    /** Giới hạn theo hợp đồng API: 30 lượt/phút/người */
    private static final int RATE_LIMIT = 30;
    private static final long RATE_WINDOW_MS = 60_000;

    private final SignRepository signRepository;
    private final UserRepository userRepository;
    private final AiCheckResultRepository resultRepository;
    private final AiCheckFeedbackRepository feedbackRepository;
    private final SignExemplarRepository exemplarRepository;
    private final VerifyThresholdRepository thresholdRepository;
    private final VerifyThresholdGroupRepository groupRepository;
    private final ExemplarService exemplarService;
    private final AiServiceClient aiClient;
    private final ProgressService progressService;
    private final StreakService streakService;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;

    /**
     * Cửa sổ trượt trong bộ nhớ. Đủ cho một bản triển khai; nhiều instance thì cần chuyển sang Redis
     * (RedisService đã có) vì mỗi instance sẽ tự đếm riêng.
     */
    private final Map<UUID, Deque<Long>> recent = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ readiness

    @Override
    public ReadinessResponse readiness(UUID signId) {
        int count = exemplarService.usableCount(signId);
        return ReadinessResponse.builder().ready(count > 0).exemplarCount(count).build();
    }

    // ------------------------------------------------------------------ verify

    @Override
    public AiVerifyResponse verify(UUID userId, AiVerifyRequest request) {
        checkRate(userId);
        validateShapes(request);

        Sign sign = signRepository.findById(request.getSignId())
                .filter(s -> Boolean.TRUE.equals(s.getIsPublished()))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy từ này"));

        List<LoadedExemplar> exemplars = exemplarService.loadUsable(sign.getId());
        if (exemplars.isEmpty()) {
            throw fail(HttpStatus.CONFLICT, "Từ này chưa có mẫu để chấm điểm, bé luyện với video nhé", "NO_EXEMPLAR");
        }

        // Cố ý KHÔNG bọc trong transaction: cuộc gọi sang ai-service kéo dài, không nên giữ kết nối CSDL.
        VerifyResult result;
        try {
            result = aiClient.verify(buildPayload(request, exemplars));
        } catch (CommonException e) {
            Object data = e.getData();
            String code = data instanceof Map<?, ?> m && m.get("code") != null ? m.get("code").toString() : null;
            String friendly = code != null ? AiHintCatalog.messageForError(code) : null;
            if (friendly != null) {
                // Lỗi do người học (chưa giơ tay, chưa thấy vai) → nói cách khắc phục, không dùng câu kỹ thuật
                throw fail(HttpStatus.UNPROCESSABLE_ENTITY, friendly, code);
            }
            throw e;
        }

        LoadedExemplar matched = exemplars.stream()
                .filter(x -> x.id().toString().equals(result.bestExemplarId()))
                .findFirst().orElse(exemplars.get(0));

        BigDecimal threshold = resolveThreshold(sign, result.handCount());
        double theta = threshold.doubleValue();

        double distance = result.distance();
        boolean passed = distance <= theta;
        double score = toScore(distance, theta);
        int handshape = toPart(result.components().handshape(), theta);
        int location = toPart(result.components().location(), theta);
        int movement = toPart(result.components().movement(), theta);
        List<String> hintCodes = hintCodes(result, handshape, location, movement);

        return tx.execute(status -> {
            AiCheckResult saved = resultRepository.save(AiCheckResult.builder()
                    .user(userRepository.getReferenceById(userId))
                    .sign(sign)
                    .region(matched.region() != null ? matched.region() : Region.COMMON)
                    .score(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP))
                    .passed(passed)
                    .dtwDistance(BigDecimal.valueOf(distance).setScale(5, RoundingMode.HALF_UP))
                    .thresholdUsed(threshold)
                    .matchedExemplar(exemplarRepository.getReferenceById(matched.id()))
                    .scoreHandshape(BigDecimal.valueOf(handshape))
                    .scoreLocation(BigDecimal.valueOf(location))
                    .scoreMovement(BigDecimal.valueOf(movement))
                    .hintCodes(mapper.valueToTree(hintCodes))
                    .trackingQuality(BigDecimal.valueOf(result.trackingQuality()).setScale(3, RoundingMode.HALF_UP))
                    .modelVersion(result.modelVersion())
                    .processingMs(result.processingMs())
                    .context(request.getContext() != null ? request.getContext() : AiCheckContext.PRACTICE)
                    .build());

            // Luyện với camera cũng là một lần học trong ngày: giữ chuỗi ngày học, nhưng KHÔNG cộng sao (Mức A)
            progressService.recordActivity(userId, ActivityDelta.builder().aiChecksDone(1).build());
            streakService.recordStudyDay(userId);

            return AiVerifyResponse.builder()
                    .resultId(saved.getId())
                    .signId(sign.getId())
                    .score(score)
                    .passed(passed)
                    .feedback(AiVerifyResponse.Feedback.builder()
                            .handshape(handshape).location(location).movement(movement)
                            .hints(hintTexts(hintCodes)).hintCodes(hintCodes).build())
                    .modelVersion(result.modelVersion())
                    .checkedAt(saved.getCheckedAt())
                    .build();
        });
    }

    // ------------------------------------------------------------------ chấm điểm

    /** Điểm tổng: 100 × 0,6^(khoảng cách / ngưỡng). Đạt ⇔ khoảng cách ≤ ngưỡng ⇔ điểm ≥ 60. */
    static double toScore(double distance, double threshold) {
        double s = 100 * Math.pow(SCORE_AT_THRESHOLD, distance / threshold);
        return Math.round(Math.max(0, Math.min(100, s)) * 100) / 100.0;
    }

    static int toPart(double partDistance, double threshold) {
        return (int) Math.round(toScore(partDistance, threshold));
    }

    /**
     * Ba tầng ngưỡng (docs/02-data-model.md §6): ngưỡng riêng của từ (đã hiệu chỉnh/đặt tay) → mặc định theo nhóm.
     *
     * Số tay dùng cho tra nhóm lấy max(cột signs.hand_count, số tay ĐO ĐƯỢC từ mẫu). Cột hand_count hiện là 1
     * cho cả kho từ (giá trị mặc định chưa ai nhập) trong khi phần lớn ký hiệu dùng hai tay, nên tin số đo hơn.
     */
    private BigDecimal resolveThreshold(Sign sign, int measuredHands) {
        var own = thresholdRepository.findBySignId(sign.getId());
        if (own.isPresent()) {
            return own.get().getThreshold();
        }
        int declared = sign.getHandCount() == null ? 1 : sign.getHandCount();
        short hands = (short) Math.max(1, Math.min(2, Math.max(declared, measuredHands)));
        return groupRepository.findByUnitTypeAndHandCount(sign.getUnitType(), hands)
                .map(g -> g.getThreshold())
                .orElse(FALLBACK_THRESHOLD);
    }

    /** Mã gợi ý: hình học từ ai-service + nhận xét từng phần dựa vào ngưỡng của từ này, tối đa 4 mục, không trùng. */
    private List<String> hintCodes(VerifyResult r, int handshape, int location, int movement) {
        Set<String> out = new LinkedHashSet<>();
        if (r.trackingQuality() < LOW_TRACKING) out.add(AiHintCatalog.TRACKING_LOW);
        out.addAll(r.hints());

        boolean locationHint = r.hints().contains(AiHintCatalog.LOCATION_TOO_LOW)
                || r.hints().contains(AiHintCatalog.LOCATION_TOO_HIGH) || r.hints().contains(AiHintCatalog.HAND_MISSING)
                || r.hints().contains(AiHintCatalog.EXTRA_HAND);
        boolean movementHint = r.hints().contains(AiHintCatalog.MOVEMENT_TOO_SMALL)
                || r.hints().contains(AiHintCatalog.MOVEMENT_TOO_LARGE);

        addPart(out, handshape, AiHintCatalog.HANDSHAPE_OK, AiHintCatalog.HANDSHAPE_OFF, false);
        addPart(out, location, AiHintCatalog.LOCATION_OK, AiHintCatalog.LOCATION_OFF, locationHint);
        addPart(out, movement, AiHintCatalog.MOVEMENT_OK, AiHintCatalog.MOVEMENT_OFF, movementHint);
        return new ArrayList<>(out).subList(0, Math.min(4, out.size()));
    }

    /** Chỉ nói "đúng" khi phần đó thật sự tốt; chỉ nói "chưa giống" khi chưa có gợi ý cụ thể hơn cho phần đó */
    private void addPart(Set<String> out, int score, String ok, String off, boolean alreadyExplained) {
        if (score >= GOOD_PART) {
            out.add(ok);
        } else if (score < BAD_PART && !alreadyExplained) {
            out.add(off);
        }
    }

    private List<String> hintTexts(List<String> codes) {
        return codes.stream().map(AiHintCatalog::textOf).filter(t -> t != null).toList();
    }

    // ------------------------------------------------------------------ đầu vào

    private Map<String, Object> buildPayload(AiVerifyRequest request, List<LoadedExemplar> exemplars) {
        List<Map<String, Object>> frames = new ArrayList<>(request.getLandmarks().getFrames().size());
        for (AiVerifyRequest.Frame f : request.getLandmarks().getFrames()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("pose", f.getPose());
            m.put("hands", f.getHands() == null ? List.of() : f.getHands());
            frames.add(m);
        }
        List<Map<String, Object>> ex = exemplars.stream()
                .map(e -> Map.<String, Object>of("id", e.id().toString(), "features", e.features()))
                .toList();
        return Map.of("aspect", request.getLandmarks().getAspect(), "frames", frames, "exemplars", ex);
    }

    /** Bộ kiểm tra hình dạng dữ liệu: ai-service kiểm lại, nhưng chặn sớm ở đây để trả lỗi 400 dễ hiểu */
    private void validateShapes(AiVerifyRequest request) {
        for (AiVerifyRequest.Frame f : request.getLandmarks().getFrames()) {
            if (f.getPose() != null) {
                if (f.getPose().size() != 33 || f.getPose().stream().anyMatch(p -> p == null || p.size() != 4)) {
                    throw fail(HttpStatus.BAD_REQUEST, "Dữ liệu pose không đúng: cần 33 điểm × [x, y, z, visibility]", "BAD_LANDMARKS");
                }
            }
            if (f.getHands() != null) {
                for (List<List<Double>> hand : f.getHands()) {
                    if (hand == null || hand.size() != 21 || hand.stream().anyMatch(p -> p == null || p.size() != 3)) {
                        throw fail(HttpStatus.BAD_REQUEST, "Dữ liệu bàn tay không đúng: cần 21 điểm × [x, y, z]", "BAD_LANDMARKS");
                    }
                }
            }
        }
    }

    private void checkRate(UUID userId) {
        long now = System.currentTimeMillis();
        Deque<Long> q = recent.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > RATE_WINDOW_MS) q.pollFirst();
            if (q.size() >= RATE_LIMIT) {
                throw fail(HttpStatus.TOO_MANY_REQUESTS, "Bé luyện nhiều quá rồi, nghỉ một chút rồi thử lại nhé", "RATE_LIMITED");
            }
            q.addLast(now);
        }
    }

    // ------------------------------------------------------------------ lịch sử + góp ý

    @Override
    public PageResponse<AiCheckHistoryResponse> history(UUID userId, UUID signId, Pageable pageable) {
        return PageResponse.of(resultRepository.findHistory(userId, signId, pageable), r -> AiCheckHistoryResponse.builder()
                .id(r.getId())
                .signId(r.getSign().getId())
                .wordVi(r.getSign().getWordVi())
                .score(r.getScore().doubleValue())
                .passed(Boolean.TRUE.equals(r.getPassed()))
                .handshape(intOrNull(r.getScoreHandshape()))
                .location(intOrNull(r.getScoreLocation()))
                .movement(intOrNull(r.getScoreMovement()))
                .checkedAt(r.getCheckedAt())
                .build());
    }

    @Override
    public void feedback(UUID userId, UUID resultId, AiFeedbackRequest request) {
        AiCheckResult result = resultRepository.findByIdAndUserId(resultId, userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy kết quả chấm này"));
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        // Trọng số CHỤP LẠI lúc góp ý: nếu tính động theo vai trò hiện tại, một người được duyệt/bị gỡ vai trò
        // sẽ làm ngưỡng đã hiệu chỉnh trong quá khứ âm thầm đổi theo.
        VslRole role = user.getVslRole() == null ? VslRole.LEARNER : user.getVslRole();
        VslRoleStatus roleStatus = user.getVslRoleStatus() == null ? VslRoleStatus.SELF_DECLARED : user.getVslRoleStatus();
        short weight = weightOf(role, roleStatus);

        tx.executeWithoutResult(status -> {
            AiCheckFeedback fb = feedbackRepository.findByUserIdAndAiCheckResultId(userId, result.getId())
                    .orElseGet(() -> AiCheckFeedback.builder().user(user).aiCheckResult(result).build());
            fb.setVerdict(request.getVerdict());
            fb.setNote(request.getNote() == null || request.getNote().isBlank() ? null : request.getNote().trim());
            fb.setWeightSnapshot(weight);
            fb.setRoleSnapshot(role);
            fb.setRoleStatusSnapshot(roleStatus);
            fb.setUpdatedAt(OffsetDateTime.now());
            feedbackRepository.save(fb);
        });
    }

    /** Chuyên gia ĐÃ XÁC MINH → 5; chuyên gia chưa xác minh → 0 (lưu, kích hoạt khi được duyệt); người học → 1 */
    static short weightOf(VslRole role, VslRoleStatus status) {
        if (role == VslRole.LEARNER) return 1;
        return status == VslRoleStatus.VERIFIED ? (short) 5 : (short) 0;
    }

    private static Integer intOrNull(BigDecimal v) {
        return v == null ? null : v.intValue();
    }

    private CommonException fail(HttpStatus status, String message, String code) {
        CommonException ex = new CommonException(message);
        ex.setHttpStatus(status);
        ex.setData(Map.of("code", code));
        return ex;
    }
}
