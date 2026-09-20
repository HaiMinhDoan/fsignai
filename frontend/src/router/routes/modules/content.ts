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
    {
      path: 'course',
      name: 'CourseManagement',
      component: () => import('@/views/content/course/index.vue'),
      meta: {
        title: 'Khoá học',
        icon: 'ant-design:read-outlined',
      },
    },
    {
      path: 'word-pack',
      name: 'WordPackManagement',
      component: () => import('@/views/content/wordpack/index.vue'),
      meta: {
        title: 'Gói từ',
        icon: 'ant-design:cluster-outlined',
      },
    },
    {
      path: 'quiz',
      name: 'QuizManagement',
      component: () => import('@/views/content/quiz/index.vue'),
      meta: {
        title: 'Ngân hàng câu hỏi',
        icon: 'ant-design:question-circle-outlined',
      },
    },
    {
      path: 'quiz-blueprint',
      name: 'QuizBlueprintManagement',
      component: () => import('@/views/content/quiz-blueprint/index.vue'),
      meta: {
        title: 'Cấu hình đề trộn',
        icon: 'ant-design:control-outlined',
      },
    },
    {
      path: 'blog',
      name: 'BlogManagement',
      component: () => import('@/views/content/blog/index.vue'),
      meta: {
        title: 'Blog',
        icon: 'ant-design:file-text-outlined',
      },
    },
  ],
};

export default content;
