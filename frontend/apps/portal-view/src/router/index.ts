import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { trackPageView } from '@/utils/pageTracker';

/**
 * Đường dẫn tiếng Việt không dấu.
 *
 * Phòng Luyện Ký Hiệu đã bỏ (2026-10-03): video mẫu, gương camera, chấm điểm AI và
 * các bước thực hiện đều đã có ở trang từng từ của Thư Viện Cử Chỉ.
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },

      // Đường dẫn cũ của Phòng Luyện vẫn còn trong dấu trang, tin nhắn đã gửi đi:
      // chuyển sang trang từ tương ứng thay vì để rơi vào trang trắng
      { path: 'phong-luyen', redirect: { name: 'dictionary-search' } },
      {
        path: 'phong-luyen/:id',
        redirect: (to) => ({ name: 'dictionary-detail', params: { id: to.params.id } }),
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
        meta: { premium: 'Thư viện cử chỉ' },
      },
      {
        path: 'tu-dien/:id',
        name: 'dictionary-detail',
        component: () => import('@/views/dictionary/SignDetailView.vue'),
        props: true,
        meta: { premium: 'Thư viện cử chỉ' },
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
        meta: { premium: 'Bài kiểm tra kiến thức' },
      },
      {
        path: 'goi-dich-vu',
        name: 'pricing',
        component: () => import('@/views/PricingView.vue'),
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
        path: 'trang-ca-nhan',
        name: 'my-profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'nguoi-dung/:id',
        name: 'user-profile',
        component: () => import('@/views/profile/ProfileView.vue'),
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

  // Trang cá nhân của tôi, lịch sử giao dịch: chưa đăng nhập thì đi đăng nhập rồi quay lại đúng chỗ
  if (to.meta.requiresAuth && !auth.isLoggedIn()) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }

  // Trang thuộc gói Premium: chưa có gói thì đưa sang trang Gói dịch vụ, kèm tên tính năng để
  // giải thích vì sao. Chỉ là lớp giao diện — bắt đầu bài kiểm tra / chấm AI còn bị chặn ở máy chủ.
  // Lượt kiểm tra đang làm dở (/kiem-tra/:attemptId) không chặn: gói hết hạn giữa chừng vẫn nộp được.
  const premiumFeature = to.meta.premium as string | undefined;
  if (premiumFeature && !auth.isPremium()) {
    return { name: 'pricing', query: { 'tinh-nang': premiumFeature } };
  }

  return true;
});

// Đếm lượt xem SAU khi điều hướng xong (bị chặn/chuyển hướng thì chỉ đếm trang cuối cùng tới được)
router.afterEach((to, _from, failure) => {
  if (!failure) trackPageView(to);
});
