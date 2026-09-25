<template>
  <a class="skip-link" href="#main-content">Bỏ qua tới nội dung chính</a>

  <!-- ===== Thanh trợ năng (Figma: Top Accessibility Quick Toolbar) =====
       Đặt TRÊN cùng, trên cả header: với người cần phụ đề to hay video chậm thì
       đây là thứ phải chạm tới trước, không phải thứ đi tìm trong cài đặt. -->
  <div class="a11y-bar">
    <div class="a11y-inner">
      <div class="a11y-label">
        <strong>Trợ Năng Cho Bạn:</strong>
        <span>Tối ưu thị giác &amp; tốc độ thao tác tay</span>
      </div>

      <div class="a11y-controls">
        <button
          type="button"
          class="a11y-pill"
          :class="{ 'a11y-pill--on': a11y.bigCaption }"
          :aria-pressed="a11y.bigCaption"
          @click="a11y.toggleBigCaption()"
        >
          Phụ đề To: {{ a11y.bigCaption ? 'BẬT' : 'TẮT' }}
        </button>

        <span class="a11y-rate-label">Tốc độ:</span>
        <button
          v-for="r in RATES"
          :key="r"
          type="button"
          class="a11y-pill a11y-pill--rate"
          :class="{ 'a11y-pill--on': a11y.playbackRate === r }"
          :aria-pressed="a11y.playbackRate === r"
          @click="a11y.setRate(r)"
        >
          {{ r }}x
        </button>
      </div>
    </div>
  </div>

  <header class="header">
    <div class="header-inner">
      <RouterLink to="/" class="logo">
        <img :src="logoUrl" alt="" class="logo-mark" />
        <span class="logo-text"><span class="logo-a">Sign</span><span class="logo-b">AI</span></span>
      </RouterLink>

      <nav class="nav" aria-label="Điều hướng chính">
        <RouterLink v-for="item in nav" :key="item.to" :to="item.to" class="nav-link">
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="header-actions">
        <template v-if="auth.isLoggedIn()">
          <!-- Số liệu nói bằng icon + số + đơn vị, không dựa vào màu (§2.2) -->
          <span class="stat-pill stat-pill--streak" :title="`Chuỗi ${statsStore.stats.streakDays} ngày học liên tiếp`">
            🔥 {{ statsStore.stats.streakDays }}&nbsp;<span class="stat-unit">ngày</span>
          </span>
          <span class="stat-pill stat-pill--star" :title="`${statsStore.stats.stars} sao đã thu thập`">
            ⭐ {{ statsStore.stats.stars }}&nbsp;<span class="stat-unit">Sao</span>
          </span>
          <RouterLink to="/thu-vien" class="icon-btn" title="Thư viện của tôi">
            <SiIcon name="star" :size="20" />
            <span class="si-visually-hidden">Thư viện của tôi</span>
          </RouterLink>
          <NotificationBell />
          <span class="user-chip">
            <span class="user-avatar"><SiIcon name="user" :size="18" /></span>
            <span class="user-name">{{ auth.user?.realName }}</span>
          </span>
          <button class="icon-btn" type="button" title="Đăng xuất" @click="handleLogout">
            <SiIcon name="logout" :size="20" />
            <span class="si-visually-hidden">Đăng xuất</span>
          </button>
        </template>
        <template v-else>
          <RouterLink to="/dang-nhap" class="btn btn-ghost">Đăng nhập</RouterLink>
          <RouterLink to="/dang-ky" class="btn btn-primary">Bắt đầu học</RouterLink>
        </template>
      </div>
    </div>
  </header>

  <main id="main-content" class="content">
    <RouterView />
  </main>

  <footer class="footer">
    <div class="footer-inner">
      <div class="footer-top">
        <div class="footer-brand">
          <img :src="logoUrl" alt="" class="logo-mark" />
          <div>
            <p class="footer-name">SignAI</p>
            <p class="footer-tagline">Học Ngôn Ngữ Ký Hiệu Bằng Cả Niềm Vui</p>
          </div>
        </div>

        <ul class="footer-chips">
          <li class="chip-mint">Phụ đề trực quan</li>
          <li class="chip-sky">Tương phản cao</li>
          <li class="chip-peach">Hỗ trợ camera AI</li>
        </ul>
      </div>

      <p class="footer-copy">
        © 2025 SignAI — Học Ngôn Ngữ Ký Hiệu Việt Nam. Thiết kế thân thiện cho mọi lứa tuổi:
        người học, phụ huynh và thầy cô.
      </p>
    </div>
  </footer>
</template>

<script lang="ts" setup>
  import { computed, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import { useAuthStore } from '@/stores/auth';
  import { useA11yStore } from '@/stores/a11y';
  import { useLearnerStatsStore } from '@/stores/learnerStats';
  import SiIcon from '@/components/SiIcon.vue';
  import logoUrl from '@/assets/logo/fsignai-logo.png';
  import NotificationBell from '@/components/NotificationBell.vue';

  defineOptions({ name: 'DefaultLayout' });

  const BASE_NAV = [
    { to: '/', label: 'Trang Chủ' },
    { to: '/khoa-hoc', label: 'Khoá Học' },
    { to: '/phong-luyen', label: 'Phòng Luyện Ký Hiệu' },
    { to: '/on-tu', label: 'Ôn Từ' },
    { to: '/kiem-tra', label: 'Kiểm Tra' },
    { to: '/goc-tro-choi', label: 'Góc Trò Chơi' },
    { to: '/tu-dien', label: 'Thư Viện Cử Chỉ' },
    { to: '/ba-me-thay-co', label: 'Dành Cho Ba Mẹ & Thầy Cô' },
  ];
  const RATES = [0.5, 0.75, 1];

  const auth = useAuthStore();
  const a11y = useA11yStore();
  const statsStore = useLearnerStatsStore();
  const router = useRouter();

  // Diễn đàn ẩn với tài khoản trẻ em (CHILD) - chỉ ẩn menu, backend vẫn cho phép
  // truy cập trực tiếp nếu biết đường dẫn, đây là quyết định sản phẩm đã chốt.
  const nav = computed(() =>
    auth.user?.accountKind === 'CHILD'
      ? BASE_NAV
      : [...BASE_NAV, { to: '/dien-dan', label: 'Diễn Đàn' }],
  );

  // Tải số liệu ngay khi biết người học đã đăng nhập. Theo dõi trạng thái thay vì
  // gọi trong onMounted vì phiên đăng nhập được khôi phục BẤT ĐỒNG BỘ ở
  // router guard — lúc layout dựng xong có thể vẫn chưa biết người học là ai.
  watch(
    () => auth.user?.userId,
    (id) => {
      if (id) statsStore.load(true);
      else statsStore.reset();
    },
    { immediate: true },
  );

  function handleLogout() {
    auth.logout();
    statsStore.reset();
    router.push('/');
  }
</script>

<style scoped>
  .skip-link {
    position: absolute;
    left: -9999px;
    top: 0;
    background: var(--sk-blue-ink);
    color: #fff;
    padding: 12px 18px;
    z-index: 100;
    border-radius: 0 0 12px 0;
    font-weight: 700;
  }
  .skip-link:focus {
    left: 0;
  }

  /* ===== Thanh trợ năng ===== */
  .a11y-bar {
    background: var(--sk-blue-100);
    color: var(--sk-blue-dark);
  }
  .a11y-inner {
    max-width: 1232px;
    margin: 0 auto;
    padding: 6px 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    flex-wrap: wrap;
  }
  .a11y-label {
    display: flex;
    align-items: baseline;
    gap: 10px;
    font-size: 14px;
  }
  .a11y-label strong {
    font-family: var(--sk-font-head);
    font-size: 16px;
    font-weight: 700;
  }
  .a11y-controls {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
  }
  .a11y-rate-label {
    font-size: 12px;
    font-weight: 700;
    margin-left: 6px;
  }
  .a11y-pill {
    min-height: 28px;
    padding: 0 12px;
    border-radius: var(--sk-r-pill);
    border: 2px solid transparent;
    background: var(--sk-surface);
    color: var(--sk-blue-dark);
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
  }
  .a11y-pill--rate {
    min-width: 46px;
  }
  /* Bật/tắt nhận ra được bằng cả viền, nền lẫn chữ — không chỉ bằng màu */
  .a11y-pill--on {
    background: var(--sk-mint);
    border-color: var(--sk-green-ink);
    color: var(--sk-green-dark);
  }

  /* ===== Header ===== */
  .header {
    position: sticky;
    top: 0;
    z-index: 10;
    background: var(--sk-surface);
    box-shadow: var(--sk-shadow);
  }
  /* Hai hàng: logo + nút bên phải ở hàng trên, thanh điều hướng ở hàng dưới.
     Chín mục điều hướng cần ~1120px, cộng logo và cụm nút bên phải là ~1720px — rộng hơn cả khung
     nội dung 1232px, nên xếp một hàng thì header luôn tràn ra ngoài màn hình (kể cả màn 1920px).
     Cho nav xuống hàng riêng thì nó vừa khít khung và không phải cuộn ngang. */
  .header-inner {
    max-width: 1232px;
    margin: 0 auto;
    padding: 10px 24px;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px 20px;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 10px;
    text-decoration: none;
    flex-shrink: 0;
  }
  /* Logo đã có nền gradient của riêng nó, không bọc thêm ô màu nào nữa */
  .logo-mark {
    width: 36px;
    height: 36px;
    object-fit: contain;
    flex-shrink: 0;
  }
  .logo-text {
    font-family: var(--sk-font-head);
    font-size: 22px;
    font-weight: 700;
    letter-spacing: -0.3px;
  }
  .logo-a {
    color: var(--sk-blue-ink);
  }
  .logo-b {
    color: var(--sk-amber-ink);
  }

  /* Cả cụm nav nằm trong một viên thuốc lavender, đúng Figma */
  .nav {
    order: 3; /* xuống hàng dưới logo và cụm nút bên phải */
    width: 100%;
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px;
    background: var(--sk-lavender);
    border-radius: var(--sk-r-pill);
    justify-content: center;
    /* Màn hẹp thì cuộn ngang trong viên thuốc, không đẩy cả trang tràn ra */
    overflow-x: auto;
    scrollbar-width: thin;
  }
  .nav-link {
    display: inline-flex;
    align-items: center;
    min-height: 40px;
    padding: 0 14px;
    border-radius: var(--sk-r-pill);
    font-family: var(--sk-font-head);
    font-size: 14px;
    font-weight: 700;
    color: var(--sk-brown);
    text-decoration: none;
    text-align: center;
    white-space: nowrap;
    transition: background 160ms ease;
  }
  .nav-link:hover {
    background: var(--sk-blue-100);
  }
  .nav-link.router-link-exact-active {
    background: var(--sk-surface);
    color: var(--sk-blue-ink);
    box-shadow: var(--sk-shadow);
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
    margin-left: auto; /* dính mép phải của hàng trên, cạnh logo */
  }

  .stat-pill {
    display: inline-flex;
    align-items: center;
    height: 30px;
    padding: 0 12px;
    border-radius: var(--sk-r-pill);
    font-size: 13px;
    font-weight: 700;
    white-space: nowrap;
  }
  .stat-pill--streak {
    background: var(--sk-peach);
    color: var(--sk-amber-ink);
  }
  .stat-pill--star {
    background: var(--sk-sky);
    color: var(--sk-blue-ink);
  }

  .user-chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 40px;
    padding: 0 12px 0 4px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-lavender-2);
    max-width: 180px;
  }
  .user-avatar {
    display: grid;
    place-items: center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--sk-amber-ink);
    color: #fff;
    flex-shrink: 0;
  }
  .user-name {
    font-size: 13px;
    font-weight: 700;
    color: var(--sk-brown);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  /* .icon-btn nằm ở styles/tokens.css — chuông thông báo là component riêng, mà style scoped
     không với tới phần tử bên trong component con (xem ghi chú trong tokens.css). */

  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 40px;
    padding: 0 18px;
    border-radius: var(--sk-r-pill);
    font-family: var(--sk-font-head);
    font-weight: 700;
    font-size: 14px;
    text-decoration: none;
    border: 2px solid transparent;
    cursor: pointer;
  }
  .btn-primary {
    background: var(--sk-amber);
    color: var(--sk-brown-dark);
  }
  .btn-ghost {
    background: var(--sk-surface);
    color: var(--sk-blue-ink);
    border-color: var(--sk-blue-150);
  }

  /* ===== Nội dung ===== */
  .content {
    max-width: 1232px;
    margin: 0 auto;
    padding: 24px 24px 64px;
    min-height: 60vh;
  }

  /* ===== Footer ===== */
  .footer {
    background: var(--sk-lavender);
  }
  .footer-inner {
    max-width: 1232px;
    margin: 0 auto;
    padding: 32px 24px;
  }
  .footer-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
    flex-wrap: wrap;
    margin-bottom: 20px;
  }
  .footer-brand {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .footer-name {
    margin: 0;
    font-family: var(--sk-font-head);
    font-size: 20px;
    font-weight: 700;
    color: var(--sk-blue-ink);
  }
  .footer-tagline {
    margin: 0;
    font-size: 14px;
    color: var(--sk-brown);
  }

  .footer-chips {
    display: flex;
    gap: 10px;
    list-style: none;
    margin: 0;
    padding: 0;
    flex-wrap: wrap;
  }
  .footer-chips li {
    padding: 3px 14px;
    border-radius: var(--sk-r-pill);
    font-size: 12px;
    font-weight: 700;
  }
  .chip-mint {
    background: var(--sk-mint);
    color: var(--sk-green-dark);
  }
  .chip-sky {
    background: var(--sk-sky);
    color: var(--sk-blue-dark);
  }
  .chip-peach {
    background: var(--sk-peach);
    color: var(--sk-amber-ink);
  }

  .footer-copy {
    margin: 0;
    font-size: 14px;
    color: var(--sk-brown);
  }

  /* Dưới 1100px nav 5 mục không còn chỗ — cho nó xuống hàng riêng, cuộn ngang */
  /* Màn hẹp hơn khung nav: căn trái để mục đầu tiên luôn thấy được khi cuộn ngang */
  @media (max-width: 1230px) {
    .nav {
      justify-content: flex-start;
    }
  }

  @media (max-width: 760px) {
    .header-inner,
    .a11y-inner {
      padding-left: 16px;
      padding-right: 16px;
    }
    /* Giữ ảnh đại diện làm mốc nhận biết, bỏ tên để cụm nút không đẩy tràn */
    .user-name {
      display: none;
    }
    .user-chip {
      padding: 0 4px;
    }
    .nav-link {
      padding: 0 12px;
      font-size: 13px;
    }
  }

  /* Điện thoại: rút gọn "1 ngày" thành "1", "21 Sao" thành "21" — con số vẫn còn,
     chỉ bỏ đơn vị, vì cụm nút bên phải rộng hơn cả màn hình nếu giữ nguyên */
  @media (max-width: 560px) {
    .stat-unit {
      display: none;
    }
    .stat-pill {
      padding: 0 9px;
    }
    .header-actions {
      gap: 6px;
    }
  }
</style>
