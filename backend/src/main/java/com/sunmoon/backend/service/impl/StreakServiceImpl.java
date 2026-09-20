package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.StreakFreezeReason;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.progress.StreakFreeze;
import com.sunmoon.backend.entity.progress.UserStreak;
import com.sunmoon.backend.repository.StreakFreezeRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.repository.UserStreakRepository;
import com.sunmoon.backend.service.StreakService;
import com.sunmoon.backend.service.support.UserTimeZoneResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StreakServiceImpl implements StreakService {

    private final UserStreakRepository userStreakRepository;
    private final StreakFreezeRepository streakFreezeRepository;
    private final UserRepository userRepository;
    private final UserTimeZoneResolver timeZoneResolver;

    @Override
    @Transactional
    public UserStreak recordStudyDay(UUID userId) {
        UserStreak streak = getOrCreate(userId);
        LocalDate today = timeZoneResolver.today(userId);
        LocalDate last = streak.getLastActivityDate();

        // Đã ghi nhận hôm nay rồi -> idempotent, không tính thêm lần nữa
        if (today.equals(last)) {
            return streak;
        }

        if (last == null) {
            // Lần đầu tiên có hoạt động
            streak.setCurrentStreak(1);
            streak.setStreakStartDate(today);
        } else {
            long gapDays = ChronoUnit.DAYS.between(last, today) - 1;

            if (gapDays == 0) {
                // Liên tiếp: hôm nay = hôm qua + 1
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            } else if (gapDays > 0 && gapDays <= streak.getFreezeAvailable()) {
                // Đứt quãng nhưng đủ khiên che hết — chuỗi coi như không đứt.
                // Mỗi ngày bị bỏ lỡ ghi một dòng streak_freezes riêng (uq_streak_freeze
                // chặn trùng ngày), để sau này người học xem lại biết ngày nào được
                // khiên cứu.
                List<StreakFreeze> used = new ArrayList<>();
                for (long i = 1; i <= gapDays; i++) {
                    LocalDate missedDate = last.plusDays(i);
                    if (streakFreezeRepository.existsByUserIdAndUsedForDate(userId, missedDate)) {
                        continue;
                    }
                    used.add(StreakFreeze.builder()
                            .user(getUserRef(userId))
                            .usedForDate(missedDate)
                            .grantedReason(StreakFreezeReason.MONTHLY_GRANT)
                            .build());
                }
                streakFreezeRepository.saveAll(used);
                streak.setFreezeAvailable(streak.getFreezeAvailable() - (int) gapDays);
                streak.setFreezeUsedTotal(streak.getFreezeUsedTotal() + (int) gapDays);
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            } else {
                // Đứt quãng và không đủ khiên -> chuỗi reset, bắt đầu lại từ hôm nay
                streak.setCurrentStreak(1);
                streak.setStreakStartDate(today);
            }
        }

        streak.setLastActivityDate(today);
        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        return userStreakRepository.save(streak);
    }

    @Override
    @Transactional
    public UserStreak getOrCreate(UUID userId) {
        return userStreakRepository.findByUserId(userId)
                .orElseGet(() -> userStreakRepository.save(
                        UserStreak.builder().user(getUserRef(userId)).build()));
    }

    private User getUserRef(UUID userId) {
        return userRepository.getReferenceById(userId);
    }
}
