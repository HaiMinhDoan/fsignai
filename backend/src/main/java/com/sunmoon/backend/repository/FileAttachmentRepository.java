package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileAttachmentRepository
        extends JpaRepository<FileAttachment, UUID>, JpaSpecificationExecutor<FileAttachment> {

    List<FileAttachment> findAllByEntityTypeAndEntityId(String entityType, UUID entityId);
}
