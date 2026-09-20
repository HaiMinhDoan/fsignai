package com.sunmoon.backend.mapper;

import com.sunmoon.backend.dto.request.catalog.LessonRequest;
import com.sunmoon.backend.dto.response.catalog.LessonResponse;
import com.sunmoon.backend.entity.catalog.Lesson;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LessonMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseTitleVi", source = "course.titleVi")
    @Mapping(target = "itemCount", ignore = true)
    @Mapping(target = "items", ignore = true)
    LessonResponse toResponse(Lesson entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "generated", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Lesson toEntity(LessonRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "generated", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Lesson entity, LessonRequest request);
}
