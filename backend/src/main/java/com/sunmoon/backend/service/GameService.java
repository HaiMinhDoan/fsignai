package com.sunmoon.backend.service;

import com.sunmoon.backend.constant.enums.GameCode;
import com.sunmoon.backend.dto.request.gamification.GameFinishRequest;
import com.sunmoon.backend.dto.request.gamification.GameStartRequest;
import com.sunmoon.backend.dto.response.gamification.GameFinishResponse;
import com.sunmoon.backend.dto.response.gamification.GameStartResponse;

import java.util.UUID;

public interface GameService {

    /** Mở một ván mới và trả nội dung ván (cặp từ / câu đố) rút ngẫu nhiên từ kho */
    GameStartResponse start(UUID userId, GameCode gameCode, GameStartRequest request);

    /** Chốt kết quả ván: tính sao, ghi điểm, cộng vào hoạt động học hôm nay */
    GameFinishResponse finish(UUID userId, UUID sessionId, GameFinishRequest request);
}
