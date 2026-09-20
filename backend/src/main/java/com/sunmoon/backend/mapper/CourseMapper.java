package com.sunmoon.backend.mapper;

import com.sunmoon.backend.dto.request.catalog.CourseRequest;
import com.sunmoon.backend.dto.response.catalog.CourseResponse;
import com.sunmoon.backend.entity.catalog.Course;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CourseMapper {

    @Mapping(target = "coverFileId", source = "coverFile.id")
    @Mapping(target = "coverUrl", expression = "java(entity.getCoverFile() == null ? null : entity.getCoverFile().getPublicUrl())")
    @Mapping(target = "topicId", source = "topic.id")
    @Mapping(target = "topicNameVi", source = "topic.nameVi")
    // Service tự điền để gom thành một truy vấn thay vì đếm theo từng dòng
    @Mapping(target = "lessonCount", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    CourseResponse toResponse(Course entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "coverFile", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "generated", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Course toEntity(CourseRequest request);

    // null trong request KHÔNG ghi đè giá trị cũ
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "coverFile", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "generated", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Course entity, CourseRequest request);
}
