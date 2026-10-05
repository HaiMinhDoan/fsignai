package com.sunmoon.backend.dto.request.content;

import com.sunmoon.backend.constant.enums.*;
import com.sunmoon.backend.dto.request.SortCriteria;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// KHONG extends BaseFilterRequest: lop do dung @Builder, ke thua se can
// @SuperBuilder va de sinh loi ngam. Thay vao do DTO nay mo ta bo loc theo
// ngon ngu nghiep vu, con SignServiceImpl dich sang BaseFilterRequest roi
// goi lai filter() cua BaseServiceImpl - tan dung dung co che co san.
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignSearchRequest {

    // Tu khoa tim kiem. Service tu bo dau truoc khi so voi cot word_vi_unaccent,
    // nen go "dia chi" van ra "địa chỉ".
    String keyword;

    UUID topicId;
    SignLevel level;
    UnitType unitType;
    WordType wordType;
    SignDomain domain;
    SignSource source;
    ReviewStatus reviewStatus;
    Boolean isPublished;

    // Hai bo loc dung nhieu nhat trong CMS: tra loi cau "con phai lam gi nua"
    Region missingVideoRegion;   // tu CHUA co video o vung mien nay
    Boolean missingExemplar;

    /**
     * Đang soạn bài cho khoá này: ẩn những từ đã thuộc một khoá KHÁC.
     * Từ đã có trong chính khoá này vẫn hiện (người soạn cần thấy để chuyển bài).
     */
    UUID hideUsedOutsideCourseId;

    /**
     * Đang soạn bài này (ô "Tìm từ vựng để thêm" của CMS):
     *  • ẩn từ ĐÃ CÓ trong chính bài này — thêm lại cũng chỉ bị bỏ qua
     *  • từ đã nằm ở bài KHÁC (cùng khoá hay khoá khác) vẫn hiện, kèm lessonUsages để
     *    giao diện gắn cảnh báo "Đã được sử dụng ở khoá học … bài học …"
     */
    UUID forLessonId;

    @Builder.Default
    List<SortCriteria> sorts = new ArrayList<>();

    @Builder.Default
    Integer page = 0;

    @Builder.Default
    Integer size = 20;
}
