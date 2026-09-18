package com.sunmoon.backend.entity.forum;

import com.sunmoon.backend.constant.enums.MediaKind;
import com.sunmoon.backend.constant.enums.MediaSource;
import com.sunmoon.backend.constant.enums.ModerationStatus;
import com.sunmoon.backend.constant.enums.TranscodeStatus;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

// Video/anh do NGUOI DUNG tai len. Tach han khoi SignVideo (noi dung bien
// tap) vi vong doi khac nhau: media cua nguoi dung can transcode, kiem
// duyet va co the bi go. File that van nam o FileAttachment.
//
// Binh luan video khong phai tinh nang phu. Voi nhieu nguoi diec, tieng
// Viet viet la ngon ngu thu hai - bat ho go chu de tham gia cong dong
// chinh la dung lai dung rao can ma san pham nay ton tai de go.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "media_assets")
public class MediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'VIDEO'")
    @Column(name = "kind", nullable = false, length = 20)
    @Builder.Default
    private MediaKind kind = MediaKind.VIDEO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'FILE_UPLOAD'")
    @Column(name = "source", nullable = false, length = 20)
    @Builder.Default
    private MediaSource source = MediaSource.FILE_UPLOAD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_file_id")
    private FileAttachment originalFile;

    // Ban da transcode ve H.264 720p de phat duoc tren moi trinh duyet
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playback_file_id")
    private FileAttachment playbackFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_file_id")
    private FileAttachment thumbnailFile;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Size(max = 100)
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    @Column(name = "transcode_status", nullable = false, length = 20)
    @Builder.Default
    private TranscodeStatus transcodeStatus = TranscodeStatus.PENDING;

    @Column(name = "transcode_error", length = Integer.MAX_VALUE)
    private String transcodeError;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    @Column(name = "moderation_status", nullable = false, length = 20)
    @Builder.Default
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by")
    private User moderatedBy;

    @Column(name = "moderated_at")
    private OffsetDateTime moderatedAt;

    @Column(name = "moderation_note", length = Integer.MAX_VALUE)
    private String moderationNote;

    // Phu de BAT BUOC khi dang video: khong co mo ta chu thi nguoi khiem
    // thi, nguoi chua biet ky hieu do, va ca cong cu tim kiem deu khong doc
    // duoc. API tu choi gan media VIDEO vao bai/binh luan neu con rong.
    @NotNull
    @ColumnDefault("''")
    @Column(name = "caption_vi", nullable = false, length = Integer.MAX_VALUE)
    @Builder.Default
    private String captionVi = "";

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
