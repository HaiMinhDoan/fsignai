package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.dictionary.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID>, JpaSpecificationExecutor<Topic> {

    Optional<Topic> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    List<Topic> findAllByParentIsNullOrderByDisplayOrderAsc();

    List<Topic> findAllByParentIdOrderByDisplayOrderAsc(UUID parentId);

    long countByParentId(UUID parentId);
}
