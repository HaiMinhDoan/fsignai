package com.sunmoon.backend.dto.request.catalog;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

/**
 * Sắp xếp lại thứ tự bằng cách gửi TOÀN BỘ danh sách id theo thứ tự mới.
 *
 * Cách này chắc hơn kiểu gửi từng cặp (id, vị trí mới): giao diện kéo–thả vốn
 * đã có sẵn mảng theo đúng thứ tự người dùng nhìn thấy, nên gửi nguyên mảng thì
 * kết quả trên màn hình và trong CSDL không bao giờ lệch nhau.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReorderRequest {

    @NotEmpty(message = "Danh sách sắp xếp không được rỗng")
    List<UUID> orderedIds;
}
