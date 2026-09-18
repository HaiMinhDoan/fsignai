import type { AppRouteModule } from '@/router/types';

import { LAYOUT } from '@/router/constant';

/**
 * Menu "Nội dung" của trang quản trị SignAI.
 * Đặt orderNo thấp để nằm ngay dưới Bảng điều khiển — đây là khu vực
 * quản trị viên làm việc hằng ngày.
 */
const content: AppRouteModule = {
  path: '/content',
  name: 'Content',
  component: LAYOUT,
  redirect: '/content/sign',
  meta: {
    orderNo: 10,
    icon: 'ant-design:book-outlined',
    title: 'Nội dung',
  },
  children: [
    {
      path: 'sign',
      name: 'SignManagement',
      component: () => import('@/views/content/sign/index.vue'),
      meta: {
        title: 'Từ vựng',
        icon: 'ant-design:translation-outlined',
      },
    },
    {
      path: 'topic',
      name: 'TopicManagement',
      component: () => import('@/views/content/topic/index.vue'),
      meta: {
        title: 'Chủ đề',
        icon: 'ant-design:apartment-outlined',
      },
    },
  ],
};

export default content;
