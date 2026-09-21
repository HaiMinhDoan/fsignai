<template>
  <!-- ===================================================================
       Dựng theo Figma 1:2 "SignKids - Bảng Đồng Hành Ba Mẹ & Thầy Cô".
       =================================================================== -->

  <!-- ===== 1. Banner + đổi người học ===== -->
  <section class="banner">
    <span class="banner-dot banner-dot--peach" aria-hidden="true"></span>
    <span class="banner-dot banner-dot--sky" aria-hidden="true"></span>

    <div class="banner-inner">
      <div class="banner-text">
        <p class="eyebrow-pill">
          <SiIcon name="user" :size="16" />
          <span>Góc Ba Mẹ &amp; Thầy Cô Đồng Hành</span>
        </p>
        <h1>
          Đồng Hành Cùng Con — Tiến Trình Ký Hiệu
          <template v-if="activeChild"> {{ activeChild.fullName }}</template>
        </h1>
        <p class="banner-sub">
          Theo dõi từng bước tiến nhỏ, nắm bắt thói quen học và đồng hành giao tiếp ký hiệu
          ấm áp mỗi ngày.
        </p>
      </div>

      <div v-if="activeChild" class="child-card">
        <img :src="guardianAvatar" :alt="`Ảnh đại diện của ${activeChild.fullName}`" />
        <div class="child-info">
          <p class="child-name">{{ activeChild.fullName }}</p>
          <span class="pill pill--peach">{{ ageLabel(activeChild.ageRange) }}</span>
          <p class="child-week">Cột mốc tuần: Hoàn thành {{ activeChild.weekPercent }}%</p>
        </div>
        <button
          v-if="children.length > 1"
          type="button"
          class="swap-btn"
          @click="nextChild"
        >
          <SiIcon name="repeat" :size="14" />
          <span>Đổi người học</span>
        </button>
      </div>
    </div>
  </section>

  <!-- ===== Chưa đăng nhập / chưa liên kết ===== -->
  <section v-if="!auth.isLoggedIn()" class="panel empty-panel">
    <h2>Đăng nhập để xem báo cáo của con</h2>
    <p>Bảng này chỉ hiện với tài khoản đã được liên kết với người học.</p>
    <RouterLink to="/dang-nhap" class="btn btn--amber">Đăng nhập</RouterLink>
  </section>

  <section v-else-if="loading" class="panel empty-panel">
    <p>Đang tải báo cáo…</p>
  </section>

  <section v-else-if="!children.length" class="panel link-panel">
    <h2>Chưa liên kết với người học nào</h2>
    <p class="link-desc">
      Người học mở trang này trên máy của mình, bấm <strong>Lấy mã của tôi</strong> rồi đọc mã cho
      phụ huynh. Phụ huynh nhập mã vào ô bên dưới là xong.
    </p>

    <div class="link-grid">
      <div class="link-box">
        <h3>Người học lấy mã</h3>
        <button type="button" class="btn btn--sky" :disabled="inviting" @click="makeInvite">
          <SiIcon name="sparkles" :size="18" />
          <span>{{ inviting ? 'Đang tạo…' : 'Lấy mã của tôi' }}</span>
        </button>
        <p v-if="inviteCode" class="invite-code">{{ inviteCode }}</p>
        <p v-if="inviteCode" class="invite-note">Mã dùng được trong 24 giờ.</p>
      </div>

      <form class="link-box" @submit.prevent="doClaim">
        <h3>Phụ huynh nhập mã</h3>
        <label class="si-visually-hidden" for="code">Mã liên kết</label>
        <input
          id="code"
          v-model="claimCode"
          class="code-input"
          type="text"
          maxlength="6"
          autocomplete="off"
          placeholder="VD: K7M2PQ"
        />
        <button type="submit" class="btn btn--amber" :disabled="claiming || !claimCode">
          <SiIcon name="check" :size="18" />
          <span>{{ claiming ? 'Đang nối…' : 'Nhận con' }}</span>
        </button>
        <p v-if="claimError" class="claim-error">{{ claimError }}</p>
      </form>
    </div>
  </section>

  <template v-else-if="report">
    <!-- ===== 2. Bốn ô chỉ số ===== -->
    <section class="metric-row">
      <article class="metric">
        <p class="metric-label">Thời gian học tuần</p>
        <p class="metric-value">
          <strong>{{ Math.floor(report.weekMinutes / 60) }}</strong><small>giờ</small>
          <strong>{{ report.weekMinutes % 60 }}</strong><small>phút</small>
        </p>
        <p class="metric-note" :class="trendClass">{{ trendLabel }}</p>
      </article>

      <article class="metric">
        <p class="metric-label">Ký hiệu thành thạo</p>
        <p class="metric-value"><strong>{{ report.masteredSigns }}</strong><small>từ vựng</small></p>
        <p class="metric-note metric-note--muted">Đếm những từ người học đã ôn đúng ít nhất một lần</p>
      </article>

      <article class="metric">
        <p class="metric-label">Độ chuẩn xác Camera AI</p>
        <!-- null chứ không phải 0: chưa dựng service chấm điểm. Hiện số 0 sẽ
             bị đọc thành "người học làm sai hết" -->
        <p v-if="report.aiAccuracyPercent === null" class="metric-value metric-value--off">
          <strong>—</strong>
        </p>
        <p v-else class="metric-value">
          <strong>{{ report.aiAccuracyPercent }}</strong><small>%</small>
        </p>
        <p class="metric-note metric-note--muted">
          {{ report.aiAccuracyPercent === null ? 'Chấm điểm bằng camera chưa bật' : 'Khớp vị trí ngón tay' }}
        </p>
      </article>

      <article class="metric">
        <p class="metric-label">Chuỗi chuyên cần</p>
        <p class="metric-value">
          <strong>{{ report.streakDays }}</strong><small>ngày liên tiếp</small>
        </p>
        <p class="metric-note metric-note--muted">
          {{
            report.streakDays >= 7
              ? 'Người học đã giữ chuỗi trọn một tuần!'
              : `Còn ${7 - report.streakDays} ngày nữa là trọn tuần`
          }}
        </p>
      </article>
    </section>

    <!-- ===== 3. Biểu đồ tuần + phân bố chủ đề ===== -->
    <section class="chart-row">
      <article class="panel chart-panel">
        <header class="panel-head">
          <div>
            <p class="eyebrow">TẦN SUẤT HỌC HÀNG NGÀY</p>
            <h2>Biểu Đồ Hoạt Động Trong Tuần</h2>
          </div>
          <span class="pill pill--lav">Mục tiêu: {{ GOAL_MINUTES }} phút/ngày</span>
        </header>

        <div class="chart">
          <div class="chart-axis">
            <span v-for="t in axisTicks" :key="t">{{ t }}m</span>
          </div>
          <ol class="chart-bars">
            <li v-for="d in report.week" :key="d.date" class="chart-col">
              <!-- Cột nói bằng cả chiều cao, màu LẪN con số ghi trên đầu (§2.2) -->
              <span class="chart-num">{{ d.minutes }}m</span>
              <span
                class="chart-bar"
                :class="{ 'chart-bar--goal': d.minutes >= GOAL_MINUTES }"
                :style="{ height: `${barHeight(d.minutes)}%` }"
                :title="`${d.label}: ${d.minutes} phút`"
              ></span>
              <span class="chart-day" :class="{ 'chart-day--today': isToday(d.date) }">
                {{ d.label }}
              </span>
            </li>
          </ol>
        </div>
      </article>

      <aside class="panel">
        <header class="panel-head">
          <div>
            <p class="eyebrow">TỔNG QUAN TUẦN</p>
            <h2>Học Có Đều Không?</h2>
          </div>
        </header>

        <!-- Vòng tròn tiến độ: vẽ bằng conic-gradient, không cần thư viện biểu đồ -->
        <div class="donut-wrap">
          <div class="donut" :style="donutStyle">
            <div class="donut-hole">
              <strong>{{ daysStudied }}/7</strong>
              <small>ngày có học</small>
            </div>
          </div>
        </div>

        <ul class="legend">
          <li><span class="dot dot--goal"></span>Ngày đạt mục tiêu: {{ daysGoalMet }}</li>
          <li><span class="dot dot--some"></span>Ngày có học ít: {{ daysStudied - daysGoalMet }}</li>
          <li><span class="dot dot--none"></span>Ngày nghỉ: {{ 7 - daysStudied }}</li>
        </ul>
      </aside>
    </section>

    <!-- ===== 4. Clip nổi bật trong tuần ===== -->
    <section class="panel">
      <header class="panel-head">
        <div>
          <p class="eyebrow">KHOẢNH KHẮC ĐÁNG NHỚ</p>
          <h2>Những Ký Hiệu Vừa Học</h2>
        </div>
        <RouterLink to="/tu-dien" class="btn btn--white">
          <span>Mở thư viện</span>
          <SiIcon name="next" :size="18" />
        </RouterLink>
      </header>

      <p v-if="!recent.length" class="muted">
        Người học chưa học ký hiệu nào trong tuần này — cùng nhau mở Phòng Luyện nhé.
      </p>
      <div v-else class="clip-row">
        <RouterLink
          v-for="(s, i) in recent"
          :key="s.id"
          :to="{ name: 'dictionary-detail', params: { id: s.id } }"
          class="clip"
        >
          <div class="clip-shot">
            <img
              :src="s.thumbnailUrl || CLIP_FALLBACK[i % 4]"
              :alt="`Ký hiệu của từ ${s.wordVi}`"
              loading="lazy"
            />
          </div>
          <strong>{{ s.wordVi }}</strong>
          <small>{{ s.primaryTopicNameVi || 'Chưa xếp chủ đề' }}</small>
        </RouterLink>
      </div>
    </section>

    <!-- ===== 5 + 6. Gợi ý ở nhà và trợ năng ===== -->
    <section class="guide-row">
      <article class="panel">
        <header class="panel-head">
          <div>
            <p class="eyebrow">CÙNG CHƠI Ở NHÀ</p>
            <h2>Ba Trò Trước Giờ Ngủ</h2>
          </div>
        </header>
        <ol class="guide-list">
          <li v-for="g in HOME_GAMES" :key="g.title">
            <span class="guide-no"><SiIcon name="star" :size="16" /></span>
            <div>
              <strong>{{ g.title }}</strong>
              <p>{{ g.desc }}</p>
            </div>
          </li>
        </ol>
      </article>

      <aside class="panel">
        <header class="panel-head">
          <div>
            <p class="eyebrow">TRỢ NĂNG</p>
            <h2>Cài Đặt Cho Người Học</h2>
          </div>
        </header>
        <ul class="a11y-list">
          <li>
            <span>Phụ đề cỡ lớn</span>
            <strong :class="a11y.bigCaption ? 'on' : 'off'">
              {{ a11y.bigCaption ? 'Đang bật' : 'Đang tắt' }}
            </strong>
          </li>
          <li>
            <span>Tốc độ phát video</span>
            <strong class="on">{{ a11y.playbackRate }}x</strong>
          </li>
          <li>
            <span>Chấm điểm bằng camera</span>
            <strong class="off">Chưa bật</strong>
          </li>
        </ul>
        <p class="muted">
          Đổi hai mục đầu ngay trên thanh trợ năng ở đầu trang; cài đặt theo người học trên máy này.
        </p>
      </aside>
    </section>
  </template>
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';
  import {
    childReportApi,
    childrenApi,
    claimChildApi,
    createInviteApi,
    type ChildReport,
    type ChildSummary,
  } from '@/api/guardian';
  import { dictionarySearchApi, type SignSummary } from '@/api/dictionary';
  import { useAuthStore } from '@/stores/auth';
  import { useA11yStore } from '@/stores/a11y';
  import SiIcon from '@/components/SiIcon.vue';

  import guardianAvatar from '@/assets/figma/guardian-avatar.png';
  import clip1 from '@/assets/figma/clip-1.png';
  import clip2 from '@/assets/figma/clip-2.png';
  import clip3 from '@/assets/figma/clip-3.png';
  import clip4 from '@/assets/figma/clip-4.png';

  defineOptions({ name: 'GuardianDashboardView' });

  const CLIP_FALLBACK = [clip1, clip2, clip3, clip4];
  const GOAL_MINUTES = 30;
  const AGE_LABELS: Record<string, string> = {
    UNDER_12: 'Dưới 12 tuổi',
    AGE_12_17: '12–17 tuổi',
    AGE_18_24: '18–24 tuổi',
    AGE_25_34: '25–34 tuổi',
    AGE_35_49: '35–49 tuổi',
    AGE_50_PLUS: 'Trên 50 tuổi',
  };
  const HOME_GAMES = [
    {
      title: 'Đố tay trước giờ ngủ',
      desc: 'Phụ huynh làm một ký hiệu, người học đoán. Đổi vai sau mỗi lượt để người học được làm thầy.',
    },
    {
      title: 'Gọi tên đồ vật trong phòng',
      desc: 'Chỉ vào một đồ vật rồi cùng người học tra ký hiệu của nó trong Thư Viện Cử Chỉ.',
    },
    {
      title: 'Kể chuyện bằng ba ký hiệu',
      desc: 'Chọn ba từ người học vừa học và cùng nghĩ ra một câu chuyện ngắn nối chúng lại.',
    },
  ];

  const auth = useAuthStore();
  const a11y = useA11yStore();

  const children = ref<ChildSummary[]>([]);
  const activeIndex = ref(0);
  const report = ref<ChildReport | null>(null);
  const recent = ref<SignSummary[]>([]);
  const loading = ref(true);

  const inviteCode = ref('');
  const inviting = ref(false);
  const claimCode = ref('');
  const claiming = ref(false);
  const claimError = ref('');

  const activeChild = computed(() => children.value[activeIndex.value]);
  const ageLabel = (a?: string) => (a ? (AGE_LABELS[a] ?? a) : 'Chưa rõ tuổi');

  const daysStudied = computed(
    () => report.value?.week.filter((d) => d.minutes > 0).length ?? 0,
  );
  const daysGoalMet = computed(
    () => report.value?.week.filter((d) => d.minutes >= GOAL_MINUTES).length ?? 0,
  );

  const trendLabel = computed(() => {
    const r = report.value;
    if (!r) return '';
    if (!r.prevWeekMinutes) return 'Tuần đầu tiên của người học';
    const diff = Math.round(((r.weekMinutes - r.prevWeekMinutes) / r.prevWeekMinutes) * 100);
    if (diff === 0) return 'Bằng tuần trước';
    return diff > 0 ? `+${diff}% so với tuần trước` : `${diff}% so với tuần trước`;
  });
  const trendClass = computed(() => {
    const r = report.value;
    if (!r || !r.prevWeekMinutes) return 'metric-note--muted';
    return r.weekMinutes >= r.prevWeekMinutes ? 'metric-note--up' : 'metric-note--down';
  });

  /** Trục dọc co theo ngày cao nhất, tối thiểu vẫn hiện mốc mục tiêu */
  const chartMax = computed(() => {
    const top = Math.max(...(report.value?.week.map((d) => d.minutes) ?? [0]), GOAL_MINUTES + 15);
    return Math.ceil(top / 15) * 15;
  });
  const axisTicks = computed(() => {
    const m = chartMax.value;
    return [m, Math.round((m * 2) / 3), Math.round(m / 3), 0];
  });
  const barHeight = (m: number) => Math.round((m / chartMax.value) * 100);
  const isToday = (iso: string) => iso === new Date().toISOString().slice(0, 10);

  const donutStyle = computed(() => {
    const met = (daysGoalMet.value / 7) * 360;
    const some = (daysStudied.value / 7) * 360;
    return {
      background: `conic-gradient(var(--sk-green) 0deg ${met}deg, var(--sk-amber) ${met}deg ${some}deg, var(--sk-blue-150) ${some}deg 360deg)`,
    };
  });

  async function loadReport() {
    const child = activeChild.value;
    if (!child) return;
    try {
      report.value = await childReportApi(child.childUserId);
    } catch {
      report.value = null;
    }
  }

  function nextChild() {
    activeIndex.value = (activeIndex.value + 1) % children.value.length;
    loadReport();
  }

  async function makeInvite() {
    inviting.value = true;
    try {
      inviteCode.value = (await createInviteApi()).inviteCode;
    } catch (e) {
      inviteCode.value = '';
      claimError.value = (e as Error).message;
    } finally {
      inviting.value = false;
    }
  }

  async function doClaim() {
    claiming.value = true;
    claimError.value = '';
    try {
      await claimChildApi(claimCode.value.trim().toUpperCase());
      claimCode.value = '';
      await boot(); // nối xong thì tải lại danh sách để hiện báo cáo ngay
    } catch (e) {
      claimError.value = (e as Error).message;
    } finally {
      claiming.value = false;
    }
  }

  async function boot() {
    loading.value = true;
    try {
      children.value = await childrenApi();
      activeIndex.value = 0;
      if (children.value.length) {
        await loadReport();
        // Vài ký hiệu gần đây để minh hoạ; kho chưa gắn lịch sử theo người học nên
        // lấy từ đầu danh sách thay vì bịa ra một "lịch sử" không có thật
        const page = await dictionarySearchApi({ page: 0, size: 4 });
        recent.value = page.items;
      }
    } catch {
      children.value = [];
    } finally {
      loading.value = false;
    }
  }

  onMounted(() => {
    if (!auth.isLoggedIn()) {
      loading.value = false;
      return;
    }
    boot();
  });
</script>

<style scoped>
  .eyebrow {
    margin: 0 0 4px;
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 1.2px;
    color: var(--sk-amber-ink);
  }
  .eyebrow-pill {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    height: 30px;
    margin: 0 0 10px;
    padding: 0 16px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-surface);
    color: var(--sk-amber-ink);
    font-family: var(--sk-font-head);
    font-size: 13px;
    font-weight: 700;
  }
  .muted {
    margin: 0;
    font-size: 14px;
    color: var(--sk-brown);
  }

  .pill {
    display: inline-flex;
    align-items: center;
    height: 24px;
    padding: 0 12px;
    border-radius: var(--sk-r-pill);
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
  }
  .pill--peach {
    background: var(--sk-peach);
    color: var(--sk-amber-ink);
  }
  .pill--lav {
    background: var(--sk-lavender-2);
    color: var(--sk-blue-dark);
  }

  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 44px;
    padding: 0 22px;
    border: none;
    border-radius: var(--sk-r-pill);
    font-family: var(--sk-font-head);
    font-size: 15px;
    font-weight: 700;
    text-decoration: none;
    cursor: pointer;
  }
  .btn--amber {
    background: var(--sk-amber);
    color: var(--sk-amber-mid);
  }
  .btn--sky {
    background: var(--sk-sky);
    color: var(--sk-blue-dark);
  }
  .btn--white {
    background: var(--sk-surface);
    color: var(--sk-blue-ink);
    box-shadow: var(--sk-shadow-card);
  }
  .btn:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }

  .panel {
    background: var(--sk-surface);
    border-radius: var(--sk-r-block);
    padding: 28px;
    box-shadow: var(--sk-shadow-card);
    margin-bottom: 24px;
  }
  .panel-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20px;
    flex-wrap: wrap;
    margin-bottom: 18px;
  }
  .panel-head h2 {
    margin: 0;
    font-size: 21px;
  }

  /* ===== 1. Banner ===== */
  .banner {
    position: relative;
    overflow: hidden;
    background: var(--sk-lavender);
    border-radius: var(--sk-r-block);
    padding: 40px;
    margin-bottom: 24px;
  }
  .banner-dot {
    position: absolute;
    border-radius: 50%;
    filter: blur(70px);
    pointer-events: none;
  }
  .banner-dot--peach {
    width: 320px;
    height: 320px;
    background: var(--sk-peach);
    opacity: 0.55;
    top: -120px;
    right: 24%;
  }
  .banner-dot--sky {
    width: 256px;
    height: 256px;
    background: var(--sk-sky);
    opacity: 0.6;
    bottom: -110px;
    right: -40px;
  }
  .banner-inner {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 40px;
    flex-wrap: wrap;
  }
  .banner-text {
    flex: 1 1 560px;
    min-width: 0;
  }
  .banner-text h1 {
    margin: 0 0 10px;
    font-size: 36px;
  }
  .banner-sub {
    margin: 0;
    font-size: 16px;
    font-weight: 500;
    color: var(--sk-brown);
    max-width: 62ch;
  }

  .child-card {
    display: flex;
    align-items: center;
    gap: 16px;
    background: var(--sk-surface);
    border-radius: var(--sk-r-card);
    padding: 18px 22px;
    box-shadow: var(--sk-shadow-card);
  }
  .child-card img {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    object-fit: cover;
    background: var(--sk-peach);
  }
  .child-name {
    margin: 0 0 4px;
    font-family: var(--sk-font-head);
    font-size: 19px;
    font-weight: 700;
  }
  .child-week {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--sk-brown);
  }
  .swap-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    height: 32px;
    padding: 0 14px;
    border: none;
    border-radius: var(--sk-r-pill);
    background: var(--sk-lavender-2);
    color: var(--sk-blue-dark);
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
  }

  /* ===== Trạng thái rỗng / liên kết ===== */
  .empty-panel {
    text-align: center;
  }
  .empty-panel h2 {
    margin: 0 0 6px;
  }
  .empty-panel p {
    margin: 0 0 16px;
    color: var(--sk-brown);
  }
  .link-panel h2 {
    margin: 0 0 6px;
  }
  .link-desc {
    margin: 0 0 20px;
    color: var(--sk-brown);
    max-width: 70ch;
  }
  .link-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 20px;
  }
  .link-box {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    background: var(--sk-lavender);
    border-radius: var(--sk-r-card);
    padding: 22px;
  }
  .link-box h3 {
    margin: 0;
    font-size: 17px;
  }
  .invite-code {
    margin: 0;
    padding: 10px 20px;
    border-radius: 14px;
    background: var(--sk-surface);
    font-family: var(--sk-font-mono);
    font-size: 30px;
    font-weight: 700;
    letter-spacing: 6px;
    color: var(--sk-amber-ink);
  }
  .invite-note,
  .claim-error {
    margin: 0;
    font-size: 13px;
    color: var(--sk-brown);
  }
  .claim-error {
    color: #c4503f;
    font-weight: 700;
  }
  .code-input {
    width: 100%;
    height: 52px;
    padding: 0 18px;
    border: 2px solid var(--sk-blue-150);
    border-radius: 16px;
    background: var(--sk-surface);
    color: var(--sk-ink);
    font-family: var(--sk-font-mono);
    font-size: 22px;
    font-weight: 700;
    letter-spacing: 4px;
    text-transform: uppercase;
  }

  /* ===== 2. Bốn ô chỉ số ===== */
  .metric-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 20px;
    margin-bottom: 24px;
  }
  .metric {
    background: var(--sk-surface);
    border-radius: var(--sk-r-card);
    padding: 22px;
    box-shadow: var(--sk-shadow-card);
  }
  .metric-label {
    margin: 0 0 6px;
    font-family: var(--sk-font-head);
    font-size: 14px;
    font-weight: 700;
    color: var(--sk-brown);
  }
  .metric-value {
    display: flex;
    align-items: baseline;
    gap: 6px;
    margin: 0 0 8px;
    flex-wrap: wrap;
  }
  .metric-value strong {
    font-family: var(--sk-font-head);
    font-size: 32px;
    font-weight: 700;
    color: var(--sk-ink);
  }
  .metric-value small {
    font-size: 18px;
    font-weight: 500;
    color: var(--sk-brown);
  }
  .metric-value--off strong {
    color: var(--sk-brown);
  }
  .metric-note {
    margin: 0;
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
  }
  .metric-note--up {
    color: var(--sk-green-ink);
  }
  .metric-note--down {
    color: var(--sk-amber-ink);
  }
  .metric-note--muted {
    font-weight: 600;
    color: var(--sk-brown);
  }

  /* ===== 3. Biểu đồ ===== */
  .chart-row {
    display: grid;
    grid-template-columns: minmax(0, 811fr) minmax(0, 389fr);
    gap: 20px;
  }
  .chart-panel {
    min-width: 0;
  }
  .chart {
    display: flex;
    gap: 14px;
    height: 260px;
  }
  .chart-axis {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    padding-bottom: 26px;
    font-size: 12px;
    color: var(--sk-brown);
  }
  .chart-bars {
    flex: 1;
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 10px;
    list-style: none;
    margin: 0;
    padding: 0;
    border-left: 2px solid var(--sk-lavender-2);
    border-bottom: 2px solid var(--sk-lavender-2);
  }
  .chart-col {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-end;
    height: 100%;
    gap: 4px;
  }
  .chart-num {
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    color: var(--sk-brown);
  }
  .chart-bar {
    width: 100%;
    max-width: 44px;
    min-height: 4px;
    border-radius: 10px 10px 0 0;
    background: var(--sk-sky);
    transition: height 320ms ease;
  }
  .chart-bar--goal {
    background: var(--sk-amber);
  }
  .chart-day {
    font-size: 13px;
    font-weight: 600;
    color: var(--sk-brown);
    margin-top: 4px;
  }
  .chart-day--today {
    font-weight: 800;
    color: var(--sk-blue-ink);
  }

  .donut-wrap {
    display: grid;
    place-items: center;
    padding: 10px 0 18px;
  }
  .donut {
    width: 180px;
    height: 180px;
    border-radius: 50%;
    display: grid;
    place-items: center;
  }
  .donut-hole {
    width: 122px;
    height: 122px;
    border-radius: 50%;
    background: var(--sk-surface);
    display: grid;
    place-items: center;
    text-align: center;
  }
  .donut-hole strong {
    font-family: var(--sk-font-head);
    font-size: 26px;
    font-weight: 700;
  }
  .donut-hole small {
    font-size: 12px;
    color: var(--sk-brown);
  }
  .legend {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 8px;
    font-size: 14px;
    color: var(--sk-brown);
  }
  .legend li {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
  }
  .dot--goal {
    background: var(--sk-green);
  }
  .dot--some {
    background: var(--sk-amber);
  }
  .dot--none {
    background: var(--sk-blue-150);
  }

  /* ===== 4. Clip ===== */
  .clip-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
    gap: 20px;
  }
  .clip {
    display: flex;
    flex-direction: column;
    gap: 4px;
    text-decoration: none;
    color: var(--sk-ink);
  }
  .clip-shot {
    aspect-ratio: 293 / 224;
    border-radius: var(--sk-r-card);
    overflow: hidden;
    /* Khung ảnh ký hiệu trung tính (§2.1) */
    background: var(--sk-stage);
    margin-bottom: 6px;
  }
  .clip-shot img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .clip strong {
    font-family: var(--sk-font-head);
    font-size: 16px;
  }
  .clip small {
    font-size: 13px;
    color: var(--sk-brown);
  }

  /* ===== 5 + 6 ===== */
  .guide-row {
    display: grid;
    grid-template-columns: minmax(0, 811fr) minmax(0, 389fr);
    gap: 20px;
  }
  .guide-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .guide-list li {
    display: flex;
    gap: 14px;
  }
  .guide-no {
    display: grid;
    place-items: center;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: var(--sk-peach);
    color: var(--sk-amber-ink);
    flex-shrink: 0;
  }
  .guide-list strong {
    font-family: var(--sk-font-head);
    font-size: 16px;
  }
  .guide-list p {
    margin: 2px 0 0;
    font-size: 14px;
    color: var(--sk-brown);
  }

  .a11y-list {
    list-style: none;
    margin: 0 0 14px;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .a11y-list li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 12px 16px;
    border-radius: 16px;
    background: var(--sk-lavender);
    font-size: 14px;
  }
  .a11y-list strong {
    font-family: var(--sk-font-head);
    font-size: 13px;
  }
  .a11y-list .on {
    color: var(--sk-green-ink);
  }
  .a11y-list .off {
    color: var(--sk-brown);
  }

  @media (max-width: 980px) {
    .chart-row,
    .guide-row {
      grid-template-columns: 1fr;
    }
    .banner {
      padding: 28px;
    }
    .banner-text h1 {
      font-size: 28px;
    }
  }
</style>
