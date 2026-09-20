package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.request.BaseFilterRequest;
import com.sunmoon.backend.dto.request.catalog.CourseRequest;
import com.sunmoon.backend.dto.request.catalog.GenerateCoursesRequest;
import com.sunmoon.backend.dto.request.catalog.ReorderRequest;
import com.sunmoon.backend.dto.response.PageResponse;
import com.sunmoon.backend.dto.response.catalog.CourseResponse;
import com.sunmoon.backend.dto.response.catalog.GenerateCoursesResult;
import com.sunmoon.backend.entity.catalog.Course;

import java.util.List;
import java.util.UUID;

public interface CourseService extends BaseService<Course, UUID> {

    /**
     * Trả thẳng PageResponse thay vì Page<Course> như filter() của lớp cha.
     * Lý do: số bài học của mỗi khoá phải gom trong một truy vấn; nếu để
     * controller tự map thì mapper sẽ phải đếm theo từng dòng (N+1).
     */
    PageResponse<CourseResponse> search(BaseFilterRequest request);

    CourseResponse createCourse(CourseRequest request);

    CourseResponse updateCourse(UUID id, CourseRequest request);

    /** Kèm danh sách bài học đã sắp thứ tự */
    CourseResponse getDetail(UUID id);

    /**
     * Chi tiết khoá cho người học: 404 nếu chưa xuất bản, chỉ trả bài học đã
     * xuất bản, kèm tiến độ của userId nếu có (null = khách chưa đăng nhập).
     */
    CourseResponse getPublishedDetail(UUID id, UUID userId);

    List<CourseResponse> getOptions();

    void deleteCourse(UUID id);

    /** Xuất bản / gỡ xuất bản hàng loạt, trả về số khoá bị ảnh hưởng */
    int setPublished(List<UUID> ids, boolean published);

    void reorderCourses(ReorderRequest request);

    /** Sinh khoá học và bài học tự động từ từ vựng theo chủ đề */
    GenerateCoursesResult generate(GenerateCoursesRequest request);
}
