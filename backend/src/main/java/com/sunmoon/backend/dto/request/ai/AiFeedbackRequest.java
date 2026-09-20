package com.sunmoon.backend.dto.request.ai;

import com.sunmoon.backend.constant.enums.FeedbackVerdict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiFeedbackRequest {

    @NotNull(message = "Thiếu lựa chọn đồng ý/không đồng ý")
    private FeedbackVerdict verdict;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;
}
