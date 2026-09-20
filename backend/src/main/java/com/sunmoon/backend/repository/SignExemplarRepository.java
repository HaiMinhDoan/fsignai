package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.ExemplarBuildStatus;
import com.sunmoon.backend.entity.ai.SignExemplar;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SignExemplarRepository extends JpaRepository<SignExemplar, UUID> {

    @Query("""
            SELECT e FROM SignExemplar e
              LEFT JOIN FETCH e.landmarkFile
            WHERE e.sign.id = :signId
            ORDER BY e.region, e.createdAt
            """)
    List<SignExemplar> findAllBySign(@Param("signId") UUID signId);

    /** Mẫu dùng được để chấm: đúng phiên bản đặc trưng hiện hành, đã dựng xong, đang bật */
    @Query("""
            SELECT e FROM SignExemplar e
              JOIN FETCH e.landmarkFile
            WHERE e.sign.id = :signId
              AND e.isActive = true
              AND e.buildStatus = com.sunmoon.backend.constant.enums.ExemplarBuildStatus.READY
              AND e.modelVersion = :version
            ORDER BY e.createdAt
            """)
    List<SignExemplar> findUsable(@Param("signId") UUID signId, @Param("version") String version);

    @Query("""
            SELECT COUNT(e) FROM SignExemplar e
            WHERE e.sign.id = :signId AND e.isActive = true
              AND e.buildStatus = com.sunmoon.backend.constant.enums.ExemplarBuildStatus.READY
              AND e.modelVersion = :version
            """)
    long countUsable(@Param("signId") UUID signId, @Param("version") String version);

    Optional<SignExemplar> findBySignVideoIdAndModelVersion(UUID signVideoId, String modelVersion);

    long countByBuildStatusAndModelVersion(ExemplarBuildStatus status, String modelVersion);

    @Query("""
            SELECT COUNT(DISTINCT e.sign.id) FROM SignExemplar e
            WHERE e.isActive = true
              AND e.buildStatus = com.sunmoon.backend.constant.enums.ExemplarBuildStatus.READY
              AND e.modelVersion = :version
            """)
    long countSignsReady(@Param("version") String version);

    /**
     * Video còn thiếu mẫu ở phiên bản hiện hành. `retryFailed` = false thì bỏ qua video từng hỏng để
     * một video xấu không bị thử đi thử lại mỗi lần chạy lô.
     */
    @Query("""
            SELECT v.id FROM SignVideo v
            WHERE v.file IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM SignExemplar e
                  WHERE e.signVideo = v AND e.modelVersion = :version
                    AND (e.buildStatus = com.sunmoon.backend.constant.enums.ExemplarBuildStatus.READY
                         OR (:retryFailed = false
                             AND e.buildStatus = com.sunmoon.backend.constant.enums.ExemplarBuildStatus.FAILED))
              )
            ORDER BY v.createdAt
            """)
    List<UUID> findVideoIdsNeedingExemplar(@Param("version") String version,
                                            @Param("retryFailed") boolean retryFailed,
                                            Pageable pageable);

    @Query("""
            SELECT COUNT(v) FROM SignVideo v
            WHERE v.file IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM SignExemplar e
                  WHERE e.signVideo = v AND e.modelVersion = :version
                    AND (e.buildStatus = com.sunmoon.backend.constant.enums.ExemplarBuildStatus.READY
                         OR (:retryFailed = false
                             AND e.buildStatus = com.sunmoon.backend.constant.enums.ExemplarBuildStatus.FAILED))
              )
            """)
    long countVideosNeedingExemplar(@Param("version") String version, @Param("retryFailed") boolean retryFailed);

    /** Sau khi dựng xong mẫu mới cho video, tắt các mẫu phiên bản cũ của cùng video */
    @Modifying
    @Query("""
            UPDATE SignExemplar e SET e.isActive = false
            WHERE e.signVideo.id = :videoId AND e.modelVersion <> :version
            """)
    int deactivateOtherVersions(@Param("videoId") UUID videoId, @Param("version") String version);
}
