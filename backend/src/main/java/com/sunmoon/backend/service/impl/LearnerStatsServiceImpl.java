package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.dto.response.gamification.LearnerStatsResponse;
import com.sunmoon.backend.entity.gamification.UserPoints;
import com.sunmoon.backend.entity.progress.UserStreak;
import com.sunmoon.backend.repository.UserPointsRepository;
import com.sunmoon.backend.repository.UserStreakRepository;
import com.sunmoon.backend.service.LearnerStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearnerStatsServiceImpl implements LearnerStatsService {

    private final UserStreakRepository userStreakRepository;
    private final UserPointsRepository userPointsRepository;

    @Override
    @Transactional(readOnly = true)
    public LearnerStatsResponse getStats(UUID userId) {
        // Cố ý KHÔNG tạo bản ghi ở đây: đây là API chỉ đọc, gọi mỗi lần đổi
        // trang. Người chưa học ngày nào thì trả số 0, bản ghi thật sẽ do
        // StreakService tạo lúc bé thực sự học.
        int current = userStreakRepository.findByUserId(userId)
                .map(UserStreak::getCurrentStreak).orElse(0);
        int longest = userStreakRepository.findByUserId(userId)
                .map(UserStreak::getLongestStreak).orElse(0);

        UserPoints points = userPointsRepository.findByUserId(userId).orElse(null);
        int total = points == null ? 0 : points.getTotalPoints();
        int weekly = points == null ? 0 : points.getWeeklyPoints();

        // Cấp độ suy ra từ tổng điểm chứ không đọc cột level: cột đó là bộ
        // nhớ đệm, còn công thức ở đây mới là định nghĩa. Hai chỗ lệch nhau
        // thì con số hiện cho bé vẫn đúng.
        int perLevel = UserPoints.POINTS_PER_LEVEL;
        int level = total / perLevel + 1;
        int inLevel = total % perLevel;

        return LearnerStatsResponse.builder()
                .streakDays(current)
                .longestStreakDays(longest)
                .stars(total)
                .weeklyStars(weekly)
                .level(level)
                .levelPoints(inLevel)
                .levelTarget(perLevel)
                .levelPercent(inLevel * 100 / perLevel)
                .build();
    }
}
