package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.content.TopicRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.content.TopicResponse;
import com.sunmoon.backend.mapper.TopicMapper;
import com.sunmoon.backend.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Tag(name = "CMS - Chủ đề", description = "Quản lý cây chủ đề từ vựng VSL")
@RestController
@RequestMapping("/api/v1/admin/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;
    private final TopicMapper topicMapper;

    @Operation(summary = "Lọc chủ đề có phân trang")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/filter")
    public ResponseEntity<ResponseData<PageResponse<TopicResponse>>> filter(
            @RequestBody BaseFilterRequest request) {
        PageResponse<TopicResponse> page =
                PageResponse.of(topicService.filter(request), topicMapper::toResponse);
        return ok(page, "TOPIC_FILTER_SUCCESS");
    }

    @Operation(summary = "Cây chủ đề đầy đủ (cho component Tree)")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @GetMapping("/tree")
    public ResponseEntity<ResponseData<List<TopicResponse>>> tree() {
        return ok(topicService.getTree(), "TOPIC_TREE_SUCCESS");
    }

    @Operation(summary = "Danh sách phẳng cho dropdown")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/options")
    public ResponseEntity<ResponseData<List<TopicResponse>>> options() {
        return ok(topicService.getOptions(), "TOPIC_OPTIONS_SUCCESS");
    }

    @Operation(summary = "Chi tiết một chủ đề")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<TopicResponse>> detail(@PathVariable UUID id) {
        return ok(topicService.getDetail(id), "TOPIC_DETAIL_SUCCESS");
    }

    @Operation(summary = "Tạo chủ đề mới")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping
    public ResponseEntity<ResponseData<TopicResponse>> create(@Valid @RequestBody TopicRequest request) {
        return ok(topicService.createTopic(request), "TOPIC_CREATED");
    }

    @Operation(summary = "Cập nhật chủ đề")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/{id}")
    public ResponseEntity<ResponseData<TopicResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody TopicRequest request) {
        return ok(topicService.updateTopic(id, request), "TOPIC_UPDATED");
    }

    @Operation(summary = "Xoá chủ đề")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> delete(@PathVariable UUID id) {
        topicService.deleteTopic(id);
        return ok(null, "TOPIC_DELETED");
    }

    // ResponseData giu nguyen contract cua du an: status la ma HTTP that,
    // data la payload, messageCode la khoa i18n cho frontend.
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
