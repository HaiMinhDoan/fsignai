package com.sunmoon.backend.controllers;

import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.customizeanotations.RequireAuth;
import com.sunmoon.backend.dto.AuthInfo;
import com.sunmoon.backend.dto.request.profile.UpdateProfileRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.dto.response.forum.ForumPostResponse;
import com.sunmoon.backend.dto.response.profile.ProfileResponse;
import com.sunmoon.backend.service.ForumPostService;
import com.sunmoon.backend.service.ProfileService;
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

import java.util.Date;
import java.util.UUID;

@Tag(name = "Trang cá nhân", description = "Ảnh đại diện (ảnh hoặc video ≤ 5 giây), giới thiệu, bài đã đăng")
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ForumPostService forumPostService;

    @Operation(summary = "Trang cá nhân của tôi")
    @RequireAuth(roles = {RoleType.ALL})
    @GetMapping("/me")
    public ResponseEntity<ResponseData<ProfileResponse>> me() {
        UUID me = SecurityContextHolder.getAuthInfo().getId();
        return ok(profileService.get(me, me), "PROFILE_SUCCESS");
    }

    @Operation(summary = "Sửa tên hiển thị và phần giới thiệu")
    @RequireAuth(roles = {RoleType.ALL})
    @PutMapping("/me")
    public ResponseEntity<ResponseData<ProfileResponse>> update(@Valid @RequestBody UpdateProfileRequest request) {
        return ok(profileService.update(SecurityContextHolder.getAuthInfo().getId(), request), "PROFILE_UPDATED");
    }

    @Operation(summary = "Đổi ảnh đại diện",
            description = "Nhận ảnh JPG/PNG/WebP hoặc video WebM/MP4 tối đa 5 giây. Với video, gửi kèm 'poster' là "
                    + "khung hình đầu do trình duyệt cắt ra và durationMs do trình duyệt đo.")
    @RequireAuth(roles = {RoleType.ALL})
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseData<ProfileResponse>> changeAvatar(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "poster", required = false) MultipartFile poster,
            @RequestParam(required = false) Integer durationMs) {
        return ok(profileService.changeAvatar(SecurityContextHolder.getAuthInfo().getId(), file, poster, durationMs),
                "PROFILE_AVATAR_UPDATED");
    }

    @Operation(summary = "Gỡ ảnh đại diện, quay về chữ cái đầu tên")
    @RequireAuth(roles = {RoleType.ALL})
    @DeleteMapping("/me/avatar")
    public ResponseEntity<ResponseData<ProfileResponse>> removeAvatar() {
        return ok(profileService.removeAvatar(SecurityContextHolder.getAuthInfo().getId()), "PROFILE_AVATAR_REMOVED");
    }

    @Operation(summary = "Trang cá nhân của một người", description = "Công khai, không cần đăng nhập")
    @GetMapping("/{userId}")
    public ResponseEntity<ResponseData<ProfileResponse>> detail(@PathVariable UUID userId) {
        return ok(profileService.get(userId, viewerId()), "PROFILE_SUCCESS");
    }

    @Operation(summary = "Bài diễn đàn của một người",
            description = "Người khác chỉ thấy bài đang hiện; chủ trang thấy cả bài đang chờ duyệt hoặc bị ẩn.")
    @GetMapping("/{userId}/posts")
    public ResponseEntity<ResponseData<PageResponse<ForumPostResponse>>> posts(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        profileService.requireVisible(userId, viewerId());
        return ok(forumPostService.listByAuthor(userId, viewerId(), PageRequest.of(page, Math.min(size, 50))),
                "PROFILE_POSTS_SUCCESS");
    }

    private static UUID viewerId() {
        AuthInfo auth = SecurityContextHolder.getAuthInfo();
        return auth == null ? null : auth.getId();
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
