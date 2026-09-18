package com.sunmoon.backend.service.impl;

import com.sunmoon.backend.dto.request.content.TopicRequest;
import com.sunmoon.backend.dto.response.content.TopicResponse;
import com.sunmoon.backend.entity.FileAttachment;
import com.sunmoon.backend.entity.dictionary.Topic;
import com.sunmoon.backend.exception.customize.ConflictException;
import com.sunmoon.backend.exception.customize.NotFoundException;
import com.sunmoon.backend.mapper.TopicMapper;
import com.sunmoon.backend.repository.FileAttachmentRepository;
import com.sunmoon.backend.repository.SignRepository;
import com.sunmoon.backend.repository.TopicRepository;
import com.sunmoon.backend.service.TopicService;
import com.sunmoon.backend.service.impl.util.VietnameseTextUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TopicServiceImpl extends BaseServiceImpl<Topic, UUID> implements TopicService {

    @PersistenceContext
    private EntityManager entityManager;

    private final TopicRepository topicRepository;
    private final SignRepository signRepository;
    private final FileAttachmentRepository fileRepository;
    private final TopicMapper topicMapper;

    public TopicServiceImpl(TopicRepository topicRepository,
                            SignRepository signRepository,
                            FileAttachmentRepository fileRepository,
                            TopicMapper topicMapper) {
        // Topic khong co truong "status". Truyen "category" de changeStatus()
        // cua lop cha khong nem loi reflection khi bi goi nham.
        super(topicRepository, "category");
        this.topicRepository = topicRepository;
        this.signRepository = signRepository;
        this.fileRepository = fileRepository;
        this.topicMapper = topicMapper;
    }

    @Override
    protected EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    @Transactional
    public TopicResponse createTopic(TopicRequest request) {
        Topic entity = topicMapper.toEntity(request);
        entity.setSlug(resolveSlug(request, null));
        applyRelations(entity, request);
        return toResponseWithCount(topicRepository.save(entity));
    }

    @Override
    @Transactional
    public TopicResponse updateTopic(UUID id, TopicRequest request) {
        Topic entity = topicRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay chu de: " + id));

        topicMapper.updateEntity(entity, request);
        entity.setSlug(resolveSlug(request, id));
        applyRelations(entity, request);
        return toResponseWithCount(topicRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public TopicResponse getDetail(UUID id) {
        Topic entity = topicRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay chu de: " + id));
        return toResponseWithCount(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> getTree() {
        return topicRepository.findAllByParentIsNullOrderByDisplayOrderAsc()
                .stream().map(this::buildNode).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> getOptions() {
        return topicRepository.findAll().stream()
                .map(topicMapper::toResponse)
                .sorted((a, b) -> Integer.compare(
                        a.getDisplayOrder() == null ? 0 : a.getDisplayOrder(),
                        b.getDisplayOrder() == null ? 0 : b.getDisplayOrder()))
                .toList();
    }

    @Override
    @Transactional
    public void deleteTopic(UUID id) {
        Topic entity = topicRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay chu de: " + id));

        // Chan xoa chu de con dang co con hoac con tu vung - tranh mo coi du lieu
        if (topicRepository.countByParentId(id) > 0) {
            throw new ConflictException("Chu de nay con chu de con, khong xoa duoc");
        }
        long signCount = signRepository.countPublishedByTopic(id);
        if (signCount > 0) {
            throw new ConflictException("Chu de nay con " + signCount + " tu vung, khong xoa duoc");
        }
        topicRepository.delete(entity);
    }

    // ================= private =================

    private TopicResponse buildNode(Topic topic) {
        TopicResponse node = toResponseWithCount(topic);
        List<Topic> children = topicRepository.findAllByParentIdOrderByDisplayOrderAsc(topic.getId());
        if (!children.isEmpty()) {
            node.setChildren(children.stream().map(this::buildNode).toList());
        }
        return node;
    }

    private TopicResponse toResponseWithCount(Topic topic) {
        TopicResponse response = topicMapper.toResponse(topic);
        response.setSignCount(signRepository.countPublishedByTopic(topic.getId()));
        return response;
    }

    // Slug bo trong thi sinh tu ten tieng Viet qua VietnameseTextUtil
    private String resolveSlug(TopicRequest request, UUID currentId) {
        String slug = (request.getSlug() == null || request.getSlug().isBlank())
                ? VietnameseTextUtil.toSlug(request.getNameVi())
                : request.getSlug().trim();

        if (slug.isBlank()) {
            throw new ConflictException("Khong sinh duoc slug tu ten chu de");
        }

        boolean duplicated = currentId == null
                ? topicRepository.existsBySlug(slug)
                : topicRepository.existsBySlugAndIdNot(slug, currentId);
        if (duplicated) {
            throw new ConflictException("Slug da ton tai: " + slug);
        }
        return slug;
    }

    private void applyRelations(Topic entity, TopicRequest request) {
        if (request.getParentId() != null) {
            if (request.getParentId().equals(entity.getId())) {
                throw new ConflictException("Chu de khong the la cha cua chinh no");
            }
            Topic parent = topicRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("Khong tim thay chu de cha"));
            entity.setParent(parent);
        } else {
            entity.setParent(null);
        }
        entity.setIconFile(findFile(request.getIconFileId()));
        entity.setCoverFile(findFile(request.getCoverFileId()));
    }

    private FileAttachment findFile(UUID fileId) {
        if (fileId == null) return null;
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay file: " + fileId));
    }
}
