package com.sunmoon.backend.dto.request.analytics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Một lượt chuyển trang của web học, do vue-router báo về sau mỗi lần điều hướng */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageViewRequest {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9-]{8,64}")
    String visitorId;

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9-]{8,64}")
    String sessionId;

    @NotBlank
    @Size(max = 300)
    String path;

    @Size(max = 80)
    String routeName;

    /** document.referrer — chỉ gửi ở lượt xem đầu phiên, và chỉ khi đến từ trang khác */
    @Size(max = 1000)
    String referrer;
}
