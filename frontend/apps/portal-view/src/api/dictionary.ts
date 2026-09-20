import { apiDelete, apiGet, apiPost } from './http';

export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface TopicRef {
  id: string;
  nameVi: string;
}

export interface SignSummary {
  id: string;
  gloss: string;
  wordVi: string;
  wordEn?: string;
  descriptionVi?: string;
  noteVi?: string;
  level: string;
  unitType: string;
  wordType: string;
  primaryTopicId?: string;
  primaryTopicNameVi?: string;
  topics?: TopicRef[];
  isPublished: boolean;
  thumbnailUrl?: string;
  availableRegions?: string[];
}

export interface SignVideo {
  id: string;
  signId: string;
  region: 'NORTH' | 'CENTRAL' | 'SOUTH' | 'COMMON';
  viewAngle: string;
  videoUrl: string;
  thumbnailUrl?: string;
  captionVi?: string;
  isPrimary: boolean;
}

export interface DictionarySearchParams {
  keyword?: string;
  topicId?: string;
  level?: string;
  unitType?: string;
  wordType?: string;
  page?: number;
  size?: number;
}

export const dictionarySearchApi = (params: DictionarySearchParams) =>
  apiGet<PageResult<SignSummary>>('/dictionary/search', params as Record<string, unknown>);

export const signDetailApi = (id: string) => apiGet<SignSummary>(`/dictionary/signs/${id}`);

export const signVideosApi = (id: string) => apiGet<SignVideo[]>(`/dictionary/signs/${id}/videos`);

export interface SignStep {
  id: string;
  signId: string;
  stepOrder: number;
  titleVi?: string;
  descriptionVi: string;
  /** LEFT_HAND | RIGHT_HAND | BOTH_HANDS | FACE | MOUTH | SHOULDER | CHEST */
  bodyFocus?: string;
  holdSeconds?: number;
  /** null khi biên tập viên mới soạn chữ, chưa gắn ảnh */
  imageUrl?: string;
}

/** Trả mảng rỗng nếu từ này chưa được soạn hướng dẫn từng bước */
export const signStepsApi = (id: string) => apiGet<SignStep[]>(`/dictionary/signs/${id}/steps`);

/** Danh sách phẳng chủ đề — dùng chung API CMS, RoleType.ALL nên tài khoản học được gọi */
export const topicOptionsApi = () => apiGet<TopicRef[]>('/admin/topics/options');

// ===== Thư viện của tôi (lưu từ) =====

export const isSignSavedApi = (id: string) =>
  apiGet<{ saved: boolean }>(`/dictionary/signs/${id}/save`);

export const saveSignApi = (id: string, note?: string) =>
  apiPost<{ saved: boolean; created: boolean }>(`/dictionary/signs/${id}/save`, { note });

export const unsaveSignApi = (id: string) => apiDelete<void>(`/dictionary/signs/${id}/save`);
