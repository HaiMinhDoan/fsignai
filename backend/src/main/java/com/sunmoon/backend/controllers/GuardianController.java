package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.guardian.ClaimChildRequest;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.guardian.ChildReportResponse;
import com.sunmoon.backend.dto.response.guardian.ChildSummaryResponse;
import com.sunmoon.backend.exception.customize.InvalidFieldException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.GuardianQueryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Tag(name = "Đồng hành - Phụ huynh", description = "Liên kết phụ huynh với con và báo cáo học tập")
@RestController
@RequestMapping("/api/v1/guardian")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianQueryRepository repo;

    private static final String[] DAY_LABELS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
    // Bỏ các ký tự dễ đọc nhầm (0/O, 1/I) — bé đọc mã cho bố mẹ nghe bằng miệng
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    @Operation(summary = "Bé tạo mã để bố mẹ nhận",
            description = "Mã sống 24 giờ. Tạo mã mới sẽ huỷ mã cũ.")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/invite")
    public ResponseEntity<ResponseData<Map<String, String>>> invite() {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
        String code = sb.toString();
        repo.putInvite(me, code);
        return ok(Map.of("inviteCode", code), "GUARDIAN_INVITE_CREATED");
    }

    @Operation(summary = "Phụ huynh nhập mã để nhận con")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/claim")
    public ResponseEntity<ResponseData<Map<String, String>>> claim(
            @Valid @RequestBody ClaimChildRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        UUID childId = repo.childByInvite(request.getInviteCode().trim().toUpperCase());
        if (childId == null) {
            throw new NotFoundException("Mã không đúng hoặc đã hết hạn");
        }
        if (childId.equals(me)) {
            // Không tự nhận chính mình làm con. Dùng exception của dự án chứ
            // không dùng ResponseStatusException: bộ bắt lỗi chung không biết
            // loại đó nên sẽ trả 500 thay vì 400.
            throw new InvalidFieldException("Không thể tự liên kết với chính mình");
        }
        String rel = Optional.ofNullable(request.getRelationship()).orElse("PARENT");
        repo.claim(me, childId, rel);
        return ok(Map.of("childUserId", childId.toString()), "GUARDIAN_CLAIMED");
    }

    @Operation(summary = "Danh sách bé đang theo dõi")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/children")
    public ResponseEntity<ResponseData<List<ChildSummaryResponse>>> children() {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        List<ChildSummaryResponse> list = new ArrayList<>();
        for (Object[] r : repo.childrenOf(me)) {
            UUID childId = (UUID) r[0];
            // Mục tiêu tuần = có học ít nhất 1 phút mỗi ngày trong 7 ngày
            long daysStudied = repo.dailyMinutes(childId, monday, today).stream()
                    .filter(d -> ((Number) d[1]).intValue() > 0).count();
            list.add(ChildSummaryResponse.builder()
                    .childUserId(childId)
                    .fullName((String) r[1])
                    .ageRange((String) r[2])
                    .relationship((String) r[3])
                    .weekPercent((int) Math.round(daysStudied * 100.0 / 7))
                    .build());
        }
        return ok(list, "GUARDIAN_CHILDREN_SUCCESS");
    }

    @Operation(summary = "Báo cáo học tập của một bé",
            description = "Chỉ xem được bé đã liên kết; người khác nhận 404 như thể bé không tồn tại")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/children/{childId}/report")
    public ResponseEntity<ResponseData<ChildReportResponse>> report(@PathVariable UUID childId) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        if (!repo.canView(me, childId)) {
            // Cùng thông điệp với "không tồn tại": không tiết lộ là có bé này
            // mà người hỏi không được xem
            throw new NotFoundException("Không tìm thấy bé được liên kết");
        }

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate prevMonday = monday.minusWeeks(1);

        Map<LocalDate, Object[]> byDate = new HashMap<>();
        for (Object[] d : repo.dailyMinutes(childId, monday, monday.plusDays(6))) {
            byDate.put(toLocalDate(d[0]), d);
        }

        List<ChildReportResponse.DayPoint> week = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            Object[] row = byDate.get(day);
            week.add(ChildReportResponse.DayPoint.builder()
                    .date(day)
                    .label(DAY_LABELS[i])
                    .minutes(row == null ? 0 : ((Number) row[1]).intValue())
                    .goalMet(row != null && Boolean.TRUE.equals(row[2]))
                    .build());
        }

        return ok(ChildReportResponse.builder()
                .childUserId(childId)
                .fullName(repo.fullNameOf(childId))
                .weekMinutes(repo.minutesBetween(childId, monday, monday.plusDays(6)))
                .prevWeekMinutes(repo.minutesBetween(childId, prevMonday, prevMonday.plusDays(6)))
                .masteredSigns(repo.masteredSigns(childId))
                // null có chủ đích: service chấm điểm AI chưa dựng. Trả 0 sẽ
                // bị đọc thành "bé làm sai hết", tệ hơn hẳn việc nói chưa có.
                .aiAccuracyPercent(null)
                .streakDays(repo.streakOf(childId))
                .week(week)
                .build(), "GUARDIAN_REPORT_SUCCESS");
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        return LocalDate.parse(o.toString());
    }

    private <T> ResponseEntity<ResponseData<T>> ok(T data, String messageCode) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseData.<T>builder()
                .status(HttpStatus.OK.value())
                .messageCode(messageCode)
                .data(data)
                .lang(SecurityContextHolder.getLang())
                .path(SecurityContextHolder.getPath())
                .timestamp(new java.util.Date())
                .build());
    }
}
