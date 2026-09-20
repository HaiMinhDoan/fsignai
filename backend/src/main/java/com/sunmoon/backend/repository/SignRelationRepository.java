package com.sunmoon.backend.repository;

import com.sunmoon.backend.constant.enums.SignRelationType;
import com.sunmoon.backend.entity.dictionary.SignRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SignRelationRepository extends JpaRepository<SignRelation, UUID> {

    List<SignRelation> findAllBySignId(UUID signId);

    /**
     * Các từ dễ nhầm với từ đã cho — dùng làm đáp án nhiễu chất lượng cao.
     *
     * Quan hệ khai báo một chiều trong bảng nhưng ý nghĩa là hai chiều: A dễ nhầm
     * với B thì B cũng dễ nhầm với A. Nên truy cả hai cột thay vì bắt người nhập
     * liệu phải khai hai dòng.
     */
    @Query("""
            SELECT CASE WHEN r.sign.id = :signId THEN r.relatedSign.id ELSE r.sign.id END
              FROM SignRelation r
             WHERE (r.sign.id = :signId OR r.relatedSign.id = :signId)
               AND r.relationType = :type
            """)
    List<UUID> findRelatedSignIds(@Param("signId") UUID signId,
                                  @Param("type") SignRelationType type);
}
