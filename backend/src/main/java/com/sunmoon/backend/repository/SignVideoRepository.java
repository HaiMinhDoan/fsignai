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
}
