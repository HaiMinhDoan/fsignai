package com.sunmoon.backend.service;

import java.util.UUID;

public interface RewardChestService {

    /**
     * Mở rương khi đã đủ sao. Rương KHÔNG trừ sao — là mốc thành tựu, không phải
     * cửa hàng. Mở lại rương đã mở thì bỏ qua, không lỗi.
     *
     * @return true nếu vừa mở lần đầu, false nếu đã mở từ trước
     */
    boolean open(UUID userId, UUID chestId);
}
