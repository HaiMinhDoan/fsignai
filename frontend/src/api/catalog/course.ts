import { defHttp } from '@/utils/http/axios';
import type { PageResult } from '@/api/content/model/contentModel';
import type {
  CourseFilterParams,
  CourseModel,
  CourseSaveParams,
  GenerateCoursesParams,
  GenerateCoursesResult,
  ReorderParams,
} from './model/catalogModel';

enum Api {
  BASE = '/api/v1/admin/courses',
  FILTER = '/api/v1/admin/courses/filter',
  PUBLISH = '/api/v1/admin/courses/publish',
  REORDER = '/api/v1/admin/courses/reorder',
  GENERATE = '/api/v1/admin/courses/generate',
  OPTIONS = '/api/v1/admin/courses/options',
}

export const courseFilterApi = (params: CourseFilterParams) =>
  defHttp.post<PageResult<CourseModel>>({ url: Api.FILTER, data: params });

export const courseOptionsApi = () =>
  defHttp.get<CourseModel[]>({ url: Api.OPTIONS }, { errorMessageMode: 'none' });

/** Kèm danh sách bài học đã sắp thứ tự */
export const courseDetailApi = (id: string) =>
  defHttp.get<CourseModel>({ url: `${Api.BASE}/${id}` });

export const courseCreateApi = (params: CourseSaveParams) =>
  defHttp.post<CourseModel>({ url: Api.BASE, data: params }, { successMessageMode: 'message' });

export const courseUpdateApi = (id: string, params: CourseSaveParams) =>
  defHttp.put<CourseModel>(
    { url: `${Api.BASE}/${id}`, data: params },
    { successMessageMode: 'message' },
  );

export const courseDeleteApi = (id: string) =>
  defHttp.delete<void>({ url: `${Api.BASE}/${id}` }, { successMessageMode: 'message' });

export const coursePublishApi = (ids: string[], published: boolean) =>
  defHttp.post<{ affected: number }>(
    { url: Api.PUBLISH, data: { ids, published } },
    { successMessageMode: 'message' },
  );

export const courseReorderApi = (params: ReorderParams) =>
  defHttp.post<void>({ url: Api.REORDER, data: params }, { errorMessageMode: 'message' });

/**
 * Sinh khoá học tự động từ từ vựng theo chủ đề.
 *
 * Việc này có thể duyệt qua vài nghìn từ nên đặt timeout riêng 5 phút —
 * mặc định 10 giây của defHttp sẽ cắt giữa chừng.
 */
export const courseGenerateApi = (params: GenerateCoursesParams) =>
  defHttp.post<GenerateCoursesResult>(
    { url: Api.GENERATE, data: params, timeout: 5 * 60 * 1000 },
    { errorMessageMode: 'modal' },
  );
