package com.sunmoon.backend.service;

import com.sunmoon.backend.constant.enums.Region;
import com.sunmoon.backend.constant.enums.ViewAngle;
import com.sunmoon.backend.dto.request.content.SignImportRequest;
import com.sunmoon.backend.dto.request.content.SignRequest;
import com.sunmoon.backend.dto.request.content.SignSearchRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.content.SignImportResultResponse;
import com.sunmoon.backend.dto.response.content.SignResponse;
import com.sunmoon.backend.dto.response.content.SignVideoResponse;
import com.sunmoon.backend.entity.dictionary.Sign;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface SignService extends BaseService<Sign, UUID> {

    // Tim kiem CMS: tu dong bo dau tu khoa truoc khi so voi word_vi_unaccent
    PageResponse<SignResponse> search(SignSearchRequest request);

    SignResponse createSign(SignRequest request);

    SignResponse updateSign(UUID id, SignRequest request);

    SignResponse getDetail(UUID id);

    void deleteSign(UUID id);

    // Xuat ban / go xuat ban hang loat tu bang danh sach
    int togglePublish(List<UUID> ids, boolean published);

    // Nhap hang loat tu Excel da duoc frontend phan tich thanh JSON
    SignImportResultResponse importSigns(SignImportRequest request);

    // ===== Video =====

    List<SignVideoResponse> getVideos(UUID signId);

    SignVideoResponse uploadVideo(UUID signId, MultipartFile file,
                                  Region region, ViewAngle viewAngle,
                                  String signerLabel, String captionVi);

    SignVideoResponse setPrimaryVideo(UUID signId, UUID videoId);

    void deleteVideo(UUID signId, UUID videoId);
}
