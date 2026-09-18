package com.sunmoon.backend.dto.request.content;

import com.sunmoon.backend.constant.enums.DuplicateStrategy;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

// Nhap hang loat tu Excel.
//
// File .xlsx duoc PHAN TICH O FRONTEND bang thu vien xlsx ma vben da co san,
// roi gui len day duoi dang JSON. Nho vay backend khong can them Apache POI,
// va nguoi dung xem truoc duoc du lieu truoc khi ghi - dung yeu cau
// "luon cho xem truoc truoc khi ghi" trong 04-admin-cms.md.
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignImportRequest {

    @NotEmpty(message = "Danh sách dòng nhập không được để trống")
    List<SignImportRow> rows;

    @Builder.Default
    DuplicateStrategy duplicateStrategy = DuplicateStrategy.SKIP;

    // true = chi kiem tra va tra ve ket qua du kien, KHONG ghi vao DB
    @Builder.Default
    Boolean dryRun = false;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SignImportRow {

        // So dong trong file Excel, de bao loi dung cho nguoi dung
        Integer rowNumber;

        String gloss;
        String wordVi;
        String wordEn;
        String unitType;
        String wordType;
        String wordSubtype;
        String domain;
        String level;
        String descriptionVi;

        // Danh sach slug chu de, ngan cach bang dau phay: "gia-dinh,co-ban"
        String topics;

        // Ma video goc tren qipedc.moet.gov.vn, vi du "W00665B".
        // Hau to B/T/N duoc suy ra vung mien tu dong, khong phai nhap tay.
        String videoId;

        // Neu de trong, service suy tu hau to cua videoId
        String region;
    }
}
