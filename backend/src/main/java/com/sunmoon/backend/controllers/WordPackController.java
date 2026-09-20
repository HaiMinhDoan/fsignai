package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.request.catalog.WordPackRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.catalog.WordPackItemResponse;
import com.sunmoon.backend.dto.response.catalog.WordPackResponse;
import com.sunmoon.backend.service.WordPackService;
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

@Tag(name = "CMS - Gói từ", description = "Quản lý gói từ cho bản đồ đảo phiêu lưu")
@RestController
@RequestMapping("/api/v1/admin/word-packs")
@RequiredArgsConstructor
public class WordPackController {

    private final WordPackService wordPackService;

    @Operation(summary = "Lọc gói từ có phân trang")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/filter")
    public ResponseEntity<ResponseData<PageResponse<WordPackResponse>>> filter(
            @RequestBody BaseFilterRequest request) {
        return ok(wordPackService.search(request), "WORD_PACK_FILTER_SUCCESS");
    }

    @Operation(summary = "Chi tiết gói từ kèm danh sách từ")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<WordPackResponse>> detail(@PathVariable UUID id) {
        return ok(wordPackService.getDetail(id), "WORD_PACK_DETAIL_SUCCESS");
    }

    @Operation(summary = "Tạo gói từ mới")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping
    public ResponseEntity<ResponseData<WordPackResponse>> create(@Valid @RequestBody WordPackRequest request) {
        return ok(wordPackService.createPack(request), "WORD_PACK_CREATED");
    }

    @Operation(summary = "Cập nhật gói từ")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PutMapping("/{id}")
    public ResponseEntity<ResponseData<WordPackResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody WordPackRequest request) {
        return ok(wordPackService.updatePack(id, request), "WORD_PACK_UPDATED");
    }

    @Operation(summary = "Xoá gói từ")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> delete(@PathVariable UUID id) {
        wordPackService.deletePack(id);
        return ok(null, "WORD_PACK_DELETED");
    }

    @Operation(summary = "Xuất bản hoặc gỡ xuất bản hàng loạt")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/publish")
    public ResponseEntity<ResponseData<Map<String, Integer>>> publish(@RequestBody PublishRequest request) {
        int affected = wordPackService.setPublished(request.ids(), request.published());
        return ok(Map.of("affected", affected),
                request.published() ? "WORD_PACK_PUBLISHED" : "WORD_PACK_UNPUBLISHED");
    }

    @Operation(summary = "Sắp xếp lại thứ tự gói từ trên bản đồ")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/reorder")
    public ResponseEntity<ResponseData<Void>> reorder(@Valid @RequestBody ReorderRequest request) {
        wordPackService.reorderPacks(request);
        return ok(null, "WORD_PACK_REORDERED");
    }

    // ==================== Nội dung gói ====================

    @Operation(summary = "Thêm nhiều từ vựng vào gói cùng lúc",
            description = "Bỏ qua các từ đã có sẵn trong gói, trả về danh sách từ thực sự được thêm.")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/{id}/items")
    public ResponseEntity<ResponseData<List<WordPackItemResponse>>> addSigns(
            @PathVariable UUID id, @RequestBody AddSignsRequest request) {
        return ok(wordPackService.addSigns(id, request.signIds()), "WORD_PACK_ITEM_ADDED");
    }

    @Operation(summary = "Xoá một từ khỏi gói")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<ResponseData<Void>> removeItem(
            @PathVariable UUID id, @PathVariable UUID itemId) {
        wordPackService.removeItem(id, itemId);
        return ok(null, "WORD_PACK_ITEM_REMOVED");
    }

    @Operation(summary = "Sắp xếp lại từ trong gói")
    @RequireAuth(roles = {RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR})
    @PostMapping("/{id}/items/reorder")
    public ResponseEntity<ResponseData<Void>> reorderItems(
            @PathVariable UUID id, @Valid @RequestBody ReorderRequest request) {
        wordPackService.reorderItems(id, request);
        return ok(null, "WORD_PACK_ITEM_REORDERED");
    }

    public record PublishRequest(List<UUID> ids, boolean published) {}

    public record AddSignsRequest(List<UUID> signIds) {}

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
