package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.request.catalog.WordPackRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.catalog.WordPackItemResponse;
import com.sunmoon.backend.dto.response.catalog.WordPackResponse;
import com.sunmoon.backend.entity.catalog.WordPack;

import java.util.List;
import java.util.UUID;

public interface WordPackService extends BaseService<WordPack, UUID> {

    /** Trả thẳng PageResponse — số từ trong gói phải gom một truy vấn, tránh N+1 ở mapper */
    PageResponse<WordPackResponse> search(BaseFilterRequest request);

    WordPackResponse createPack(WordPackRequest request);

    WordPackResponse updatePack(UUID id, WordPackRequest request);

    /** Kèm danh sách từ đã sắp thứ tự */
    WordPackResponse getDetail(UUID id);

    void deletePack(UUID id);

    int setPublished(List<UUID> ids, boolean published);

    void reorderPacks(ReorderRequest request);

    // ==================== Nội dung gói ====================

    List<WordPackItemResponse> addSigns(UUID packId, List<UUID> signIds);

    void removeItem(UUID packId, UUID itemId);

    void reorderItems(UUID packId, ReorderRequest request);

    // ==================== Người học ====================

    /**
     * Danh sách gói đã xuất bản cho bản đồ đảo, kèm trạng thái mở khoá và
     * tiến độ của userId (null = khách chưa đăng nhập — mọi gói có điều kiện
     * mở khoá đều hiện khoá, vì chưa biết đã hoàn thành gói nào).
     */
    List<WordPackResponse> listPublishedForLearner(UUID userId);

    /** 404 nếu gói chưa xuất bản. Cho xem trước dù đang khoá — chỉ chặn lúc GHI tiến độ. */
    WordPackResponse getPublishedDetail(UUID packId, UUID userId);

    /**
     * Bắt đầu học một gói — tạo bản ghi tiến độ nếu chưa có (chụp lại số từ
     * hiện tại làm mốc), trả về bản ghi cũ nếu đã từng bắt đầu. Từ chối nếu
     * gói đang khoá.
     */
    WordPackResponse startPack(UUID packId, UUID userId);

    /**
     * Cập nhật số từ đã học. Chỉ được TĂNG, không lùi — học xong bao nhiêu
     * từ giữ nguyên bấy nhiêu dù học lại. Tự chuyển COMPLETED khi đủ số từ.
     */
    WordPackResponse updateProgress(UUID packId, UUID userId, int itemsCompleted);
}
