package com.sunmoon.backend.mapper;

import com.sunmoon.backend.dto.request.catalog.LessonItemRequest;
import com.sunmoon.backend.dto.response.catalog.LessonItemResponse;
import com.sunmoon.backend.entity.catalog.LessonItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LessonItemMapper {

    @Mapping(target = "lessonId", source = "lesson.id")
    @Mapping(target = "signId", source = "sign.id")
    @Mapping(target = "signWordVi", source = "sign.wordVi")
    @Mapping(target = "signGloss", source = "sign.gloss")
    @Mapping(target = "quizId", source = "quiz.id")
    @Mapping(target = "quizTitleVi", source = "quiz.titleVi")
    // Video chính phải tra riêng trong bảng sign_videos, service tự điền
    @Mapping(target = "signPrimaryVideoUrl", ignore = true)
    LessonItemResponse toResponse(LessonItem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "sign", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    LessonItem toEntity(LessonItemRequest request);
}
