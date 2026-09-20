import { apiGet, apiPost, apiPut, apiDelete } from './http';
import type { PageResult } from './dictionary';

export interface ForumCategory {
  id: string;
  slug: string;
  nameVi: string;
  descriptionVi?: string;
  iconName?: string;
  displayOrder: number;
  isLocked: boolean;
  isPublished: boolean;
  postCount: number;
}

export type ForumPostStatus = 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'HIDDEN' | 'REMOVED';

export interface ForumPost {
  id: string;
  categoryId: string;
  categoryNameVi: string;
  authorId: string;
  authorName: string;
  authorAvatarUrl?: string;
  titleVi: string;
  bodyMd: string;
  signId?: string;
  signWordVi?: string;
  viewCount: number;
  commentCount: number;
  reactionCount: number;
  myReaction: boolean;
  isPinned: boolean;
  isLocked: boolean;
  status: ForumPostStatus;
  publishedAt?: string;
  lastActivityAt: string;
  createdAt: string;
}

export interface ForumComment {
  id: string;
  postId: string;
  parentId?: string;
  depth: number;
  authorId: string;
  authorName: string;
  authorAvatarUrl?: string;
  bodyText: string;
  reactionCount: number;
  myReaction: boolean;
  status: string;
  createdAt: string;
}

export interface ForumPostSaveParams {
  categoryId: string;
  titleVi: string;
  bodyMd: string;
  signId?: string;
}

export type ReportReason = 'SPAM' | 'ABUSE' | 'WRONG_SIGN' | 'OFF_TOPIC' | 'SENSITIVE' | 'OTHER';

export const forumCategoriesApi = () => apiGet<ForumCategory[]>('/forum/categories');

export const forumPostsApi = (categoryId: string | undefined, page = 0, size = 20) =>
  apiGet<PageResult<ForumPost>>('/forum/posts', { categoryId, page, size });

export const forumPostDetailApi = (id: string) => apiGet<ForumPost>(`/forum/posts/${id}`);

export const forumPostCreateApi = (params: ForumPostSaveParams) => apiPost<ForumPost>('/forum/posts', params);

export const forumPostUpdateApi = (id: string, params: ForumPostSaveParams) =>
  apiPut<ForumPost>(`/forum/posts/${id}`, params);

export const forumPostDeleteApi = (id: string) => apiDelete<void>(`/forum/posts/${id}`);

export const forumCommentsApi = (postId: string) => apiGet<ForumComment[]>(`/forum/posts/${postId}/comments`);

export const forumCommentCreateApi = (postId: string, bodyText: string, parentId?: string) =>
  apiPost<ForumComment>(`/forum/posts/${postId}/comments`, { bodyText, parentId });

export const forumCommentDeleteApi = (id: string) => apiDelete<void>(`/forum/comments/${id}`);

export const forumPostLikeApi = (id: string) => apiPost<{ liked: boolean }>(`/forum/posts/${id}/like`);

export const forumCommentLikeApi = (id: string) => apiPost<{ liked: boolean }>(`/forum/comments/${id}/like`);

export const forumReportApi = (params: {
  targetType: 'POST' | 'COMMENT';
  targetId: string;
  reason: ReportReason;
  note?: string;
}) => apiPost<unknown>('/forum/reports', params);
