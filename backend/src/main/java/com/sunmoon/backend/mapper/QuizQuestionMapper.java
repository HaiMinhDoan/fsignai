package com.sunmoon.backend.mapper;

import com.sunmoon.backend.dto.response.practice.QuizQuestionResponse;
import com.sunmoon.backend.entity.practice.QuizQuestion;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface QuizQuestionMapper {

    @Mapping(target = "quizId", source = "quiz.id")
    @Mapping(target = "signId", source = "sign.id")
    @Mapping(target = "signWordVi", source = "sign.wordVi")
    @Mapping(target = "signGloss", source = "sign.gloss")
    // options nằm trong cột jsonb và videoUrl phải tra bảng sign_videos,
    // cả hai đều do service điền
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "signVideoUrl", ignore = true)
    QuizQuestionResponse toResponse(QuizQuestion entity);
}
