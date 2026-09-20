package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.PointSource;
import com.sunmoon.backend.entity.gamification.PointEvent;
import com.sunmoon.backend.entity.gamification.UserPoints;
import com.sunmoon.backend.repository.PointEventRepository;
import com.sunmoon.backend.repository.UserPointsRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    // Mốc "hôm nay" theo giờ Việt Nam, không theo giờ server (docs/02-data-model.md)
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PointEventRepository pointEventRepository;
    private final UserPointsRepository userPointsRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public int award(UUID userId, PointSource source, UUID sourceId, int points, String noteVi) {
        if (points == 0) return 0;
        if (sourceId != null && pointEventRepository.existsByUserIdAndSourceAndSourceId(userId, source, sourceId)) {
            return 0;
        }

        LocalDate today = LocalDate.now(ZONE);
        pointEventRepository.save(PointEvent.builder()
                .user(userRepository.getReferenceById(userId))
                .source(source)
                .sourceId(sourceId)
                .points(points)
                .noteVi(noteVi)
                .earnedDate(today)
                .build());

        UserPoints up = userPointsRepository.findByUserId(userId)
                .orElseGet(() -> UserPoints.builder().user(userRepository.getReferenceById(userId)).build());

        // Sang tuần/tháng mới thì điểm kỳ về 0 trước khi cộng
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);
        if (!weekStart.equals(up.getWeekStartDate())) {
            up.setWeekStartDate(weekStart);
            up.setWeeklyPoints(0);
        }
        if (!monthStart.equals(up.getMonthStartDate())) {
            up.setMonthStartDate(monthStart);
            up.setMonthlyPoints(0);
        }

        up.setTotalPoints(Math.max(0, up.getTotalPoints() + points));
        up.setWeeklyPoints(Math.max(0, up.getWeeklyPoints() + points));
        up.setMonthlyPoints(Math.max(0, up.getMonthlyPoints() + points));
        up.setLevel(up.getTotalPoints() / UserPoints.POINTS_PER_LEVEL + 1);
        userPointsRepository.save(up);
        return points;
    }
}
