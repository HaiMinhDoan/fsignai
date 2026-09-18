package com.sunmoon.backend.mapper;

import com.sunmoon.backend.dto.request.content.SignRequest;
import com.sunmoon.backend.dto.response.content.SignResponse;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.entity.dictionary.SignRelation;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        uses = SignVideoMapper.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SignMapper {

    @Mapping(target = "primaryTopicId", source = "primaryTopic.id")
    @Mapping(target = "primaryTopicNameVi", source = "primaryTopic.nameVi")
    // Cac truong tong hop duoi day do service tinh va gan sau, vi chung can
    // truy van them (video, exemplar, chu de nhieu-nhieu)
    @Mapping(target = "topics", ignore = true)
    @Mapping(target = "videos", ignore = true)
    @Mapping(target = "relations", ignore = true)
    @Mapping(target = "thumbnailUrl", ignore = true)
    @Mapping(target = "availableRegions", ignore = true)
    @Mapping(target = "aiReady", ignore = true)
    SignResponse toResponse(Sign entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "primaryTopic", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "reviewStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "wordViUnaccent", ignore = true)
    @Mapping(target = "searchVector", ignore = true)
    Sign toEntity(SignRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "primaryTopic", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "reviewStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "wordViUnaccent", ignore = true)
    @Mapping(target = "searchVector", ignore = true)
    void updateEntity(@MappingTarget Sign entity, SignRequest request);

    @Mapping(target = "relatedSignId", source = "relatedSign.id")
    @Mapping(target = "relatedGloss", source = "relatedSign.gloss")
    @Mapping(target = "relatedWordVi", source = "relatedSign.wordVi")
    SignResponse.SignRelationResponse toRelationResponse(SignRelation entity);
}
