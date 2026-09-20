package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.practice.QuizBlueprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizBlueprintRepository
        extends JpaRepository<QuizBlueprint, UUID>, JpaSpecificationExecutor<QuizBlueprint> {

    Optional<QuizBlueprint> findByCode(String code);

    boolean existsByCode(String code);

    /** Dùng khi đổi mã: bỏ qua chính bản ghi đang sửa */
    boolean existsByCodeAndIdNot(String code, UUID id);

    List<QuizBlueprint> findAllByIsActiveTrueOrderByTitleViAsc();
}
