package com.sunmoon.backend.entity.notification;

import com.sunmoon.backend.constant.enums.BlogCategory;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

// "blog info VSL", "our mission" o trang chu
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "blog_posts")
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 160)
    @NotNull
    @Column(name = "slug", nullable = false, unique = true, length = 160)
    private String slug;

    @Size(max = 255)
    @NotNull
    @Column(name = "title_vi", nullable = false, length = 255)
    private String titleVi;

    @Column(name = "excerpt_vi", length = Integer.MAX_VALUE)
    private String excerptVi;

    @NotNull
    @Column(name = "content_md", nullable = false, length = Integer.MAX_VALUE)
    private String contentMd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_file_id")
    private FileAttachment coverFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'BLOG'")
    @Column(name = "category", nullable = false, length = 30)
    @Builder.Default
    private BlogCategory category = BlogCategory.BLOG;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

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

    // Cot sinh o DB (tsvector) - chi doc
    @Column(name = "search_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String searchVector;
}
