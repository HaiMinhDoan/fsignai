package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.dictionary.SignTopic;
import com.sunmoon.backend.entity.dictionary.SignTopicId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SignTopicRepository extends JpaRepository<SignTopic, SignTopicId> {

    List<SignTopic> findAllBySignId(UUID signId);

    @Modifying
    @Query("DELETE FROM SignTopic st WHERE st.sign.id = :signId")
    void deleteAllBySignId(@Param("signId") UUID signId);

    /**
     * Từ vựng thuộc một chủ đề, sắp theo bảng chữ cái đã bỏ dấu.
     *
     * Sắp theo wordViUnaccent chứ không theo wordVi: PostgreSQL sắp chuỗi có dấu
     * theo collation của CSDL, dễ cho ra thứ tự lạ mắt với tiếng Việt. Cột bỏ dấu
     * đã được sinh sẵn ở tầng CSDL nên sắp theo nó vừa nhanh vừa đoán được.
     *
     * Dùng khi sinh khoá học tự động: thứ tự phải ỔN ĐỊNH để chạy lại nhiều lần
     * vẫn ra cùng một cách chia bài.
     */
    @Query("""
            SELECT st.sign.id
              FROM SignTopic st
             WHERE st.topic.id = :topicId
               AND (:onlyPublished = false OR st.sign.isPublished = true)
             ORDER BY st.sign.wordViUnaccent ASC, st.sign.id ASC
            """)
    List<UUID> findSignIdsByTopicId(@Param("topicId") UUID topicId,
                                    @Param("onlyPublished") boolean onlyPublished);

    List<SignTopic> findAllBySignIdIn(List<UUID> signIds);

    @Modifying
    @Query("DELETE FROM SignTopic st WHERE st.sign.id IN :signIds")
    void deleteAllBySignIdIn(@Param("signIds") List<UUID> signIds);

    /** Đếm từ vựng theo từng chủ đề trong một truy vấn, tránh N+1 khi dựng cây chủ đề */
    @Query("""
            SELECT st.topic.id, COUNT(st.sign.id)
              FROM SignTopic st
             GROUP BY st.topic.id
            """)
    List<Object[]> countSignsPerTopic();
}
