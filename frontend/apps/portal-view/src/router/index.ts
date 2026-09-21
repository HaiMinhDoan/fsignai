import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

/**
 * Đường dẫn tiếng Việt không dấu, khớp 5 mục điều hướng trong Figma:
 * Trang Chủ · Phòng Luyện Ký Hiệu · Góc Trò Chơi · Thư Viện Cử Chỉ ·
 * Dành Cho Ba Mẹ & Thầy Cô.
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },

      {
        path: 'phong-luyen',
        name: 'practice',
        component: () => import('@/views/practice/PracticeRoomView.vue'),
      },
      {
        // Cùng một màn hình, chỉ khác là đã biết luyện từ nào
        path: 'phong-luyen/:id',
        name: 'practice-sign',
        component: () => import('@/views/practice/PracticeRoomView.vue'),
        props: true,
      },

      {
        path: 'goc-tro-choi',
        name: 'games',
        component: () => import('@/views/games/GameCornerView.vue'),
      },

      {
        path: 'tu-dien',
        name: 'dictionary-search',
        component: () => import('@/views/dictionary/DictionarySearchView.vue'),
      },
      {
        path: 'tu-dien/:id',
        name: 'dictionary-detail',
        component: () => import('@/views/dictionary/SignDetailView.vue'),
        props: true,
      },

      {
        path: 'goi-tu/:id',
        name: 'word-pack-journey',
        component: () => import('@/views/wordpack/WordPackJourneyView.vue'),
        props: true,
      },

      {
        path: 'thu-vien',
        name: 'library',
        component: () => import('@/views/library/LibraryView.vue'),
      },

      {
        path: 'dien-dan',
        name: 'forum',
        component: () => import('@/views/forum/ForumView.vue'),
      },

      {
        path: 'on-tu',
        name: 'flashcard',
        component: () => import('@/views/flashcard/FlashcardView.vue'),
      },

      {
        path: 'khoa-hoc',
        name: 'course-list',
        component: () => import('@/views/course/CourseListView.vue'),
      },
      {
        path: 'khoa-hoc/:id',
        name: 'course-detail',
        component: () => import('@/views/course/CourseDetailView.vue'),
        props: true,
      },
      {
        path: 'bai-hoc/:id',
        name: 'lesson',
        component: () => import('@/views/course/LessonView.vue'),
        props: true,
      },

      {
        path: 'kiem-tra',
        name: 'quiz-hub',
        component: () => import('@/views/quiz/QuizHubView.vue'),
      },
      {
        path: 'kiem-tra/:attemptId',
        name: 'quiz-attempt',
        component: () => import('@/views/quiz/QuizAttemptView.vue'),
        props: true,
      },
      {
        path: 'dien-dan/:id',
        name: 'forum-post',
        component: () => import('@/views/forum/ForumPostView.vue'),
        props: true,
      },

      {
        path: 'ba-me-thay-co',
        name: 'guardian-dashboard',
        component: () => import('@/views/guardian/GuardianDashboardView.vue'),
      },
    ],
  },
  {
    path: '/dang-nhap',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
  },
  {
    path: '/dang-ky',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
  },
  {
    path: '/lam-quen',
    name: 'onboarding',
    component: () => import('@/views/OnboardingView.vue'),
  },
  {
    // Gõ sai đường dẫn thì về trang chủ, đừng để người học nhìn màn hình trắng
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
];

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

// Khôi phục phiên đăng nhập từ token cũ TRƯỚC khi vào bất kỳ trang nào —
// tránh nháy trạng thái "chưa đăng nhập" một khắc rồi mới hiện đúng
const PUBLIC_ROUTES = new Set(['login', 'register']);

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (!auth.isReady) {
    await auth.restoreSession();
  }

  // Đăng nhập rồi mà chưa trả lời 4 câu onboarding thì chặn lại, trừ khi đang
  // đi tới chính màn onboarding hoặc các trang công khai (đăng nhập/đăng ký)
  if (
    auth.isLoggedIn() &&
    auth.user?.onboardingCompleted === false &&
    to.name !== 'onboarding' &&
    !PUBLIC_ROUTES.has(to.name as string)
  ) {
    return { name: 'onboarding' };
  }

  return true;
});
