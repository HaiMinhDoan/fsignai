package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.request.catalog.WordPackRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.catalog.WordPackItemResponse;
import com.sunmoon.backend.dto.response.catalog.WordPackResponse;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.auth.User;
import com.sunmoon.backend.entity.catalog.WordPack;
import com.sunmoon.backend.entity.catalog.WordPackItem;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.entity.dictionary.SignVideo;
import com.sunmoon.backend.entity.dictionary.Topic;
import com.sunmoon.backend.entity.progress.UserPackProgress;
import com.sunmoon.backend.constant.enums.PointSource;
import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.mapper.WordPackMapper;
import com.sunmoon.backend.repository.*;
import com.sunmoon.backend.service.PointService;
import com.sunmoon.backend.service.WordPackService;
import com.sunmoon.backend.service.impl.util.VietnameseTextUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class WordPackServiceImpl extends BaseServiceImpl<WordPack, UUID> implements WordPackService {

    @PersistenceContext
    private EntityManager entityManager;

    private final WordPackRepository wordPackRepository;
    private final WordPackItemRepository wordPackItemRepository;
    private final UserPackProgressRepository packProgressRepository;
    private final TopicRepository topicRepository;
    private final SignRepository signRepository;
    private final SignVideoRepository signVideoRepository;
    private final FileAttachmentRepository fileRepository;
    private final UserRepository userRepository;
    private final WordPackMapper wordPackMapper;
    private final PointService pointService;

    public WordPackServiceImpl(WordPackRepository wordPackRepository,
                               WordPackItemRepository wordPackItemRepository,
                               UserPackProgressRepository packProgressRepository,
                               TopicRepository topicRepository,
                               SignRepository signRepository,
                               SignVideoRepository signVideoRepository,
                               FileAttachmentRepository fileRepository,
                               UserRepository userRepository,
                               WordPackMapper wordPackMapper,
                               PointService pointService) {
        super(wordPackRepository, "level");
        this.wordPackRepository = wordPackRepository;
        this.wordPackItemRepository = wordPackItemRepository;
        this.packProgressRepository = packProgressRepository;
        this.topicRepository = topicRepository;
        this.signRepository = signRepository;
        this.signVideoRepository = signVideoRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.wordPackMapper = wordPackMapper;
        this.pointService = pointService;
    }

    @Override
    protected EntityManager getEntityManager() {
        return entityManager;
    }

    // ==================== ĐỌC (quản trị) ====================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WordPackResponse> search(BaseFilterRequest request) {
        Page<WordPack> page = filter(request);
        List<WordPackResponse> items = page.getContent().stream().map(wordPackMapper::toResponse).toList();
        fillItemCounts(items);
        return PageResponse.<WordPackResponse>builder()
                .items(items)
                .total(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WordPackResponse getDetail(UUID id) {
        WordPack pack = mustFind(id);
        WordPackResponse response = wordPackMapper.toResponse(pack);
        response.setItems(loadItems(id));
        response.setItemCount((long) response.getItems().size());
        return response;
    }

    // ==================== GHI (quản trị) ====================

    @Override
    @Transactional
    public WordPackResponse createPack(WordPackRequest request) {
        WordPack entity = wordPackMapper.toEntity(request);
        entity.setCode(resolveCode(request, null));
        applyRelations(entity, request);
        WordPack saved = wordPackRepository.save(entity);

        WordPackResponse response = wordPackMapper.toResponse(saved);
        response.setItemCount(0L);
        return response;
    }

    @Override
    @Transactional
    public WordPackResponse updatePack(UUID id, WordPackRequest request) {
        WordPack entity = mustFind(id);

        wordPackMapper.updateEntity(entity, request);
        entity.setCode(resolveCode(request, id));
        applyRelations(entity, request);
        WordPack saved = wordPackRepository.save(entity);

        WordPackResponse response = wordPackMapper.toResponse(saved);
        response.setItemCount(wordPackItemRepository.countByPackId(id));
        return response;
    }

    @Override
    @Transactional
    public void deletePack(UUID id) {
        if (!wordPackRepository.existsById(id)) {
            throw new NotFoundException("Không tìm thấy gói từ: " + id);
        }
        if (wordPackRepository.existsByUnlockAfterPackId(id)) {
            // Xoá gói đang là điều kiện mở khoá của gói khác sẽ để lại tham
            // chiếu mồ côi trong logic mở khoá (ON DELETE SET NULL ở CSDL
            // khiến gói con đột nhiên mở tự do, không phải điều biên tập
            // viên chủ ý) — bắt gỡ liên kết trước cho tường minh.
            throw new ConflictException(
                    "Có gói từ khác đang lấy gói này làm điều kiện mở khoá. Hãy gỡ liên kết đó trước.");
        }
        wordPackItemRepository.deleteAllByPackId(id);
        wordPackRepository.deleteById(id);
    }

    @Override
    @Transactional
    public int setPublished(List<UUID> ids, boolean published) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<WordPack> packs = wordPackRepository.findAllById(ids);

        if (published) {
            // Không cho xuất bản gói rỗng: bé bấm vào sẽ thấy đảo không có gì
            List<String> empty = packs.stream()
                    .filter(p -> wordPackItemRepository.countByPackId(p.getId()) == 0)
                    .map(WordPack::getTitleVi)
                    .toList();
            if (!empty.isEmpty()) {
                throw new ConflictException("Gói chưa có từ nào, không thể xuất bản: " + String.join(", ", empty));
            }
        }

        packs.forEach(p -> p.setIsPublished(published));
        wordPackRepository.saveAll(packs);
        return packs.size();
    }

    @Override
    @Transactional
    public void reorderPacks(ReorderRequest request) {
        List<UUID> ordered = request.getOrderedIds();
        Map<UUID, WordPack> byId = new HashMap<>();
        wordPackRepository.findAllById(ordered).forEach(p -> byId.put(p.getId(), p));

        List<WordPack> toSave = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            WordPack pack = byId.get(ordered.get(i));
            if (pack == null) {
                throw new NotFoundException("Không tìm thấy gói từ: " + ordered.get(i));
            }
            pack.setDisplayOrder(i);
            toSave.add(pack);
        }
        wordPackRepository.saveAll(toSave);
    }

    // ==================== Nội dung gói ====================

    @Override
    @Transactional
    public List<WordPackItemResponse> addSigns(UUID packId, List<UUID> signIds) {
        WordPack pack = mustFind(packId);
        if (signIds == null || signIds.isEmpty()) {
            return List.of();
        }

        // Bỏ từ đã có sẵn — chọn trùng là chuyện thường khi tích chọn hàng
        // loạt trên danh sách dài, không đáng để báo lỗi và bắt làm lại.
        Set<UUID> already = new HashSet<>(wordPackItemRepository.findSignIdsByPackId(packId));
        List<UUID> toAdd = signIds.stream().distinct().filter(id -> !already.contains(id)).toList();
        if (toAdd.isEmpty()) {
            return List.of();
        }

        List<Sign> signs = signRepository.findAllById(toAdd);
        if (signs.size() != toAdd.size()) {
            throw new NotFoundException("Có từ vựng không tồn tại trong danh sách đã chọn");
        }

        int order = wordPackItemRepository.nextDisplayOrder(packId);
        List<WordPackItem> items = new ArrayList<>();
        for (Sign sign : signs) {
            items.add(WordPackItem.builder()
                    .pack(pack)
                    .sign(sign)
                    .displayOrder(order++)
                    .build());
        }
        wordPackItemRepository.saveAll(items);

        List<WordPackItemResponse> responses = items.stream().map(wordPackMapper::toItemResponse).toList();
        fillVideoUrls(responses);
        return responses;
    }

    @Override
    @Transactional
    public void removeItem(UUID packId, UUID itemId) {
        WordPackItem item = wordPackItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy từ trong gói: " + itemId));
        if (!item.getPack().getId().equals(packId)) {
            throw new ConflictException("Từ này không thuộc gói đã chỉ định");
        }
        wordPackItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void reorderItems(UUID packId, ReorderRequest request) {
        List<WordPackItem> items = wordPackItemRepository.findAllByPackIdOrderByDisplayOrderAsc(packId);
        Map<UUID, WordPackItem> byId = new HashMap<>();
        items.forEach(i -> byId.put(i.getId(), i));

        List<UUID> ordered = request.getOrderedIds();
        if (ordered.size() != items.size()) {
            throw new ConflictException(
                    "Danh sách sắp xếp không khớp từ hiện có. Hãy tải lại trang rồi thử lại.");
        }

        for (int i = 0; i < ordered.size(); i++) {
            WordPackItem item = byId.get(ordered.get(i));
            if (item == null) {
                throw new NotFoundException("Từ không thuộc gói này: " + ordered.get(i));
            }
            item.setDisplayOrder(i);
        }
        wordPackItemRepository.saveAll(items);
    }

    // ==================== Người học ====================

    @Override
    @Transactional(readOnly = true)
    public List<WordPackResponse> listPublishedForLearner(UUID userId) {
        List<WordPack> packs = wordPackRepository.findAllByIsPublishedTrueOrderByDisplayOrderAsc();
        List<WordPackResponse> responses = packs.stream().map(wordPackMapper::toResponse).toList();
        fillItemCounts(responses);

        Map<UUID, UserPackProgress> myProgress = userId == null
                ? Map.of()
                : progressByPackId(userId);
        // Gói đã hoàn thành — dùng để quyết định gói nào được mở khoá tiếp theo
        Set<UUID> completedPackIds = new HashSet<>();
        myProgress.forEach((packId, p) -> {
            if ("COMPLETED".equals(p.getStatus())) completedPackIds.add(packId);
        });

        for (WordPackResponse r : responses) {
            r.setUnlocked(isUnlocked(r.getUnlockAfterPackId(), completedPackIds));
            UserPackProgress p = myProgress.get(r.getId());
            if (p != null) {
                r.setMyStatus(p.getStatus());
                r.setMyItemsCompleted(p.getItemsCompleted());
                r.setMyStars(p.getStars());
            }
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public WordPackResponse getPublishedDetail(UUID packId, UUID userId) {
        WordPack pack = wordPackRepository.findById(packId)
                .filter(p -> Boolean.TRUE.equals(p.getIsPublished()))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói từ: " + packId));

        WordPackResponse response = wordPackMapper.toResponse(pack);
        response.setItems(loadItems(packId));
        response.setItemCount((long) response.getItems().size());

        Set<UUID> completedPackIds = userId == null
                ? Set.of()
                : new HashSet<>(completedPackIdsOf(userId));
        response.setUnlocked(isUnlocked(response.getUnlockAfterPackId(), completedPackIds));

        if (userId != null) {
            packProgressRepository.findByUserIdAndPackId(userId, packId).ifPresent(p -> {
                response.setMyStatus(p.getStatus());
                response.setMyItemsCompleted(p.getItemsCompleted());
                response.setMyStars(p.getStars());
            });
        }
        return response;
    }

    @Override
    @Transactional
    public WordPackResponse startPack(UUID packId, UUID userId) {
        WordPack pack = wordPackRepository.findById(packId)
                .filter(p -> Boolean.TRUE.equals(p.getIsPublished()))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói từ: " + packId));

        // Idempotent: đã bắt đầu rồi thì trả lại nguyên trạng, không tạo mới
        // và không đếm thêm lượt — chỉ POST /progress mới đổi số liệu.
        Optional<UserPackProgress> existing = packProgressRepository.findByUserIdAndPackId(userId, packId);
        if (existing.isPresent()) {
            return toLearnerResponse(pack, existing.get());
        }

        if (!isUnlocked(pack.getUnlockAfterPack() == null ? null : pack.getUnlockAfterPack().getId(),
                new HashSet<>(completedPackIdsOf(userId)))) {
            throw new ConflictException("Gói từ này đang khoá — hãy hoàn thành gói trước đó đã.");
        }

        long itemsTotal = wordPackItemRepository.countByPackId(packId);
        User user = entityManager.getReference(User.class, userId);
        UserPackProgress progress = packProgressRepository.save(UserPackProgress.builder()
                .user(user)
                .pack(pack)
                .status("IN_PROGRESS")
                .itemsCompleted(0)
                .itemsTotal((int) itemsTotal)
                .attempts(1)
                .build());

        return toLearnerResponse(pack, progress);
    }

    @Override
    @Transactional
    public WordPackResponse updateProgress(UUID packId, UUID userId, int itemsCompleted) {
        UserPackProgress progress = packProgressRepository.findByUserIdAndPackId(userId, packId)
                .orElseThrow(() -> new NotFoundException(
                        "Chưa bắt đầu gói từ này — gọi start trước khi ghi tiến độ"));

        // Chỉ tăng, không lùi: học lại các từ đã qua không được làm tụt tiến độ
        int clamped = Math.max(progress.getItemsCompleted(),
                Math.min(itemsCompleted, progress.getItemsTotal()));
        progress.setItemsCompleted(clamped);
        progress.setLastActivityAt(OffsetDateTime.now());

        if (!"COMPLETED".equals(progress.getStatus()) && clamped >= progress.getItemsTotal()
                && progress.getItemsTotal() > 0) {
            progress.setStatus("COMPLETED");
            progress.setCompletedAt(OffsetDateTime.now());
            // 3 sao nếu xong ngay lần đầu, 2 sao nếu phải học lại — thang điểm
            // đơn giản có chủ đích: gói từ "học xong là xong", không chấm điểm
            // gắt như một bài thi (khác hẳn quiz/blueprint).
            progress.setStars(progress.getAttempts() <= 1 ? 3 : 2);
            pointService.award(userId, PointSource.PACK, packId, progress.getStars() * 10, "Hoàn thành gói từ");
        }

        UserPackProgress saved = packProgressRepository.save(progress);
        WordPack pack = wordPackRepository.findById(packId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói từ: " + packId));
        return toLearnerResponse(pack, saved);
    }

    // ==================== riêng ====================

    private WordPackResponse toLearnerResponse(WordPack pack, UserPackProgress progress) {
        WordPackResponse response = wordPackMapper.toResponse(pack);
        response.setMyStatus(progress.getStatus());
        response.setMyItemsCompleted(progress.getItemsCompleted());
        response.setMyStars(progress.getStars());
        return response;
    }

    private Map<UUID, UserPackProgress> progressByPackId(UUID userId) {
        Map<UUID, UserPackProgress> map = new HashMap<>();
        packProgressRepository.findAllByUserId(userId).forEach(p -> map.put(p.getPack().getId(), p));
        return map;
    }

    private List<UUID> completedPackIdsOf(UUID userId) {
        List<UUID> ids = new ArrayList<>();
        packProgressRepository.findAllByUserId(userId).forEach(p -> {
            if ("COMPLETED".equals(p.getStatus())) ids.add(p.getPack().getId());
        });
        return ids;
    }

    /** null = mở sẵn từ đầu; có điều kiện thì phải nằm trong tập đã hoàn thành */
    private static boolean isUnlocked(UUID unlockAfterPackId, Set<UUID> completedPackIds) {
        return unlockAfterPackId == null || completedPackIds.contains(unlockAfterPackId);
    }

    private WordPack mustFind(UUID id) {
        return wordPackRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói từ: " + id));
    }

    private void applyRelations(WordPack entity, WordPackRequest request) {
        if (request.getCoverFileId() != null) {
            entity.setCoverFile(entityManager.getReference(FileAttachment.class, request.getCoverFileId()));
        } else {
            entity.setCoverFile(null);
        }

        if (request.getTopicId() != null) {
            Topic topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề: " + request.getTopicId()));
            entity.setTopic(topic);
        } else {
            entity.setTopic(null);
        }

        if (request.getUnlockAfterPackId() != null) {
            if (request.getUnlockAfterPackId().equals(entity.getId())) {
                throw new ConflictException("Một gói không thể tự khoá chính nó");
            }
            WordPack prerequisite = wordPackRepository.findById(request.getUnlockAfterPackId())
                    .orElseThrow(() -> new NotFoundException(
                            "Không tìm thấy gói điều kiện: " + request.getUnlockAfterPackId()));
            entity.setUnlockAfterPack(prerequisite);
        } else {
            entity.setUnlockAfterPack(null);
        }
    }

    private String resolveCode(WordPackRequest request, UUID currentId) {
        String base = (request.getCode() == null || request.getCode().isBlank())
                ? VietnameseTextUtil.toSlug(request.getTitleVi())
                : VietnameseTextUtil.toSlug(request.getCode());
        if (base.isEmpty()) {
            throw new ConflictException("Không sinh được mã từ tên gói từ");
        }
        return uniqueCode(base, currentId);
    }

    /** Thêm hậu tố -2, -3... cho tới khi mã không đụng bản ghi nào khác */
    private String uniqueCode(String base, UUID currentId) {
        String candidate = base.length() > 100 ? base.substring(0, 100) : base;
        int suffix = 2;
        while (taken(candidate, currentId)) {
            String tail = "-" + suffix++;
            int keep = Math.min(base.length(), 100 - tail.length());
            candidate = base.substring(0, keep) + tail;
        }
        return candidate;
    }

    private boolean taken(String code, UUID currentId) {
        return currentId == null
                ? wordPackRepository.existsByCode(code)
                : wordPackRepository.existsByCodeAndIdNot(code, currentId);
    }

    private List<WordPackItemResponse> loadItems(UUID packId) {
        List<WordPackItem> items = wordPackItemRepository.findAllByPackIdOrderByDisplayOrderAsc(packId);
        List<WordPackItemResponse> responses = items.stream().map(wordPackMapper::toItemResponse).toList();
        fillVideoUrls(responses);
        return responses;
    }

    private void fillItemCounts(List<WordPackResponse> items) {
        if (items.isEmpty()) return;
        List<UUID> ids = items.stream().map(WordPackResponse::getId).toList();
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : wordPackItemRepository.countByPackIds(ids)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        items.forEach(item -> item.setItemCount(counts.getOrDefault(item.getId(), 0L)));
    }

    /**
     * Mỗi từ có thể có nhiều video chính — một cho mỗi vùng miền. Ưu tiên bản
     * dùng chung (COMMON), giống fillPrimaryVideoUrls của LessonServiceImpl.
     */
    private void fillVideoUrls(List<WordPackItemResponse> items) {
        List<UUID> signIds = items.stream()
                .map(WordPackItemResponse::getSignId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (signIds.isEmpty()) return;

        Map<UUID, SignVideo> best = new HashMap<>();
        for (SignVideo video : signVideoRepository.findPrimaryVideosBySignIds(signIds)) {
            UUID signId = video.getSign().getId();
            SignVideo current = best.get(signId);
            if (current == null || (video.getRegion() == Region.COMMON && current.getRegion() != Region.COMMON)) {
                best.put(signId, video);
            }
        }

        items.forEach(item -> {
            SignVideo video = item.getSignId() == null ? null : best.get(item.getSignId());
            if (video != null && video.getFile() != null) {
                item.setSignPrimaryVideoUrl(video.getFile().getPublicUrl());
                if (video.getThumbnailFile() != null) {
                    item.setSignThumbnailUrl(video.getThumbnailFile().getPublicUrl());
                }
            }
        });
    }
}
