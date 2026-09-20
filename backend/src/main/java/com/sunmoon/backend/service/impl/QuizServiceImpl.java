package com.sunmoon.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sunmoon.backend.constant.enums.QuestionType;
import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.request.practice.GenerateQuestionsRequest;
import com.sunmoon.backend.dto.request.practice.QuizQuestionRequest;
import com.sunmoon.backend.dto.request.practice.QuizRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.practice.GenerateQuestionsResult;
import com.sunmoon.backend.dto.response.practice.QuizQuestionResponse;
import com.sunmoon.backend.dto.response.practice.QuizResponse;
import com.sunmoon.backend.entity.catalog.Lesson;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.entity.dictionary.Topic;
import com.sunmoon.backend.entity.practice.Quiz;
import com.sunmoon.backend.entity.practice.QuizQuestion;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.mapper.QuizMapper;
import com.sunmoon.backend.repository.*;
import com.sunmoon.backend.service.QuizService;
import com.sunmoon.backend.service.support.QuestionGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class QuizServiceImpl extends BaseServiceImpl<Quiz, UUID> implements QuizService {

    private static final ObjectMapper JSON = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final LessonRepository lessonRepository;
    private final TopicRepository topicRepository;
    private final SignRepository signRepository;
    private final SignTopicRepository signTopicRepository;
    private final QuizMapper quizMapper;
    private final QuestionGenerator generator;

    public QuizServiceImpl(QuizRepository quizRepository,
                           QuizQuestionRepository questionRepository,
                           LessonRepository lessonRepository,
                           TopicRepository topicRepository,
                           SignRepository signRepository,
                           SignTopicRepository signTopicRepository,
                           QuizMapper quizMapper,
                           QuestionGenerator generator) {
        // Quiz không có trường "status" — xem ghi chú ở CourseServiceImpl
        super(quizRepository, "quizType");
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.lessonRepository = lessonRepository;
        this.topicRepository = topicRepository;
        this.signRepository = signRepository;
        this.signTopicRepository = signTopicRepository;
        this.quizMapper = quizMapper;
        this.generator = generator;
    }

    @Override
    protected EntityManager getEntityManager() {
        return entityManager;
    }

    // ==================== ĐỌC ====================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<QuizResponse> search(BaseFilterRequest request) {
        Page<Quiz> page = filter(request);
        List<QuizResponse> items = page.getContent().stream().map(quizMapper::toResponse).toList();
        fillQuestionCounts(items);
        return PageResponse.<QuizResponse>builder()
                .items(items)
                .total(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizResponse> listByLesson(UUID lessonId) {
        List<QuizResponse> items = quizRepository.findAllByLessonIdOrderByTitleViAsc(lessonId)
                .stream().map(quizMapper::toResponse).toList();
        fillQuestionCounts(items);
        return items;
    }

    @Override
    @Transactional(readOnly = true)
    public QuizResponse getDetail(UUID id) {
        Quiz quiz = findQuiz(id);
        QuizResponse response = quizMapper.toResponse(quiz);

        List<QuizQuestion> questions = questionRepository.findAllByQuizIdOrderByDisplayOrderAsc(id);
        List<QuizQuestionResponse> items = new ArrayList<>(
                questions.stream().map(generator::toResponse).toList());
        generator.fillVideoUrls(items);

        response.setQuestions(items);
        response.setQuestionCount((long) items.size());
        return response;
    }

    // ==================== ĐỀ ====================

    @Override
    @Transactional
    public QuizResponse createQuiz(QuizRequest request) {
        Quiz quiz = quizMapper.toEntity(request);
        applyRelations(quiz, request);
        QuizResponse response = quizMapper.toResponse(quizRepository.save(quiz));
        response.setQuestionCount(0L);
        return response;
    }

    @Override
    @Transactional
    public QuizResponse updateQuiz(UUID id, QuizRequest request) {
        Quiz quiz = findQuiz(id);

        if (Boolean.TRUE.equals(request.getIsPublished())
                && questionRepository.countByQuizId(id) == 0) {
            // Cùng lý do như khoá học rỗng: người học mở đề trống sẽ tưởng hệ thống hỏng
            throw new ConflictException("Không thể xuất bản đề chưa có câu hỏi nào");
        }

        quizMapper.updateEntity(quiz, request);
        applyRelations(quiz, request);

        QuizResponse response = quizMapper.toResponse(quizRepository.save(quiz));
        response.setQuestionCount(questionRepository.countByQuizId(id));
        return response;
    }

    @Override
    @Transactional
    public void deleteQuiz(UUID id) {
        Quiz quiz = findQuiz(id);
        questionRepository.deleteAllByQuizId(id);
        quizRepository.delete(quiz);
    }

    @Override
    @Transactional
    public int setPublished(List<UUID> ids, boolean published) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<Quiz> quizzes = quizRepository.findAllById(ids);

        if (published) {
            List<String> empty = quizzes.stream()
                    .filter(q -> questionRepository.countByQuizId(q.getId()) == 0)
                    .map(Quiz::getTitleVi)
                    .toList();
            if (!empty.isEmpty()) {
                throw new ConflictException(
                        "Không thể xuất bản đề chưa có câu hỏi: " + String.join(", ", empty));
            }
        }

        quizzes.forEach(q -> q.setIsPublished(published));
        quizRepository.saveAll(quizzes);
        return quizzes.size();
    }

    // ==================== CÂU HỎI ====================

    @Override
    @Transactional
    public QuizQuestionResponse addQuestion(UUID quizId, QuizQuestionRequest request) {
        Quiz quiz = findQuiz(quizId);
        QuizQuestion question = buildQuestion(quiz, request, new QuizQuestion());
        if (request.getDisplayOrder() == null) {
            question.setDisplayOrder(questionRepository.nextDisplayOrder(quizId));
        }
        QuizQuestionResponse response = generator.toResponse(questionRepository.save(question));
        generator.fillVideoUrls(List.of(response));
        return response;
    }

    @Override
    @Transactional
    public QuizQuestionResponse updateQuestion(UUID quizId, UUID questionId,
                                               QuizQuestionRequest request) {
        QuizQuestion existing = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi: " + questionId));
        if (!existing.getQuiz().getId().equals(quizId)) {
            throw new ConflictException("Câu hỏi này không thuộc đề đã chỉ định");
        }
        QuizQuestion updated = buildQuestion(existing.getQuiz(), request, existing);
        QuizQuestionResponse response = generator.toResponse(questionRepository.save(updated));
        generator.fillVideoUrls(List.of(response));
        return response;
    }

    @Override
    @Transactional
    public void deleteQuestion(UUID quizId, UUID questionId) {
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi: " + questionId));
        if (!question.getQuiz().getId().equals(quizId)) {
            throw new ConflictException("Câu hỏi này không thuộc đề đã chỉ định");
        }
        questionRepository.delete(question);
    }

    @Override
    @Transactional
    public void reorderQuestions(UUID quizId, ReorderRequest request) {
        List<QuizQuestion> questions = questionRepository.findAllByQuizIdOrderByDisplayOrderAsc(quizId);
        Map<UUID, QuizQuestion> byId = new HashMap<>();
        questions.forEach(q -> byId.put(q.getId(), q));

        List<UUID> ordered = request.getOrderedIds();
        if (ordered.size() != questions.size()) {
            throw new ConflictException(
                    "Danh sách sắp xếp không khớp số câu hỏi hiện có. Hãy tải lại trang rồi thử lại.");
        }
        for (int i = 0; i < ordered.size(); i++) {
            QuizQuestion question = byId.get(ordered.get(i));
            if (question == null) {
                throw new NotFoundException("Câu hỏi không thuộc đề này: " + ordered.get(i));
            }
            question.setDisplayOrder(i);
        }
        questionRepository.saveAll(questions);
    }

    // ==================== SINH CÂU HỎI TỰ ĐỘNG ====================

    /**
     * Sinh câu trắc nghiệm từ kho từ vựng.
     *
     * Lõi chọn đáp án nhiễu, dựng câu dẫn, dàn tỉ lệ dạng câu... nằm chung ở
     * QuestionGenerator để đề soạn tay (ở đây) và đề trộn (QuizBlueprint) sinh
     * câu theo ĐÚNG cùng một luật. Phần còn lại của hàm này chỉ lo việc riêng
     * của đề soạn tay: xác định nhóm từ ứng viên theo chủ đề của ĐỀ NÀY, loại
     * từ đã hỏi rồi TRONG ĐỀ NÀY, và lưu câu hỏi vào đúng đề.
     *
     * Hạt ngẫu nhiên lấy từ id của đề, nên chạy thử và ghi thật cho ra ĐÚNG
     * cùng một bộ câu — bản xem trước chính là thứ sẽ được ghi.
     */
    @Override
    @Transactional
    public GenerateQuestionsResult generateQuestions(UUID quizId, GenerateQuestionsRequest request) {
        Quiz quiz = findQuiz(quizId);
        boolean dryRun = Boolean.TRUE.equals(request.getDryRun());

        GenerateQuestionsResult result = GenerateQuestionsResult.builder()
                .dryRun(dryRun)
                .warnings(new ArrayList<>())
                .questions(new ArrayList<>())
                .build();

        List<UUID> candidateIds = resolveCandidateSigns(quiz, request, result);
        if (candidateIds.isEmpty()) {
            result.getWarnings().add("Không tìm được từ vựng nào phù hợp để ra đề.");
            return result;
        }

        // Bỏ từ đã được hỏi trong đề này, tránh hỏi trùng
        Set<UUID> alreadyAsked = new HashSet<>(questionRepository.findSignIdsByQuizId(quizId));
        int optionCount = request.getOptionCount() == null ? 4 : request.getOptionCount();

        QuestionGenerator.Outcome outcome = generator.generate(candidateIds, alreadyAsked,
                QuestionGenerator.Params.builder()
                        .seed(quizId.getMostSignificantBits())
                        .questionCount(request.getQuestionCount() == null ? 10 : request.getQuestionCount())
                        .optionCount(optionCount)
                        .questionTypeMix(request.getQuestionTypeMix())
                        .distractorStrategy(request.getDistractorStrategy())
                        .build());
        result.getWarnings().addAll(outcome.warnings());

        if (outcome.questions().isEmpty()) {
            if (alreadyAsked.containsAll(candidateIds)) {
                result.getWarnings().add("Mọi từ phù hợp đều đã được hỏi trong đề này.");
            }
            return result;
        }

        int order = questionRepository.nextDisplayOrder(quizId);
        List<QuizQuestion> toSave = new ArrayList<>();
        for (QuizQuestion question : outcome.questions()) {
            question.setQuiz(quiz);
            question.setDisplayOrder(order++);
            toSave.add(question);
        }

        if (!dryRun) {
            questionRepository.saveAll(toSave);
        }

        result.setCreated(toSave.size());
        List<QuizQuestionResponse> preview =
                new ArrayList<>(toSave.stream().map(generator::toResponse).toList());
        generator.fillVideoUrls(preview);
        result.setQuestions(preview);
        return result;
    }

    /** Lấy danh sách từ ứng viên theo chủ đề hoặc theo danh sách chỉ định */
    private List<UUID> resolveCandidateSigns(Quiz quiz, GenerateQuestionsRequest request,
                                             GenerateQuestionsResult result) {
        List<UUID> ids;

        if (request.getSignIds() != null && !request.getSignIds().isEmpty()) {
            ids = request.getSignIds().stream().distinct().toList();
        } else {
            List<UUID> topicIds = request.getTopicIds();
            if ((topicIds == null || topicIds.isEmpty()) && quiz.getTopic() != null) {
                // Không chỉ định thì dùng chủ đề của chính đề thi
                topicIds = List.of(quiz.getTopic().getId());
            }
            if (topicIds == null || topicIds.isEmpty()) {
                result.getWarnings().add(
                        "Chưa chọn chủ đề và đề này cũng chưa gắn chủ đề nào.");
                return List.of();
            }
            LinkedHashSet<UUID> collected = new LinkedHashSet<>();
            for (UUID topicId : topicIds) {
                collected.addAll(signTopicRepository.findSignIdsByTopicId(topicId, false));
            }
            ids = new ArrayList<>(collected);
        }

        if (ids.isEmpty()) {
            result.getWarnings().add(
                    "Chủ đề đã chọn chưa có từ vựng nào. Hãy gán chủ đề cho từ vựng trước.");
            return List.of();
        }

        if (Boolean.TRUE.equals(request.getRequireVideo())) {
            int before = ids.size();
            ids = generator.filterHavingVideo(ids);
            int skipped = before - ids.size();
            if (skipped > 0) {
                result.setSignsSkipped(skipped);
                result.getWarnings().add(skipped + " từ bị bỏ qua vì chưa có video.");
            }
        }
        return ids;
    }

    // ==================== TIỆN ÍCH ====================
    // Chọn đáp án nhiễu, dựng câu dẫn mặc định, dàn tỉ lệ dạng câu... đã dời
    // sang QuestionGenerator để đề soạn tay và đề trộn dùng chung một luật.

    private Quiz findQuiz(UUID id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề: " + id));
    }

    private void applyRelations(Quiz quiz, QuizRequest request) {
        if (request.getLessonId() != null) {
            Lesson lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new NotFoundException(
                            "Không tìm thấy bài học: " + request.getLessonId()));
            quiz.setLesson(lesson);
        }
        if (request.getTopicId() != null) {
            Topic topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new NotFoundException(
                            "Không tìm thấy chủ đề: " + request.getTopicId()));
            quiz.setTopic(topic);
        }
    }

    private QuizQuestion buildQuestion(Quiz quiz, QuizQuestionRequest request, QuizQuestion target) {
        Sign sign = signRepository.findById(request.getSignId())
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy từ vựng: " + request.getSignId()));

        boolean aiPerform = request.getQuestionType() == QuestionType.AI_PERFORM;
        List<QuizQuestionRequest.QuizOptionRequest> options =
                request.getOptions() == null ? List.of() : request.getOptions();

        // CSDL có ràng buộc chk_quiz_question_answer. Kiểm ở đây để trả về thông
        // điệp tiếng Việt rõ ràng thay vì để Postgres ném lỗi ràng buộc khó hiểu.
        if (aiPerform) {
            if (request.getCorrectOptionIndex() != null) {
                throw new ConflictException(
                        "Câu dạng thực hiện ký hiệu không có đáp án để chọn");
            }
        } else {
            if (options.size() < 2) {
                throw new ConflictException("Câu trắc nghiệm phải có ít nhất 2 lựa chọn");
            }
            Integer index = request.getCorrectOptionIndex();
            if (index == null || index < 0 || index >= options.size()) {
                throw new ConflictException(
                        "Vị trí đáp án đúng phải nằm trong khoảng 0 đến " + (options.size() - 1));
            }
        }

        target.setQuiz(quiz);
        target.setQuestionType(request.getQuestionType());
        target.setSign(sign);
        target.setPromptVi(request.getPromptVi() == null || request.getPromptVi().isBlank()
                ? generator.defaultPrompt(request.getQuestionType(), sign)
                : request.getPromptVi());
        target.setCorrectOptionIndex(aiPerform ? null : request.getCorrectOptionIndex());
        target.setPoints(request.getPoints() == null ? 1 : request.getPoints());
        if (request.getDisplayOrder() != null) {
            target.setDisplayOrder(request.getDisplayOrder());
        }

        ArrayNode array = JSON.createArrayNode();
        for (QuizQuestionRequest.QuizOptionRequest option : options) {
            ObjectNode node = JSON.createObjectNode();
            if (option.getSignId() != null) node.put("signId", option.getSignId().toString());
            node.put("label", option.getLabel() == null ? "" : option.getLabel());
            array.add(node);
        }
        target.setOptionsJson(array);
        return target;
    }

    private void fillQuestionCounts(List<QuizResponse> items) {
        if (items.isEmpty()) return;
        List<UUID> ids = items.stream().map(QuizResponse::getId).toList();
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : quizRepository.countQuestionsByQuizIds(ids)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        items.forEach(item -> item.setQuestionCount(counts.getOrDefault(item.getId(), 0L)));
    }
}
