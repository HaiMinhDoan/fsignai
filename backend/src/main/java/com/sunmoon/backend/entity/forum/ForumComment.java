package com.sunmoon.backend.entity.forum;

import com.sunmoon.backend.constant.enums.ForumCommentStatus;
import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

// Tra loi long toi da 2 cap
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "forum_comments")
public class ForumComment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private ForumPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ForumComment parent;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "depth", nullable = false)
    @Builder.Default
    private Short depth = 0;

    // Co the rong neu binh luan chi co video
    @Column(name = "body_text", length = Integer.MAX_VALUE)
    private String bodyText;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "reaction_count", nullable = false)
    @Builder.Default
    private Integer reactionCount = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING_REVIEW'")
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ForumCommentStatus status = ForumCommentStatus.PENDING_REVIEW;

    @Column(name = "edited_at")
    private OffsetDateTime editedAt;

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
}
