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
    Boolean missingExemplar;     // tu CHUA cham diem AI duoc

    @Builder.Default
    List<SortCriteria> sorts = new ArrayList<>();

    @Builder.Default
    Integer page = 0;

    @Builder.Default
    Integer size = 20;
}
