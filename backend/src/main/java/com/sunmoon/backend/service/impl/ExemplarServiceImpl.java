package com.sunmoon.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunmoon.backend.constant.enums.ExemplarBuildStatus;
import com.sunmoon.backend.dto.response.ai.ExemplarJobStatusResponse;
import com.sunmoon.backend.dto.response.ai.ExemplarResponse;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.ai.SignExemplar;
import com.sunmoon.backend.entity.dictionary.SignVideo;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.FileAttachmentRepository;
import com.sunmoon.backend.repository.SignExemplarRepository;
import com.sunmoon.backend.repository.SignRepository;
import com.sunmoon.backend.repository.SignVideoRepository;
import com.sunmoon.backend.service.ExemplarService;
import com.sunmoon.backend.service.MinioService;
import com.sunmoon.backend.service.support.AiServiceClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Service
public class ExemplarServiceImpl implements ExemplarService {

    /** Chuỗi đặc trưng của mẫu và người học đều dài 32 bước, 134 chiều (xem ai-service/app/features.py) */
    private static final int FRAMES = 32;
    private static final int DIM = 134;

    /** Mẫu mà MediaPipe theo dõi kém (tay bị che, ngoài khung) sẽ làm mọi người học chấm sai — loại ngay từ đầu */
    private static final double MIN_QUALITY = 0.5;

    private static final String ENTITY_TYPE = "SIGN_EXEMPLAR";
    private static final int MAX_CACHE = 3000;
    private static final int MAX_JOB_SIZE = 6000;

    private final SignExemplarRepository exemplarRepository;
    private final SignVideoRepository videoRepository;
    private final SignRepository signRepository;
    private final FileAttachmentRepository fileRepository;
    private final MinioService minioService;
    private final AiServiceClient aiClient;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;
    private final String featureVersion;
    private final int jobWorkers;

    /** Mẫu đã giải nén, giữ trong RAM: mỗi lần chấm không phải tải lại từ MinIO. Truy cập-gần-nhất bị loại đầu tiên. */
    private final Map<UUID, double[][]> cache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, double[][]> eldest) {
            return size() > MAX_CACHE;
        }
    };

    private final ExecutorService jobExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "exemplar-build");
        t.setDaemon(true);
        return t;
    });
    private final JobState job = new JobState();

    public ExemplarServiceImpl(SignExemplarRepository exemplarRepository, SignVideoRepository videoRepository,
                               SignRepository signRepository, FileAttachmentRepository fileRepository,
                               MinioService minioService, AiServiceClient aiClient, ObjectMapper mapper,
                               TransactionTemplate tx,
                               @Value("${ai.feature-version}") String featureVersion,
                               @Value("${ai.exemplar-job.workers:4}") int jobWorkers) {
        this.exemplarRepository = exemplarRepository;
        this.videoRepository = videoRepository;
        this.signRepository = signRepository;
        this.fileRepository = fileRepository;
        this.minioService = minioService;
        this.aiClient = aiClient;
        this.mapper = mapper;
        this.tx = tx;
        this.featureVersion = featureVersion;
        this.jobWorkers = Math.max(1, jobWorkers);
    }

    // ------------------------------------------------------------------ đọc

    @Override
    public List<ExemplarResponse> listForSign(UUID signId) {
        return exemplarRepository.findAllBySign(signId).stream().map(this::toResponse).toList();
    }

    @Override
    public int usableCount(UUID signId) {
        return (int) exemplarRepository.countUsable(signId, featureVersion);
    }

    @Override
    public List<LoadedExemplar> loadUsable(UUID signId) {
        return exemplarRepository.findUsable(signId, featureVersion).stream()
                .map(e -> new LoadedExemplar(e.getId(), e.getRegion(), features(e)))
                .toList();
    }

    private double[][] features(SignExemplar e) {
        synchronized (cache) {
            double[][] hit = cache.get(e.getId());
            if (hit != null) return hit;
        }
        double[][] loaded = readFeatures(e.getLandmarkFile());
        synchronized (cache) {
            cache.put(e.getId(), loaded);
        }
        return loaded;
    }

    private double[][] readFeatures(FileAttachment file) {
        try {
            byte[] gz = minioService.download(file.getBucket(), file.getObjectKey());
            try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(gz))) {
                JsonNode features = mapper.readTree(in).get("features");
                double[][] out = new double[features.size()][];
                for (int i = 0; i < out.length; i++) {
                    JsonNode row = features.get(i);
                    out[i] = new double[row.size()];
                    for (int j = 0; j < out[i].length; j++) out[i][j] = row.get(j).asDouble();
                }
                if (out.length != FRAMES || out[0].length != DIM) {
                    throw new IllegalStateException("Kích thước mẫu " + out.length + "x" + out[0].length);
                }
                return out;
            }
        } catch (Exception ex) {
            throw new CommonException("Không đọc được mẫu chấm điểm: " + ex.getMessage(), ex);
        }
    }

    // ------------------------------------------------------------------ sinh mẫu

    @Override
    public List<ExemplarResponse> rebuildForSign(UUID signId) {
        signRepository.findById(signId).orElseThrow(() -> new NotFoundException("Không tìm thấy từ này"));
        List<SignVideo> videos = videoRepository.findAllBySignIdOrderByRegionAscViewAngleAsc(signId).stream()
                .filter(v -> v.getFile() != null).toList();
        if (videos.isEmpty()) {
            throw badRequest("Từ này chưa có video nào để sinh mẫu");
        }
        for (SignVideo v : videos) {
            buildOne(v.getId());
        }
        return listForSign(signId);
    }

    @Override
    public void setActive(UUID exemplarId, boolean active) {
        SignExemplar e = exemplarRepository.findById(exemplarId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mẫu"));
        e.setIsActive(active);
        e.setUpdatedAt(OffsetDateTime.now());
        exemplarRepository.save(e);
        synchronized (cache) {
            cache.remove(exemplarId);
        }
    }

    /**
     * Dựng mẫu cho một video. Trả về true nếu thành công. KHÔNG ném lỗi: một video hỏng chỉ đánh dấu FAILED
     * và ghi lý do, để job nền không dừng giữa chừng vì một video.
     *
     * Cố ý không bọc cả hàm trong một transaction: cuộc gọi sang ai-service mất vài giây, giữ kết nối CSDL
     * suốt thời gian đó sẽ cạn pool khi job chạy nền song song với người dùng.
     */
    private boolean buildOne(UUID videoId) {
        SignVideo video = videoRepository.findWithFileById(videoId).orElse(null);
        if (video == null) return false;

        SignExemplar ex = exemplarRepository.findBySignVideoIdAndModelVersion(videoId, featureVersion)
                .orElseGet(() -> SignExemplar.builder()
                        .sign(video.getSign()).signVideo(video).region(video.getRegion())
                        .modelVersion(featureVersion).build());
        ex.setBuildStatus(ExemplarBuildStatus.PROCESSING);
        ex.setBuildError(null);
        ex.setUpdatedAt(OffsetDateTime.now());
        ex = exemplarRepository.save(ex);

        try {
            byte[] bytes = minioService.download(video.getFile().getBucket(), video.getFile().getObjectKey());
            AiServiceClient.ExtractResult result = aiClient.extract(bytes, filenameOf(video));

            if (!featureVersion.equals(result.featureVersion())) {
                throw new IllegalStateException("ai-service dùng đặc trưng " + result.featureVersion()
                        + " nhưng backend cấu hình " + featureVersion + " (ai.feature-version)");
            }
            if (result.quality() < MIN_QUALITY) {
                throw new IllegalStateException(String.format(
                        "Chất lượng theo dõi thấp (%.2f) — tay/người bị che hoặc ngoài khung", result.quality()));
            }

            FileAttachment file = storeFeatures(video, ex, result);
            finish(ex.getId(), file, result);
            synchronized (cache) {
                cache.remove(ex.getId());
            }
            return true;
        } catch (Exception e) {
            log.warn("Sinh mẫu cho video {} thất bại: {}", videoId, e.getMessage());
            fail(ex.getId(), e.getMessage());
            return false;
        }
    }

    private FileAttachment storeFeatures(SignVideo video, SignExemplar ex, AiServiceClient.ExtractResult result)
            throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(buf)) {
            mapper.writeValue(gz, Map.of(
                    "version", result.featureVersion(),
                    "handCount", result.handCount(),
                    "features", result.features()));
        }
        byte[] bytes = buf.toByteArray();
        String key = "ai/exemplars/" + video.getSign().getId() + "/" + video.getId() + "-" + featureVersion + ".json.gz";
        String bucket = minioService.getBucketName();

        minioService.getMinioClient().putObject(PutObjectArgs.builder()
                .bucket(bucket).object(key)
                .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                .contentType("application/gzip").build());

        // Dựng lại cùng video + cùng phiên bản → ghi đè cùng object, dùng lại bản ghi file_attachments
        // Chỉ lấy id của proxy lazy (không khởi tạo nó): ta đang chạy ngoài phiên Hibernate
        UUID existingFileId = ex.getLandmarkFile() != null ? ex.getLandmarkFile().getId() : null;
        FileAttachment file = existingFileId != null
                ? fileRepository.findById(existingFileId).orElseGet(FileAttachment::new)
                : new FileAttachment();
        file.setBucket(bucket);
        file.setObjectKey(key);
        file.setOriginalName(key.substring(key.lastIndexOf('/') + 1));
        file.setMimeType("application/gzip");
        file.setExtension("json.gz");
        file.setSizeBytes((long) bytes.length);
        file.setEntityType(ENTITY_TYPE);
        file.setEntityId(ex.getId());
        file.setUpdatedAt(OffsetDateTime.now());
        return fileRepository.save(file);
    }

    private void finish(UUID exemplarId, FileAttachment file, AiServiceClient.ExtractResult result) {
        tx.executeWithoutResult(status -> {
            SignExemplar e = exemplarRepository.findById(exemplarId).orElseThrow();
            e.setLandmarkFile(file);
            e.setFrameCount(FRAMES);
            e.setLandmarkDim(DIM);
            e.setQualityScore(BigDecimal.valueOf(result.quality()));
            e.setBuildStatus(ExemplarBuildStatus.READY);
            e.setBuildError(null);
            e.setIsActive(true);
            e.setUpdatedAt(OffsetDateTime.now());
            exemplarRepository.save(e);
            // Mẫu phiên bản cũ của cùng video không còn dùng để chấm
            if (e.getSignVideo() != null) {
                exemplarRepository.deactivateOtherVersions(e.getSignVideo().getId(), featureVersion);
            }
        });
    }

    private void fail(UUID exemplarId, String message) {
        String text = message == null ? "Lỗi không rõ" : message;
        if (text.length() > 500) text = text.substring(0, 500);
        final String error = text;
        tx.executeWithoutResult(status -> exemplarRepository.findById(exemplarId).ifPresent(e -> {
            e.setBuildStatus(ExemplarBuildStatus.FAILED);
            e.setBuildError(error);
            e.setUpdatedAt(OffsetDateTime.now());
            exemplarRepository.save(e);
        }));
    }

    // ------------------------------------------------------------------ job nền

    private static final class JobState {
        volatile boolean running;
        volatile int total;
        final AtomicInteger done = new AtomicInteger();
        final AtomicInteger failed = new AtomicInteger();
        volatile String message = "Chưa chạy lần nào";
        volatile OffsetDateTime startedAt;
        volatile OffsetDateTime finishedAt;
    }

    @Override
    public synchronized ExemplarJobStatusResponse startBuildJob(int limit, boolean retryFailed) {
        if (job.running) {
            CommonException ex = new CommonException("Đang có một lượt sinh mẫu chạy, chờ nó xong đã");
            ex.setHttpStatus(HttpStatus.CONFLICT);
            throw ex;
        }
        int size = Math.max(1, Math.min(limit, MAX_JOB_SIZE));
        List<UUID> ids = exemplarRepository.findVideoIdsNeedingExemplar(featureVersion, retryFailed, PageRequest.of(0, size));
        if (ids.isEmpty()) {
            job.message = "Mọi video đều đã có mẫu";
            return jobStatus();
        }

        job.running = true;
        job.total = ids.size();
        job.done.set(0);
        job.failed.set(0);
        job.startedAt = OffsetDateTime.now();
        job.finishedAt = null;
        job.message = "Đang chạy";

        jobExecutor.submit(() -> {
            // Mỗi video tốn ~2 giây tính toán nhưng còn hơn một giây nữa chỉ để tải video từ MinIO và
            // đẩy kết quả lên lại. Chạy tuần tự thì CPU ngồi chờ mạng gần nửa thời gian, nên dùng một
            // nhóm luồng nhỏ; trần song song thật sự do ai-service quyết (AI_MAX_CONCURRENCY).
            ExecutorService pool = Executors.newFixedThreadPool(jobWorkers);
            try {
                List<Future<?>> tasks = new ArrayList<>(ids.size());
                for (UUID id : ids) {
                    tasks.add(pool.submit(() -> {
                        if (!buildOne(id)) job.failed.incrementAndGet();
                        job.done.incrementAndGet();
                    }));
                }
                for (Future<?> f : tasks) {
                    f.get();
                }
                job.message = "Xong: " + (job.done.get() - job.failed.get()) + " thành công, " + job.failed.get() + " lỗi";
            } catch (Throwable t) {
                log.error("Job sinh mẫu dừng bất thường", t);
                job.message = "Dừng bất thường: " + t.getMessage();
            } finally {
                pool.shutdownNow();
                job.finishedAt = OffsetDateTime.now();
                job.running = false;
            }
        });
        return jobStatus();
    }

    @Override
    public ExemplarJobStatusResponse jobStatus() {
        return ExemplarJobStatusResponse.builder()
                .running(job.running)
                .total(job.total)
                .done(job.done.get())
                .failed(job.failed.get())
                .lastMessage(job.message)
                .startedAt(job.startedAt)
                .finishedAt(job.finishedAt)
                .featureVersion(featureVersion)
                .totalVideos(videoRepository.count())
                .readyExemplars(exemplarRepository.countByBuildStatusAndModelVersion(ExemplarBuildStatus.READY, featureVersion))
                .failedExemplars(exemplarRepository.countByBuildStatusAndModelVersion(ExemplarBuildStatus.FAILED, featureVersion))
                .signsReady(exemplarRepository.countSignsReady(featureVersion))
                .totalSigns(signRepository.count())
                .build();
    }

    // ------------------------------------------------------------------ phụ

    private ExemplarResponse toResponse(SignExemplar e) {
        return ExemplarResponse.builder()
                .id(e.getId())
                .signVideoId(e.getSignVideo() != null ? e.getSignVideo().getId() : null)
                .region(e.getRegion())
                .buildStatus(e.getBuildStatus())
                .buildError(e.getBuildError())
                .frameCount(e.getFrameCount())
                .qualityScore(e.getQualityScore())
                .modelVersion(e.getModelVersion())
                .isActive(e.getIsActive())
                .current(featureVersion.equals(e.getModelVersion()))
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private String filenameOf(SignVideo video) {
        String key = video.getFile().getObjectKey();
        return key.substring(key.lastIndexOf('/') + 1);
    }

    private CommonException badRequest(String message) {
        CommonException ex = new CommonException(message);
        ex.setHttpStatus(HttpStatus.BAD_REQUEST);
        return ex;
    }
}
