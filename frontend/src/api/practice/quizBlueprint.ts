import { defHttp } from '@/utils/http/axios';
import type { PageResult } from '@/api/content/model/contentModel';
import type {
  GenerateQuestionsResult,
  QuizBlueprintModel,
  QuizBlueprintSaveParams,
  QuizFilterParams,
} from './model/practiceModel';

enum Api {
  BASE = '/api/v1/admin/quiz-blueprints',
  FILTER = '/api/v1/admin/quiz-blueprints/filter',
  SET_ACTIVE = '/api/v1/admin/quiz-blueprints/set-active',
}

export const quizBlueprintFilterApi = (params: QuizFilterParams) =>
  defHttp.post<PageResult<QuizBlueprintModel>>({ url: Api.FILTER, data: params });

export const quizBlueprintDetailApi = (id: string) =>
  defHttp.get<QuizBlueprintModel>({ url: `${Api.BASE}/${id}` });

export const quizBlueprintCreateApi = (params: QuizBlueprintSaveParams) =>
  defHttp.post<QuizBlueprintModel>(
    { url: Api.BASE, data: params },
    { successMessageMode: 'message' },
  );

export const quizBlueprintUpdateApi = (id: string, params: QuizBlueprintSaveParams) =>
  defHttp.put<QuizBlueprintModel>(
    { url: `${Api.BASE}/${id}`, data: params },
    { successMessageMode: 'message' },
  );

export const quizBlueprintDeleteApi = (id: string) =>
  defHttp.delete<void>({ url: `${Api.BASE}/${id}` }, { successMessageMode: 'message' });

export const quizBlueprintSetActiveApi = (ids: string[], active: boolean) =>
  defHttp.post<{ affected: number }>(
    { url: Api.SET_ACTIVE, data: { ids, active } },
    { successMessageMode: 'message' },
  );

/**
 * Rút thử một đề theo cấu hình hiện tại. Có thể phải duyệt qua nhiều từ nên
 * đặt timeout riêng, giống quizGenerateQuestionsApi.
 */
export const quizBlueprintPreviewApi = (id: string) =>
  defHttp.post<GenerateQuestionsResult>(
    { url: `${Api.BASE}/${id}/preview`, timeout: 2 * 60 * 1000 },
    { errorMessageMode: 'modal' },
  );
