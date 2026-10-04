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

/**
 * Xuất bản / gỡ xuất bản mọi bài của khoá trong một lần.
 * Bài chưa có nội dung bị bỏ qua khi xuất bản và được đếm vào `skipped`.
 */
export const lessonPublishAllApi = (courseId: string, published: boolean) =>
  // Tham số nằm thẳng trên URL: với POST, lớp http của vben gộp `params` vào thân
  // yêu cầu, máy chủ không thấy query nào và trả 400 "thiếu tham số published"
  defHttp.post<{ affected: number; skipped: number }>({
    url: `${COURSES}/${courseId}/lessons/publish?published=${published}`,
  });

/** Tải một tấm ảnh lên kho tệp chung, trả về id để gắn vào khoá học (ảnh đại diện) */
export const imageUploadApi = (file: File, entityType?: string) => {
  const formData = new FormData();
  formData.append('file', file);
  return defHttp.post<{ id: string; url: string }>({
    url: `/api/v1/admin/files/images${entityType ? `?entityType=${entityType}` : ''}`,
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 2 * 60 * 1000,
  });
};

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
  defHttp.post<{ added: number; skipped: number; usedInCourse: number; usedElsewhere: number }>({
    url: `${LESSONS}/${lessonId}/items/signs`,
    data: { signIds },
    timeout: 60 * 1000,
  });

export const lessonRemoveItemApi = (lessonId: string, itemId: string) =>
  defHttp.delete<void>({ url: `${LESSONS}/${lessonId}/items/${itemId}` });

export const lessonReorderItemsApi = (lessonId: string, params: ReorderParams) =>
  defHttp.post<void>({ url: `${LESSONS}/${lessonId}/items/reorder`, data: params });
