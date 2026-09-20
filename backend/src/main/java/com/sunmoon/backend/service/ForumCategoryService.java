package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.forum.ForumCategoryRequest;
import com.sunmoon.backend.dto.response.forum.ForumCategoryResponse;

import java.util.List;
import java.util.UUID;

public interface ForumCategoryService {

    List<ForumCategoryResponse> listAll();

    List<ForumCategoryResponse> listPublished();

    ForumCategoryResponse create(ForumCategoryRequest request);

    ForumCategoryResponse update(UUID id, ForumCategoryRequest request);

    void delete(UUID id);
}
