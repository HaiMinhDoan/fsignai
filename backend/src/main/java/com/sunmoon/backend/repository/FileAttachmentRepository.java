package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileAttachmentRepository
        extends JpaRepository<FileAttachment, UUID>, JpaSpecificationExecutor<FileAttachment> {

    List<FileAttachment> findAllByEntityTypeAndEntityId(String entityType, UUID entityId);

    /**
     * Moi tep thuoc ve mot tu vung: video, anh dai dien video, anh cua tung buoc,
     * landmark cua exemplar va cua ket qua cham diem.
     *
     * Hoi thang qua khoa ngoai cua tung bang, KHONG loc theo entity_type/entity_id vi
     * hai cot do chi la ghi chu truy vet - du lieu crawl ve co dong bo trong, xoa theo
     * chung se bo sot tep that.
     *
     * Goi TRUOC khi xoa sign: xoa roi thi cac bang con da cascade mat, khong con duong
     * nao lan ra cac tep nay nua.
     */
    @Query(value = """
            SELECT fa.* FROM file_attachments fa WHERE fa.id IN (
                SELECT sv.file_id           FROM sign_videos sv      WHERE sv.sign_id = :signId
                UNION SELECT sv.thumbnail_file_id FROM sign_videos sv      WHERE sv.sign_id = :signId
                UNION SELECT st.image_file_id     FROM sign_steps st       WHERE st.sign_id = :signId
                UNION SELECT se.landmark_file_id  FROM sign_exemplars se   WHERE se.sign_id = :signId
                UNION SELECT ar.landmark_file_id  FROM ai_check_results ar WHERE ar.sign_id = :signId
            )""", nativeQuery = true)
    List<FileAttachment> findAllOwnedBySign(@Param("signId") UUID signId);
}
