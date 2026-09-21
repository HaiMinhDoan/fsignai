package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.GameCode;
import com.sunmoon.backend.constant.enums.PointSource;
import com.sunmoon.backend.dto.request.gamification.GameFinishRequest;
import com.sunmoon.backend.dto.request.gamification.GameStartRequest;
import com.sunmoon.backend.dto.response.gamification.GameFinishResponse;
import com.sunmoon.backend.dto.response.gamification.GameStartResponse;
import com.sunmoon.backend.dto.response.gamification.GameStartResponse.GameQuestion;
import com.sunmoon.backend.dto.response.gamification.GameStartResponse.GameSign;
import com.sunmoon.backend.dto.response.gamification.GameStartResponse.Option;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.entity.dictionary.SignVideo;
import com.sunmoon.backend.entity.gamification.GameSession;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.InvalidFieldException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.GameSessionRepository;
import com.sunmoon.backend.repository.SignRepository;
import com.sunmoon.backend.repository.SignVideoRepository;
import com.sunmoon.backend.repository.UserPointsRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.ActivityDelta;
import com.sunmoon.backend.service.GameService;
import com.sunmoon.backend.service.PointService;
import com.sunmoon.backend.service.ProgressService;
import com.sunmoon.backend.service.StreakService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Bốn trò chạy hoàn toàn trên kho từ sẵn có. Máy chủ chỉ rút nội dung và chốt
 * điểm; luật chơi (kéo thả, đồng hồ, lật thẻ...) nằm ở giao diện.
 *
 * Vì người học chơi ở trình duyệt nên kết quả gửi lên KHÔNG kiểm chứng tuyệt đối được.
 * Bù lại bằng ba chốt chặn rẻ tiền: ván phải do máy chủ mở, chỉ chốt được một
 * lần, và thời lượng lấy từ đồng hồ máy chủ (không tin số client gửi) — số câu
 * đúng bị chặn theo tốc độ tối thiểu hợp lý của một đứa trẻ.
 */
@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    /** Số sao tối đa mỗi ván — khớp nhãn "+N Sao" trên thẻ trò chơi ở Figma 1:611 */
    private static final int REWARD_MATCH_PAIR = 50;
    private static final int REWARD_SPEED_GUESS = 40;
    private static final int REWARD_FINGER_DANCE = 60;
    private static final int REWARD_MEMORY_FLIP = 45;

    private static final int PAIR_ROUNDS = 6;
    private static final int SPEED_ROUNDS = 5;
    private static final int DANCE_ROUNDS = 10;

    /** Nhanh nhất mà một người học thật sự làm đúng được một vòng: dưới mức này là gửi số bừa */
    private static final double MIN_SECONDS_PER_CORRECT = 1.5;

    private final GameSessionRepository gameSessionRepository;
    private final SignRepository signRepository;
    private final SignVideoRepository signVideoRepository;
    private final UserRepository userRepository;
    private final UserPointsRepository userPointsRepository;
    private final PointService pointService;
    private final ProgressService progressService;
    private final StreakService streakService;
    private final EntityManager em;

    @Override
    @Transactional
    public GameStartResponse start(UUID userId, GameCode gameCode, GameStartRequest request) {
        UUID packId = request == null ? null : request.getPackId();
        UUID topicId = (packId != null || request == null) ? null : request.getTopicId();

        int rounds = roundsOf(gameCode);
        int optionCount = gameCode == GameCode.FINGER_DANCE ? 3 : 4;
        boolean isPairGame = gameCode == GameCode.MATCH_PAIR || gameCode == GameCode.MEMORY_FLIP;

        List<UUID> pickedIds = randomSignIds(topicId, packId, rounds, Set.of());
        if (pickedIds.size() < rounds) {
            throw new ConflictException(
                    "Phạm vi này chưa đủ " + rounds + " từ có video để chơi, hãy chọn phạm vi rộng hơn");
        }

        GameSession session = gameSessionRepository.save(GameSession.builder()
                .user(userRepository.getReferenceById(userId))
                .gameCode(gameCode)
                .packId(packId)
                .topicId(topicId)
                .maxScore(rewardOf(gameCode))
                .build());

        GameStartResponse.GameStartResponseBuilder response = GameStartResponse.builder()
                .sessionId(session.getId())
                .gameCode(gameCode)
                .maxScore(session.getMaxScore())
                .totalRounds(rounds);

        List<Sign> signs = new ArrayList<>(signRepository.findAllById(pickedIds));
        Collections.shuffle(signs);

        if (isPairGame) {
            response.pairs(signs.stream().map(this::toGameSign).toList());
        } else {
            List<GameQuestion> questions = new ArrayList<>();
            Set<UUID> used = new HashSet<>(pickedIds);
            for (Sign sign : signs) {
                questions.add(buildQuestion(sign, optionCount, used));
            }
            response.questions(questions);
        }
        return response.build();
    }

    @Override
    @Transactional
    public GameFinishResponse finish(UUID userId, UUID sessionId, GameFinishRequest request) {
        GameSession session = gameSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ván chơi"));
        if (session.getFinishedAt() != null) {
            throw new ConflictException("Ván này đã kết thúc rồi, không chốt lại được");
        }

        OffsetDateTime now = OffsetDateTime.now();
        int elapsed = (int) Math.max(0, Duration.between(session.getStartedAt(), now).getSeconds());
        int rounds = roundsOf(session.getGameCode());

        // Chặn số câu đúng vượt quá tốc độ có thể — không tin thời lượng client báo
        int plausibleMax = Math.max(1, (int) (elapsed / MIN_SECONDS_PER_CORRECT));
        int correct = Math.min(Math.min(request.getCorrectCount(), rounds), plausibleMax);
        int wrong = request.getWrongCount();

        int score = computeScore(session.getMaxScore(), rounds, correct, wrong);

        session.setCorrectCount(correct);
        session.setWrongCount(wrong);
        session.setScore(score);
        session.setDurationSeconds(elapsed);
        session.setFinishedAt(now);
        gameSessionRepository.save(session);

        int earned = pointService.award(userId, PointSource.GAME, session.getId(), score,
                "Chơi " + gameLabel(session.getGameCode()));

        progressService.recordActivity(userId, ActivityDelta.builder()
                .minutesStudied(Math.max(1, elapsed / 60))
                .build());
        streakService.recordStudyDay(userId);

        int total = userPointsRepository.findByUserId(userId).map(p -> p.getTotalPoints()).orElse(0);
        return GameFinishResponse.builder()
                .score(score)
                .maxScore(session.getMaxScore())
                .correctCount(correct)
                .wrongCount(wrong)
                .durationSeconds(elapsed)
                .starsEarned(earned)
                .totalStars(total)
                .build();
    }

    /** Đúng hết = đủ sao tối đa; mỗi lần sai trừ nửa công của một câu đúng; không âm */
    static int computeScore(int maxScore, int rounds, int correct, int wrong) {
        double ratio = (correct - 0.5 * wrong) / rounds;
        ratio = Math.max(0, Math.min(1, ratio));
        return (int) Math.round(maxScore * ratio);
    }

    private int roundsOf(GameCode code) {
        return switch (code) {
            case MATCH_PAIR, MEMORY_FLIP -> PAIR_ROUNDS;
            case SPEED_GUESS -> SPEED_ROUNDS;
            case FINGER_DANCE -> DANCE_ROUNDS;
        };
    }

    private int rewardOf(GameCode code) {
        return switch (code) {
            case MATCH_PAIR -> REWARD_MATCH_PAIR;
            case SPEED_GUESS -> REWARD_SPEED_GUESS;
            case FINGER_DANCE -> REWARD_FINGER_DANCE;
            case MEMORY_FLIP -> REWARD_MEMORY_FLIP;
        };
    }

    private String gameLabel(GameCode code) {
        return switch (code) {
            case MATCH_PAIR -> "Nối Hình & Ký Hiệu";
            case SPEED_GUESS -> "Thám Tử Ký Hiệu";
            case FINGER_DANCE -> "Vũ Điệu Ngón Tay";
            case MEMORY_FLIP -> "Truy Tìm Ký Hiệu Bị Ẩn";
        };
    }

    // ==================== RÚT NỘI DUNG ====================

    @SuppressWarnings("unchecked")
    private List<UUID> randomSignIds(UUID topicId, UUID packId, int limit, Set<UUID> exclude) {
        StringBuilder sql = new StringBuilder("""
                SELECT s.id FROM signs s
                 WHERE s.is_published = TRUE
                   AND EXISTS (SELECT 1 FROM sign_videos v WHERE v.sign_id = s.id AND v.file_id IS NOT NULL)
                """);
        if (packId != null) {
            sql.append(" AND s.id IN (SELECT i.sign_id FROM word_pack_items i WHERE i.pack_id = :packId)");
        } else if (topicId != null) {
            sql.append(" AND s.primary_topic_id = :topicId");
        }
        if (!exclude.isEmpty()) {
            sql.append(" AND s.id NOT IN (:exclude)");
        }
        sql.append(" ORDER BY random() LIMIT :lim");

        var query = em.createNativeQuery(sql.toString()).setParameter("lim", limit);
        if (packId != null) query.setParameter("packId", packId);
        else if (topicId != null) query.setParameter("topicId", topicId);
        if (!exclude.isEmpty()) query.setParameter("exclude", exclude);

        List<Object> rows = query.getResultList();
        return rows.stream().map(o -> (UUID) o).collect(Collectors.toList());
    }

    private GameQuestion buildQuestion(Sign correct, int optionCount, Set<UUID> used) {
        // Chữ nhiễu phải khác chữ đáp án — hai từ khác nhau vẫn có thể trùng wordVi
        // (biến thể vùng miền), khi đó người học không có cách nào phân biệt hai lựa chọn
        Set<String> labels = new HashSet<>();
        labels.add(normalize(correct.getWordVi()));

        List<Option> options = new ArrayList<>();
        options.add(Option.builder().signId(correct.getId()).label(correct.getWordVi()).build());

        // Rút dư để bù các từ trùng chữ bị loại
        List<UUID> distractorIds = randomSignIds(null, null, (optionCount - 1) * 4, used);
        for (Sign d : signRepository.findAllById(distractorIds)) {
            if (options.size() >= optionCount) break;
            if (labels.add(normalize(d.getWordVi()))) {
                options.add(Option.builder().signId(d.getId()).label(d.getWordVi()).build());
            }
        }
        if (options.size() < optionCount) {
            throw new InvalidFieldException("Kho từ không đủ để dựng lựa chọn cho trò chơi");
        }
        Collections.shuffle(options);

        GameSign media = toGameSign(correct);
        return GameQuestion.builder()
                .signId(correct.getId())
                .videoUrl(media.getVideoUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .options(options)
                .build();
    }

    private GameSign toGameSign(Sign sign) {
        List<SignVideo> videos = signVideoRepository.findAllBySignIdOrderByRegionAscViewAngleAsc(sign.getId());
        SignVideo primary = videos.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsPrimary()) && v.getFile() != null)
                .findFirst()
                .orElseGet(() -> videos.stream().filter(v -> v.getFile() != null).findFirst().orElse(null));
        return GameSign.builder()
                .signId(sign.getId())
                .wordVi(sign.getWordVi())
                .videoUrl(primary == null ? null : primary.getFile().getPublicUrl())
                .thumbnailUrl(primary == null || primary.getThumbnailFile() == null
                        ? null : primary.getThumbnailFile().getPublicUrl())
                .build();
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
