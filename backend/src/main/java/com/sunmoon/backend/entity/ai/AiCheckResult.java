package com.sunmoon.backend.entity.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sunmoon.backend.constant.enums.AiCheckContext;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// Giai doan dau chay o MUC A: chi luyen tap, KHONG tinh diem. FastAPI la
// service stateless - no chi tra score; Spring Boot moi la noi so nguong
// va quyet dinh passed, roi ghi vao tien do hoc.
//
// consentToStore mac dinh FALSE: du lieu chuyen dong cua nguoi khiem thinh
// la du lieu sinh trac. Chi luu landmark khi nguoi dung chu dong dong y.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "ai_check_results")
public class AiCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_id", nullable = false)
    private Sign sign;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'COMMON'")
    @Column(name = "region", nullable = false, length = 20)
    @Builder.Default
    private Region region = Region.COMMON;

    @NotNull
    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "passed", nullable = false)
    @Builder.Default
    private Boolean passed = false;

    @Column(name = "dtw_distance", precision = 8, scale = 5)
    private BigDecimal dtwDistance;

    @Column(name = "threshold_used", precision = 6, scale = 4)
    private BigDecimal thresholdUsed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_exemplar_id")
    private SignExemplar matchedExemplar;

    // Diem theo tung tham so ngon ngu ky hieu - day moi la thu nguoi hoc
    // sua duoc. Mot con so tong "72/100" khong cho biet phai sua gi.
    @Column(name = "score_handshape", precision = 5, scale = 2)
    private BigDecimal scoreHandshape;

    @Column(name = "score_location", precision = 5, scale = 2)
    private BigDecimal scoreLocation;

    @Column(name = "score_movement", precision = 5, scale = 2)
    private BigDecimal scoreMovement;

    // Ma goi y, vi du ["HAND_SHAPE_OK","LOCATION_TOO_LOW"]. Luu MA chu khong
    // luu cau tieng Viet: doi cach dien dat hoac them ngon ngu thi khong
    // phai deploy lai service Python.
    @NotNull
    @Type(JsonType.class)
    @ColumnDefault("'[]'")
    @Column(name = "hint_codes", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode hintCodes = JsonNodeFactory.instance.arrayNode();

    @Column(name = "tracking_quality", precision = 4, scale = 3)
    private BigDecimal trackingQuality;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'verify-dtw-v1'")
    @Column(name = "model_version", nullable = false, length = 50)
    @Builder.Default
    private String modelVersion = "verify-dtw-v1";

    @Column(name = "processing_ms")
    private Integer processingMs;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PRACTICE'")
    @Column(name = "context", nullable = false, length = 20)
    @Builder.Default
    private AiCheckContext context = AiCheckContext.PRACTICE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landmark_file_id")
    private FileAttachment landmarkFile;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "consent_to_store", nullable = false)
    @Builder.Default
    private Boolean consentToStore = false;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "checked_at", nullable = false)
    @Builder.Default
    private OffsetDateTime checkedAt = OffsetDateTime.now();

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
