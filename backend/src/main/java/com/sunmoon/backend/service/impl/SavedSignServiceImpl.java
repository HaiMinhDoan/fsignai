package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.dictionary.SavedSignResponse;
import com.sunmoon.backend.entity.dictionary.SavedSign;
import com.sunmoon.backend.entity.dictionary.Sign;
import com.sunmoon.backend.entity.dictionary.SignVideo;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.repository.SavedSignRepository;
import com.sunmoon.backend.repository.SignRepository;
import com.sunmoon.backend.repository.SignVideoRepository;
import com.sunmoon.backend.repository.UserRepository;
import com.sunmoon.backend.service.SavedSignService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedSignServiceImpl implements SavedSignService {

    private final SavedSignRepository savedSignRepository;
    private final SignRepository signRepository;
    private final SignVideoRepository signVideoRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public boolean save(UUID userId, UUID signId, String note) {
        if (savedSignRepository.existsByUserIdAndSignId(userId, signId)) {
            return false; // Da luu tu truoc - khong luu trung, khong bao loi
        }
        Sign sign = signRepository.findById(signId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy từ này"));

        savedSignRepository.save(SavedSign.builder()
                .user(userRepository.getReferenceById(userId))
                .sign(sign)
                .note(note)
                .build());
        return true;
    }

    @Override
    @Transactional
    public void unsave(UUID userId, UUID signId) {
        savedSignRepository.deleteByUserIdAndSignId(userId, signId);
    }

    @Override
    public boolean isSaved(UUID userId, UUID signId) {
        return savedSignRepository.existsByUserIdAndSignId(userId, signId);
    }

    @Override
    public PageResponse<SavedSignResponse> list(UUID userId, Pageable pageable) {
        Page<SavedSign> page = savedSignRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(page, this::toResponse);
    }

    private SavedSignResponse toResponse(SavedSign saved) {
        Sign sign = saved.getSign();
        List<SignVideo> videos = signVideoRepository.findAllBySignIdOrderByRegionAscViewAngleAsc(sign.getId());
        SignVideo primary = videos.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsPrimary()))
                .findFirst()
                .orElse(videos.isEmpty() ? null : videos.get(0));

        return SavedSignResponse.builder()
                .id(saved.getId())
                .signId(sign.getId())
                .wordVi(sign.getWordVi())
                .gloss(sign.getGloss())
                .level(sign.getLevel())
                .videoUrl(primary != null && primary.getFile() != null ? primary.getFile().getPublicUrl() : null)
                .thumbnailUrl(primary != null && primary.getThumbnailFile() != null
                        ? primary.getThumbnailFile().getPublicUrl() : null)
                .note(saved.getNote())
                .savedAt(saved.getCreatedAt())
                .build();
    }
}
