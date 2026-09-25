package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.forum.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    /** Media của một bài, đúng thứ tự người đăng sắp */
    @Query("""
            SELECT m FROM MediaAsset m
            WHERE m.id IN (SELECT pm.media.id FROM ForumPostMedia pm WHERE pm.post.id = :postId)
            ORDER BY (SELECT pm2.displayOrder FROM ForumPostMedia pm2
                      WHERE pm2.post.id = :postId AND pm2.media.id = m.id)
            """)
    List<MediaAsset> findAllOfPost(@Param("postId") UUID postId);

    /** Media của nhiều bài cùng lúc - danh sách bài đọc một lần, không hỏi từng bài */
    @Query("""
            SELECT pm.post.id, m FROM ForumPostMedia pm JOIN pm.media m
            WHERE pm.post.id IN :postIds
            ORDER BY pm.displayOrder
            """)
    List<Object[]> findAllOfPosts(@Param("postIds") List<UUID> postIds);

    @Query("""
            SELECT cm.comment.id, m FROM ForumCommentMedia cm JOIN cm.media m
            WHERE cm.comment.id IN :commentIds
            ORDER BY cm.displayOrder
            """)
    List<Object[]> findAllOfComments(@Param("commentIds") List<UUID> commentIds);

    /**
     * Media đã tải lên nhưng chưa gắn vào bài, bình luận hay tiêu đề nào.
     *
     * Người dùng quay xong rồi bấm huỷ là sinh ra loại này. Không quét dọn thì
     * mỗi lần đổi ý là bỏ lại một tệp video nằm trên MinIO vĩnh viễn.
     */
    @Query("""
            SELECT m FROM MediaAsset m
            WHERE m.createdAt < :truoc
              AND NOT EXISTS (SELECT 1 FROM ForumPostMedia pm WHERE pm.media.id = m.id)
              AND NOT EXISTS (SELECT 1 FROM ForumCommentMedia cm WHERE cm.media.id = m.id)
              AND NOT EXISTS (SELECT 1 FROM ForumPost p WHERE p.titleMedia.id = m.id)
            """)
    List<MediaAsset> findMoCoi(@Param("truoc") java.time.OffsetDateTime truoc);
}
