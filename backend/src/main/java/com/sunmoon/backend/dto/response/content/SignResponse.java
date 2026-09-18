package com.sunmoon.backend.dto.response.content;

import com.sunmoon.backend.constant.enums.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Dung cho CA bang danh sach lan trang chi tiet.
// Truong nao chi co o trang chi tiet thi de null khi tra ve tu danh sach,
// nho spring.jackson.default-property-inclusion=non_null nen khong lot ra JSON.
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignResponse {

    UUID id;
    String gloss;
    String wordVi;
    String wordEn;
    String descriptionVi;
    String noteVi;

    SignLevel level;
    UnitType unitType;
    WordType wordType;
    WordSubtype wordSubtype;
    SignDomain domain;
    Short handCount;

    UUID primaryTopicId;
    String primaryTopicNameVi;
    List<TopicRefResponse> topics;

    SignSource source;
    String sourceRef;

    Boolean isPublished;
    ReviewStatus reviewStatus;
    OffsetDateTime reviewedAt;

    // Anh dai dien lay tu video chinh - cot thumbnail cua bang
    String thumbnailUrl;

    // Ba cham tron B / T / N tren bang: vung mien nao da co video
    List<Region> availableRegions;

    // Da co exemplar READY chua - cot "Exemplar" tren bang
    Boolean aiReady;

    // Chi co o trang chi tiet
    List<SignVideoResponse> videos;
    List<SignRelationResponse> relations;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TopicRefResponse {
        UUID id;
        String slug;
        String nameVi;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SignRelationResponse {
        UUID id;
        UUID relatedSignId;
        String relatedGloss;
        String relatedWordVi;
        SignRelationType relationType;
    }
}
