import { defHttp } from '@/utils/http/axios';
import type { PageResult, TopicModel, TopicSaveParams } from './model/contentModel';

enum Api {
  BASE = '/api/v1/admin/topics',
  FILTER = '/api/v1/admin/topics/filter',
  TREE = '/api/v1/admin/topics/tree',
  OPTIONS = '/api/v1/admin/topics/options',
}

/** Lọc chủ đề có phân trang — dùng BaseFilterRequest của backend */
export const topicFilterApi = (params: {
  filters?: Array<{ fieldName: string; operation: string; value: any; logicType?: string }>;
  sorts?: Array<{ fieldName: string; direction: string }>;
  page?: number;
  size?: number;
}) =>
  defHttp.post<PageResult<TopicModel>>({
    url: Api.FILTER,
    data: params,
  });

/** Cây chủ đề đầy đủ, dùng cho component Tree của vben */
export const topicTreeApi = () => defHttp.get<TopicModel[]>({ url: Api.TREE });

/**
 * Danh sách phẳng cho dropdown.
 * Gọi rất nhiều lần (mỗi lần mở form từ vựng) nên để errorMessageMode='none'
 * tránh spam toast khi mạng chập chờn.
 */
export const topicOptionsApi = () =>
  defHttp.get<TopicModel[]>({ url: Api.OPTIONS }, { errorMessageMode: 'none' });

export const topicDetailApi = (id: string) =>
  defHttp.get<TopicModel>({ url: `${Api.BASE}/${id}` });

export const topicCreateApi = (params: TopicSaveParams) =>
  defHttp.post<TopicModel>({ url: Api.BASE, data: params }, { successMessageMode: 'message' });

export const topicUpdateApi = (id: string, params: TopicSaveParams) =>
  defHttp.put<TopicModel>({ url: `${Api.BASE}/${id}`, data: params }, { successMessageMode: 'message' });

export const topicDeleteApi = (id: string) =>
  defHttp.delete<void>({ url: `${Api.BASE}/${id}` }, { successMessageMode: 'message' });
