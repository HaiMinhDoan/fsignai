import { defHttp } from '@/utils/http/axios';
import type { PageResult } from '@/api/content/model/contentModel';
import type { BlogPostModel, BlogPostSaveParams } from './model/blogModel';

const BASE = '/api/v1/admin/blog';

export const blogFilterApi = (params: { category?: string; isPublished?: boolean; page?: number; size?: number }) =>
  defHttp.get<PageResult<BlogPostModel>>({ url: BASE, params });

export const blogDetailApi = (id: string) => defHttp.get<BlogPostModel>({ url: `${BASE}/${id}` });

export const blogCreateApi = (params: BlogPostSaveParams) =>
  defHttp.post<BlogPostModel>({ url: BASE, data: params }, { successMessageMode: 'message' });

export const blogUpdateApi = (id: string, params: BlogPostSaveParams) =>
  defHttp.put<BlogPostModel>({ url: `${BASE}/${id}`, data: params }, { successMessageMode: 'message' });

export const blogDeleteApi = (id: string) =>
  defHttp.delete<void>({ url: `${BASE}/${id}` }, { successMessageMode: 'message' });
