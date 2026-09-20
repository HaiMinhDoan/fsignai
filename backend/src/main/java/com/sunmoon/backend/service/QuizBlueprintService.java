package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.practice.QuizBlueprintRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.practice.GenerateQuestionsResult;
import com.sunmoon.backend.dto.response.practice.QuizBlueprintResponse;

import java.util.List;
import java.util.UUID;

/**
 * Cấu hình đề trộn: khai báo LUẬT sinh đề (chủ đề, cấp độ, tỉ lệ dạng câu...)
 * để hệ thống rút ngẫu nhiên từ kho, thay vì soạn tay từng câu.
 */
public interface QuizBlueprintService {

    PageResponse<QuizBlueprintResponse> search(BaseFilterRequest request);

    QuizBlueprintResponse getDetail(UUID id);

    QuizBlueprintResponse create(QuizBlueprintRequest request);

    QuizBlueprintResponse update(UUID id, QuizBlueprintRequest request);

    void delete(UUID id);

    int setActive(List<UUID> ids, boolean active);

    /**
     * Rút thử một đề theo cấu hình hiện tại để admin soát lại luật trước khi
     * bật dùng. KHÔNG ghi vào đâu cả — khác với lúc người học thi thật (mục
     * tiến độ & luyện tập), nơi mỗi lượt thi sinh và lưu một đề riêng.
     */
    GenerateQuestionsResult preview(UUID id);
}
