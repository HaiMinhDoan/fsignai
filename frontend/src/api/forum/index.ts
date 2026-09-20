import { defHttp } from '@/utils/http/axios';
import type { PageResult } from '@/api/content/model/contentModel';
import type {
  ForumCategoryModel,
  ForumCategorySaveParams,
  ForumPostModel,
  ForumPostStatus,
  ForumReportModel,
} from './model/forumModel';

enum Api {
  CATEGORIES = '/api/v1/admin/forum/categories',
  POSTS = '/api/v1/admin/forum/posts',
  COMMENTS = '/api/v1/admin/forum/comments',
  REPORTS = '/api/v1/admin/forum/reports',
}

// ===== Chuyên mục =====

export const forumCategoryListApi = () => defHttp.get<ForumCategoryModel[]>({ url: Api.CATEGORIES });

export const forumCategoryCreateApi = (params: ForumCategorySaveParams) =>
  defHttp.post<ForumCategoryModel>({ url: Api.CATEGORIES, data: params }, { successMessageMode: 'message' });

export const forumCategoryUpdateApi = (id: string, params: ForumCategorySaveParams) =>
  defHttp.put<ForumCategoryModel>({ url: `${Api.CATEGORIES}/${id}`, data: params }, { successMessageMode: 'message' });

export const forumCategoryDeleteApi = (id: string) =>
  defHttp.delete<void>({ url: `${Api.CATEGORIES}/${id}` }, { successMessageMode: 'message' });

// ===== Bài viết =====

export const forumPostFilterApi = (params: {
  status?: ForumPostStatus;
  categoryId?: string;
  page?: number;
  size?: number;
}) => defHttp.get<PageResult<ForumPostModel>>({ url: Api.POSTS, params });

export const forumPostModerateApi = (id: string, status: ForumPostStatus) =>
  defHttp.put<ForumPostModel>(
    { url: `${Api.POSTS}/${id}/status`, data: { status } },
    { successMessageMode: 'message' },
  );

export const forumPostSetPinnedApi = (id: string, pinned: boolean) =>
  defHttp.put<ForumPostModel>(
    { url: `${Api.POSTS}/${id}/pin`, params: { pinned } },
    { successMessageMode: 'message' },
  );

export const forumPostSetLockedApi = (id: string, locked: boolean) =>
  defHttp.put<ForumPostModel>(
    { url: `${Api.POSTS}/${id}/lock`, params: { locked } },
    { successMessageMode: 'message' },
  );

// ===== Bình luận =====

export const forumCommentModerateApi = (id: string, status: ForumPostStatus) =>
  defHttp.put<void>({ url: `${Api.COMMENTS}/${id}/status`, data: { status } }, { successMessageMode: 'message' });

// ===== Báo cáo vi phạm =====

export const forumReportFilterApi = (params: { openOnly?: boolean; page?: number; size?: number }) =>
  defHttp.get<PageResult<ForumReportModel>>({ url: Api.REPORTS, params });

export const forumReportHandleApi = (id: string, status: 'RESOLVED' | 'DISMISSED' | 'REVIEWING', handlerNote?: string) =>
  defHttp.put<ForumReportModel>(
    { url: `${Api.REPORTS}/${id}`, data: { status, handlerNote } },
    { successMessageMode: 'message' },
  );
