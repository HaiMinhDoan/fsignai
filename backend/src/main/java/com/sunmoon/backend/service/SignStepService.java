package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.request.content.SignStepRequest;
import com.sunmoon.backend.dto.response.content.SignStepResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Soạn hướng dẫn thực hiện ký hiệu theo từng bước (ảnh thế tay + mô tả).
 *
 * Tách khỏi SignService như video ký hiệu đã tách: đây là một khía cạnh nội
 * dung riêng, có vòng đời và bộ ảnh riêng, gộp vào sẽ làm SignServiceImpl
 * phình to không cần thiết.
 */
public interface SignStepService {

    List<SignStepResponse> list(UUID signId);

    /** stepOrder trong request có thể trùng bước đã có — bước cũ và các bước
     * sau đó tự dồn lên một để nhường chỗ, không báo lỗi trùng số thứ tự. */
    SignStepResponse create(UUID signId, SignStepRequest request);

    SignStepResponse update(UUID signId, UUID stepId, SignStepRequest request);

    void delete(UUID signId, UUID stepId);

    /** Sắp xếp lại toàn bộ bước theo danh sách id mới — cùng cách lesson item
     * và câu hỏi đề thi đang làm (gửi nguyên mảng, không gửi từng cặp id/vị trí). */
    List<SignStepResponse> reorder(UUID signId, ReorderRequest request);

    SignStepResponse uploadImage(UUID signId, UUID stepId, MultipartFile file);

    void deleteImage(UUID signId, UUID stepId);
}
