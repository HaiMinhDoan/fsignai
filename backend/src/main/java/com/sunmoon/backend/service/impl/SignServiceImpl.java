package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.constant.enums.*;
import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.FilterCriteria;
import com.sunmoon.backend.dto.request.content.AssignTopicsRequest;
import com.sunmoon.backend.dto.request.content.SignImportRequest;
import com.sunmoon.backend.dto.request.content.SignRequest;
import com.sunmoon.backend.dto.request.content.SignSearchRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.content.SignImportResultResponse;
import com.sunmoon.backend.dto.response.content.SignResponse;
import com.sunmoon.backend.dto.response.content.SignVideoResponse;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.dictionary.*;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.mapper.SignMapper;
import com.sunmoon.backend.mapper.SignVideoMapper;
import com.sunmoon.backend.repository.*;
import com.sunmoon.backend.service.MinioService;
import com.sunmoon.backend.service.SignService;
import com.sunmoon.backend.service.impl.util.VietnameseTextUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
public class SignServiceImpl extends BaseServiceImpl<Sign, UUID> implements SignService {

    private static final String QIPEDC_VIDEO_BASE = "https://qipedc.moet.gov.vn/videos/";

    @PersistenceContext
    private EntityManager entityManager;

    private final SignRepository signRepository;
    private final SignVideoRepository signVideoRepository;
    private final SignTopicRepository signTopicRepository;
    private final TopicRepository topicRepository;
    private final FileAttachmentRepository fileRepository;
    private final SignMapper signMapper;
    private final SignVideoMapper signVideoMapper;
    private final MinioService minioService;

    public SignServiceImpl(SignRepository signRepository,
                           SignVideoRepository signVideoRepository,
                           SignTopicRepository signTopicRepository,
                           TopicRepository topicRepository,
                           FileAttachmentRepository fileRepository,
                           SignMapper signMapper,
                           SignVideoMapper signVideoMapper,
                           MinioService minioService) {
        // Sign khong co truong "status"; truong trang thai nghiep vu la reviewStatus
        super(signRepository, "reviewStatus");
        this.signRepository = signRepository;
        this.signVideoRepository = signVideoRepository;
        this.signTopicRepository = signTopicRepository;
        this.topicRepository = topicRepository;
        this.fileRepository = fileRepository;
        this.signMapper = signMapper;
        this.signVideoMapper = signVideoMapper;
        this.minioService = minioService;
    }

    @Override
    protected EntityManager getEntityManager() {
        return entityManager;
    }

    // ==================== TIM KIEM ====================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SignResponse> search(SignSearchRequest request) {
        // Dich bo loc nghiep vu sang BaseFilterRequest roi dung lai co che filter
        // dong cua BaseServiceImpl - khong viet lai Specification tu dau.
        List<FilterCriteria> filters = new ArrayList<>();

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            // Diem mau chot: bo dau tu khoa o Java de khop voi cot word_vi_unaccent
            // ma PostgreSQL da sinh san bang f_unaccent(). Go "dia chi" van ra "dia chi" co dau.
            filters.add(FilterCriteria.builder()
                    .fieldName("wordViUnaccent")
                    .operation(FilterOperation.LIKE)
                    .value(VietnameseTextUtil.unaccent(request.getKeyword()))
                    .logicType(FilterLogicType.AND)
                    .build());
        }

        addEquals(filters, "level", request.getLevel());
        addEquals(filters, "unitType", request.getUnitType());
        addEquals(filters, "wordType", request.getWordType());
        addEquals(filters, "domain", request.getDomain());
        addEquals(filters, "source", request.getSource());
        addEquals(filters, "reviewStatus", request.getReviewStatus());
        addEquals(filters, "isPublished", request.getIsPublished());
        addEquals(filters, "primaryTopic.id", request.getTopicId());

        BaseFilterRequest baseRequest = BaseFilterRequest.builder()
                .filters(filters)
                .sorts(request.getSorts())
                .page(request.getPage())
                .size(request.getSize())
                .build();

        Page<Sign> page = super.filter(baseRequest);

        List<SignResponse> items = page.getContent().stream()
                .map(this::toListItem)
                .filter(item -> matchesVideoFilters(item, request))
                .toList();

        return PageResponse.<SignResponse>builder()
                .items(items)
                .total(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }

    // Hai bo loc nay doc tu bang phu (sign_videos, sign_exemplars) nen loc sau
    // khi da lay trang. Chap nhan duoc vi chung chi dung trong CMS voi so luong nho.
    private boolean matchesVideoFilters(SignResponse item, SignSearchRequest request) {
        if (request.getMissingVideoRegion() != null) {
            List<Region> regions = item.getAvailableRegions();
            if (regions != null && regions.contains(request.getMissingVideoRegion())) {
                return false;
            }
        }
        if (Boolean.TRUE.equals(request.getMissingExemplar()) && Boolean.TRUE.equals(item.getAiReady())) {
            return false;
        }
        return true;
    }

    // ==================== CRUD ====================

    @Override
    @Transactional
    public SignResponse createSign(SignRequest request) {
        Sign entity = signMapper.toEntity(request);
        entity.setGloss(resolveGloss(request, null));
        applyPrimaryTopic(entity, request);

        Sign saved = signRepository.save(entity);
        applyTopics(saved, request.getTopicIds());
        return toDetail(saved);
    }

    @Override
    @Transactional
    public SignResponse updateSign(UUID id, SignRequest request) {
        Sign entity = signRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay tu vung: " + id));

        signMapper.updateEntity(entity, request);
        entity.setGloss(resolveGloss(request, id));
        applyPrimaryTopic(entity, request);

        Sign saved = signRepository.save(entity);
        if (request.getTopicIds() != null) {
            applyTopics(saved, request.getTopicIds());
        }
        return toDetail(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SignResponse getDetail(UUID id) {
        Sign entity = signRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay tu vung: " + id));
        return toDetail(entity);
    }

    @Override
    @Transactional
    public void deleteSign(UUID id) {
        Sign entity = signRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay tu vung: " + id));
        // sign_topics, sign_videos, sign_exemplars deu ON DELETE CASCADE o DB
        signRepository.delete(entity);
    }

    @Override
    @Transactional
    public int togglePublish(List<UUID> ids, boolean published) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<Sign> signs = signRepository.findAllById(ids);
        signs.forEach(s -> s.setIsPublished(published));
        signRepository.saveAll(signs);
        return signs.size();
    }

    // ==================== GAN CHU DE HANG LOAT ====================

    /**
     * Gan chu de cho nhieu tu vung cung luc.
     *
     * Doc toan bo cap (sign, topic) da co trong MOT truy van roi loc trong bo nho,
     * thay vi hoi CSDL cho tung cap. Voi lo 500 tu x 3 chu de, kieu hoi tung cap
     * se thanh 1.500 truy van.
     */
    @Override
    @Transactional
    public int assignTopics(AssignTopicsRequest request) {
        List<UUID> signIds = request.getSignIds().stream().distinct().toList();
        List<UUID> topicIds = request.getTopicIds().stream().distinct().toList();

        List<Sign> signs = signRepository.findAllById(signIds);
        if (signs.size() != signIds.size()) {
            throw new NotFoundException("Co tu vung khong ton tai trong danh sach da chon");
        }
        List<Topic> topics = topicRepository.findAllById(topicIds);
        if (topics.size() != topicIds.size()) {
            throw new NotFoundException("Co chu de khong ton tai trong danh sach da chon");
        }

        if (Boolean.TRUE.equals(request.getReplace())) {
            signTopicRepository.deleteAllBySignIdIn(signIds);
            // Xoa xong phai day xuong CSDL ngay, neu khong lenh INSERT ben duoi
            // se chay truoc lenh DELETE dang nam trong hang doi cua Hibernate
            // va dung khoa chinh (sign_id, topic_id).
            entityManager.flush();
        }

        Set<String> existing = new HashSet<>();
        if (!Boolean.TRUE.equals(request.getReplace())) {
            for (SignTopic st : signTopicRepository.findAllBySignIdIn(signIds)) {
                existing.add(st.getSign().getId() + "|" + st.getTopic().getId());
            }
        }

        List<SignTopic> toInsert = new ArrayList<>();
        for (Sign sign : signs) {
            for (Topic topic : topics) {
                if (existing.contains(sign.getId() + "|" + topic.getId())) {
                    continue;
                }
                toInsert.add(SignTopic.builder()
                        .id(new SignTopicId(sign.getId(), topic.getId()))
                        .sign(sign)
                        .topic(topic)
                        .build());
            }
        }
        signTopicRepository.saveAll(toInsert);

        // Chu de chinh chi dat duoc khi nguoi dung chon dung mot chu de,
        // vi khong co can cu nao de chon "chinh" trong nhieu chu de.
        if (Boolean.TRUE.equals(request.getSetPrimary()) && topics.size() == 1) {
            Topic primary = topics.get(0);
            signs.forEach(s -> s.setPrimaryTopic(primary));
            signRepository.saveAll(signs);
        }

        return signs.size();
    }

    // ==================== NHAP HANG LOAT ====================

    @Override
    @Transactional
    public SignImportResultResponse importSigns(SignImportRequest request) {
        SignImportResultResponse result = SignImportResultResponse.builder()
                .dryRun(Boolean.TRUE.equals(request.getDryRun()))
                .totalRows(request.getRows().size())
                .createdCount(0).updatedCount(0).skippedCount(0).failedCount(0)
                .errors(new ArrayList<>())
                .build();

        // Nap truoc chu de theo slug de khong truy van lai o tung dong
        Map<String, Topic> topicBySlug = new HashMap<>();
        topicRepository.findAll().forEach(t -> topicBySlug.put(t.getSlug(), t));

        for (SignImportRequest.SignImportRow row : request.getRows()) {
            try {
                processImportRow(row, request, topicBySlug, result);
            } catch (Exception ex) {
                result.setFailedCount(result.getFailedCount() + 1);
                result.getErrors().add(SignImportResultResponse.RowError.builder()
                        .rowNumber(row.getRowNumber())
                        .wordVi(row.getWordVi())
                        .message(ex.getMessage())
                        .build());
            }
        }
        return result;
    }

    private void processImportRow(SignImportRequest.SignImportRow row,
                                  SignImportRequest request,
                                  Map<String, Topic> topicBySlug,
                                  SignImportResultResponse result) {

        if (row.getWordVi() == null || row.getWordVi().isBlank()) {
            throw new CommonException("Thieu cot word_vi");
        }

        String gloss = (row.getGloss() == null || row.getGloss().isBlank())
                ? VietnameseTextUtil.toGloss(row.getWordVi())
                : row.getGloss().trim();

        Optional<Sign> existing = signRepository.findByGloss(gloss);

        if (existing.isPresent() && request.getDuplicateStrategy() == DuplicateStrategy.SKIP) {
            result.setSkippedCount(result.getSkippedCount() + 1);
            return;
        }

        Sign sign = existing.orElseGet(Sign::new);
        boolean isNew = existing.isEmpty();

        sign.setGloss(gloss);
        sign.setWordVi(row.getWordVi().trim());
        sign.setWordEn(blankToNull(row.getWordEn()));
        sign.setDescriptionVi(blankToNull(row.getDescriptionVi()));
        sign.setUnitType(parseEnum(UnitType.class, row.getUnitType(), UnitType.WORD));
        sign.setWordType(parseEnum(WordType.class, row.getWordType(), WordType.KHONG_XAC_DINH));
        sign.setWordSubtype(parseEnum(WordSubtype.class, row.getWordSubtype(), null));
        sign.setDomain(parseEnum(SignDomain.class, row.getDomain(), null));
        sign.setLevel(parseEnum(SignLevel.class, row.getLevel(), SignLevel.BEGINNER));
        sign.setSource(SignSource.MOET_QIPEDC);

        if (row.getVideoId() != null && !row.getVideoId().isBlank()) {
            sign.setSourceRef(stripRegionSuffix(row.getVideoId().trim()));
        }

        // CREATE_DRAFT: tu trung se tao ban nhap thay vi ghi de ra ban dang xuat ban
        if (existing.isPresent() && request.getDuplicateStrategy() == DuplicateStrategy.CREATE_DRAFT) {
            sign.setIsPublished(false);
        }

        if (Boolean.TRUE.equals(request.getDryRun())) {
            // Chi kiem tra, khong ghi. Van dem de nguoi dung biet ket qua du kien
            // truoc khi bam nhap that - dung yeu cau "luon cho xem truoc truoc khi ghi".
            if (isNew) {
                result.setCreatedCount(result.getCreatedCount() + 1);
            } else {
                result.setUpdatedCount(result.getUpdatedCount() + 1);
            }
            return;
        }

        Sign saved = signRepository.save(sign);

        // Gan chu de theo cot topics dang "gia-dinh,co-ban"
        if (row.getTopics() != null && !row.getTopics().isBlank()) {
            List<UUID> topicIds = Arrays.stream(row.getTopics().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(slug -> {
                        Topic t = topicBySlug.get(slug);
                        if (t == null) {
                            throw new CommonException("Khong tim thay chu de voi slug: " + slug);
                        }
                        return t.getId();
                    })
                    .toList();
            applyTopics(saved, topicIds);
            if (saved.getPrimaryTopic() == null && !topicIds.isEmpty()) {
                saved.setPrimaryTopic(topicRepository.getReferenceById(topicIds.get(0)));
                signRepository.save(saved);
            }
        }

        // Tao ban ghi video tro toi URL goc, de job nap hang loat tai ve sau
        if (row.getVideoId() != null && !row.getVideoId().isBlank()) {
            createPendingVideo(saved, row);
        }

        if (isNew) {
            result.setCreatedCount(result.getCreatedCount() + 1);
        } else {
            result.setUpdatedCount(result.getUpdatedCount() + 1);
        }
    }

    /**
     * Tao SignVideo o trang thai PENDING, chua tai file ve.
     * Job nap hang loat se doc nhung ban ghi nay va tai video tu sourceUrl.
     * Tach lam hai buoc vi tai 4.000 video khong the lam dong bo trong mot request.
     */
    private void createPendingVideo(Sign sign, SignImportRequest.SignImportRow row) {
        String videoId = row.getVideoId().trim();
        String sourceUrl = QIPEDC_VIDEO_BASE + videoId + ".mp4";

        if (signVideoRepository.existsBySourceUrl(sourceUrl)) {
            return;
        }

        Region region = (row.getRegion() != null && !row.getRegion().isBlank())
                ? parseEnum(Region.class, row.getRegion(), Region.COMMON)
                : regionFromVideoId(videoId);

        signVideoRepository.save(SignVideo.builder()
                .sign(sign)
                .region(region)
                .viewAngle(ViewAngle.FRONT)
                .sourceUrl(sourceUrl)
                .ingestStatus(IngestStatus.PENDING)
                .isPrimary(false)
                .build());
    }

    /**
     * Hau to tren ma video cua Bo GD&DT: W00665B / W00665T / W00665N
     * = Bac / Trung / Nam. Suy ra vung mien tu dong, khong bat admin nhap tay.
     */
    private Region regionFromVideoId(String videoId) {
        if (videoId == null || videoId.isEmpty()) {
            return Region.COMMON;
        }
        char last = Character.toUpperCase(videoId.charAt(videoId.length() - 1));
        if (last == 'B') return Region.NORTH;
        if (last == 'T') return Region.CENTRAL;
        if (last == 'N') return Region.SOUTH;
        return Region.COMMON;
    }

    private String stripRegionSuffix(String videoId) {
        if (videoId.length() < 2) {
            return videoId;
        }
        char last = Character.toUpperCase(videoId.charAt(videoId.length() - 1));
        if (last == 'B' || last == 'T' || last == 'N') {
            return videoId.substring(0, videoId.length() - 1);
        }
        return videoId;
    }

    // ==================== VIDEO ====================

    @Override
    @Transactional(readOnly = true)
    public List<SignVideoResponse> getVideos(UUID signId) {
        return signVideoRepository.findAllBySignIdOrderByRegionAscViewAngleAsc(signId)
                .stream().map(signVideoMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public SignVideoResponse uploadVideo(UUID signId, MultipartFile file,
                                         Region region, ViewAngle viewAngle,
                                         String signerLabel, String captionVi) {
        Sign sign = signRepository.findById(signId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay tu vung: " + signId));

        if (file == null || file.isEmpty()) {
            throw new CommonException("File video rong");
        }

        String objectName = "signs/" + sign.getGloss() + "/"
                + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            minioService.upload(file, objectName);
        } catch (Exception e) {
            throw new CommonException("Tai video len MinIO that bai: " + e.getMessage(), e);
        }

        // Moi file deu di qua bang file_attachments - dung dung quy uoc da co cua du an
        FileAttachment attachment = fileRepository.save(FileAttachment.builder()
                .bucket(minioService.getBucketName())
                .objectKey(objectName)
                .originalName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .extension(extensionOf(file.getOriginalFilename()))
                .sizeBytes(file.getSize())
                .entityType(EntityType.SIGN_VIDEO)
                .build());

        SignVideo video = signVideoRepository.save(SignVideo.builder()
                .sign(sign)
                .file(attachment)
                .region(region == null ? Region.COMMON : region)
                .viewAngle(viewAngle == null ? ViewAngle.FRONT : viewAngle)
                .signerLabel(signerLabel)
                .captionVi(captionVi)
                .ingestStatus(IngestStatus.READY)
                .isPrimary(false)
                .build());

        // Gan entityId nguoc lai de tra cuu duoc hai chieu
        attachment.setEntityId(video.getId());
        fileRepository.save(attachment);

        // Video dau tien cua mot vung mien tu dong tro thanh video chinh
        if (signVideoRepository.findBySignIdAndRegionAndIsPrimaryTrue(signId, video.getRegion()).isEmpty()) {
            video.setIsPrimary(true);
            signVideoRepository.save(video);
        }

        return signVideoMapper.toResponse(video);
    }

    @Override
    @Transactional
    public SignVideoResponse setPrimaryVideo(UUID signId, UUID videoId) {
        SignVideo video = signVideoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay video: " + videoId));

        if (!video.getSign().getId().equals(signId)) {
            throw new ConflictException("Video khong thuoc tu vung nay");
        }

        // Phai go co primary cu TRUOC, vi DB co unique index uq_sign_videos_primary_per_region
        signVideoRepository.clearPrimaryExcept(signId, video.getRegion(), videoId);
        signVideoRepository.flush();

        video.setIsPrimary(true);
        return signVideoMapper.toResponse(signVideoRepository.save(video));
    }

    @Override
    @Transactional
    public void deleteVideo(UUID signId, UUID videoId) {
        SignVideo video = signVideoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay video: " + videoId));

        if (!video.getSign().getId().equals(signId)) {
            throw new ConflictException("Video khong thuoc tu vung nay");
        }

        // Xoa object tren MinIO truoc, roi moi xoa ban ghi
        FileAttachment file = video.getFile();
        if (file != null) {
            try {
                minioService.delete(file.getBucket(), file.getObjectKey());
            } catch (Exception e) {
                // Khong chan viec xoa ban ghi neu MinIO loi - ghi log de don sau
                log.warn("Khong xoa duoc object MinIO {}: {}", file.getObjectKey(), e.getMessage());
            }
        }
        signVideoRepository.delete(video);
    }

    // ==================== private helper ====================

    private SignResponse toListItem(Sign sign) {
        SignResponse response = signMapper.toResponse(sign);

        List<SignVideo> videos = signVideoRepository.findAllBySignIdOrderByRegionAscViewAngleAsc(sign.getId());
        response.setAvailableRegions(videos.stream()
                .map(SignVideo::getRegion)
                .distinct()
                .toList());

        videos.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsPrimary()) && v.getThumbnailFile() != null)
                .findFirst()
                .ifPresent(v -> response.setThumbnailUrl(v.getThumbnailFile().getPublicUrl()));

        response.setAiReady(!signRepository.findIdsHavingReadyExemplar(List.of(sign.getId())).isEmpty());
        response.setTopics(loadTopicRefs(sign.getId()));
        return response;
    }

    private SignResponse toDetail(Sign sign) {
        SignResponse response = toListItem(sign);
        response.setVideos(getVideos(sign.getId()));
        return response;
    }

    private List<SignResponse.TopicRefResponse> loadTopicRefs(UUID signId) {
        return signTopicRepository.findAllBySignId(signId).stream()
                .map(st -> SignResponse.TopicRefResponse.builder()
                        .id(st.getTopic().getId())
                        .slug(st.getTopic().getSlug())
                        .nameVi(st.getTopic().getNameVi())
                        .build())
                .toList();
    }

    private void applyTopics(Sign sign, List<UUID> topicIds) {
        signTopicRepository.deleteAllBySignId(sign.getId());
        signTopicRepository.flush();

        if (topicIds == null || topicIds.isEmpty()) {
            return;
        }
        List<SignTopic> links = topicIds.stream().distinct().map(topicId -> {
            Topic topic = topicRepository.findById(topicId)
                    .orElseThrow(() -> new NotFoundException("Khong tim thay chu de: " + topicId));
            return SignTopic.builder()
                    .id(new SignTopicId(sign.getId(), topic.getId()))
                    .sign(sign)
                    .topic(topic)
                    .build();
        }).toList();
        signTopicRepository.saveAll(links);
    }

    private void applyPrimaryTopic(Sign entity, SignRequest request) {
        if (request.getPrimaryTopicId() == null) {
            entity.setPrimaryTopic(null);
            return;
        }
        Topic topic = topicRepository.findById(request.getPrimaryTopicId())
                .orElseThrow(() -> new NotFoundException("Khong tim thay chu de chinh"));
        entity.setPrimaryTopic(topic);
    }

    private String resolveGloss(SignRequest request, UUID currentId) {
        String gloss = (request.getGloss() == null || request.getGloss().isBlank())
                ? VietnameseTextUtil.toGloss(request.getWordVi())
                : request.getGloss().trim();

        if (gloss.isBlank()) {
            throw new ConflictException("Khong sinh duoc gloss tu tu tieng Viet");
        }

        boolean duplicated = (currentId == null)
                ? signRepository.existsByGloss(gloss)
                : signRepository.existsByGlossAndIdNot(gloss, currentId);
        if (duplicated) {
            throw new ConflictException("Gloss da ton tai: " + gloss);
        }
        return gloss;
    }

    private void addEquals(List<FilterCriteria> filters, String field, Object value) {
        if (value == null) {
            return;
        }
        filters.add(FilterCriteria.builder()
                .fieldName(field)
                .operation(FilterOperation.EQUALS)
                .value(value)
                .logicType(FilterLogicType.AND)
                .build());
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new CommonException("Gia tri khong hop le cho " + type.getSimpleName() + ": " + raw);
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? null : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
