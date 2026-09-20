import type { AppRouteModule } from '@/router/types';

import { LAYOUT } from '@/router/constant';

/** Menu "Diễn đàn": chuyên mục, kiểm duyệt bài viết/bình luận, báo cáo vi phạm */
const forum: AppRouteModule = {
  path: '/forum',
  name: 'Forum',
  component: LAYOUT,
  redirect: '/forum/moderation',
  meta: {
    orderNo: 20,
    icon: 'ant-design:message-outlined',
    title: 'Diễn đàn',
  },
  children: [
    {
      path: 'moderation',
      name: 'ForumModeration',
      component: () => import('@/views/forum/moderation/index.vue'),
      meta: {
        title: 'Kiểm duyệt bài viết',
        icon: 'ant-design:audit-outlined',
      },
    },
    {
      path: 'reports',
      name: 'ForumReports',
      component: () => import('@/views/forum/reports/index.vue'),
      meta: {
        title: 'Báo cáo vi phạm',
        icon: 'ant-design:warning-outlined',
      },
    },
    {
      path: 'category',
      name: 'ForumCategoryManagement',
      component: () => import('@/views/forum/category/index.vue'),
      meta: {
        title: 'Chuyên mục',
        icon: 'ant-design:apartment-outlined',
      },
    },
  ],
};

export default forum;
