package com.sunmoon.backend.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunmoon.backend.exception.customize.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Gọi service Python chấm điểm (ai-service/). Service đó stateless và chỉ Spring Boot được gọi nó —
 * trình duyệt không bao giờ gọi thẳng, để không ai tự đặt điểm cho mình.
 */
@Slf4j
@Component
public class AiServiceClient {

    public record ExtractResult(double[][] features, double quality, int activeFrames, int handCount,
                                double durationSec, String featureVersion) {}

    public record Components(double handshape, double location, double movement) {}

    public record VerifyResult(double distance, Components components, String bestExemplarId, boolean mirrored,
                               List<String> hints, double trackingQuality, int activeFrames, int handCount,
                               String modelVersion, int processingMs) {}

    private final RestClient client;
    private final ObjectMapper mapper;

    public AiServiceClient(@Value("${ai.service.url}") String url,
                           @Value("${ai.service.secret:}") String secret,
                           @Value("${ai.service.connect-timeout-ms:3000}") int connectTimeoutMs,
                           @Value("${ai.service.read-timeout-ms:60000}") int readTimeoutMs,
                           ObjectMapper mapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        RestClient.Builder builder = RestClient.builder().baseUrl(url).requestFactory(factory);
        if (secret != null && !secret.isBlank()) {
            builder.defaultHeader("X-AI-Secret", secret);
        }
        this.client = builder.build();
        this.mapper = mapper;
    }

    public JsonNode health() {
        return call(() -> client.get().uri("/health").retrieve().body(JsonNode.class));
    }

    /** Video mẫu → chuỗi đặc trưng chuẩn hoá. Ném CommonException(422, code) nếu video không dùng được làm mẫu. */
    public ExtractResult extract(byte[] video, String filename) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("video", new ByteArrayResource(video) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        return call(() -> client.post().uri("/extract")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(ExtractResult.class));
    }

    public VerifyResult verify(Map<String, Object> payload) {
        return call(() -> client.post().uri("/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(VerifyResult.class));
    }

    private <T> T call(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (RestClientResponseException e) {
            throw translate(e);
        } catch (ResourceAccessException e) {
            log.error("Không gọi được ai-service: {}", e.getMessage());
            throw failure(HttpStatus.SERVICE_UNAVAILABLE,
                    "Dịch vụ chấm điểm AI đang tạm ngưng, bạn thử lại sau nhé", "AI_SERVICE_DOWN");
        }
    }

    /** Lỗi 4xx của ai-service mang mã trong `detail.code` — giữ lại để tầng trên dịch sang lời khuyên cho người học. */
    private CommonException translate(RestClientResponseException e) {
        String code = "AI_SERVICE_ERROR";
        String message = "Dịch vụ chấm điểm AI trả lỗi";
        try {
            JsonNode detail = mapper.readTree(e.getResponseBodyAsString()).path("detail");
            if (detail.hasNonNull("code")) code = detail.get("code").asText();
            if (detail.hasNonNull("message")) message = detail.get("message").asText();
        } catch (Exception ignored) {
            // thân phản hồi không phải JSON như mong đợi → dùng thông điệp mặc định
        }
        HttpStatus status = e.getStatusCode().is5xxServerError() ? HttpStatus.BAD_GATEWAY
                : HttpStatus.valueOf(e.getStatusCode().value());
        log.warn("ai-service {} {}: {}", e.getStatusCode().value(), code, message);
        return failure(status, message, code);
    }

    private CommonException failure(HttpStatus status, String message, String code) {
        CommonException ex = new CommonException(message);
        ex.setHttpStatus(status);
        ex.setData(Map.of("code", code));
        return ex;
    }
}
