package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.practice.FlashcardReviewRequest;
import com.sunmoon.backend.dto.response.practice.FlashcardResponse;
import com.sunmoon.backend.dto.response.practice.FlashcardReviewResultResponse;

import java.util.List;
import java.util.UUID;

public interface FlashcardService {

    /**
     * Thẻ đến hạn ôn trước, chưa đủ `limit` thì lấp bằng từ hoàn toàn mới
     * (chưa từng ôn). Đây là hai nguồn khác nhau, không phải một truy vấn.
     */
    List<FlashcardResponse> getDue(UUID userId, UUID topicId, int limit);

    FlashcardReviewResultResponse review(UUID userId, UUID signId, FlashcardReviewRequest request);
}
