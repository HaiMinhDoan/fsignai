package com.sunmoon.backend.dto.request.guardian;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClaimChildRequest {

    /** Mã bé đọc cho bố mẹ nhập */
    @NotBlank(message = "Cần nhập mã liên kết")
    private String inviteCode;

    /** PARENT | GUARDIAN | TEACHER | RELATIVE */
    private String relationship;
}
