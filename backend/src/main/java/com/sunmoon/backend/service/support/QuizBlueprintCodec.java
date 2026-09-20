package com.sunmoon.backend.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sunmoon.backend.constant.enums.QuestionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chuyển đổi qua lại giữa JsonNode (cột jsonb của QuizBlueprint) và các kiểu
 * Java thường (List/Map). Tách riêng vì CẢ QuizBlueprintServiceImpl (soạn và
 * xem trước cấu hình) LẪN QuizAttemptServiceImpl (rút đề thật lúc người học
 * thi) đều cần đọc đúng những trường này — trùng logic ở hai nơi mà lệch một
 * chỗ thì đề trộn xem trước và đề trộn thi thật sẽ hiểu cấu hình khác nhau.
 */
@Component
public class QuizBlueprintCodec {

    public JsonNode toUuidArray(List<UUID> ids) {
        ArrayNode arr = JsonNodeFactory.instance.arrayNode();
        if (ids != null) ids.forEach(id -> arr.add(id.toString()));
        return arr;
    }

    public <E extends Enum<E>> JsonNode toEnumArray(List<E> values) {
        ArrayNode arr = JsonNodeFactory.instance.arrayNode();
        if (values != null) values.forEach(v -> arr.add(v.name()));
        return arr;
    }

    public JsonNode toIntMap(Map<QuestionType, Integer> mix) {
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        if (mix != null) mix.forEach((k, v) -> obj.put(k.name(), v));
        return obj;
    }

    public List<UUID> readUuidArray(JsonNode node) {
        List<UUID> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> out.add(UUID.fromString(n.asText())));
        }
        return out;
    }

    public <E extends Enum<E>> List<E> readEnumArray(JsonNode node, Class<E> type) {
        List<E> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> out.add(Enum.valueOf(type, n.asText())));
        }
        return out;
    }

    public Map<QuestionType, Integer> readTypeMix(JsonNode node) {
        Map<QuestionType, Integer> map = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(
                    e -> map.put(QuestionType.valueOf(e.getKey()), e.getValue().asInt()));
        }
        return map;
    }
}
