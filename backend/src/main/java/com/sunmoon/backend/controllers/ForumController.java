package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.MediaSource;
import com.sunmoon.backend.constant.enums.ReactionTargetType;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.request.forum.ForumCommentRequest;
import com.sunmoon.backend.dto.request.forum.ForumPostRequest;
import com.sunmoon.backend.dto.request.forum.ForumReportRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.forum.ForumCategoryResponse;
import com.sunmoon.backend.dto.response.forum.ForumCommentResponse;
import com.sunmoon.backend.dto.response.forum.ForumPostResponse;
import com.sunmoon.backend.dto.response.forum.ForumReportResponse;
import com.sunmoon.backend.dto.response.forum.MediaResponse;
import com.sunmoon.backend.service.ForumCategoryService;
import com.sunmoon.backend.service.ForumMediaService;
import com.sunmoon.backend.service.ForumCommentService;
import com.sunmoon.backend.service.ForumPostService;
import com.sunmoon.backend.service.ForumReactionService;
import com.sunmoon.backend.service.ForumReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Diễn đàn", description = "Bài viết, bình luận, thích, báo cáo vi phạm")
@RestController
@RequestMapping("/api/v1/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumCategoryService forumCategoryService;
    private final ForumPostService forumPostService;
    private final ForumCommentService forumCommentService;
    private final ForumReactionService forumReactionService;
    private final ForumReportService forumReportService;
    private final ForumMediaService forumMediaService;

    @Operation(summary = "Danh sách chuyên mục đang mở")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/categories")
    public ResponseEntity<ResponseData<List<ForumCategoryResponse>>> categories() {
        return ok(forumCategoryService.listPublished(), "FORUM_CATEGORY_LIST_SUCCESS");
    }

    @Operation(summary = "Danh sách bài viết đã đăng, ghim lên trước")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/posts")
    public ResponseEntity<ResponseData<PageResponse<ForumPostResponse>>> posts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(forumPostService.listPublished(categoryId, me, PageRequest.of(page, size)),
                "FORUM_POST_LIST_SUCCESS");
    }

    @Operation(summary = "Chi tiết bài viết, tự tăng lượt xem")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/posts/{id}")
    public ResponseEntity<ResponseData<ForumPostResponse>> postDetail(@PathVariable UUID id) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(forumPostService.detail(id, me, true), "FORUM_POST_DETAIL_SUCCESS");
    }

    @Operation(summary = "Tải video ký hiệu hoặc ảnh lên",
            description = "Gọi TRƯỚC khi đăng bài: người dùng quay xong, xem lại, quay lại nếu chưa ưng, "
                    + "rồi mới bấm Đăng. Trả về id để truyền vào titleMediaId hoặc mediaIds. "
                    + "Ảnh đại diện của video do trình duyệt cắt rồi gửi kèm ở phần 'poster' — "
                    + "máy chủ Java không giải mã được video.")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseData<MediaResponse>> uploadMedia(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "poster", required = false) MultipartFile poster,
            @RequestParam(required = false) MediaSource source,
            @RequestParam(required = false) Integer durationMs,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(forumMediaService.upload(me, file, poster, source, durationMs, width, height),
                "FORUM_MEDIA_UPLOADED");
    }

    @Operation(summary = "Bỏ tệp vừa tải lên nhưng chưa đăng",
            description = "Chỉ xoá được tệp của chính mình và chưa gắn vào bài hay bình luận nào.")
    @RequireAuth(roles = {RoleType.ALL})
    @DeleteMapping("/media/{id}")
    public ResponseEntity<ResponseData<Void>> deleteMedia(@PathVariable UUID id) {
        forumMediaService.deleteOwnUnattached(SecurityContextHolder.getAuthInfo().getId(), id);
        return ok(null, "FORUM_MEDIA_DELETED");
    }

    @Operation(summary = "Đăng bài mới - hiện công khai ngay, chỉ gỡ khi bị báo cáo và duyệt gỡ",
            description = "Tiêu đề và nội dung đều có thể là chữ, video ký hiệu, ảnh — hoặc bỏ trống, "
                    + "miễn là bài nói được điều gì đó bằng ít nhất một kênh.")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/posts")
    public ResponseEntity<ResponseData<ForumPostResponse>> createPost(@Valid @RequestBody ForumPostRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(forumPostService.create(me, request), "FORUM_POST_CREATED");
    }

    @Operation(summary = "Sửa bài của chính mình")
    @RequireAuth(roles = {RoleType.ALL})
    @PutMapping("/posts/{id}")
    public ResponseEntity<ResponseData<ForumPostResponse>> updatePost(
            @PathVariable UUID id, @Valid @RequestBody ForumPostRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(forumPostService.update(me, id, request), "FORUM_POST_UPDATED");
    }

    @Operation(summary = "Gỡ bài của chính mình")
    @RequireAuth(roles = {RoleType.ALL})
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ResponseData<Void>> deletePost(@PathVariable UUID id) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        forumPostService.delete(me, id);
        return ok(null, "FORUM_POST_DELETED");
    }

    @Operation(summary = "Danh sách bình luận của một bài (đã dựng phẳng, tối đa 2 cấp)")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ResponseData<List<ForumCommentResponse>>> comments(@PathVariable UUID postId) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(forumCommentService.listForPost(postId, me), "FORUM_COMMENT_LIST_SUCCESS");
    }

    @Operation(summary = "Bình luận vào bài - hiện ngay")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ResponseData<ForumCommentResponse>> createComment(
            @PathVariable UUID postId, @Valid @RequestBody ForumCommentRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(forumCommentService.create(me, postId, request), "FORUM_COMMENT_CREATED");
    }

    @Operation(summary = "Gỡ bình luận của chính mình")
    @RequireAuth(roles = {RoleType.ALL})
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<ResponseData<Void>> deleteComment(@PathVariable UUID id) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        forumCommentService.delete(me, id);
        return ok(null, "FORUM_COMMENT_DELETED");
    }

    @Operation(summary = "Bật/tắt thích một bài viết")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/posts/{id}/like")
    public ResponseEntity<ResponseData<Map<String, Boolean>>> togglePostLike(@PathVariable UUID id) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        boolean liked = forumReactionService.toggleLike(me, ReactionTargetType.POST, id);
        return ok(Map.of("liked", liked), "FORUM_REACTION_TOGGLED");
    }

    @Operation(summary = "Bật/tắt thích một bình luận")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/comments/{id}/like")
    public ResponseEntity<ResponseData<Map<String, Boolean>>> toggleCommentLike(@PathVariable UUID id) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        boolean liked = forumReactionService.toggleLike(me, ReactionTargetType.COMMENT, id);
        return ok(Map.of("liked", liked), "FORUM_REACTION_TOGGLED");
    }

    @Operation(summary = "Báo cáo bài viết hoặc bình luận vi phạm")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping("/reports")
    public ResponseEntity<ResponseData<ForumReportResponse>> report(@Valid @RequestBody ForumReportRequest request) {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(forumReportService.create(me, request), "FORUM_REPORT_CREATED");
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
