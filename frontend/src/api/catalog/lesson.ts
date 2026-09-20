import { defHttp } from '@/utils/http/axios';
import type {
  LessonItemModel,
  LessonModel,
  LessonSaveParams,
  ReorderParams,
} from './model/catalogModel';

const COURSES = '/api/v1/admin/courses';
const LESSONS = '/api/v1/admin/lessons';

export const lessonListApi = (courseId: string) =>
  defHttp.get<LessonModel[]>({ url: `${COURSES}/${courseId}/lessons` });

export const lessonCreateApi = (courseId: string, params: LessonSaveParams) =>
  defHttp.post<LessonModel>(
    { url: `${COURSES}/${courseId}/lessons`, data: params },
    { successMessageMode: 'message' },
  );

export const lessonReorderApi = (courseId: string, params: ReorderParams) =>
  defHttp.post<void>({ url: `${COURSES}/${courseId}/lessons/reorder`, data: params });

/** Kèm toàn bộ nội dung bên trong bài học */
export const lessonDetailApi = (lessonId: string) =>
  defHttp.get<LessonModel>({ url: `${LESSONS}/${lessonId}` });

export const lessonUpdateApi = (lessonId: string, params: LessonSaveParams) =>
  defHttp.put<LessonModel>(
    { url: `${LESSONS}/${lessonId}`, data: params },
    { successMessageMode: 'message' },
  );

export const lessonDeleteApi = (lessonId: string) =>
  defHttp.delete<void>({ url: `${LESSONS}/${lessonId}` }, { successMessageMode: 'message' });

export const lessonAddItemApi = (lessonId: string, data: Record<string, unknown>) =>
  defHttp.post<LessonItemModel>({ url: `${LESSONS}/${lessonId}/items`, data });

/**
 * Thêm nhiều từ vựng cùng lúc.
 * Backend bỏ qua từ đã có trong bài và trả về số thực sự được thêm.
 */
export const lessonAddSignsApi = (lessonId: string, signIds: string[]) =>
  defHttp.post<{ added: number; skipped: number }>({
    url: `${LESSONS}/${lessonId}/items/signs`,
    data: { signIds },
    timeout: 60 * 1000,
  });

export const lessonRemoveItemApi = (lessonId: string, itemId: string) =>
  defHttp.delete<void>({ url: `${LESSONS}/${lessonId}/items/${itemId}` });

export const lessonReorderItemsApi = (lessonId: string, params: ReorderParams) =>
  defHttp.post<void>({ url: `${LESSONS}/${lessonId}/items/reorder`, data: params });
