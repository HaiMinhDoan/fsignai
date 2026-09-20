package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.catalog.WordPackItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WordPackItemRepository extends JpaRepository<WordPackItem, UUID> {

    List<WordPackItem> findAllByPackIdOrderByDisplayOrderAsc(UUID packId);

    long countByPackId(UUID packId);

    boolean existsByPackIdAndSignId(UUID packId, UUID signId);

    void deleteAllByPackId(UUID packId);

    @Query("SELECT COALESCE(MAX(i.displayOrder), -1) + 1 FROM WordPackItem i WHERE i.pack.id = :packId")
    int nextDisplayOrder(@Param("packId") UUID packId);

    /** Từ đã nằm trong gói — dùng để loại khỏi danh sách chọn thêm */
    @Query("SELECT i.sign.id FROM WordPackItem i WHERE i.pack.id = :packId")
    List<UUID> findSignIdsByPackId(@Param("packId") UUID packId);

    /**
     * Đếm gộp theo từng gói trong một truy vấn — mapper gọi count() theo
     * từng dòng cho một danh sách 20 gói sẽ bắn 20 truy vấn con (N+1).
     */
    @Query("SELECT i.pack.id, COUNT(i.id) FROM WordPackItem i WHERE i.pack.id IN :packIds GROUP BY i.pack.id")
    List<Object[]> countByPackIds(@Param("packIds") List<UUID> packIds);
}
