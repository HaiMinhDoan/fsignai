import { apiGet, apiPut } from './http';
import type { PageResult } from './dictionary';

export interface AppNotification {
  id: string;
  type: string;
  titleVi: string;
  bodyVi?: string;
  actionUrl?: string;
  readAt?: string;
  createdAt: string;
}

export const notificationsApi = (page = 0, size = 10) =>
  apiGet<PageResult<AppNotification>>('/notifications', { page, size });

export const unreadCountApi = () => apiGet<{ count: number }>('/notifications/unread-count');

export const markNotificationReadApi = (id: string) => apiPut<void>(`/notifications/${id}/read`);

export const markAllNotificationsReadApi = () => apiPut<void>('/notifications/read-all');
