package com.sunmoon.backend.mapper;

import com.sunmoon.backend.dto.response.content.SignVideoResponse;
import com.sunmoon.backend.entity.dictionary.SignVideo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SignVideoMapper {

    @Mapping(target = "signId", source = "sign.id")
    @Mapping(target = "videoUrl", expression = "java(entity.getFile() == null ? null : entity.getFile().getPublicUrl())")
    @Mapping(target = "thumbnailUrl", expression = "java(entity.getThumbnailFile() == null ? null : entity.getThumbnailFile().getPublicUrl())")
    SignVideoResponse toResponse(SignVideo entity);
}
