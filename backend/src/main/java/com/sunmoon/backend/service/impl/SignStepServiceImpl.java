package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.EntityType;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.request.content.SignStepRequest;
import com.sunmoon.backend.dto.response.content.SignStepResponse;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.entity.dictionary.SignStep;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.FileAttachmentRepository;
import com.sunmoon.backend.repository.SignRepository;
import com.sunmoon.backend.repository.SignStepRepository;
import com.sunmoon.backend.service.MinioService;
import com.sunmoon.backend.service.SignStepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Soạn hướng dẫn thực hiện ký hiệu theo từng bước.
 *
 * step_order có ràng buộc UNIQUE(sign_id, step_order) ở CSDL (V11), nên chèn
 * hoặc di chuyển một bước không thể gán số thứ tự trực tiếp — số mới rất có
 * thể đang bị bước khác giữ. Xử lý bằng {@link #applyOrder}: đẩy TOÀN BỘ bước
 * qua số âm (không đụng ràng buộc, vì âm không trùng bất kỳ số dương nào) rồi
 * mới gán lại số dương đúng thứ tự cuối cùng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignStepServiceImpl implements SignStepService {

    private final SignStepRepository signStepRepository;
    private final SignRepository signRepository;
    private final FileAttachmentRepository fileRepository;
    private final MinioService minioService;

    @Override
    @Transactional(readOnly = true)
    public List<SignStepResponse> list(UUID signId) {
        return signStepRepository.findBySignIdOrdered(signId).stream()
                .map(SignStepServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SignStepResponse create(UUID signId, SignStepRequest request) {
        Sign sign = requireSign(signId);

        SignStep step = signStepRepository.save(SignStep.builder()
                .sign(sign)
                // Số tạm — chắc chắn còn trống vì luôn lớn hơn số bước hiện có.
                // applyOrder() ngay sau đây sẽ gán lại số thật.
                .stepOrder((int) signStepRepository.countBySignId(signId) + 1)
                .titleVi(request.getTitleVi())
                .descriptionVi(request.getDescriptionVi())
                .bodyFocus(request.getBodyFocus())
                .holdSeconds(request.getHoldSeconds())
                .build());

        // Loại chính bước vừa lưu khỏi danh sách "hiện có" trước khi chèn lại
        // theo vị trí mong muốn — thiếu bước này thì nó bị tính hai lần, đẩy
        // vị trí thật lệch đi một bậc so với yêu cầu.
        List<UUID> finalOrder = currentOrderExcluding(signId, step.getId());
        int position = clampPosition(request.getStepOrder(), finalOrder.size() + 1);
        finalOrder.add(position - 1, step.getId());
        applyOrder(signId, finalOrder);

        return toResponse(mustFind(step.getId()));
    }

    @Override
    @Transactional
    public SignStepResponse update(UUID signId, UUID stepId, SignStepRequest request) {
        SignStep step = requireStep(signId, stepId);

        step.setTitleVi(request.getTitleVi());
        step.setDescriptionVi(request.getDescriptionVi());
        step.setBodyFocus(request.getBodyFocus());
        step.setHoldSeconds(request.getHoldSeconds());
        signStepRepository.save(step);

        if (!request.getStepOrder().equals(step.getStepOrder())) {
            List<UUID> finalOrder = currentOrderExcluding(signId, stepId);
            int position = clampPosition(request.getStepOrder(), finalOrder.size() + 1);
            finalOrder.add(position - 1, stepId);
            applyOrder(signId, finalOrder);
        }

        return toResponse(mustFind(stepId));
    }

    @Override
    @Transactional
    public void delete(UUID signId, UUID stepId) {
        SignStep step = requireStep(signId, stepId);
        deleteImageFile(step);
        signStepRepository.delete(step);

        // Dồn lại cho liền mạch 1..N — bước xoá ở giữa không để lại khoảng trống
        applyOrder(signId, currentOrderExcluding(signId, null));
    }

    @Override
    @Transactional
    public List<SignStepResponse> reorder(UUID signId, ReorderRequest request) {
        List<UUID> current = currentOrderExcluding(signId, null);
        List<UUID> requested = request.getOrderedIds();

        if (requested.size() != current.size() || !new java.util.HashSet<>(requested).containsAll(current)) {
            throw new ConflictException(
                    "Danh sách sắp xếp không khớp các bước hiện có. Hãy tải lại rồi thử lại.");
        }

        applyOrder(signId, requested);
        return list(signId);
    }

    @Override
    @Transactional
    public SignStepResponse uploadImage(UUID signId, UUID stepId, MultipartFile file) {
        Sign sign = requireSign(signId);
        SignStep step = requireStep(signId, stepId);

        if (file == null || file.isEmpty()) {
            throw new CommonException("File ảnh rỗng");
        }

        // Bước cũ có ảnh rồi thì dọn ảnh cũ trước — mỗi bước chỉ giữ một ảnh
        deleteImageFile(step);

        String objectName = "signs/" + sign.getGloss() + "/steps/"
                + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            minioService.upload(file, objectName);
        } catch (Exception e) {
            throw new CommonException("Tải ảnh lên MinIO thất bại: " + e.getMessage(), e);
        }

        FileAttachment attachment = fileRepository.save(FileAttachment.builder()
                .bucket(minioService.getBucketName())
                .objectKey(objectName)
                .originalName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .extension(extensionOf(file.getOriginalFilename()))
                .sizeBytes(file.getSize())
                .entityType(EntityType.SIGN_STEP)
                .entityId(step.getId())
                .build());

        step.setImageFile(attachment);
        signStepRepository.save(step);

        return toResponse(mustFind(stepId));
    }

    @Override
    @Transactional
    public void deleteImage(UUID signId, UUID stepId) {
        SignStep step = requireStep(signId, stepId);
        deleteImageFile(step);
        step.setImageFile(null);
        signStepRepository.save(step);
    }

    // ==================== riêng ====================

    /**
     * Đẩy toàn bộ bước trong orderedIds qua số ÂM rồi mới gán số DƯƠNG cuối
     * cùng — tránh đụng ràng buộc UNIQUE(sign_id, step_order) ở bước trung
     * gian khi số mới của một bước trùng số cũ của bước khác chưa kịp đổi.
     *
     * Số tạm phải là SỐ DƯƠNG lớn (không phải số âm): cột step_order còn có
     * CHECK (step_order >= 1), số âm đúng ra tránh được va chạm UNIQUE nhưng
     * lại vỡ ngay ràng buộc CHECK đó.
     */
    private void applyOrder(UUID signId, List<UUID> orderedIds) {
        List<SignStep> steps = signStepRepository.findBySignIdOrdered(signId);
        Map<UUID, SignStep> byId = new HashMap<>();
        steps.forEach(s -> byId.put(s.getId(), s));

        int tempBase = 1_000_000; // chắc chắn không một từ nào có tới một triệu bước
        for (int i = 0; i < orderedIds.size(); i++) {
            SignStep step = byId.get(orderedIds.get(i));
            if (step == null) {
                throw new NotFoundException("Bước không thuộc từ này: " + orderedIds.get(i));
            }
            step.setStepOrder(tempBase + i + 1);
        }
        signStepRepository.saveAll(byId.values());
        signStepRepository.flush();

        for (int i = 0; i < orderedIds.size(); i++) {
            byId.get(orderedIds.get(i)).setStepOrder(i + 1);
        }
        signStepRepository.saveAll(byId.values());
    }

    /** Danh sách id các bước hiện có theo đúng thứ tự, bỏ qua một id nếu có (dùng khi id đó sắp được chèn lại ở vị trí khác) */
    private List<UUID> currentOrderExcluding(UUID signId, UUID excludeId) {
        List<UUID> ids = new ArrayList<>();
        for (SignStep s : signStepRepository.findBySignIdOrdered(signId)) {
            if (!s.getId().equals(excludeId)) {
                ids.add(s.getId());
            }
        }
        return ids;
    }

    /** Ép vị trí mong muốn về khoảng hợp lệ [1, maxPosition] — người soạn gõ 0 hay 999 vẫn không vỡ danh sách */
    private static int clampPosition(int requested, int maxPosition) {
        return Math.max(1, Math.min(requested, maxPosition));
    }

    private void deleteImageFile(SignStep step) {
        FileAttachment file = step.getImageFile();
        if (file == null) {
            return;
        }
        try {
            minioService.delete(file.getBucket(), file.getObjectKey());
        } catch (Exception e) {
            // Không chặn thao tác chính nếu MinIO lỗi — ghi log để dọn sau,
            // giống cách deleteVideo() của SignServiceImpl đang xử lý.
            log.warn("Không xoá được object MinIO {}: {}", file.getObjectKey(), e.getMessage());
        }
    }

    private Sign requireSign(UUID signId) {
        return signRepository.findById(signId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy từ vựng: " + signId));
    }

    private SignStep requireStep(UUID signId, UUID stepId) {
        SignStep step = mustFind(stepId);
        if (!step.getSign().getId().equals(signId)) {
            throw new ConflictException("Bước này không thuộc từ vựng đã chỉ định");
        }
        return step;
    }

    private SignStep mustFind(UUID stepId) {
        return signStepRepository.findById(stepId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bước: " + stepId));
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? null : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static SignStepResponse toResponse(SignStep step) {
        return SignStepResponse.builder()
                .id(step.getId())
                .signId(step.getSign().getId())
                .stepOrder(step.getStepOrder())
                .titleVi(step.getTitleVi())
                .descriptionVi(step.getDescriptionVi())
                .bodyFocus(step.getBodyFocus())
                .holdSeconds(step.getHoldSeconds())
                .imageUrl(step.getImageFile() == null ? null : step.getImageFile().getPublicUrl())
                .build();
    }
}
