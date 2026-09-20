package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.dictionary.SignStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SignStepRepository extends JpaRepository<SignStep, UUID> {

    /**
     * LEFT JOIN FETCH ảnh ngay từ đầu: imageFile là LAZY, đụng vào trong vòng
     * lặp dựng DTO sẽ thành một truy vấn cho mỗi bước.
     */
    @Query("""
            SELECT s FROM SignStep s
              LEFT JOIN FETCH s.imageFile
             WHERE s.sign.id = :signId
             ORDER BY s.stepOrder
            """)
    List<SignStep> findBySignIdOrdered(@Param("signId") UUID signId);

    long countBySignId(UUID signId);
}
