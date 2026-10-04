package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.AuthInfo;
import com.sunmoon.backend.dto.request.analytics.PageViewRequest;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.analytics.DashboardResponse;
import com.sunmoon.backend.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Tag(name = "Thống kê", description = "Ghi lượt xem trang của web học và số liệu Dashboard CMS")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Ghi một lượt xem trang",
            description = "Công khai — khách chưa đăng nhập cũng được đếm. Không lưu địa chỉ IP; "
                    + "chỉ giữ tên miền nguồn chứ không giữ đường dẫn đầy đủ của trang giới thiệu.")
    @PostMapping("/analytics/page-view")
    public ResponseEntity<Void> pageView(@Valid @RequestBody PageViewRequest request, HttpServletRequest http) {
        AuthInfo auth = SecurityContextHolder.getAuthInfo();
        analyticsService.trackPageView(request, auth == null ? null : auth.getId(), http.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Số liệu Dashboard",
            description = "Truy cập, người dùng, doanh thu, học tập cho N ngày gần nhất (giờ Việt Nam), "
                    + "kèm số của N ngày liền trước để so sánh.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN})
    @GetMapping("/admin/dashboard")
    public ResponseEntity<ResponseData<DashboardResponse>> dashboard(@RequestParam(defaultValue = "30") Integer days) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseData.<DashboardResponse>builder()
                .status(HttpStatus.OK.value())
                .messageCode("DASHBOARD_SUCCESS")
                .data(analyticsService.dashboard(days))
                .lang(SecurityContextHolder.getLang())
                .path(SecurityContextHolder.getPath())
                .timestamp(new Date())
                .build());
    }
}
