package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.catalog.CourseRequest;
import com.sunmoon.backend.dto.request.catalog.GenerateCoursesRequest;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.catalog.CourseResponse;
import com.sunmoon.backend.dto.response.catalog.GenerateCoursesResult;
import com.sunmoon.backend.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "CMS - Khoá học", description = "Quản lý khoá học VSL")
@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "Lọc khoá học có phân trang")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/filter")
    public ResponseEntity<ResponseData<PageResponse<CourseResponse>>> filter(
            @RequestBody BaseFilterRequest request) {
        return ok(courseService.search(request), "COURSE_FILTER_SUCCESS");
    }

    @Operation(summary = "Danh sách phẳng cho dropdown")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/options")
    public ResponseEntity<ResponseData<List<CourseResponse>>> options() {
        return ok(courseService.getOptions(), "COURSE_OPTIONS_SUCCESS");
    }

    @Operation(summary = "Chi tiết khoá học kèm danh sách bài học")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<CourseResponse>> detail(@PathVariable UUID id) {
        return ok(courseService.getDetail(id), "COURSE_DETAIL_SUCCESS");
    }

    @Operation(summary = "Tạo khoá học mới")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping
    public ResponseEntity<ResponseData<CourseResponse>> create(@Valid @RequestBody CourseRequest request) {
        return ok(courseService.createCourse(request), "COURSE_CREATED");
    }

    @Operation(summary = "Cập nhật khoá học")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/{id}")
    public ResponseEntity<ResponseData<CourseResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CourseRequest request) {
        return ok(courseService.updateCourse(id, request), "COURSE_UPDATED");
    }

    @Operation(summary = "Xoá khoá học cùng toàn bộ bài học bên trong")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> delete(@PathVariable UUID id) {
        courseService.deleteCourse(id);
        return ok(null, "COURSE_DELETED");
    }

    @Operation(summary = "Xuất bản hoặc gỡ xuất bản hàng loạt")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/publish")
    public ResponseEntity<ResponseData<Map<String, Integer>>> publish(
            @RequestBody PublishRequest request) {
        int affected = courseService.setPublished(request.ids(), request.published());
        return ok(Map.of("affected", affected),
                request.published() ? "COURSE_PUBLISHED" : "COURSE_UNPUBLISHED");
    }

    @Operation(summary = "Sắp xếp lại thứ tự khoá học")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/reorder")
    public ResponseEntity<ResponseData<Void>> reorder(@Valid @RequestBody ReorderRequest request) {
        courseService.reorderCourses(request);
        return ok(null, "COURSE_REORDERED");
    }

    @Operation(summary = "Sinh khoá học và bài học tự động từ từ vựng theo chủ đề",
            description = "Mặc định chạy thử (dryRun=true) và chỉ trả về kết quả dự kiến. "
                    + "Đặt dryRun=false để ghi thật. Không bao giờ ghi đè khoá đã có bài học.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/generate")
    public ResponseEntity<ResponseData<GenerateCoursesResult>> generate(
            @Valid @RequestBody GenerateCoursesRequest request) {
        GenerateCoursesResult result = courseService.generate(request);
        return ok(result, Boolean.TRUE.equals(result.getDryRun())
                ? "COURSE_GENERATE_PREVIEW" : "COURSE_GENERATED");
    }

    /** Dùng record cho payload nhỏ, không cần DTO riêng */
    public record PublishRequest(List<UUID> ids, boolean published) {}

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
