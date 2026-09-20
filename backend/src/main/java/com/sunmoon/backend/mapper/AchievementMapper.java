package com.sunmoon.backend.mapper;

import com.sunmoon.backend.dto.request.progress.AchievementRequest;
import com.sunmoon.backend.dto.response.progress.AchievementResponse;
import com.sunmoon.backend.entity.progress.Achievement;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AchievementMapper {

    @Mapping(target = "iconFileId", source = "iconFile.id")
    @Mapping(target = "iconUrl", source = "iconFile.publicUrl")
    @Mapping(target = "earned", ignore = true)
    @Mapping(target = "earnedAt", ignore = true)
    AchievementResponse toResponse(Achievement entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "iconFile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Achievement toEntity(AchievementRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "iconFile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Achievement entity, AchievementRequest request);
}
