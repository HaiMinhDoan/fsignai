package com.sunmoon.backend.service;

import com.sunmoon.backend.constant.enums.MediaSource;
import com.sunmoon.backend.dto.response.forum.MediaResponse;
import com.sunmoon.backend.entity.forum.ForumComment;
import com.sunmoon.backend.entity.forum.ForumPost;
import com.sunmoon.backend.entity.forum.MediaAsset;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Video ký hiệu và ảnh do người dùng tải lên diễn đàn.
 *
 * Tách khỏi ForumPostService vì vòng đời khác hẳn: người dùng quay/chọn tệp TRƯỚC,
 * xem lại, quay lại nếu chưa ưng, rồi mới bấm Đăng. Tệp phải tồn tại trên máy chủ
 * từ lúc quay xong, chứ không phải lúc đăng bài.
 */
public interface ForumMediaService {

    /**
     * Nhận một tệp người dùng vừa quay hoặc vừa chọn.
     *
     * @param poster ảnh đại diện do TRÌNH DUYỆT cắt từ video (máy chủ Java không giải
     *               mã được video). Không có thì video hiện khung đen cho tới khi bấm phát.
     */
    MediaResponse upload(UUID ownerId, MultipartFile file, MultipartFile poster,
                         MediaSource source, Integer durationMs, Integer width, Integer height);

    /** Bỏ một tệp vừa tải lên nhưng chưa gắn vào bài nào (người dùng bấm xoá trong ô soạn) */
    void deleteOwnUnattached(UUID ownerId, UUID mediaId);

    /** Gắn danh sách media vào bài, đúng thứ tự truyền vào; thay thế toàn bộ danh sách cũ */
    void attachToPost(ForumPost post, UUID ownerId, List<UUID> mediaIds);

    void attachToComment(ForumComment comment, UUID ownerId, List<UUID> mediaIds);

    /** Media dùng làm tiêu đề; null để gỡ */
    MediaAsset resolveTitleMedia(UUID ownerId, UUID mediaId);

    /** Xoá hẳn media của một bài (kể cả tệp trên MinIO) - gọi khi bài bị gỡ */
    void deleteAllOfPost(ForumPost post);

    void deleteAllOfComment(ForumComment comment);

    Map<UUID, List<MediaResponse>> ofPosts(List<UUID> postIds);

    Map<UUID, List<MediaResponse>> ofComments(List<UUID> commentIds);

    MediaResponse toResponse(MediaAsset media);
}
