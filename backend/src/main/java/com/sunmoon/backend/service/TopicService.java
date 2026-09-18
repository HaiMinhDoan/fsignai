package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.content.TopicRequest;
import com.sunmoon.backend.dto.response.content.TopicResponse;
import com.sunmoon.backend.entity.dictionary.Topic;

import java.util.List;
import java.util.UUID;

// Ke thua BaseService de co san CRUD + filter dong, chi bo sung viec rieng cua chu de.
public interface TopicService extends BaseService<Topic, UUID> {

    TopicResponse createTopic(TopicRequest request);

    TopicResponse updateTopic(UUID id, TopicRequest request);

    TopicResponse getDetail(UUID id);

    // Cay chu de day du cho component Tree cua vben
    List<TopicResponse> getTree();

    // Danh sach phang, dung cho dropdown trong form tu vung
    List<TopicResponse> getOptions();

    void deleteTopic(UUID id);
}
