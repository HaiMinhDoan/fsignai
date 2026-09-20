package com.sunmoon.backend.mapper;

import com.sunmoon.backend.dto.request.catalog.WordPackRequest;
import com.sunmoon.backend.dto.response.catalog.WordPackItemResponse;
import com.sunmoon.backend.dto.response.catalog.WordPackResponse;
import com.sunmoon.backend.entity.catalog.WordPack;
import com.sunmoon.backend.entity.catalog.WordPackItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WordPackMapper {

    @Mapping(target = "coverFileId", source = "coverFile.id")
    @Mapping(target = "coverUrl", expression = "java(entity.getCoverFile() == null ? null : entity.getCoverFile().getPublicUrl())")
    @Mapping(target = "topicId", source = "topic.id")
    @Mapping(target = "topicNameVi", source = "topic.nameVi")
    @Mapping(target = "unlockAfterPackId", source = "unlockAfterPack.id")
    @Mapping(target = "unlockAfterPackTitleVi", source = "unlockAfterPack.titleVi")
    // Service tự điền để gom thành một truy vấn thay vì đếm/truy vấn theo từng dòng
    @Mapping(target = "itemCount", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "unlocked", ignore = true)
    @Mapping(target = "myStatus", ignore = true)
    @Mapping(target = "myItemsCompleted", ignore = true)
    @Mapping(target = "myStars", ignore = true)
    WordPackResponse toResponse(WordPack entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "coverFile", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "unlockAfterPack", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    WordPack toEntity(WordPackRequest request);

    // null trong request KHÔNG ghi đè giá trị cũ
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "coverFile", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "unlockAfterPack", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget WordPack entity, WordPackRequest request);

    @Mapping(target = "signId", source = "sign.id")
    @Mapping(target = "signWordVi", source = "sign.wordVi")
    @Mapping(target = "signGloss", source = "sign.gloss")
    @Mapping(target = "signPrimaryVideoUrl", ignore = true)
    @Mapping(target = "signThumbnailUrl", ignore = true)
    WordPackItemResponse toItemResponse(WordPackItem entity);
}
