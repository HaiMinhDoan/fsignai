package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.dictionary.SavedSignResponse;
import com.sunmoon.backend.service.SavedSignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Thư viện của tôi", description = "Lưu/bỏ lưu từ vựng, xem danh sách đã lưu")
@RestController
@RequiredArgsConstructor
public class SavedSignController {

    private final SavedSignService savedSignService;

    @Operation(summary = "Từ này đã được lưu chưa")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/api/v1/dictionary/signs/{id}/save")
    public ResponseEntity<ResponseData<Map<String, Boolean>>> isSaved(@PathVariable UUID id) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        return ok(Map.of("saved", savedSignService.isSaved(userId, id)), "SAVED_SIGN_STATUS_SUCCESS");
    }

    @Operation(summary = "Lưu một từ vào Thư viện của tôi", description = "Lưu trùng thì bỏ qua, không báo lỗi")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/api/v1/dictionary/signs/{id}/save")
    public ResponseEntity<ResponseData<Map<String, Boolean>>> save(
            @PathVariable UUID id, @RequestBody(required = false) SaveSignRequest request) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        boolean created = savedSignService.save(userId, id, request == null ? null : request.note());
        return ok(Map.of("saved", true, "created", created), "SAVED_SIGN_ADDED");
    }

    public record SaveSignRequest(String note) {}

    @Operation(summary = "Bỏ lưu một từ", description = "Chưa lưu thì bỏ qua, không báo lỗi")
    @RequireAuth(roles = {RoleType.ALL})
    @DeleteMapping("/api/v1/dictionary/signs/{id}/save")
    public ResponseEntity<ResponseData<Void>> unsave(@PathVariable UUID id) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        savedSignService.unsave(userId, id);
        return ok(null, "SAVED_SIGN_REMOVED");
    }

    @Operation(summary = "Thư viện của tôi - danh sách từ đã lưu")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/api/v1/me/saved-signs")
    public ResponseEntity<ResponseData<PageResponse<SavedSignResponse>>> mySavedSigns(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        UUID userId = SecurityContextHolder.getAuthInfo().getId();
        return ok(savedSignService.list(userId, PageRequest.of(page, size)), "SAVED_SIGN_LIST_SUCCESS");
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
