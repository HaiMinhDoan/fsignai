package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.gamification.LeaderboardRowResponse;
import com.sunmoon.backend.dto.response.gamification.RewardChestResponse;
import com.sunmoon.backend.repository.GamificationQueryRepository;
import com.sunmoon.backend.service.RewardChestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Học tập - Trò chơi", description = "Bảng xếp hạng và rương thưởng của Góc Trò Chơi")
@RestController
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationQueryRepository queryRepository;
    private final RewardChestService rewardChestService;

    @Operation(summary = "Bảng xếp hạng",
            description = "Mặc định xếp theo điểm tuần. Dòng của chính người đang xem được đánh dấu isMe.")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/api/v1/learn/leaderboard")
    public ResponseEntity<ResponseData<List<LeaderboardRowResponse>>> leaderboard(
            @RequestParam(defaultValue = "weekly") String period,
            @RequestParam(defaultValue = "10") int limit) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        boolean weekly = !"total".equalsIgnoreCase(period);
        // Chặn trên: ai đó gọi limit=100000 thì cũng không kéo sập truy vấn
        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<LeaderboardRowResponse> rows = new ArrayList<>();
        int rank = 1;
        for (Object[] r : queryRepository.topLearners(weekly, safeLimit)) {
            UUID uid = (UUID) r[0];
            rows.add(LeaderboardRowResponse.builder()
                    .rank(rank++)
                    .userId(uid)
                    .fullName((String) r[1])
                    .points(toInt(r[2]))
                    .gamesWon(toInt(r[3]))
                    .isMe(uid.equals(me))
                    .build());
        }
        return ok(rows, "LEADERBOARD_SUCCESS");
    }

    @Operation(summary = "Rương thưởng của người học",
            description = "Kèm đã mở chưa và còn thiếu bao nhiêu sao để mở được")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/api/v1/learn/reward-chests")
    public ResponseEntity<ResponseData<List<RewardChestResponse>>> chests() {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        int stars = queryRepository.starsOf(me);

        List<RewardChestResponse> list = new ArrayList<>();
        for (Object[] r : queryRepository.chestsFor(me)) {
            int required = toInt(r[5]);
            OffsetDateTime openedAt = toTime(r[6]);
            list.add(RewardChestResponse.builder()
                    .id((UUID) r[0])
                    .code((String) r[1])
                    .nameVi((String) r[2])
                    .descriptionVi((String) r[3])
                    .iconName((String) r[4])
                    .requiredPoints(required)
                    .opened(openedAt != null)
                    .openedAt(openedAt)
                    .unlockable(stars >= required)
                    .pointsShort(Math.max(required - stars, 0))
                    .build());
        }
        return ok(list, "REWARD_CHEST_LIST_SUCCESS");
    }

    @Operation(summary = "Mở một rương thưởng",
            description = "Phải đủ sao; không trừ sao. Mở lại rương đã mở thì không lỗi (opened=false).")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/api/v1/learn/reward-chests/{id}/open")
    public ResponseEntity<ResponseData<Map<String, Boolean>>> openChest(@PathVariable UUID id) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        boolean firstTime = rewardChestService.open(me, id);
        return ok(Map.of("opened", firstTime), "REWARD_CHEST_OPENED");
    }

    /** Postgres trả count() là BigInteger/Long tuỳ driver — gom về một kiểu */
    private static Integer toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof BigInteger b) return b.intValue();
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(o.toString());
    }

    private static OffsetDateTime toTime(Object o) {
        if (o == null) return null;
        if (o instanceof OffsetDateTime t) return t;
        if (o instanceof Timestamp ts) return ts.toInstant().atOffset(ZoneOffset.UTC);
        // Hibernate 6 trả cột timestamptz của native query dưới dạng Instant
        if (o instanceof java.time.Instant i) return i.atOffset(ZoneOffset.UTC);
        if (o instanceof java.time.LocalDateTime l) return l.atOffset(ZoneOffset.UTC);
        throw new IllegalStateException("Kiểu thời gian không nhận ra: " + o.getClass());
    }

    private <T> ResponseEntity<ResponseData<T>> ok(T data, String messageCode) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseData.<T>builder()
                .status(HttpStatus.OK.value())
                .messageCode(messageCode)
                .data(data)
                .lang(SecurityContextHolder.getLang())
                .path(SecurityContextHolder.getPath())
                .timestamp(new Date())
                .build());
    }
}
