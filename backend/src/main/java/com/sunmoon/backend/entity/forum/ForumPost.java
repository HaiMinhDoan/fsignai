package com.sunmoon.backend.entity.forum;

import com.sunmoon.backend.constant.enums.ForumPostStatus;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.dictionary.Sign;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "forum_posts")
public class ForumPost {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ForumCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // Co the rong: tieu de duoc phep la video ky hieu, hoac khong co gi ca
    @Size(max = 255)
    @Column(name = "title_vi", length = 255)
    private String titleVi;

    // Video ky hieu dung LAM TIEU DE - hien ngoai danh sach thay cho dong chu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "title_media_id")
    private MediaAsset titleMedia;

    // Co the rong neu bai chi co video
    @Column(name = "body_md", length = Integer.MAX_VALUE)
    private String bodyMd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_id")
    private Sign sign;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "comment_count", nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "reaction_count", nullable = false)
    @Builder.Default
    private Integer reactionCount = 0;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "last_activity_at", nullable = false)
    @Builder.Default
    private OffsetDateTime lastActivityAt = OffsetDateTime.now();

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING_REVIEW'")
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ForumPostStatus status = ForumPostStatus.PENDING_REVIEW;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Cot sinh o DB (tsvector) - chi doc, dung cho full-text search
    @Column(name = "search_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String searchVector;
}
