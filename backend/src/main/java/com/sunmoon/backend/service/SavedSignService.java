package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.dictionary.SavedSignResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SavedSignService {

    /** true nếu vừa lưu mới; false nếu đã lưu từ trước (không lưu trùng, không lỗi) */
    boolean save(UUID userId, UUID signId, String note);

    void unsave(UUID userId, UUID signId);

    boolean isSaved(UUID userId, UUID signId);

    PageResponse<SavedSignResponse> list(UUID userId, Pageable pageable);
}
