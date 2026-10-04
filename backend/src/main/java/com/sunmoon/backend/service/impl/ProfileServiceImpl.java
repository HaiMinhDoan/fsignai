package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.EntityType;
import com.sunmoon.backend.constant.enums.ForumCommentStatus;
import com.sunmoon.backend.constant.enums.ForumPostStatus;
import com.sunmoon.backend.constant.enums.UserStatus;
import com.sunmoon.backend.constant.enums.VslRoleStatus;
import com.sunmoon.backend.dto.request.profile.UpdateProfileRequest;
import com.sunmoon.backend.dto.response.gamification.LearnerStatsResponse;
import com.sunmoon.backend.dto.response.profile.ProfileResponse;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.FileAttachmentRepository;
import com.sunmoon.backend.repository.ForumCommentRepository;
import com.sunmoon.backend.repository.ForumPostRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.LearnerStatsService;
import com.sunmoon.backend.service.MinioService;
import com.sunmoon.backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> VIDEO_TYPES = Set.of("video/webm", "video/mp4", "video/quicktime");

    private final UserRepository userRepository;
    private final FileAttachmentRepository fileRepository;
    private final ForumPostRepository forumPostRepository;
    private final ForumCommentRepository forumCommentRepository;
    private final LearnerStatsService learnerStatsService;
    private final MinioService minioService;

    @Value("${profile.avatar.max-video-seconds:5}")
    private int maxVideoSeconds;

    @Value("${profile.avatar.max-video-mb:8}")
    private int maxVideoMb;

    @Value("${profile.avatar.max-image-mb:5}")
    private int maxImageMb;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse get(UUID userId, UUID viewerId) {
        return toResponse(visibleUser(userId, viewerId), userId.equals(viewerId));
    }

    @Override
    @Transactional(readOnly = true)
    public void requireVisible(UUID userId, UUID viewerId) {
        visibleUser(userId, viewerId);
    }

    @Override
    @Transactional
    public ProfileResponse update(UUID userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        user.setFullName(request.getFullName().trim());
        String bio = request.getBio() == null ? null : request.getBio().trim();
        user.setBio(bio == null || bio.isEmpty() ? null : bio);
        user.setUpdatedAt(OffsetDateTime.now());
        return toResponse(userRepository.save(user), true);
    }

    @Override
    @Transactional
    public ProfileResponse changeAvatar(UUID userId, MultipartFile file, MultipartFile poster, Integer durationMs) {
        if (file == null || file.isEmpty()) {
            throw new CommonException("Chưa chọn tệp nào");
        }
        String mime = mimeOf(file);
        boolean video = VIDEO_TYPES.contains(mime);
        if (!video && !IMAGE_TYPES.contains(mime)) {
            throw new CommonException("Ảnh đại diện nhận ảnh JPG/PNG/WebP hoặc video WebM/MP4");
        }

        if (video) {
            if (file.getSize() > maxVideoMb * 1024L * 1024L) {
                throw new CommonException("Video ảnh đại diện nặng quá " + maxVideoMb + "MB");
            }
            // Thời lượng do trình duyệt đo (Java không giải mã được video). Người cố sửa số vẫn bị
            // chặn bởi giới hạn dung lượng ở trên, và trình phát ở giao diện tự quay về giây 0
            // sau 5 giây — người khác không bao giờ xem quá 5 giây dù tệp có dài hơn.
            if (durationMs == null || durationMs <= 0) {
                throw new CommonException("Thiếu thời lượng video");
            }
            if (durationMs > maxVideoSeconds * 1000 + 500) {
                throw new CommonException("Video ảnh đại diện tối đa " + maxVideoSeconds + " giây");
            }
            if (poster == null || poster.isEmpty() || !IMAGE_TYPES.contains(mimeOf(poster))) {
                throw new CommonException("Thiếu khung hình đầu của video");
            }
            if (poster.getSize() > maxImageMb * 1024L * 1024L) {
                throw new CommonException("Khung hình đầu nặng quá " + maxImageMb + "MB");
            }
        } else if (file.getSize() > maxImageMb * 1024L * 1024L) {
            throw new CommonException("Ảnh đại diện nặng quá " + maxImageMb + "MB");
        }

        User user = requireUser(userId);
        List<FileAttachment> cu = filesOf(user);

        FileAttachment anh = luuTep(video ? poster : file, userId);
        user.setAvatarFile(anh);
        user.setAvatarVideoFile(video ? luuTep(file, userId) : null);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.saveAndFlush(user);

        xoaTep(cu);
        return toResponse(user, true);
    }

    @Override
    @Transactional
    public ProfileResponse removeAvatar(UUID userId) {
        User user = requireUser(userId);
        List<FileAttachment> cu = filesOf(user);
        user.setAvatarFile(null);
        user.setAvatarVideoFile(null);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.saveAndFlush(user);
        xoaTep(cu);
        return toResponse(user, true);
    }

    // ==================== riêng ====================

    private ProfileResponse toResponse(User u, boolean isMe) {
        LearnerStatsResponse s = learnerStatsService.getStats(u.getId());
        return ProfileResponse.builder()
                .userId(u.getId())
                .fullName(u.getFullName())
                .bio(u.getBio())
                .avatarUrl(u.getAvatarFile() == null ? null : u.getAvatarFile().getPublicUrl())
                .avatarVideoUrl(u.getAvatarVideoFile() == null ? null : u.getAvatarVideoFile().getPublicUrl())
                .accountKind(u.getAccountKind())
                .verifiedVslRole(u.getVslRoleStatus() == VslRoleStatus.VERIFIED ? u.getVslRole() : null)
                .joinedAt(u.getCreatedAt())
                .isMe(isMe)
                .stats(ProfileResponse.Stats.builder()
                        .posts(forumPostRepository.countByAuthor_IdAndStatus(u.getId(), ForumPostStatus.PUBLISHED))
                        .comments(forumCommentRepository.countByAuthor_IdAndStatus(u.getId(), ForumCommentStatus.PUBLISHED))
                        .reactionsReceived(forumPostRepository.sumReactionsByAuthor(u.getId(), ForumPostStatus.PUBLISHED))
                        .streakDays(nz(s.getStreakDays()))
                        .longestStreakDays(nz(s.getLongestStreakDays()))
                        .stars(nz(s.getStars()))
                        .level(nz(s.getLevel()))
                        .build())
                .build();
    }

    private User visibleUser(UUID userId, UUID viewerId) {
        User user = requireUser(userId);
        // Tài khoản bị khoá/vô hiệu hoá không còn trang cá nhân công khai
        if (!userId.equals(viewerId)
                && (user.getStatus() == UserStatus.BANNED || user.getStatus() == UserStatus.DISABLED)) {
            throw new NotFoundException("Không tìm thấy người dùng");
        }
        return user;
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
    }

    private static List<FileAttachment> filesOf(User u) {
        List<FileAttachment> ds = new ArrayList<>();
        for (FileAttachment f : new FileAttachment[]{u.getAvatarFile(), u.getAvatarVideoFile()}) {
            if (f != null && ds.stream().noneMatch(x -> Objects.equals(x.getId(), f.getId()))) {
                ds.add(f);
            }
        }
        return ds;
    }

    private FileAttachment luuTep(MultipartFile file, UUID userId) {
        String ten = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "avatar" : file.getOriginalFilename().replaceAll("[^A-Za-z0-9._-]", "_");
        String objectName = "avatars/" + userId + "/" + UUID.randomUUID() + "_" + ten;
        try {
            minioService.upload(file, objectName);
        } catch (Exception e) {
            throw new CommonException("Tải ảnh đại diện lên MinIO thất bại: " + e.getMessage(), e);
        }
        int dot = ten.lastIndexOf('.');
        return fileRepository.save(FileAttachment.builder()
                .bucket(minioService.getBucketName())
                .objectKey(objectName)
                .originalName(ten)
                .mimeType(mimeOf(file))
                .extension(dot < 0 || dot == ten.length() - 1 ? null : ten.substring(dot + 1).toLowerCase())
                .sizeBytes(file.getSize())
                .entityType(EntityType.USER)
                .entityId(userId)
                .uploadedBy(userId)
                .build());
    }

    /** Gỡ ảnh cũ SAU khi users đã trỏ sang ảnh mới — xoá trước là vướng khoá ngoại */
    private void xoaTep(List<FileAttachment> tep) {
        for (FileAttachment f : tep) {
            try {
                minioService.delete(f.getBucket(), f.getObjectKey());
            } catch (Exception e) {
                // Không chặn việc đổi ảnh nếu MinIO lỗi — ghi log để dọn sau
                log.warn("Không xoá được ảnh đại diện cũ {}: {}", f.getObjectKey(), e.getMessage());
            }
        }
        fileRepository.deleteAll(tep);
    }

    private static String mimeOf(MultipartFile f) {
        String m = f.getContentType() == null ? "" : f.getContentType().toLowerCase();
        int semi = m.indexOf(';');           // "video/webm;codecs=vp9" → "video/webm"
        return semi < 0 ? m.trim() : m.substring(0, semi).trim();
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
