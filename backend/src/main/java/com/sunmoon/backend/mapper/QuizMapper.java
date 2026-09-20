package com.sunmoon.backend.mapper;

import com.sunmoon.backend.dto.request.practice.QuizRequest;
import com.sunmoon.backend.dto.response.practice.QuizResponse;
import com.sunmoon.backend.entity.practice.Quiz;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface QuizMapper {

    @Mapping(target = "lessonId", source = "lesson.id")
    @Mapping(target = "lessonTitleVi", source = "lesson.titleVi")
    @Mapping(target = "courseId", source = "lesson.course.id")
    @Mapping(target = "courseTitleVi", source = "lesson.course.titleVi")
    @Mapping(target = "topicId", source = "topic.id")
    @Mapping(target = "topicNameVi", source = "topic.nameVi")
    @Mapping(target = "questionCount", ignore = true)
    @Mapping(target = "questions", ignore = true)
    QuizResponse toResponse(Quiz entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Quiz toEntity(QuizRequest request);

    // null trong request KHÔNG ghi đè giá trị cũ
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Quiz entity, QuizRequest request);
}
