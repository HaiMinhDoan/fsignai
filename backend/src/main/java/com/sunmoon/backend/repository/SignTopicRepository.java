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
}
