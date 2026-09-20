package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.catalog.WordPack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WordPackRepository extends JpaRepository<WordPack, UUID>, JpaSpecificationExecutor<WordPack> {

    Optional<WordPack> findByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    boolean existsByCode(String code);

    List<WordPack> findAllByIsPublishedTrueOrderByDisplayOrderAsc();

    /** Gói khác đang lấy gói này làm điều kiện mở khoá — chặn xoá nếu có, kẻo mồ côi tham chiếu */
    boolean existsByUnlockAfterPackId(UUID packId);
}
