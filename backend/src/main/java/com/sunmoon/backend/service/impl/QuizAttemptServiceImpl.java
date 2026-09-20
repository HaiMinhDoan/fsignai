package com.sunmoon.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunmoon.backend.constant.enums.PointSource;
import com.sunmoon.backend.constant.enums.QuestionType;
import com.sunmoon.backend.constant.enums.QuizAttemptStatus;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.constant.enums.SignLevel;
import com.sunmoon.backend.constant.enums.UnitType;
import com.sunmoon.backend.constant.enums.WordType;
import com.sunmoon.backend.dto.request.practice.SubmitAttemptRequest;
import com.sunmoon.backend.dto.response.practice.QuizAttemptResponse;
import com.sunmoon.backend.dto.response.practice.QuizOptionResponse;
import com.sunmoon.backend.dto.response.practice.QuizQuestionResponse;
import com.sunmoon.backend.entity.practice.Quiz;
import com.sunmoon.backend.entity.practice.QuizAnswer;
import com.sunmoon.backend.entity.practice.QuizAttempt;
import com.sunmoon.backend.entity.practice.QuizBlueprint;
import com.sunmoon.backend.entity.practice.QuizQuestion;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.mapper.AchievementMapper;
import com.sunmoon.backend.repository.QuizAnswerRepository;
import com.sunmoon.backend.repository.QuizAttemptRepository;
import com.sunmoon.backend.repository.QuizBlueprintRepository;
import com.sunmoon.backend.repository.QuizQuestionRepository;
import com.sunmoon.backend.repository.QuizRepository;
import com.sunmoon.backend.repository.SignRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.ActivityDelta;
import com.sunmoon.backend.service.AchievementService;
import com.sunmoon.backend.service.PointService;
import com.sunmoon.backend.service.ProgressService;
import com.sunmoon.backend.service.QuizAttemptService;
import com.sunmoon.backend.service.StreakService;
import com.sunmoon.backend.service.support.QuestionGenerator;
import com.sunmoon.backend.service.support.QuizBlueprintCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Một lượt thi — nơi mục "Ngân hàng câu hỏi + đề trộn" và mục "Tiến độ &
 * chuỗi ngày học" gặp nhau. Xem ghi chú ở QuizAttemptService.
 *
 * GIỚI HẠN ĐÃ BIẾT: lượt thi từ một Quiz soạn tay không chốt ảnh (snapshot)
 * bộ câu hỏi lúc bắt đầu — luôn đọc quiz_questions HIỆN TẠI. Nếu admin sửa
 * hoặc xoá câu hỏi giữa lúc người học đang làm dở, phần xem lại/nộp bài có
 * thể lệch với những gì người học đã thấy lúc bắt đầu. Lượt thi từ đề trộn
 * KHÔNG có giới hạn này vì bộ câu hỏi được chốt vào generated_questions ngay
 * từ đầu.
 */
@Service
@RequiredArgsConstructor
public class QuizAttemptServiceImpl implements QuizAttemptService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizBlueprintRepository blueprintRepository;
    private final SignRepository signRepository;
    private final UserRepository userRepository;
    private final QuestionGenerator generator;
    private final QuizBlueprintCodec codec;
    private final ProgressService progressService;
    private final PointService pointService;
    private final StreakService streakService;
    private final AchievementService achievementService;
    private final AchievementMapper achievementMapper;

    // ==================== BẮT ĐẦU ====================

    @Override
    @Transactional
    public QuizAttemptResponse startFromQuiz(UUID userId, UUID quizId, Region region) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề: " + quizId));
        if (!Boolean.TRUE.equals(quiz.getIsPublished())) {
            throw new ConflictException("Đề chưa được xuất bản");
        }

        List<QuizQuestion> questions = questionRepository.findAllByQuizIdOrderByDisplayOrderAsc(quizId);
        if (questions.isEmpty()) {
            throw new ConflictException("Đề chưa có câu hỏi nào");
        }

        int maxScore = questions.stream().mapToInt(q -> q.getPoints() == null ? 0 : q.getPoints()).sum();

        QuizAttempt attempt = attemptRepository.save(QuizAttempt.builder()
                .user(userRepository.getReferenceById(userId))
                .quiz(quiz)
                .region(region == null ? Region.COMMON : region)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .startedAt(OffsetDateTime.now())
                .score(0)
                .maxScore(maxScore)
                .build());

        List<QuizQuestionResponse> display = new ArrayList<>(
                questions.stream().map(generator::toResponse).toList());
        generator.fillVideoUrls(display, attempt.getRegion());
        stripAnswers(display);

        return toResponse(attempt, display, null, List.of());
    }

    @Override
    @Transactional
    public QuizAttemptResponse startFromBlueprint(UUID userId, UUID blueprintId, Region region) {
        QuizBlueprint blueprint = blueprintRepository.findById(blueprintId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình đề trộn: " + blueprintId));
        if (!Boolean.TRUE.equals(blueprint.getIsActive())) {
            throw new ConflictException("Cấu hình đề trộn này đang tắt");
        }

        List<UUID> candidateIds = resolveBlueprintCandidates(blueprint);
        if (candidateIds.isEmpty()) {
            throw new ConflictException(
                    "Không tìm được từ vựng nào khớp cấu hình này. Hãy báo cho quản trị viên.");
        }

        Set<UUID> excludeSignIds = new HashSet<>();
        if (blueprint.getAvoidRecentDays() != null && blueprint.getAvoidRecentDays() > 0) {
            OffsetDateTime since = OffsetDateTime.now().minusDays(blueprint.getAvoidRecentDays());
            excludeSignIds.addAll(answerRepository.findRecentlyAskedSignIds(userId, since));
        }

        // Hạt NGẪU NHIÊN thật cho mỗi lượt thi — khác hẳn hạt cố định lúc admin
        // rút thử. Nhờ vậy mỗi lần thi ra một đề khác nhau, đúng mục đích của
        // đề trộn, trong khi vẫn tái dùng đúng một lõi sinh câu (QuestionGenerator).
        long seed = ThreadLocalRandom.current().nextLong();

        QuestionGenerator.Outcome outcome = generator.generate(candidateIds, excludeSignIds,
                QuestionGenerator.Params.builder()
                        .seed(seed)
                        .questionCount(blueprint.getQuestionCount())
                        .optionCount(blueprint.getOptionCount())
                        .questionTypeMix(codec.readTypeMix(blueprint.getQuestionTypeMix()))
                        .distractorStrategy(blueprint.getDistractorStrategy())
                        .build());

        if (outcome.questions().isEmpty()) {
            // Hết từ mới do avoidRecentDays quá rộng so với kho — thử lại không
            // tránh từ gần đây còn hơn là chặn hẳn người học không thi được
            outcome = generator.generate(candidateIds, Set.of(),
                    QuestionGenerator.Params.builder()
                            .seed(seed)
                            .questionCount(blueprint.getQuestionCount())
                            .optionCount(blueprint.getOptionCount())
                            .questionTypeMix(codec.readTypeMix(blueprint.getQuestionTypeMix()))
                            .distractorStrategy(blueprint.getDistractorStrategy())
                            .build());
        }
        if (outcome.questions().isEmpty()) {
            throw new ConflictException("Không đủ từ vựng để tạo đề. Hãy thử lại sau.");
        }

        List<QuizQuestionResponse> snapshot = new ArrayList<>(
                outcome.questions().stream().map(generator::toResponse).toList());
        Region effectiveRegion = region == null ? Region.COMMON : region;
        generator.fillVideoUrls(snapshot, effectiveRegion);

        // Chốt ảnh CÓ đáp án đúng vào CSDL trước, rồi mới xoá đáp án khỏi bản
        // trả về cho client — valueToTree() sao chép dữ liệu ngay lúc gọi nên
        // sửa các object Java sau đó không ảnh hưởng ngược lại JSON đã chốt.
        JsonNode storedSnapshot = JSON.valueToTree(snapshot);
        int maxScore = snapshot.stream()
                .mapToInt(q -> q.getPoints() == null ? 0 : q.getPoints()).sum();

        QuizAttempt attempt = attemptRepository.save(QuizAttempt.builder()
                .user(userRepository.getReferenceById(userId))
                .blueprint(blueprint)
                .region(effectiveRegion)
                .status(QuizAttemptStatus.IN_PROGRESS)
                .startedAt(OffsetDateTime.now())
                .seed(seed)
                .generatedQuestions(storedSnapshot)
                .score(0)
                .maxScore(maxScore)
                .build());

        List<QuizQuestionResponse> display = new ArrayList<>(snapshot);
        stripAnswers(display);

        return toResponse(attempt, display, null, List.of());
    }

    // ==================== XEM & NỘP BÀI ====================

    @Override
    @Transactional(readOnly = true)
    public QuizAttemptResponse getAttempt(UUID userId, UUID attemptId) {
        QuizAttempt attempt = findOwnedAttempt(userId, attemptId);
        List<QuizQuestionResponse> questions = loadDisplayQuestions(attempt);

        List<Integer> selected = null;
        if (attempt.getStatus() == QuizAttemptStatus.SUBMITTED) {
            selected = loadSelectedIndexes(attempt, questions.size());
        } else {
            stripAnswers(questions);
        }

        return toResponse(attempt, questions, selected, List.of());
    }

    @Override
    @Transactional
    public QuizAttemptResponse submit(UUID userId, UUID attemptId, SubmitAttemptRequest request) {
        QuizAttempt attempt = findOwnedAttempt(userId, attemptId);
        if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS) {
            throw new ConflictException("Lượt thi này đã kết thúc, không nộp lại được");
        }

        List<GradedQuestion> gradingList = loadGradingList(attempt);
        if (gradingList.isEmpty()) {
            throw new ConflictException("Đề này không còn câu hỏi nào để chấm");
        }

        Map<Integer, Integer> answersByIndex = new HashMap<>();
        for (SubmitAttemptRequest.AnswerSubmission a : request.getAnswers()) {
            if (a.getQuestionIndex() == null) continue;
            answersByIndex.put(a.getQuestionIndex(), a.getSelectedOptionIndex());
        }

        List<QuizAnswer> toSave = new ArrayList<>();
        List<Integer> selectedIndexes = new ArrayList<>();
        int totalScore = 0;
        int maxScore = 0;

        for (int i = 0; i < gradingList.size(); i++) {
            GradedQuestion graded = gradingList.get(i);
            Integer selected = answersByIndex.get(i);
            boolean isCorrect = graded.correctOptionIndex() != null
                    && Objects.equals(selected, graded.correctOptionIndex());
            int pointsEarned = isCorrect ? graded.points() : 0;

            maxScore += graded.points();
            totalScore += pointsEarned;
            selectedIndexes.add(selected);

            toSave.add(QuizAnswer.builder()
                    .attempt(attempt)
                    .question(graded.questionId() == null ? null
                            : questionRepository.getReferenceById(graded.questionId()))
                    .questionIndex(i)
                    .sign(graded.signId() == null ? null : signRepository.getReferenceById(graded.signId()))
                    .selectedOptionIndex(selected)
                    .isCorrect(isCorrect)
                    .pointsEarned(pointsEarned)
                    .answeredAt(OffsetDateTime.now())
                    .build());
        }
        answerRepository.saveAll(toSave);

        OffsetDateTime submittedAt = OffsetDateTime.now();
        int durationSeconds = (int) Duration.between(attempt.getStartedAt(), submittedAt).getSeconds();
        int passScoreThreshold = attempt.getQuiz() != null
                ? attempt.getQuiz().getPassScore() : attempt.getBlueprint().getPassScore();

        attempt.setScore(totalScore);
        attempt.setMaxScore(maxScore);
        attempt.setPassed(maxScore > 0 && (totalScore * 100.0 / maxScore) >= passScoreThreshold);
        attempt.setStatus(QuizAttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(submittedAt);
        attempt.setDurationSeconds(durationSeconds);
        attemptRepository.save(attempt);

        // 2 sao mỗi điểm đúng, thưởng thêm 10 sao nếu đạt; sourceId=attempt nên nộp không cộng trùng
        pointService.award(userId, PointSource.QUIZ, attempt.getId(),
                totalScore * 2 + (Boolean.TRUE.equals(attempt.getPassed()) ? 10 : 0), "Làm bài kiểm tra");

        // Một lượt thi vừa nộp luôn tính là "có học hôm nay" — ít nhất 1 phút để
        // một lượt thi rất nhanh vẫn được ghi nhận, không rơi vào "0 phút" vô nghĩa
        progressService.recordActivity(userId, ActivityDelta.builder()
                .minutesStudied(Math.max(1, durationSeconds / 60))
                .quizzesTaken(1)
                .build());
        streakService.recordStudyDay(userId);
        List<com.sunmoon.backend.entity.progress.Achievement> newAchievements =
                achievementService.evaluateAndAward(userId);

        List<QuizQuestionResponse> questions = loadDisplayQuestions(attempt); // đã SUBMITTED -> giữ đáp án đúng
        return toResponse(attempt, questions, selectedIndexes,
                newAchievements.stream().map(achievementMapper::toResponse).toList());
    }

    // ==================== TIỆN ÍCH ====================

    private QuizAttempt findOwnedAttempt(UUID userId, UUID attemptId) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lượt thi: " + attemptId));
        // Không phân biệt "không tồn tại" với "không phải của mình" — tránh lộ
        // việc một attemptId có tồn tại hay không cho người dùng khác
        if (!attempt.getUser().getId().equals(userId)) {
            throw new NotFoundException("Không tìm thấy lượt thi: " + attemptId);
        }
        return attempt;
    }

    private List<UUID> resolveBlueprintCandidates(QuizBlueprint blueprint) {
        List<UUID> topicIds = codec.readUuidArray(blueprint.getTopicIds());
        List<SignLevel> levels = codec.readEnumArray(blueprint.getLevels(), SignLevel.class);
        List<UnitType> unitTypes = codec.readEnumArray(blueprint.getUnitTypes(), UnitType.class);
        List<WordType> wordTypes = blueprint.getWordTypes() == null
                ? null : codec.readEnumArray(blueprint.getWordTypes(), WordType.class);

        List<UUID> ids = generator.resolveCandidatesByAttributes(
                topicIds, levels, unitTypes, wordTypes, true);
        return generator.filterHavingVideo(ids);
    }

    /** Câu hỏi để HIỂN THỊ cho một lượt thi đã có (xem lại/tiếp tục) */
    private List<QuizQuestionResponse> loadDisplayQuestions(QuizAttempt attempt) {
        if (attempt.getQuiz() != null) {
            List<QuizQuestion> questions =
                    questionRepository.findAllByQuizIdOrderByDisplayOrderAsc(attempt.getQuiz().getId());
            List<QuizQuestionResponse> display = new ArrayList<>(
                    questions.stream().map(generator::toResponse).toList());
            generator.fillVideoUrls(display, attempt.getRegion());
            return display;
        }
        return parseSnapshot(attempt.getGeneratedQuestions());
    }

    private List<QuizQuestionResponse> parseSnapshot(JsonNode node) {
        if (node == null || !node.isArray()) return new ArrayList<>();
        List<QuizQuestionResponse> list = new ArrayList<>();
        for (JsonNode item : node) {
            QuizQuestionResponse response = QuizQuestionResponse.builder()
                    .questionType(QuestionType.valueOf(item.path("questionType").asText()))
                    .promptVi(item.path("promptVi").asText(null))
                    .points(item.path("points").asInt(1))
                    .signId(parseUuidOrNull(item.path("signId")))
                    .signWordVi(item.path("signWordVi").asText(null))
                    .signGloss(item.path("signGloss").asText(null))
                    .signVideoUrl(item.path("signVideoUrl").asText(null))
                    .correctOptionIndex(item.hasNonNull("correctOptionIndex")
                            ? item.path("correctOptionIndex").asInt() : null)
                    .options(parseOptions(item.path("options")))
                    .build();
            list.add(response);
        }
        return list;
    }

    private List<QuizOptionResponse> parseOptions(JsonNode node) {
        List<QuizOptionResponse> options = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode o : node) {
                options.add(QuizOptionResponse.builder()
                        .signId(parseUuidOrNull(o.path("signId")))
                        .label(o.path("label").asText(""))
                        .videoUrl(o.path("videoUrl").asText(null))
                        .build());
            }
        }
        return options;
    }

    private UUID parseUuidOrNull(JsonNode node) {
        String text = node.isMissingNode() ? null : node.asText(null);
        return text == null || text.isBlank() ? null : UUID.fromString(text);
    }

    /** Danh sách gọn để CHẤM ĐIỂM — nguồn dữ liệu khác nhau tuỳ quiz hay blueprint, chấm thì như nhau */
    private record GradedQuestion(UUID questionId, UUID signId, Integer correctOptionIndex, int points) {}

    private List<GradedQuestion> loadGradingList(QuizAttempt attempt) {
        if (attempt.getQuiz() != null) {
            return questionRepository.findAllByQuizIdOrderByDisplayOrderAsc(attempt.getQuiz().getId())
                    .stream()
                    .map(q -> new GradedQuestion(q.getId(), q.getSign().getId(),
                            q.getCorrectOptionIndex(), q.getPoints() == null ? 1 : q.getPoints()))
                    .toList();
        }
        return parseSnapshot(attempt.getGeneratedQuestions()).stream()
                .map(r -> new GradedQuestion(null, r.getSignId(), r.getCorrectOptionIndex(),
                        r.getPoints() == null ? 1 : r.getPoints()))
                .toList();
    }

    private List<Integer> loadSelectedIndexes(QuizAttempt attempt, int questionCount) {
        Integer[] result = new Integer[questionCount];
        for (QuizAnswer answer : answerRepository.findAllByAttemptIdOrderByQuestionIndexAsc(attempt.getId())) {
            if (answer.getQuestionIndex() != null
                    && answer.getQuestionIndex() >= 0 && answer.getQuestionIndex() < questionCount) {
                result[answer.getQuestionIndex()] = answer.getSelectedOptionIndex();
            }
        }
        // Arrays.asList (khong phai List.of) - cau bo trong la null trong mang nay,
        // va List.of() nem NullPointerException ngay khi gap phan tu null
        return new ArrayList<>(Arrays.asList(result));
    }

    private void stripAnswers(List<QuizQuestionResponse> questions) {
        questions.forEach(q -> q.setCorrectOptionIndex(null));
    }

    private QuizAttemptResponse toResponse(QuizAttempt attempt, List<QuizQuestionResponse> questions,
                                           List<Integer> selectedIndexes,
                                           List<com.sunmoon.backend.dto.response.progress.AchievementResponse> newAchievements) {
        return QuizAttemptResponse.builder()
                .id(attempt.getId())
                .quizId(attempt.getQuiz() != null ? attempt.getQuiz().getId() : null)
                .quizTitleVi(attempt.getQuiz() != null ? attempt.getQuiz().getTitleVi() : null)
                .blueprintId(attempt.getBlueprint() != null ? attempt.getBlueprint().getId() : null)
                .blueprintTitleVi(attempt.getBlueprint() != null ? attempt.getBlueprint().getTitleVi() : null)
                .region(attempt.getRegion())
                .status(attempt.getStatus())
                .score(attempt.getScore())
                .maxScore(attempt.getMaxScore())
                .passed(attempt.getPassed())
                .passScoreRequired(attempt.getQuiz() != null
                        ? attempt.getQuiz().getPassScore()
                        : attempt.getBlueprint() != null ? attempt.getBlueprint().getPassScore() : null)
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .durationSeconds(attempt.getDurationSeconds())
                .questions(questions)
                .selectedOptionIndexes(selectedIndexes)
                .newAchievements(newAchievements)
                .build();
    }
}
