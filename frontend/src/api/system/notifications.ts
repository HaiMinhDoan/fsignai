import { defHttp } from '@/utils/http/axios';
import type { PageResult } from '@/api/content/model/contentModel';
import type { NotificationModel } from './model/notificationModel';

export const notificationAdminFilterApi = (params: {
  type?: string;
  channel?: string;
  status?: string;
  page?: number;
  size?: number;
}) => defHttp.get<PageResult<NotificationModel>>({ url: '/api/v1/admin/notifications', params });
