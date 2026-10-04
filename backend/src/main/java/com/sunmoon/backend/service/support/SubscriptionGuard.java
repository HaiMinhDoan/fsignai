package com.sunmoon.backend.service.support;

import com.sunmoon.backend.configs.PaymentProperties;
import com.sunmoon.backend.constant.context.SecurityContextHolder;
import com.sunmoon.backend.constant.enums.RoleType;
import com.sunmoon.backend.dto.AuthInfo;
import com.sunmoon.backend.dto.response.payment.PlanResponse;
import com.sunmoon.backend.dto.response.payment.SubscriptionResponse;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gói Free / Premium.
 *
 * Free:    khoá học đầy đủ, ôn tập từ vựng, góc trò chơi, diễn đàn.
 * Premium: thêm bài kiểm tra kiến thức, thư viện cử chỉ, AI chấm điểm cử chỉ.
 *
 * Chốt ở MÁY CHỦ cho bài kiểm tra và AI chấm điểm (hai thứ tốn tài nguyên và là giá trị chính
 * của gói). Thư viện cử chỉ chốt ở giao diện: các API tra từ còn được chính các tính năng miễn
 * phí (trò chơi, gói từ, trang chủ) dùng lại, khoá ở máy chủ là làm hỏng luôn tính năng Free.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionGuard {

    public static final String FREE = "FREE";
    public static final String PREMIUM = "PREMIUM";

    /** Nhân sự nội bộ dùng mọi tính năng để soạn và kiểm tra nội dung, không phải mua gói */
    private static final Set<String> STAFF = Set.of(
            RoleType.SYSTEM_ADMIN, RoleType.CONTENT_EDITOR, RoleType.MODERATOR, RoleType.VSL_REVIEWER);

    private final UserRepository userRepository;
    private final PaymentProperties props;

    public boolean isStaff(AuthInfo auth) {
        return auth != null && auth.getRoles() != null && auth.getRoles().stream().anyMatch(STAFF::contains);
    }

    public boolean isPremium(UUID userId, AuthInfo auth) {
        if (isStaff(auth)) {
            return true;
        }
        return userRepository.findById(userId)
                .map(User::getPremiumUntil)
                .map(until -> until.isAfter(OffsetDateTime.now()))
                .orElse(false);
    }

    /** Chặn tính năng Premium. Lỗi 403 kèm data.code = PREMIUM_REQUIRED để giao diện mở trang nâng cấp. */
    public void requirePremium(String featureVi) {
        AuthInfo auth = SecurityContextHolder.getAuthInfo();
        if (!isPremium(auth.getId(), auth)) {
            CommonException ex = new CommonException(featureVi + " thuộc gói Premium. Nâng cấp để dùng tính năng này nhé.");
            ex.setHttpStatus(HttpStatus.FORBIDDEN);
            ex.setData(Map.of("code", "PREMIUM_REQUIRED"));
            throw ex;
        }
    }

    public SubscriptionResponse current(UUID userId, AuthInfo auth) {
        OffsetDateTime until = userRepository.findById(userId).map(User::getPremiumUntil).orElse(null);
        boolean staff = isStaff(auth);
        boolean premium = staff || (until != null && until.isAfter(OffsetDateTime.now()));
        return SubscriptionResponse.builder()
                .plan(premium ? PREMIUM : FREE)
                .premium(premium)
                .premiumUntil(until)
                .staff(staff)
                .build();
    }

    public List<PlanResponse> plans() {
        return List.of(
                PlanResponse.builder()
                        .code(FREE)
                        .nameVi("Gói Free")
                        .taglineVi("Miễn phí")
                        .descriptionVi("Phù hợp với người mới bắt đầu học Ngôn ngữ ký hiệu Việt Nam.")
                        .price(0)
                        .benefits(List.of("Khóa học đầy đủ", "Ôn tập từ vựng", "Góc trò chơi", "Diễn đàn cộng đồng"))
                        .includesFree(false)
                        .build(),
                PlanResponse.builder()
                        .code(PREMIUM)
                        .nameVi("Gói Premium")
                        .taglineVi("Nâng cao")
                        .descriptionVi("Phù hợp với người học muốn kiểm tra kiến thức và mở rộng khả năng nhận biết các cử chỉ.")
                        .price(props.getPremiumPrice())
                        .durationDays(props.getPremiumDurationDays())
                        .benefits(List.of("Bài kiểm tra kiến thức", "Thư viện cử chỉ", "AI checking (chấm điểm cử chỉ)"))
                        .includesFree(true)
                        .build());
    }
}
