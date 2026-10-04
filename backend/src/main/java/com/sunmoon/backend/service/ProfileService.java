package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.profile.UpdateProfileRequest;
import com.sunmoon.backend.dto.response.profile.ProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Trang cá nhân: tên hiển thị, giới thiệu, ảnh đại diện (ảnh tĩnh hoặc video ≤ 5 giây) */
public interface ProfileService {

    /** @param viewerId người đang xem, null nếu chưa đăng nhập */
    ProfileResponse get(UUID userId, UUID viewerId);

    /** Ném 404 nếu trang không công khai với người xem (không tồn tại, bị khoá, bị vô hiệu hoá) */
    void requireVisible(UUID userId, UUID viewerId);

    ProfileResponse update(UUID userId, UpdateProfileRequest request);

    /**
     * Đổi ảnh đại diện.
     *
     * @param file      ảnh (jpeg/png/webp) hoặc video ≤ 5 giây
     * @param poster    khung hình đầu do trình duyệt cắt ra — BẮT BUỘC với video, vì máy chủ Java
     *                  không giải mã được video mà mọi chỗ hiện ảnh tĩnh vẫn cần một tấm ảnh
     * @param durationMs thời lượng video do trình duyệt đo
     */
    ProfileResponse changeAvatar(UUID userId, MultipartFile file, MultipartFile poster, Integer durationMs);

    ProfileResponse removeAvatar(UUID userId);
}
