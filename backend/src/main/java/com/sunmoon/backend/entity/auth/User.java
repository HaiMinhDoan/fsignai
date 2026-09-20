package com.sunmoon.backend.entity.auth;

import com.sunmoon.backend.constant.enums.*;
import com.sunmoon.backend.entity.FileAttachment;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Email
    @Size(max = 255)
    @NotNull
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    // Cột sinh ở DB (lower(email)) - chỉ đọc, không set từ tầng Java
    @Column(name = "email_normalized", insertable = false, updatable = false)
    private String emailNormalized;

    // NULL nếu người dùng chỉ đăng nhập bằng Google
    @Size(max = 255)
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Size(max = 255)
    @Column(name = "google_id", unique = true, length = 255)
    private String googleId;

    @Size(max = 150)
    @NotNull
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_file_id")
    private FileAttachment avatarFile;

    @Size(max = 255)
    @Column(name = "address", length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_range", length = 20)
    private AgeRange ageRange;

    // Chọn lúc đăng ký: học sinh (CHILD) / phụ huynh (PARENT) / giáo viên (TEACHER).
    // ADULT là giá trị dự phòng của cột, không có lựa chọn nào ở form đăng ký trỏ tới nó.
    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ADULT'")
    @Column(name = "account_kind", nullable = false, length = 20)
    @Builder.Default
    private AccountKind accountKind = AccountKind.ADULT;

    // Quan hệ với cộng đồng khiếm thính - câu hỏi bắt buộc lúc đăng ký
    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'OTHER'")
    @Column(name = "user_type", nullable = false, length = 30)
    @Builder.Default
    private UserType userType = UserType.OTHER;

    // Vùng miền dùng để chọn video ký hiệu phù hợp
    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'COMMON'")
    @Column(name = "region", nullable = false, length = 20)
    @Builder.Default
    private Region region = Region.COMMON;

    // Chuyên môn VSL tự khai: quyết định TRỌNG SỐ góp ý hiệu chỉnh ngưỡng chấm điểm.
    // Khác hoàn toàn với roles/permissions (quyền vào trang quản trị).
    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'LEARNER'")
    @Column(name = "vsl_role", nullable = false, length = 30)
    @Builder.Default
    private VslRole vslRole = VslRole.LEARNER;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'SELF_DECLARED'")
    @Column(name = "vsl_role_status", nullable = false, length = 30)
    @Builder.Default
    private VslRoleStatus vslRoleStatus = VslRoleStatus.SELF_DECLARED;

    // Người dùng tự khai: trường/trung tâm, số chứng chỉ...
    @Column(name = "vsl_role_evidence", length = Integer.MAX_VALUE)
    private String vslRoleEvidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vsl_role_verified_by")
    private User vslRoleVerifiedBy;

    @Column(name = "vsl_role_verified_at")
    private OffsetDateTime vslRoleVerifiedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "banned_until")
    private OffsetDateTime bannedUntil;

    @Column(name = "ban_reason", length = Integer.MAX_VALUE)
    private String banReason;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

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
