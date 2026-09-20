package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.SignLevel;
import com.sunmoon.backend.constant.enums.UnitType;
import com.sunmoon.backend.constant.enums.WordType;
import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.practice.QuizBlueprintRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.practice.GenerateQuestionsResult;
import com.sunmoon.backend.dto.response.practice.QuizBlueprintResponse;
import com.sunmoon.backend.dto.response.practice.QuizQuestionResponse;
import com.sunmoon.backend.entity.dictionary.Topic;
import com.sunmoon.backend.entity.practice.QuizBlueprint;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.QuizBlueprintRepository;
import com.sunmoon.backend.repository.TopicRepository;
import com.sunmoon.backend.service.QuizBlueprintService;
import com.sunmoon.backend.service.impl.util.VietnameseTextUtil;
import com.sunmoon.backend.service.support.QuestionGenerator;
import com.sunmoon.backend.service.support.QuizBlueprintCodec;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Cấu hình đề trộn: khai báo LUẬT sinh đề, không soạn từng câu.
 *
 * Dùng chung lõi sinh câu hỏi với QuizServiceImpl qua QuestionGenerator, để
 * sửa luật đáp án nhiễu hay câu dẫn ở một chỗ là cả đề soạn tay lẫn đề trộn
 * đổi theo NHƯ NHAU.
 *
 * Các trường topicIds/levels/unitTypes/wordTypes/questionTypeMix của entity
 * lưu dưới dạng jsonb (JsonNode) nên KHÔNG dùng MapStruct ở đây — MapStruct
 * không tự biết chuyển List<UUID>/Map<Enum,Integer> sang JsonNode, lỗi kiểu
 * này thường im lặng ra mảng rỗng thay vì báo lỗi biên dịch. Việc chuyển đổi
 * được viết tay, tường minh, ngay trong service.
 */
@Service
public class QuizBlueprintServiceImpl extends BaseServiceImpl<QuizBlueprint, UUID>
        implements QuizBlueprintService {

    @PersistenceContext
    private EntityManager entityManager;

    private final QuizBlueprintRepository blueprintRepository;
    private final TopicRepository topicRepository;
    private final QuestionGenerator generator;
    private final QuizBlueprintCodec codec;

    public QuizBlueprintServiceImpl(QuizBlueprintRepository blueprintRepository,
                                    TopicRepository topicRepository,
                                    QuestionGenerator generator,
                                    QuizBlueprintCodec codec) {
        // QuizBlueprint không dùng trường "status" chung — xem ghi chú ở CourseServiceImpl.
        // "titleVi" chỉ để thoả constructor, controller không gọi updateStatus() cho entity này.
        super(blueprintRepository, "titleVi");
        this.blueprintRepository = blueprintRepository;
        this.topicRepository = topicRepository;
        this.generator = generator;
        this.codec = codec;
    }

    @Override
    protected EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<QuizBlueprintResponse> search(BaseFilterRequest request) {
        Page<QuizBlueprint> page = filter(request);
        List<QuizBlueprintResponse> items = page.getContent().stream().map(this::toResponse).toList();
        return PageResponse.<QuizBlueprintResponse>builder()
                .items(items)
                .total(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizBlueprintResponse getDetail(UUID id) {
        return toResponse(findBlueprint(id));
    }

    @Override
    @Transactional
    public QuizBlueprintResponse create(QuizBlueprintRequest request) {
        validateCode(request.getCode(), null);
        QuizBlueprint entity = new QuizBlueprint();
        applyRequest(entity, request);
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            // Cột code NOT NULL + UNIQUE ở CSDL, nhưng request không bắt buộc
            // phải nhập mã — admin có thể để trống và sửa lại sau trên giao diện.
            entity.setCode(uniqueCode(VietnameseTextUtil.toSlug(request.getTitleVi()), null));
        }
        return toResponse(blueprintRepository.save(entity));
    }

    @Override
    @Transactional
    public QuizBlueprintResponse update(UUID id, QuizBlueprintRequest request) {
        QuizBlueprint entity = findBlueprint(id);
        validateCode(request.getCode(), id);
        applyRequest(entity, request);
        return toResponse(blueprintRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        blueprintRepository.delete(findBlueprint(id));
    }

    @Override
    @Transactional
    public int setActive(List<UUID> ids, boolean active) {
        if (ids == null || ids.isEmpty()) return 0;
        List<QuizBlueprint> items = blueprintRepository.findAllById(ids);
        items.forEach(b -> b.setIsActive(active));
        blueprintRepository.saveAll(items);
        return items.size();
    }

    @Override
    @Transactional(readOnly = true)
    public GenerateQuestionsResult preview(UUID id) {
        QuizBlueprint blueprint = findBlueprint(id);

        GenerateQuestionsResult result = GenerateQuestionsResult.builder()
                .dryRun(true)
                .warnings(new ArrayList<>())
                .questions(new ArrayList<>())
                .build();

        List<UUID> candidateIds = resolveCandidates(blueprint, true, result.getWarnings());
        if (candidateIds.isEmpty()) {
            result.getWarnings().add("Không có từ vựng nào khớp cấu hình này.");
            return result;
        }

        // Hạt cố định theo id cấu hình: chỉ để admin soát lại luật khi sửa, rút
        // thử nhiều lần ra cùng một đề. Lúc người học thi thật (mục tiến độ &
        // luyện tập), mỗi lượt thi dùng hạt riêng theo id lượt thi, nên mỗi lần
        // thi là một đề khác nhau — đúng như mục đích của đề trộn.
        QuestionGenerator.Outcome outcome = generator.generate(candidateIds, Set.of(),
                QuestionGenerator.Params.builder()
                        .seed(id.getMostSignificantBits())
                        .questionCount(blueprint.getQuestionCount())
                        .optionCount(blueprint.getOptionCount())
                        .questionTypeMix(codec.readTypeMix(blueprint.getQuestionTypeMix()))
                        .distractorStrategy(blueprint.getDistractorStrategy())
                        .build());

        result.getWarnings().addAll(outcome.warnings());
        List<QuizQuestionResponse> preview = new ArrayList<>(
                outcome.questions().stream().map(generator::toResponse).toList());
        generator.fillVideoUrls(preview);
        result.setQuestions(preview);
        result.setCreated(preview.size());
        return result;
    }

    // ==================== TIỆN ÍCH ====================

    private QuizBlueprint findBlueprint(UUID id) {
        return blueprintRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình đề trộn: " + id));
    }

    private void validateCode(String code, UUID excludeId) {
        if (code == null || code.isBlank()) return;
        boolean exists = excludeId == null
                ? blueprintRepository.existsByCode(code)
                : blueprintRepository.existsByCodeAndIdNot(code, excludeId);
        if (exists) {
            throw new ConflictException("Mã cấu hình đã tồn tại: " + code);
        }
    }

    /** Thêm hậu tố -2, -3... cho tới khi mã không đụng bản ghi nào khác */
    private String uniqueCode(String base, UUID excludeId) {
        String safeBase = (base == null || base.isBlank()) ? "de-tron" : base;
        String candidate = safeBase.length() > 90 ? safeBase.substring(0, 90) : safeBase;
        int suffix = 2;
        while (excludeId == null
                ? blueprintRepository.existsByCode(candidate)
                : blueprintRepository.existsByCodeAndIdNot(candidate, excludeId)) {
            candidate = safeBase + "-" + suffix++;
        }
        return candidate;
    }

    private void applyRequest(QuizBlueprint entity, QuizBlueprintRequest request) {
        if (request.getCode() != null && !request.getCode().isBlank()) {
            entity.setCode(request.getCode());
        }
        entity.setTitleVi(request.getTitleVi());
        entity.setDescriptionVi(request.getDescriptionVi());
        entity.setTopicIds(codec.toUuidArray(request.getTopicIds()));
        entity.setLevels(codec.toEnumArray(request.getLevels()));
        entity.setUnitTypes(codec.toEnumArray(request.getUnitTypes()));
        entity.setWordTypes(request.getWordTypes() == null ? null : codec.toEnumArray(request.getWordTypes()));
        if (request.getQuestionTypeMix() != null) {
            entity.setQuestionTypeMix(codec.toIntMap(request.getQuestionTypeMix()));
        }
        if (request.getQuestionCount() != null) entity.setQuestionCount(request.getQuestionCount());
        if (request.getOptionCount() != null) entity.setOptionCount(request.getOptionCount());
        if (request.getPassScore() != null) entity.setPassScore(request.getPassScore());
        entity.setTimeLimitSeconds(request.getTimeLimitSeconds());
        if (request.getDistractorStrategy() != null) {
            entity.setDistractorStrategy(request.getDistractorStrategy());
        }
        if (request.getAvoidRecentDays() != null) entity.setAvoidRecentDays(request.getAvoidRecentDays());
        if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
    }

    private QuizBlueprintResponse toResponse(QuizBlueprint entity) {
        List<UUID> topicIds = codec.readUuidArray(entity.getTopicIds());
        List<SignLevel> levels = codec.readEnumArray(entity.getLevels(), SignLevel.class);
        List<UnitType> unitTypes = codec.readEnumArray(entity.getUnitTypes(), UnitType.class);
        List<WordType> wordTypes = entity.getWordTypes() == null
                ? null : codec.readEnumArray(entity.getWordTypes(), WordType.class);

        List<String> topicNames = topicIds.isEmpty() ? List.of()
                : topicRepository.findAllById(topicIds).stream().map(Topic::getNameVi).toList();

        long matchingCount = resolveCandidates(entity, true, null).size();

        return QuizBlueprintResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .titleVi(entity.getTitleVi())
                .descriptionVi(entity.getDescriptionVi())
                .topicIds(topicIds)
                .topicNames(topicNames)
                .levels(levels)
                .unitTypes(unitTypes)
                .wordTypes(wordTypes)
                .questionTypeMix(codec.readTypeMix(entity.getQuestionTypeMix()))
                .questionCount(entity.getQuestionCount())
                .optionCount(entity.getOptionCount())
                .passScore(entity.getPassScore())
                .timeLimitSeconds(entity.getTimeLimitSeconds())
                .distractorStrategy(entity.getDistractorStrategy())
                .avoidRecentDays(entity.getAvoidRecentDays())
                .isActive(entity.getIsActive())
                .matchingSignCount(matchingCount)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Từ vựng khớp bộ lọc của cấu hình này. requireVideo luôn bật: đề trộn chỉ
     * hỏi được những từ có video, y hệt lý do ở đề soạn tay.
     */
    private List<UUID> resolveCandidates(QuizBlueprint entity, boolean requireVideo, List<String> warnings) {
        List<UUID> topicIds = codec.readUuidArray(entity.getTopicIds());
        List<SignLevel> levels = codec.readEnumArray(entity.getLevels(), SignLevel.class);
        List<UnitType> unitTypes = codec.readEnumArray(entity.getUnitTypes(), UnitType.class);
        List<WordType> wordTypes = entity.getWordTypes() == null
                ? null : codec.readEnumArray(entity.getWordTypes(), WordType.class);

        List<UUID> ids = generator.resolveCandidatesByAttributes(
                topicIds, levels, unitTypes, wordTypes, true);

        if (requireVideo) {
            int before = ids.size();
            ids = generator.filterHavingVideo(ids);
            int skipped = before - ids.size();
            if (skipped > 0 && warnings != null) {
                warnings.add(skipped + " từ bị bỏ qua vì chưa có video.");
            }
        }
        return ids;
    }
}
