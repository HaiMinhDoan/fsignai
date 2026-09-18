package com.sunmoon.backend.entity.dictionary;

import com.sunmoon.backend.constant.enums.*;
import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

// Một mục từ vựng VSL - trung tâm của toàn hệ thống. Bài học, flashcard,
// quiz, AI checking đều trỏ về bảng này.
//
// Bốn trục phân loại độc lập, phục vụ bốn việc khác nhau:
//   unitType  -> lọc bài học (bài đánh vần chỉ lấy LETTER)
//   wordType  -> từ loại tiếng Việt, dạy ngữ pháp và lọc bài tập
//   domain    -> gom khoá chuyên ngành (VSL y tế, VSL trường học)
//   SignTopic -> chủ đề ngữ nghĩa, là điều hướng chính của người dùng
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "signs")
public class Sign {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    // Mã định danh: 'ME', 'CHA', 'GIA_DINH'
    @Size(max = 150)
    @NotNull
    @Column(name = "gloss", nullable = false, unique = true, length = 150)
    private String gloss;

    @Size(max = 255)
    @NotNull
    @Column(name = "word_vi", nullable = false, length = 255)
    private String wordVi;

    @Size(max = 255)
    @Column(name = "word_en", length = 255)
    private String wordEn;

    // Mô tả cách làm ký hiệu bằng chữ - bắt buộc cho a11y (người khiếm thị, SEO)
    @Column(name = "description_vi", length = Integer.MAX_VALUE)
    private String descriptionVi;

    @Column(name = "note_vi", length = Integer.MAX_VALUE)
    private String noteVi;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'BEGINNER'")
    @Column(name = "level", nullable = false, length = 20)
    @Builder.Default
    private SignLevel level = SignLevel.BEGINNER;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'WORD'")
    @Column(name = "unit_type", nullable = false, length = 20)
    @Builder.Default
    private UnitType unitType = UnitType.WORD;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'KHONG_XAC_DINH'")
    @Column(name = "word_type", nullable = false, length = 30)
    @Builder.Default
    private WordType wordType = WordType.KHONG_XAC_DINH;

    @Enumerated(EnumType.STRING)
    @Column(name = "word_subtype", length = 30)
    private WordSubtype wordSubtype;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain", length = 30)
    private SignDomain domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_topic_id")
    private Topic primaryTopic;

    // Số tay dùng khi thực hiện ký hiệu; dùng để chọn ngưỡng DTW mặc định
    @NotNull
    @ColumnDefault("1")
    @Column(name = "hand_count", nullable = false)
    @Builder.Default
    private Short handCount = 1;

    // Nguồn gốc dữ liệu, phục vụ ghi công và truy vết bản quyền
    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'MANUAL'")
    @Column(name = "source", nullable = false, length = 30)
    @Builder.Default
    private SignSource source = SignSource.MANUAL;

    // Mã video gốc, ví dụ 'W00665'
    @Size(max = 255)
    @Column(name = "source_ref", length = 255)
    private String sourceRef;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'UNREVIEWED'")
    @Column(name = "review_status", nullable = false, length = 30)
    @Builder.Default
    private ReviewStatus reviewStatus = ReviewStatus.UNREVIEWED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

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

    // Cột sinh ở DB (lower(unaccent(word_vi))) - chỉ đọc, dùng để search không dấu
    @Column(name = "word_vi_unaccent", insertable = false, updatable = false)
    private String wordViUnaccent;

    // Cột sinh ở DB (tsvector) - chỉ đọc, dùng cho full-text search
    @Column(name = "search_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String searchVector;
}
