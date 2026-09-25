package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.EntityType;
import com.sunmoon.backend.constant.enums.MediaKind;
import com.sunmoon.backend.constant.enums.MediaSource;
import com.sunmoon.backend.constant.enums.ModerationStatus;
import com.sunmoon.backend.constant.enums.TranscodeStatus;
import com.sunmoon.backend.dto.response.forum.MediaResponse;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.forum.ForumComment;
import com.sunmoon.backend.entity.forum.ForumCommentMedia;
import com.sunmoon.backend.entity.forum.ForumCommentMediaId;
import com.sunmoon.backend.entity.forum.ForumPost;
import com.sunmoon.backend.entity.forum.ForumPostMedia;
import com.sunmoon.backend.entity.forum.ForumPostMediaId;
import com.sunmoon.backend.entity.forum.MediaAsset;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.FileAttachmentRepository;
import com.sunmoon.backend.repository.ForumCommentMediaRepository;
import com.sunmoon.backend.repository.ForumPostMediaRepository;
import com.sunmoon.backend.repository.MediaAssetRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.ForumMediaService;
import com.sunmoon.backend.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForumMediaServiceImpl implements ForumMediaService {

    private final MediaAssetRepository mediaRepository;
    private final ForumPostMediaRepository postMediaRepository;
    private final ForumCommentMediaRepository commentMediaRepository;
    private final FileAttachmentRepository fileRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;

    @Value("${forum.media.max-video-seconds:60}")
    private int maxVideoSeconds;

    @Value("${forum.media.max-video-mb:60}")
    private int maxVideoMb;

    @Value("${forum.media.max-image-mb:10}")
    private int maxImageMb;

    @Value("${forum.media.max-per-post:6}")
    private int maxPerPost;

    @Override
    @Transactional
    public MediaResponse upload(UUID ownerId, MultipartFile file, MultipartFile poster,
                                MediaSource source, Integer durationMs, Integer width, Integer height) {
        if (file == null || file.isEmpty()) {
            throw new CommonException("Chưa chọn tệp nào");
        }
        String mime = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        MediaKind kind = mime.startsWith("video/") ? MediaKind.VIDEO
                : mime.startsWith("image/") ? MediaKind.IMAGE : null;
        if (kind == null) {
            throw new CommonException("Chỉ nhận video hoặc ảnh");
        }

        long gioiHan = (kind == MediaKind.VIDEO ? maxVideoMb : maxImageMb) * 1024L * 1024L;
        if (file.getSize() > gioiHan) {
            throw new CommonException(kind == MediaKind.VIDEO
                    ? "Video nặng quá " + maxVideoMb + "MB. Quay ngắn lại hoặc chọn tệp nhẹ hơn nhé."
                    : "Ảnh nặng quá " + maxImageMb + "MB.");
        }
        // Thời lượng do TRÌNH DUYỆT đo rồi gửi lên: Java không giải mã được video.
        // Người cố tình sửa số vẫn bị chặn bởi giới hạn dung lượng ở trên.
        if (kind == MediaKind.VIDEO && durationMs != null && durationMs > maxVideoSeconds * 1000 + 1500) {
            throw new CommonException("Video dài quá " + maxVideoSeconds + " giây. Quay ngắn lại nhé.");
        }

        FileAttachment goc = luuTep(file, ownerId, kind == MediaKind.VIDEO ? "videos" : "images");
        FileAttachment anh = null;
        if (poster != null && !poster.isEmpty()) {
            anh = luuTep(poster, ownerId, "posters");
        } else if (kind == MediaKind.IMAGE) {
            anh = goc;      // ảnh thì chính nó là ảnh đại diện
        }

        MediaAsset media = MediaAsset.builder()
                .owner(userRepository.getReferenceById(ownerId))
                .kind(kind)
                .source(source == null ? MediaSource.FILE_UPLOAD : source)
                .originalFile(goc)
                .playbackFile(goc)          // chưa chuyển mã: phát thẳng tệp gốc
                .thumbnailFile(anh)
                .durationMs(durationMs != null && durationMs > 0 ? durationMs : null)
                .width(width)
                .height(height)
                .sizeBytes(file.getSize())
                .mimeType(mime)
                .transcodeStatus(TranscodeStatus.READY)
                // Diễn đàn kiểm duyệt HẬU KIỂM: bài hiện ngay, chỉ gỡ khi bị báo cáo.
                // Để PENDING ở đây là media không hiện được trong chính bài vừa đăng.
                .moderationStatus(ModerationStatus.APPROVED)
                .captionVi("")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        media = mediaRepository.save(media);
        ganChuSoHuu(goc, media.getId());
        if (anh != null && anh != goc) {
            ganChuSoHuu(anh, media.getId());
        }
        return toResponse(media);
    }

    @Override
    @Transactional
    public void deleteOwnUnattached(UUID ownerId, UUID mediaId) {
        MediaAsset media = laMediaCuaMinh(ownerId, mediaId);
        if (postMediaRepository.existsByMediaId(mediaId) || commentMediaRepository.existsByMediaId(mediaId)) {
            throw new CommonException("Tệp này đã gắn vào một bài viết, sửa bài để gỡ nhé");
        }
        xoaHan(List.of(media));
    }

    @Override
    @Transactional
    public void attachToPost(ForumPost post, UUID ownerId, List<UUID> mediaIds) {
        List<MediaAsset> moi = kiemTraDanhSach(ownerId, mediaIds);

        // Media bị bỏ ra khỏi bài khi sửa: xoá hẳn, vì nó là tệp riêng của bài này
        List<MediaAsset> cu = postMediaRepository.findAllByPostIdOrderByDisplayOrderAsc(post.getId())
                .stream().map(ForumPostMedia::getMedia).toList();
        postMediaRepository.deleteAllByPostId(post.getId());
        postMediaRepository.flush();

        for (int i = 0; i < moi.size(); i++) {
            postMediaRepository.save(ForumPostMedia.builder()
                    .id(new ForumPostMediaId(post.getId(), moi.get(i).getId()))
                    .post(post)
                    .media(moi.get(i))
                    .displayOrder(i)
                    .build());
        }
        xoaHan(cu.stream().filter(m -> moi.stream().noneMatch(n -> n.getId().equals(m.getId()))).toList());
    }

    @Override
    @Transactional
    public void attachToComment(ForumComment comment, UUID ownerId, List<UUID> mediaIds) {
        List<MediaAsset> moi = kiemTraDanhSach(ownerId, mediaIds);
        commentMediaRepository.deleteAllByCommentId(comment.getId());
        commentMediaRepository.flush();
        for (int i = 0; i < moi.size(); i++) {
            commentMediaRepository.save(ForumCommentMedia.builder()
                    .id(new ForumCommentMediaId(comment.getId(), moi.get(i).getId()))
                    .comment(comment)
                    .media(moi.get(i))
                    .displayOrder(i)
                    .build());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MediaAsset resolveTitleMedia(UUID ownerId, UUID mediaId) {
        if (mediaId == null) {
            return null;
        }
        MediaAsset media = laMediaCuaMinh(ownerId, mediaId);
        if (media.getKind() != MediaKind.VIDEO) {
            throw new CommonException("Tiêu đề bằng hình chỉ nhận video ký hiệu");
        }
        return media;
    }

    @Override
    @Transactional
    public void deleteAllOfPost(ForumPost post) {
        List<MediaAsset> tatCa = new ArrayList<>(postMediaRepository
                .findAllByPostIdOrderByDisplayOrderAsc(post.getId())
                .stream().map(ForumPostMedia::getMedia).toList());
        if (post.getTitleMedia() != null) {
            tatCa.add(post.getTitleMedia());
        }
        postMediaRepository.deleteAllByPostId(post.getId());
        postMediaRepository.flush();
        xoaHan(tatCa);
    }

    @Override
    @Transactional
    public void deleteAllOfComment(ForumComment comment) {
        List<MediaAsset> tatCa = commentMediaRepository
                .findAllByCommentIdOrderByDisplayOrderAsc(comment.getId())
                .stream().map(ForumCommentMedia::getMedia).toList();
        commentMediaRepository.deleteAllByCommentId(comment.getId());
        commentMediaRepository.flush();
        xoaHan(tatCa);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, List<MediaResponse>> ofPosts(List<UUID> postIds) {
        return gomTheoChuSoHuu(postIds.isEmpty() ? List.of() : mediaRepository.findAllOfPosts(postIds));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, List<MediaResponse>> ofComments(List<UUID> commentIds) {
        return gomTheoChuSoHuu(commentIds.isEmpty() ? List.of() : mediaRepository.findAllOfComments(commentIds));
    }

    @Override
    public MediaResponse toResponse(MediaAsset m) {
        if (m == null) {
            return null;
        }
        FileAttachment phat = m.getPlaybackFile() != null ? m.getPlaybackFile() : m.getOriginalFile();
        return MediaResponse.builder()
                .id(m.getId())
                .kind(m.getKind())
                .source(m.getSource())
                .url(phat == null ? null : phat.getPublicUrl())
                .thumbnailUrl(m.getThumbnailFile() == null ? null : m.getThumbnailFile().getPublicUrl())
                .durationMs(m.getDurationMs())
                .width(m.getWidth())
                .height(m.getHeight())
                .sizeBytes(m.getSizeBytes())
                .mimeType(m.getMimeType())
                .build();
    }

    // ==================== riêng ====================

    private Map<UUID, List<MediaResponse>> gomTheoChuSoHuu(List<Object[]> hang) {
        Map<UUID, List<MediaResponse>> theo = new LinkedHashMap<>();
        for (Object[] r : hang) {
            theo.computeIfAbsent((UUID) r[0], k -> new ArrayList<>()).add(toResponse((MediaAsset) r[1]));
        }
        return theo;
    }

    private List<MediaAsset> kiemTraDanhSach(UUID ownerId, List<UUID> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return List.of();
        }
        if (mediaIds.size() > maxPerPost) {
            throw new CommonException("Mỗi bài tối đa " + maxPerPost + " tệp");
        }
        Map<UUID, MediaAsset> daCo = new HashMap<>();
        List<MediaAsset> ket = new ArrayList<>(mediaIds.size());
        for (UUID id : mediaIds) {
            if (daCo.containsKey(id)) {
                continue;               // gửi trùng id thì lấy một lần
            }
            MediaAsset m = laMediaCuaMinh(ownerId, id);
            daCo.put(id, m);
            ket.add(m);
        }
        return ket;
    }

    private MediaAsset laMediaCuaMinh(UUID ownerId, UUID mediaId) {
        MediaAsset media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tệp đã tải lên"));
        if (!media.getOwner().getId().equals(ownerId)) {
            // Cùng thông điệp với "không tồn tại": không tiết lộ là có tệp này
            throw new NotFoundException("Không tìm thấy tệp đã tải lên");
        }
        return media;
    }

    private FileAttachment luuTep(MultipartFile file, UUID ownerId, String thuMuc) {
        String ten = file.getOriginalFilename() == null ? "tep" : file.getOriginalFilename();
        String objectName = "forum/" + ownerId + "/" + thuMuc + "/" + UUID.randomUUID() + "_" + ten;
        try {
            minioService.upload(file, objectName);
        } catch (Exception e) {
            throw new CommonException("Tải tệp lên MinIO thất bại: " + e.getMessage(), e);
        }
        return fileRepository.save(FileAttachment.builder()
                .bucket(minioService.getBucketName())
                .objectKey(objectName)
                .originalName(ten)
                .mimeType(file.getContentType())
                .extension(duoiTep(ten))
                .sizeBytes(file.getSize())
                .entityType(EntityType.MEDIA_ASSET)
                .build());
    }

    private void ganChuSoHuu(FileAttachment file, UUID mediaId) {
        file.setEntityId(mediaId);
        fileRepository.save(file);
    }

    /**
     * Xoá hẳn: tệp trên MinIO, dòng file_attachments, rồi dòng media_assets.
     *
     * Đúng thứ tự đó vì thumbnail_file_id / playback_file_id là khoá ngoại; xoá
     * file_attachments trước khi gỡ media_assets sẽ chỉ làm chúng thành NULL và
     * để lại một dòng media trỏ vào hư không.
     */
    private void xoaHan(Collection<MediaAsset> danhSach) {
        if (danhSach == null || danhSach.isEmpty()) {
            return;
        }
        List<FileAttachment> tep = new ArrayList<>();
        for (MediaAsset m : danhSach) {
            for (FileAttachment f : List.of(m.getOriginalFile(), m.getPlaybackFile(), m.getThumbnailFile())
                    .stream().filter(java.util.Objects::nonNull).toList()) {
                if (tep.stream().noneMatch(x -> x.getId().equals(f.getId()))) {
                    tep.add(f);
                }
            }
        }
        mediaRepository.deleteAll(danhSach);
        mediaRepository.flush();

        for (FileAttachment f : tep) {
            try {
                minioService.delete(f.getBucket(), f.getObjectKey());
            } catch (Exception e) {
                // Không chặn thao tác chính nếu MinIO lỗi - ghi log để dọn sau
                log.warn("Không xoá được tệp diễn đàn {}: {}", f.getObjectKey(), e.getMessage());
            }
        }
        fileRepository.deleteAll(tep);
    }

    private static String duoiTep(String ten) {
        int i = ten.lastIndexOf('.');
        return i < 0 || i == ten.length() - 1 ? null : ten.substring(i + 1).toLowerCase();
    }
}
