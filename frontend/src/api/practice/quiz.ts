import { defHttp } from '@/utils/http/axios';
import type { PageResult } from '@/api/content/model/contentModel';
import type { ReorderParams } from '@/api/catalog/model/catalogModel';
import type {
  GenerateQuestionsParams,
  GenerateQuestionsResult,
  QuizFilterParams,
  QuizModel,
  QuizQuestionModel,
  QuizQuestionSaveParams,
  QuizSaveParams,
} from './model/practiceModel';

enum Api {
  BASE = '/api/v1/admin/quizzes',
  FILTER = '/api/v1/admin/quizzes/filter',
  PUBLISH = '/api/v1/admin/quizzes/publish',
}

export const quizFilterApi = (params: QuizFilterParams) =>
  defHttp.post<PageResult<QuizModel>>({ url: Api.FILTER, data: params });

export const quizByLessonApi = (lessonId: string) =>
  defHttp.get<QuizModel[]>({ url: `${Api.BASE}/by-lesson/${lessonId}` });

/** Kèm toàn bộ câu hỏi VÀ đáp án đúng — chỉ dùng cho màn hình quản trị */
export const quizDetailApi = (id: string) => defHttp.get<QuizModel>({ url: `${Api.BASE}/${id}` });

export const quizCreateApi = (params: QuizSaveParams) =>
  defHttp.post<QuizModel>({ url: Api.BASE, data: params }, { successMessageMode: 'message' });

export const quizUpdateApi = (id: string, params: QuizSaveParams) =>
  defHttp.put<QuizModel>(
    { url: `${Api.BASE}/${id}`, data: params },
    { successMessageMode: 'message' },
  );

export const quizDeleteApi = (id: string) =>
  defHttp.delete<void>({ url: `${Api.BASE}/${id}` }, { successMessageMode: 'message' });

export const quizPublishApi = (ids: string[], published: boolean) =>
  defHttp.post<{ affected: number }>(
    { url: Api.PUBLISH, data: { ids, published } },
    { successMessageMode: 'message' },
  );

// ===== Câu hỏi =====

export const quizAddQuestionApi = (quizId: string, params: QuizQuestionSaveParams) =>
  defHttp.post<QuizQuestionModel>({ url: `${Api.BASE}/${quizId}/questions`, data: params });

export const quizUpdateQuestionApi = (
  quizId: string,
  questionId: string,
  params: QuizQuestionSaveParams,
) =>
  defHttp.put<QuizQuestionModel>({
    url: `${Api.BASE}/${quizId}/questions/${questionId}`,
    data: params,
  });

export const quizDeleteQuestionApi = (quizId: string, questionId: string) =>
  defHttp.delete<void>({ url: `${Api.BASE}/${quizId}/questions/${questionId}` });

export const quizReorderQuestionsApi = (quizId: string, params: ReorderParams) =>
  defHttp.post<void>({ url: `${Api.BASE}/${quizId}/questions/reorder`, data: params });

/**
 * Sinh câu hỏi tự động.
 *
 * Có thể phải duyệt qua hàng nghìn từ nên đặt timeout riêng 2 phút —
 * mặc định 10 giây của defHttp sẽ cắt giữa chừng.
 */
export const quizGenerateQuestionsApi = (quizId: string, params: GenerateQuestionsParams) =>
  defHttp.post<GenerateQuestionsResult>(
    { url: `${Api.BASE}/${quizId}/questions/generate`, data: params, timeout: 2 * 60 * 1000 },
    { errorMessageMode: 'modal' },
  );
