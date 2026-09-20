package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.ai.AiFeedbackRequest;
import com.sunmoon.backend.dto.request.ai.AiVerifyRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ai.AiCheckHistoryResponse;
import com.sunmoon.backend.dto.response.ai.AiVerifyResponse;
import com.sunmoon.backend.dto.response.ai.ReadinessResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Chấm ký hiệu của người học bằng camera. Mức A: chỉ luyện tập, chưa tính vào bài học hay bài kiểm tra.
 *
 * Phân vai: ai-service chỉ trả KHOẢNG CÁCH; Spring Boot mới tra ngưỡng của từ, quyết định đạt/chưa,
 * quy ra điểm và ghi kết quả.
 */
public interface AiCheckService {

    ReadinessResponse readiness(UUID signId);

    AiVerifyResponse verify(UUID userId, AiVerifyRequest request);

    PageResponse<AiCheckHistoryResponse> history(UUID userId, UUID signId, Pageable pageable);

    /** "Chấm như vậy có đúng không?" — mỗi người chỉ có một góp ý cho mỗi kết quả, gửi lại thì cập nhật */
    void feedback(UUID userId, UUID resultId, AiFeedbackRequest request);
}
