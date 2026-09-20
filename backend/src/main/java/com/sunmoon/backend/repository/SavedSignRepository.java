package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.dictionary.SavedSign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedSignRepository extends JpaRepository<SavedSign, UUID> {

    Optional<SavedSign> findByUserIdAndSignId(UUID userId, UUID signId);

    boolean existsByUserIdAndSignId(UUID userId, UUID signId);

    Page<SavedSign> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    void deleteByUserIdAndSignId(UUID userId, UUID signId);
}
