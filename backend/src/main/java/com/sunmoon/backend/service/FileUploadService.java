package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.response.FileAttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Tải tệp lên dùng chung cho mọi module.
 *
 * Trước đây chỉ SignController có upload, và chỉ nhận video. Hậu quả: form Khoá
 * học có ô coverFileId, form Chủ đề có iconFileId/coverFileId nhưng không có
 * đường nào lấy được id đó, nên ba ô ấy vô dụng.
 *
 * Luồng dùng: giao diện gọi upload trước, nhận về id, rồi gửi id đó kèm trong
 * form lưu khoá học / chủ đề. Tách hai bước như vậy để tệp lớn không phải gửi
 * lại mỗi lần người dùng sửa một chữ trong form.
 */
public interface FileUploadService {

    /**
     * @param entityType hằng số trong EntityType — nói tệp này thuộc về bảng nào
     * @param entityId   id bản ghi sở hữu; để null khi tải trước lúc tạo bản ghi
     */
    FileAttachmentResponse uploadImage(MultipartFile file, String entityType, UUID entityId);

    /** Xoá cả object trên MinIO lẫn bản ghi file_attachments */
    void delete(UUID fileId);
}
