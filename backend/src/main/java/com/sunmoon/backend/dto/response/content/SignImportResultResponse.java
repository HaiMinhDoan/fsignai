package com.sunmoon.backend.dto.response.content;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignImportResultResponse {

    Boolean dryRun;
    Integer totalRows;
    Integer createdCount;
    Integer updatedCount;
    Integer skippedCount;
    Integer failedCount;

    // Chi tiet tung dong loi, de frontend to do dung dong do trong bang xem truoc
    @Builder.Default
    List<RowError> errors = new ArrayList<>();

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RowError {
        Integer rowNumber;
        String wordVi;
        String message;
    }
}
