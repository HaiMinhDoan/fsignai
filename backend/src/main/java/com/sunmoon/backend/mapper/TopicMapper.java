package com.sunmoon.backend.mapper;

import com.sunmoon.backend.dto.request.content.TopicRequest;
import com.sunmoon.backend.dto.response.content.TopicResponse;
import com.sunmoon.backend.entity.dictionary.Topic;
import org.mapstruct.*;

// MapStruct sinh implementation luc bien dich - khong reflection, khong tra gia runtime.
// componentModel = "spring" de inject duoc bang @Autowired nhu bean thuong.
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TopicMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentNameVi", source = "parent.nameVi")
    @Mapping(target = "iconUrl", expression = "java(entity.getIconFile() == null ? null : entity.getIconFile().getPublicUrl())")
    @Mapping(target = "coverUrl", expression = "java(entity.getCoverFile() == null ? null : entity.getCoverFile().getPublicUrl())")
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "signCount", ignore = true)
    TopicResponse toResponse(Topic entity);

    // Quan he va khoa chinh do service tu gan, mapper khong dung toi
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "iconFile", ignore = true)
    @Mapping(target = "coverFile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Topic toEntity(TopicRequest request);

    // Cap nhat tai cho: null trong request se KHONG ghi de gia tri cu
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "iconFile", ignore = true)
    @Mapping(target = "coverFile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Topic entity, TopicRequest request);
}
