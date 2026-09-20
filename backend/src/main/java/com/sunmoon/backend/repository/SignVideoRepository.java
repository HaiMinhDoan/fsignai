package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.entity.dictionary.SignVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SignVideoRepository extends JpaRepository<SignVideo, UUID>, JpaSpecificationExecutor<SignVideo> {

    List<SignVideo> findAllBySignIdOrderByRegionAscViewAngleAsc(UUID signId);

    Optional<SignVideo> findBySignIdAndRegionAndIsPrimaryTrue(UUID signId, Region region);

    boolean existsBySourceUrl(String sourceUrl);

    // Go co primary cua cac video khac cung (sign, region) truoc khi dat primary moi.
    // DB co unique index uq_sign_videos_primary_per_region, khong go truoc se vi pham.
    @Modifying
    @Query("""
            UPDATE SignVideo v SET v.isPrimary = false
            WHERE v.sign.id = :signId AND v.region = :region AND v.id <> :keepId
            """)
    int clearPrimaryExcept(@Param("signId") UUID signId,
                           @Param("region") Region region,
                           @Param("keepId") UUID keepId);

    // Vung mien nao da co video - phuc vu bo loc "vung mien con thieu video"
    @Query("SELECT DISTINCT v.region FROM SignVideo v WHERE v.sign.id = :signId")
    List<Region> findRegionsBySignId(@Param("signId") UUID signId);

    /**
     * Video chính của NHIỀU từ trong một truy vấn.
     *
     * Bài học có thể chứa vài chục từ; nếu tra video cho từng từ thì một lần mở
     * bài học sẽ bắn vài chục truy vấn. Trả về cả các vùng miền rồi để service
     * chọn (ưu tiên COMMON) vì mỗi vùng miền có một video chính riêng.
     */
    @Query("""
            SELECT v FROM SignVideo v
              JOIN FETCH v.file
              LEFT JOIN FETCH v.thumbnailFile
             WHERE v.sign.id IN :signIds
               AND v.isPrimary = true
               AND v.file IS NOT NULL
            """)
    List<SignVideo> findPrimaryVideosBySignIds(@Param("signIds") List<UUID> signIds);

    /** Video kèm file và từ — dùng khi sinh exemplar, nơi chạy ngoài transaction sẽ gặp lazy-loading */
    @Query("""
            SELECT v FROM SignVideo v
              JOIN FETCH v.file
              JOIN FETCH v.sign
             WHERE v.id = :id
            """)
    Optional<SignVideo> findWithFileById(@Param("id") UUID id);
}
