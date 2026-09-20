package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.practice.FlashcardReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlashcardReviewRepository extends JpaRepository<FlashcardReview, UUID> {

    Optional<FlashcardReview> findByUserIdAndSignId(UUID userId, UUID signId);

    /** Thẻ đến hạn ôn, thẻ hạn xa nhất... à không, hạn GẦN nhất lên trước - đến hạn lâu nhất là cần ôn nhất */
    @Query("""
            SELECT fr FROM FlashcardReview fr
            WHERE fr.user.id = :userId AND fr.dueAt <= :now
              AND (:topicId IS NULL OR fr.sign.primaryTopic.id = :topicId)
            ORDER BY fr.dueAt ASC
            """)
    List<FlashcardReview> findDue(@Param("userId") UUID userId, @Param("topicId") UUID topicId,
                                   @Param("now") OffsetDateTime now, Pageable pageable);

    /**
     * Số từ người dùng THỰC SỰ đã học — mỗi (user, sign) chỉ có một dòng nên
     * đếm dòng là đếm đúng số từ khác nhau, không cần DISTINCT. Dùng cho huy
     * hiệu "signs_learned"; KHÔNG dùng tổng daily_activity.signs_reviewed vì
     * một từ ôn lại nhiều lần sẽ bị đếm trùng ở đó.
     */
    long countByUserIdAndRepetitionsGreaterThanEqual(UUID userId, int minRepetitions);
}
