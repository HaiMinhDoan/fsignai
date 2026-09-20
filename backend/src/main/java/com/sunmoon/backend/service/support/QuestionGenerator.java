package com.sunmoon.backend.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sunmoon.backend.constant.enums.DistractorStrategy;
import com.sunmoon.backend.constant.enums.QuestionType;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.constant.enums.SignLevel;
import com.sunmoon.backend.constant.enums.SignRelationType;
import com.sunmoon.backend.constant.enums.UnitType;
import com.sunmoon.backend.constant.enums.WordType;
import com.sunmoon.backend.dto.response.practice.QuizOptionResponse;
import com.sunmoon.backend.dto.response.practice.QuizQuestionResponse;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.entity.dictionary.SignVideo;
import com.sunmoon.backend.entity.practice.QuizQuestion;
import com.sunmoon.backend.mapper.QuizQuestionMapper;
import com.sunmoon.backend.repository.SignRelationRepository;
import com.sunmoon.backend.repository.SignRepository;
import com.sunmoon.backend.repository.SignTopicRepository;
import com.sunmoon.backend.repository.SignVideoRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Lõi sinh câu hỏi trắc nghiệm, dùng chung cho hai đường sinh đề:
 *
 * 1. Soạn đề tay (Quiz) — QuizServiceImpl.generateQuestions(): sinh xong thì
 *    LƯU thành các bản ghi quiz_questions, gắn cố định vào một đề cụ thể.
 * 2. Đề trộn (QuizBlueprint) — QuizBlueprintServiceImpl.preview(): sinh ra để
 *    admin soát luật, KHÔNG lưu; lúc người học thi thật đề trộn cũng gọi lại
 *    đúng lõi này với hạt ngẫu nhiên riêng theo lượt thi.
 *
 * Tách ra một chỗ để sửa luật chọn đáp án nhiễu hay cách dựng câu dẫn thì cả
 * hai đường sinh đề đổi theo NHƯ NHAU — tránh tình trạng đề soạn tay và đề
 * trộn cho ra kết quả khác nhau dù cấu hình giống hệt nhau.
 */
@Component
@RequiredArgsConstructor
public class QuestionGenerator {

    private final SignRepository signRepository;
    private final SignTopicRepository signTopicRepository;
    private final SignRelationRepository signRelationRepository;
    private final SignVideoRepository signVideoRepository;
    private final QuizQuestionMapper questionMapper;

    /** Gói tham số một lượt sinh câu, để chữ ký generate() khỏi dài dằng dặc */
    @Builder
    @Getter
    public static class Params {
        long seed;
        int questionCount;
        int optionCount;
        Map<QuestionType, Integer> questionTypeMix;
        DistractorStrategy distractorStrategy;
    }

    /** Câu hỏi CHƯA lưu (chưa gắn quiz, chưa có displayOrder) kèm cảnh báo phát sinh lúc sinh */
    public record Outcome(List<QuizQuestion> questions, List<String> warnings) {
        public Outcome {
            questions = questions == null ? List.of() : questions;
            warnings = warnings == null ? List.of() : warnings;
        }
    }

    /**
     * Sinh câu hỏi từ một nhóm từ ứng viên.
     *
     * Ba điểm đáng nói (xem thêm ghi chú gốc ở QuizServiceImpl trước khi tách):
     *
     * 1. Hạt ngẫu nhiên do người gọi truyền vào (seed trong Params) chứ không
     *    tự sinh ngẫu nhiên — nhờ vậy chạy lại với cùng seed luôn ra cùng một
     *    bộ câu. Soạn đề tay dùng seed theo id đề để "xem trước" = "ghi thật";
     *    đề trộn dùng seed theo id lượt thi để mỗi lần thi ra một đề khác nhau.
     *
     * 2. excludeSignIds loại từ đã hỏi rồi (tránh hỏi trùng trong cùng một đề /
     *    một khoảng thời gian gần đây).
     *
     * 3. Đáp án nhiễu ưu tiên từ dễ nhầm, sau đó mới tới cùng nhóm ứng viên.
     */
    public Outcome generate(List<UUID> candidateIds, Set<UUID> excludeSignIds, Params params) {
        List<String> warnings = new ArrayList<>();
        List<UUID> pool = new ArrayList<>(candidateIds.stream()
                .filter(id -> !excludeSignIds.contains(id))
                .toList());
        if (pool.isEmpty()) {
            return new Outcome(List.of(), warnings);
        }

        Random random = new Random(params.getSeed());
        Collections.shuffle(pool, random);

        int optionCount = params.getOptionCount();
        if (pool.size() < optionCount) {
            warnings.add("Kho chỉ có " + pool.size()
                    + " từ phù hợp, ít hơn số lựa chọn mỗi câu (" + optionCount
                    + "). Đáp án nhiễu sẽ bị lặp lại giữa các câu.");
        }

        int total = Math.min(params.getQuestionCount(), pool.size());
        List<QuestionType> plan = buildTypePlan(params.getQuestionTypeMix(), total);
        Map<UUID, Sign> signById = loadSigns(candidateIds);

        List<QuizQuestion> result = new ArrayList<>();
        for (int i = 0; i < plan.size() && i < pool.size(); i++) {
            UUID answerId = pool.get(i);
            Sign answer = signById.get(answerId);
            if (answer == null) continue;

            QuestionType type = plan.get(i);
            List<UUID> distractors = pickDistractors(
                    answerId, candidateIds, optionCount - 1, params.getDistractorStrategy(), random);

            List<UUID> optionIds = new ArrayList<>(distractors);
            int correctIndex = distractors.isEmpty() ? 0 : random.nextInt(distractors.size() + 1);
            optionIds.add(correctIndex, answerId);

            result.add(QuizQuestion.builder()
                    .questionType(type)
                    .sign(answer)
                    .promptVi(defaultPrompt(type, answer))
                    .optionsJson(buildOptionsJson(optionIds, signById))
                    .correctOptionIndex(correctIndex)
                    .points(1)
                    .build());
        }
        return new Outcome(result, warnings);
    }

    /** Dàn số câu theo tỉ lệ các dạng đã khai */
    public List<QuestionType> buildTypePlan(Map<QuestionType, Integer> mix, int total) {
        if (mix == null || mix.isEmpty()) {
            // Mặc định chia đều hai dạng cơ bản: xem video chọn từ, và ngược lại
            mix = Map.of(QuestionType.VIDEO_TO_WORD, 1, QuestionType.WORD_TO_VIDEO, 1);
        }

        int weightSum = mix.values().stream().mapToInt(Integer::intValue).sum();
        if (weightSum <= 0) {
            mix = Map.of(QuestionType.VIDEO_TO_WORD, 1);
            weightSum = 1;
        }

        List<QuestionType> plan = new ArrayList<>();
        for (Map.Entry<QuestionType, Integer> entry : mix.entrySet()) {
            int count = Math.round((float) total * entry.getValue() / weightSum);
            for (int i = 0; i < count; i++) {
                plan.add(entry.getKey());
            }
        }
        // Làm tròn có thể thừa hoặc thiếu vài câu so với yêu cầu
        while (plan.size() > total) plan.remove(plan.size() - 1);
        while (plan.size() < total) plan.add(plan.isEmpty() ? QuestionType.VIDEO_TO_WORD : plan.get(0));
        return plan;
    }

    /**
     * Chọn đáp án nhiễu.
     *
     * Thứ tự ưu tiên: từ dễ nhầm đã khai trong sign_relations, rồi tới từ cùng
     * nhóm ứng viên. Bảng sign_relations hiện đang rỗng nên thực tế sẽ rơi về
     * nhánh thứ hai — vẫn hợp lý vì nhóm ứng viên vốn đã cùng chủ đề.
     */
    public List<UUID> pickDistractors(UUID answerId, List<UUID> candidates, int needed,
                                      DistractorStrategy strategy, Random random) {
        LinkedHashSet<UUID> picked = new LinkedHashSet<>();

        boolean useConfused = strategy == DistractorStrategy.EASILY_CONFUSED
                || strategy == DistractorStrategy.MIXED;
        if (useConfused) {
            List<UUID> confused = new ArrayList<>(signRelationRepository
                    .findRelatedSignIds(answerId, SignRelationType.EASILY_CONFUSED));
            Collections.shuffle(confused, random);
            for (UUID id : confused) {
                if (picked.size() >= needed) break;
                if (!id.equals(answerId)) picked.add(id);
            }
        }

        if (picked.size() < needed) {
            List<UUID> rest = new ArrayList<>(candidates);
            rest.remove(answerId);
            rest.removeAll(picked);
            Collections.shuffle(rest, random);
            for (UUID id : rest) {
                if (picked.size() >= needed) break;
                picked.add(id);
            }
        }
        return new ArrayList<>(picked);
    }

    public String defaultPrompt(QuestionType type, Sign answer) {
        return switch (type) {
            case VIDEO_TO_WORD -> "Ký hiệu trong video có nghĩa là gì?";
            case WORD_TO_VIDEO -> "Đâu là ký hiệu của từ \"" + answer.getWordVi() + "\"?";
            case MULTIPLE_CHOICE -> "Chọn đáp án đúng cho từ \"" + answer.getWordVi() + "\".";
            case MATCHING -> "Ghép từ với ký hiệu tương ứng.";
            case AI_PERFORM -> "Hãy thực hiện ký hiệu của từ \"" + answer.getWordVi() + "\".";
        };
    }

    public JsonNode buildOptionsJson(List<UUID> optionIds, Map<UUID, Sign> signById) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        for (UUID id : optionIds) {
            Sign sign = signById.get(id);
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.put("signId", id.toString());
            node.put("label", sign == null ? "" : sign.getWordVi());
            array.add(node);
        }
        return array;
    }

    public Map<UUID, Sign> loadSigns(List<UUID> ids) {
        Map<UUID, Sign> map = new HashMap<>();
        signRepository.findAllById(ids).forEach(s -> map.put(s.getId(), s));
        return map;
    }

    /** Chuyển một câu hỏi (đã lưu hoặc chưa lưu) sang DTO trả về, giải mã optionsJson */
    public QuizQuestionResponse toResponse(QuizQuestion entity) {
        QuizQuestionResponse response = questionMapper.toResponse(entity);
        List<QuizOptionResponse> options = new ArrayList<>();
        JsonNode json = entity.getOptionsJson();
        if (json != null && json.isArray()) {
            for (JsonNode node : json) {
                String rawId = node.path("signId").asText(null);
                options.add(QuizOptionResponse.builder()
                        .signId(rawId == null || rawId.isBlank() ? null : UUID.fromString(rawId))
                        .label(node.path("label").asText(""))
                        .build());
            }
        }
        response.setOptions(options);
        return response;
    }

    /**
     * Gắn URL video cho câu hỏi và mọi lựa chọn, bằng MỘT truy vấn cho cả lô.
     * Một đề 10 câu x 4 lựa chọn là 50 lượt tra nếu làm từng cái.
     *
     * Ưu tiên video dùng chung (COMMON) — dùng khi soạn đề/xem trước, nơi
     * chưa biết người xem thuộc vùng miền nào.
     */
    public void fillVideoUrls(List<QuizQuestionResponse> questions) {
        fillVideoUrls(questions, Region.COMMON);
    }

    /**
     * Như trên, nhưng ưu tiên video của MỘT vùng miền cụ thể trước — dùng lúc
     * người học đang làm bài thật và đã chọn vùng miền học. Vẫn rơi về COMMON
     * (rồi bất kỳ vùng nào có sẵn) nếu vùng ưu tiên chưa có video cho từ đó.
     */
    public void fillVideoUrls(List<QuizQuestionResponse> questions, Region preferredRegion) {
        Set<UUID> signIds = new HashSet<>();
        for (QuizQuestionResponse q : questions) {
            if (q.getSignId() != null) signIds.add(q.getSignId());
            if (q.getOptions() != null) {
                q.getOptions().stream()
                        .map(QuizOptionResponse::getSignId)
                        .filter(Objects::nonNull)
                        .forEach(signIds::add);
            }
        }
        if (signIds.isEmpty()) return;

        Region preferred = preferredRegion == null ? Region.COMMON : preferredRegion;
        Map<UUID, String> urlBySign = new HashMap<>();
        Map<UUID, String> thumbBySign = new HashMap<>();
        Map<UUID, Region> regionBySign = new HashMap<>();
        for (SignVideo video : signVideoRepository.findPrimaryVideosBySignIds(new ArrayList<>(signIds))) {
            UUID signId = video.getSign().getId();
            Region current = regionBySign.get(signId);
            // Vùng ưu tiên thắng tuyệt đối một khi đã thấy; trước đó lấy tạm bản đầu tiên gặp
            if (current == null || (video.getRegion() == preferred && current != preferred)) {
                regionBySign.put(signId, video.getRegion());
                if (video.getFile() != null) {
                    urlBySign.put(signId, video.getFile().getPublicUrl());
                }
                // Ảnh phải đi theo ĐÚNG video vừa chọn, không phải ảnh của vùng
                // khác — nếu không, poster sẽ là ký hiệu miền Bắc trong khi
                // video phát là miền Nam.
                if (video.getThumbnailFile() != null) {
                    thumbBySign.put(signId, video.getThumbnailFile().getPublicUrl());
                } else {
                    thumbBySign.remove(signId);
                }
            }
        }

        for (QuizQuestionResponse q : questions) {
            if (q.getSignId() != null) {
                q.setSignVideoUrl(urlBySign.get(q.getSignId()));
                q.setSignThumbnailUrl(thumbBySign.get(q.getSignId()));
            }
            if (q.getOptions() != null) {
                q.getOptions().forEach(o -> {
                    if (o.getSignId() != null) o.setVideoUrl(urlBySign.get(o.getSignId()));
                });
            }
        }
    }

    /**
     * Từ vựng khớp một tổ hợp thuộc tính: chủ đề / cấp độ / đơn vị ngôn ngữ / từ
     * loại. Danh sách rỗng hoặc null ở một thuộc tính nghĩa là KHÔNG lọc theo
     * thuộc tính đó. Dùng cho đề trộn (QuizBlueprint), nơi bộ lọc phong phú hơn
     * đề soạn tay (chỉ lọc theo chủ đề).
     *
     * Có chủ đề: lấy từ sign_topics rồi lọc thêm trong bộ nhớ (kho chỉ ~3300
     * từ, không đáng để dựng truy vấn JOIN động). Không có chủ đề: truy vấn
     * thẳng bảng signs theo thuộc tính, tránh tải cả kho vào bộ nhớ.
     */
    public List<UUID> resolveCandidatesByAttributes(List<UUID> topicIds, List<SignLevel> levels,
                                                     List<UnitType> unitTypes, List<WordType> wordTypes,
                                                     boolean onlyPublished) {
        Set<UUID> byTopic = null;
        if (topicIds != null && !topicIds.isEmpty()) {
            LinkedHashSet<UUID> collected = new LinkedHashSet<>();
            for (UUID topicId : topicIds) {
                collected.addAll(signTopicRepository.findSignIdsByTopicId(topicId, onlyPublished));
            }
            if (collected.isEmpty()) return List.of();
            byTopic = collected;
        }

        boolean hasAttributeFilter = (levels != null && !levels.isEmpty())
                || (unitTypes != null && !unitTypes.isEmpty())
                || (wordTypes != null && !wordTypes.isEmpty());

        if (byTopic != null && !hasAttributeFilter) {
            return new ArrayList<>(byTopic);
        }

        Specification<Sign> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (onlyPublished) predicates.add(cb.isTrue(root.get("isPublished")));
            if (levels != null && !levels.isEmpty()) predicates.add(root.get("level").in(levels));
            if (unitTypes != null && !unitTypes.isEmpty()) predicates.add(root.get("unitType").in(unitTypes));
            if (wordTypes != null && !wordTypes.isEmpty()) predicates.add(root.get("wordType").in(wordTypes));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Sort stableOrder = Sort.by(Sort.Order.asc("wordViUnaccent"), Sort.Order.asc("id"));
        List<UUID> byAttrs = signRepository.findAll(spec, stableOrder)
                .stream().map(Sign::getId).toList();

        if (byTopic == null) return byAttrs;
        Set<UUID> finalTopic = byTopic;
        return byAttrs.stream().filter(finalTopic::contains).toList();
    }

    /** Loại từ chưa có video — câu hỏi dạng xem video mà không có video thì không ai trả lời được */
    public List<UUID> filterHavingVideo(List<UUID> ids) {
        if (ids.isEmpty()) return ids;
        Set<UUID> withVideo = new HashSet<>();
        signVideoRepository.findPrimaryVideosBySignIds(ids)
                .forEach(v -> withVideo.add(v.getSign().getId()));
        return ids.stream().filter(withVideo::contains).toList();
    }
}
