package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.dictionary.Sign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// JpaSpecificationExecutor la BAT BUOC: BaseServiceImpl ep kieu sang no
// de dung filter dong. Thieu interface nay se nem IllegalArgumentException
// ngay luc khoi tao service.
@Repository
public interface SignRepository extends JpaRepository<Sign, UUID>, JpaSpecificationExecutor<Sign> {

    Optional<Sign> findByGloss(String gloss);

    boolean existsByGloss(String gloss);

    boolean existsByGlossAndIdNot(String gloss, UUID id);

    List<Sign> findAllByGlossIn(List<String> glosses);

    // Dem so tu theo tung chu de, phuc vu canh bao "kho tu khong du sinh de"
    @Query("""
            SELECT COUNT(s) FROM Sign s
            JOIN SignTopic st ON st.sign = s
            WHERE st.topic.id = :topicId AND s.isPublished = true
            """)
    long countPublishedByTopic(@Param("topicId") UUID topicId);

    // Tu da co exemplar san sang cham diem AI
    @Query("""
            SELECT DISTINCT s.id FROM Sign s
            JOIN SignExemplar e ON e.sign = s
            WHERE e.isActive = true AND e.buildStatus = 'READY' AND s.id IN :signIds
            """)
    List<UUID> findIdsHavingReadyExemplar(@Param("signIds") List<UUID> signIds);
}
