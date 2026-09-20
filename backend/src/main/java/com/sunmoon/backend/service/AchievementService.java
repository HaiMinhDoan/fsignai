package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.progress.AchievementRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.progress.AchievementResponse;
import com.sunmoon.backend.entity.progress.Achievement;

import java.util.List;
import java.util.UUID;

public interface AchievementService {

    PageResponse<AchievementResponse> search(BaseFilterRequest request);

    AchievementResponse getDetail(UUID id);

    AchievementResponse create(AchievementRequest request);

    AchievementResponse update(UUID id, AchievementRequest request);

    void delete(UUID id);

    int setActive(List<UUID> ids, boolean active);

    /** Toàn bộ huy hiệu đang bật, kèm người học hiện tại đã đạt hay chưa */
    List<AchievementResponse> listForLearner(UUID userId);

    /**
     * Soát mọi huy hiệu đang bật mà người dùng chưa có, trao những cái đủ điều
     * kiện. Gọi sau mỗi hành động có thể ảnh hưởng điều kiện đạt huy hiệu
     * (hoàn thành bài học, nộp bài trắc nghiệm, ôn từ, chấm AI...) — không tự
     * chạy định kỳ, vì huy hiệu chỉ nên xuất hiện ngay sau hành động liên quan.
     *
     * @return huy hiệu MỚI được trao trong lần gọi này (rỗng nếu không có gì mới)
     */
    List<Achievement> evaluateAndAward(UUID userId);
}
