import type { AppRouteModule } from '@/router/types';

import { LAYOUT } from '@/router/constant';

/** Menu "Hệ thống": người dùng/vai trò, cài đặt, nhật ký audit */
const system: AppRouteModule = {
  path: '/system',
  name: 'System',
  component: LAYOUT,
  redirect: '/system/user',
  meta: {
    orderNo: 90,
    icon: 'ant-design:setting-outlined',
    title: 'Hệ thống',
  },
  children: [
    {
      path: 'user',
      name: 'UserManagement',
      component: () => import('@/views/system/user/index.vue'),
      meta: {
        title: 'Người dùng & Vai trò',
        icon: 'ant-design:team-outlined',
      },
    },
    {
      path: 'settings',
      name: 'SystemSettingManagement',
      component: () => import('@/views/system/settings/index.vue'),
      meta: {
        title: 'Cài đặt hệ thống',
        icon: 'ant-design:tool-outlined',
      },
    },
    {
      path: 'notifications',
      name: 'NotificationAdminViewer',
      component: () => import('@/views/system/notifications/index.vue'),
      meta: {
        title: 'Thông báo đã gửi',
        icon: 'ant-design:bell-outlined',
      },
    },
    {
      path: 'audit-log',
      name: 'AuditLogViewer',
      component: () => import('@/views/system/audit-log/index.vue'),
      meta: {
        title: 'Nhật ký Audit',
        icon: 'ant-design:history-outlined',
      },
    },
  ],
};

export default system;
