<template>
  <!-- ===================================================================
       Dựng theo Figma 1:611 "SignKids - Góc Trò Chơi Ôn Tập Ký Hiệu".
       =================================================================== -->

  <!-- ===== 1. Hero (thẻ trắng r32, 1232×256) ===== -->
  <section class="hero">
    <span class="hero-dot hero-dot--sky" aria-hidden="true"></span>
    <span class="hero-dot hero-dot--peach" aria-hidden="true"></span>

    <div class="hero-card">
      <div class="hero-text">
        <p class="eyebrow-pill">
          <SiIcon name="sparkles" :size="16" />
          <span>SÂN CHƠI CỦA BÉ</span>
        </p>
        <h1>Vừa Chơi Vừa Nhớ — Thu Thập Sao Siêu Cấp 🌟</h1>
        <p class="hero-sub">
          Luyện ngón tay dẻo dai, mắt thần quan sát và biến mỗi cử chỉ thành một trò chơi.
        </p>
      </div>

      <div class="mochi-widget">
        <div class="mochi-frame">
          <img :src="mochiWidget" alt="Gấu Mochi, linh vật của góc trò chơi" />
          <span class="mochi-tag">Mochi</span>
        </div>
        <div class="mochi-info">
          <p class="mochi-stars">
            <SiIcon name="star" :size="18" />
            <strong>{{ stats.stars }} ⭐</strong>
          </p>
          <p class="mochi-level">Cấp độ: {{ levelTitle }}</p>
        </div>
      </div>
    </div>
  </section>

  <!-- ===== 2. Bốn trò chơi ===== -->
  <section class="games" aria-labelledby="games-heading">
    <div class="section-head">
      <div>
        <p class="eyebrow">BỐN SÂN CHƠI</p>
        <h2 id="games-heading">Chọn trò bé thích 🎲</h2>
      </div>
      <span class="pill pill--peach">Cả 4 trò đều dùng kho {{ totalSigns || '—' }} từ có sẵn</span>
    </div>

    <div class="game-grid">
      <article v-for="g in GAMES" :key="g.code" class="game-card" :class="`game-card--${g.size}`">
        <div class="game-head">
          <span class="pill pill--peach">{{ g.tag }}</span>
          <span class="game-reward">+{{ g.reward }} Sao</span>
        </div>

        <h3 class="game-title">{{ g.index }}. {{ g.name }}</h3>
        <p class="game-desc">{{ g.desc }}</p>

        <!-- Sân chơi mô phỏng, nền #F0F3FF như Figma -->
        <div class="game-board">
          <p class="board-head">
            <span>{{ g.boardLeft }}</span>
            <span>{{ g.boardRight }}</span>
          </p>
          <div class="board-body">
            <span v-for="(e, i) in g.tokens" :key="i" class="board-token">{{ e }}</span>
          </div>
        </div>

        <div class="game-foot">
          <span class="game-hint">
            <SiIcon name="star" :size="15" />
            <span>{{ g.hint }}</span>
          </span>
          <button v-if="auth.isLoggedIn()" type="button" class="play-btn" @click="startPlaying(g.code, g.name)">
            <SiIcon name="play" :size="18" />
            <span>Chơi ngay</span>
          </button>
          <RouterLink v-else to="/dang-nhap" class="play-btn">
            <SiIcon name="lock" :size="18" />
            <span>Đăng nhập để chơi</span>
          </RouterLink>
        </div>
      </article>
    </div>
  </section>

  <!-- ===== 3. Bảng xếp hạng + Rương thưởng ===== -->
  <section class="board-grid">
    <!-- --- Bảng xếp hạng (705×522, trắng r32) --- -->
    <article class="panel">
      <header class="panel-head">
        <div>
          <h2>Hiệp Sĩ Ký Hiệu Tuần Này</h2>
          <p class="panel-sub">Bảng vàng vinh danh những bàn tay kiên trì nhất</p>
        </div>
        <span class="pill pill--lav">{{ weekLabel }}</span>
      </header>

      <p v-if="!auth.isLoggedIn()" class="notice">
        <RouterLink to="/dang-nhap">Đăng nhập</RouterLink> để xem bé đang đứng thứ mấy.
      </p>
      <p v-else-if="boardLoading" class="notice">Đang xếp hạng…</p>
      <p v-else-if="!leaderboard.length" class="notice">
        Chưa ai ghi điểm tuần này — bé chơi ván đầu là đứng đầu bảng ngay.
      </p>

      <ol v-else class="rank-rows">
        <li
          v-for="row in leaderboard"
          :key="row.userId"
          class="rank-row"
          :class="[`rank-row--${Math.min(row.rank, 4)}`, { 'rank-row--me': row.isMe }]"
        >
          <span class="rank-no">{{ row.rank }}</span>
          <span class="rank-body">
            <strong class="rank-name">
              {{ row.fullName }}
              <span v-if="row.isMe" class="rank-me">Bé đó!</span>
            </strong>
            <small>Đã chơi {{ row.gamesWon }} màn</small>
          </span>
          <span class="rank-points">{{ row.points.toLocaleString('vi-VN') }}</span>
        </li>
      </ol>

      <p class="quote">
        <SiIcon name="sparkles" :size="20" />
        <span>Mẹo nhỏ: chơi thêm một ván là sao của bé lại nhích lên một bậc.</span>
      </p>
    </article>

    <!-- --- Rương thưởng (495×682, trắng r32) --- -->
    <aside class="panel">
      <header class="panel-head">
        <div>
          <h2>Hộp Kho Báu Đổi Thưởng</h2>
          <p class="panel-sub">Gom đủ sao là mở được rương</p>
        </div>
      </header>

      <div class="chest-top">
        <img :src="mochiDressing" alt="Gấu Mochi trong bộ phụ kiện" class="chest-mochi" />
        <div>
          <p class="chest-now">Số sao bé tích luỹ: <strong>{{ stats.stars }} ⭐</strong></p>
          <p class="chest-sub">Mở rương để nhận phụ kiện cho Mochi</p>
        </div>
      </div>

      <p v-if="!auth.isLoggedIn()" class="notice">
        <RouterLink to="/dang-nhap">Đăng nhập</RouterLink> để mở kho báu của riêng bé.
      </p>
      <p v-else-if="chestLoading" class="notice">Đang mở kho…</p>

      <ul v-else class="chest-list">
        <li
          v-for="c in chests"
          :key="c.id"
          class="chest"
          :class="{ 'chest--ready': c.unlockable, 'chest--opened': c.opened }"
        >
          <span class="chest-icon">
            <SiIcon :name="c.opened ? 'check' : c.unlockable ? 'trophy' : 'lock'" :size="24" />
          </span>
          <span class="chest-body">
            <strong>{{ c.nameVi }}</strong>
            <small>{{ c.descriptionVi }}</small>
          </span>
          <!-- Trạng thái nói bằng cả icon lẫn chữ, không chỉ bằng màu (§2.2) -->
          <span class="chest-state">
            <template v-if="c.opened">Đã mở</template>
            <button
              v-else-if="c.unlockable"
              type="button"
              class="chest-open"
              :disabled="openingId === c.id"
              @click="openChest(c.id)"
            >
              {{ openingId === c.id ? 'Đang mở…' : 'Mở rương!' }}
            </button>
            <template v-else>Còn {{ c.pointsShort }} ⭐</template>
          </span>
        </li>
      </ul>
    </aside>
  </section>

  <GamePlayModal v-if="playing" :code="playing.code" :title="playing.title" @close="onGameClosed" />
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';
  import {
    leaderboardApi,
    rewardChestsApi,
    type LeaderboardRow,
    type RewardChest,
  } from '@/api/catalog';
  import { dictionarySearchApi } from '@/api/dictionary';
  import { openChestApi, type GameCode } from '@/api/games';
  import GamePlayModal from './GamePlayModal.vue';
  import { useAuthStore } from '@/stores/auth';
  import { useLearnerStatsStore } from '@/stores/learnerStats';
  import SiIcon from '@/components/SiIcon.vue';

  import mochiWidget from '@/assets/figma/mochi-widget.png';
  import mochiDressing from '@/assets/figma/mochi-dressing.png';

  defineOptions({ name: 'GameCornerView' });

  const auth = useAuthStore();
  const statsStore = useLearnerStatsStore();
  const stats = computed(() => statsStore.stats);

  /**
   * Bốn trò đúng như Figma 1:611. Mã khớp cột game_sessions.game_code sau
   * migration V15 — trước đó tôi tự đặt 'PICK_SIGN', không có trong thiết kế.
   */
  const GAMES = [
    {
      code: 'MATCH_PAIR',
      index: 1,
      size: 'wide',
      tag: 'Kéo Thả Trực Quan',
      reward: 50,
      name: 'Bàn Tay Vui Nhộn — Nối Hình & Ký Hiệu',
      desc: 'Bé quan sát hình dáng các ngón tay và kéo dây nối đúng với bạn thú cưng thân quen.',
      boardLeft: '👉 Bàn tay ký hiệu',
      boardRight: 'Hình con vật 👈',
      tokens: ['🐱', '🐶', '🐰', '✋', '🤟', '👌'],
      hint: '3 vòng thử thách',
    },
    {
      code: 'SPEED_GUESS',
      index: 2,
      size: 'narrow',
      tag: 'Thử Thách 10 Giây',
      reward: 40,
      name: 'Thám Tử Ký Hiệu Bí Ẩn',
      desc: 'Gấu Mochi đang làm ký hiệu gì thế nhỉ? Đồng hồ đếm ngược, bé quan sát thật nhanh nhé.',
      boardLeft: 'Thời gian: 10s',
      boardRight: 'Câu 1/5',
      tokens: ['👋', '❤️', '🙏'],
      hint: 'Luyện tinh mắt',
    },
    {
      code: 'FINGER_DANCE',
      index: 3,
      size: 'narrow',
      tag: 'Nhịp Điệu & Khớp Tay',
      reward: 60,
      name: 'Vũ Điệu Ngón Tay',
      desc: 'Bấm theo nhịp các thế tay rơi từ trên xuống, giúp khớp tay của bé dẻo dai hơn.',
      boardLeft: 'Combo ×0',
      boardRight: 'Điểm: 0 🎵',
      tokens: ['✌️', '👍', '🤟', '👌'],
      hint: 'Rèn phản xạ',
    },
    {
      code: 'MEMORY_FLIP',
      index: 4,
      size: 'wide',
      tag: 'Thẻ Bài Trí Nhớ',
      reward: 45,
      name: 'Truy Tìm Ký Hiệu Bị Ẩn',
      desc: 'Lật mở các quân bài để tìm cặp cử chỉ giống nhau, rèn khả năng ghi nhớ mặt chữ và thế tay.',
      boardLeft: 'Đã tìm: 0/3 cặp',
      boardRight: 'Lật thẻ tự do',
      tokens: ['👋', '🙋', '?', '👨‍👩‍👧', '?', '?'],
      hint: 'Không giới hạn lượt',
    },
  ] as const;

  const LEVEL_TITLES = [
    'Bé Mới Tập Ký Hiệu 🌱',
    'Bạn Nhỏ Chăm Chỉ 🌟',
    'Nhà Thám Hiểm Ký Hiệu 🧭',
    'Bậc Thầy Ngón Tay ✋',
    'Siêu Sao Ký Hiệu 🏆',
  ];
  const levelTitle = computed(
    () => LEVEL_TITLES[Math.min(stats.value.level - 1, LEVEL_TITLES.length - 1)]!,
  );

  const leaderboard = ref<LeaderboardRow[]>([]);
  const chests = ref<RewardChest[]>([]);
  const boardLoading = ref(false);
  const chestLoading = ref(false);
  const totalSigns = ref(0);

  /** Nhãn tuần theo ISO — "Tuần 42 (T2 - CN)" như Figma */
  const weekLabel = computed(() => {
    const d = new Date();
    const target = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));
    const dayNum = (target.getUTCDay() + 6) % 7; // T2 = 0
    target.setUTCDate(target.getUTCDate() - dayNum + 3);
    const firstThursday = new Date(Date.UTC(target.getUTCFullYear(), 0, 4));
    const week =
      1 +
      Math.round(
        ((target.getTime() - firstThursday.getTime()) / 86400000 -
          3 +
          ((firstThursday.getUTCDay() + 6) % 7)) /
          7,
      );
    return `Tuần ${week} (T2 – CN)`;
  });

  const playing = ref<{ code: GameCode; title: string } | null>(null);
  const openingId = ref<string | null>(null);

  function startPlaying(code: GameCode, title: string) {
    playing.value = { code, title };
  }

  async function reloadBoards() {
    statsStore.load(true);
    try {
      leaderboard.value = await leaderboardApi('weekly', 5);
    } catch {
      /* giữ bảng cũ */
    }
    try {
      chests.value = await rewardChestsApi();
    } catch {
      /* giữ danh sách cũ */
    }
  }

  async function onGameClosed(changed: boolean) {
    playing.value = null;
    // Chỉ tải lại khi bé thật sự chơi xong một ván — đóng ngang thì không có gì đổi
    if (changed) await reloadBoards();
  }

  async function openChest(id: string) {
    openingId.value = id;
    try {
      await openChestApi(id);
      chests.value = await rewardChestsApi();
    } finally {
      openingId.value = null;
    }
  }

  onMounted(async () => {
    statsStore.load();
    dictionarySearchApi({ page: 0, size: 1 })
      .then((r) => (totalSigns.value = r.total))
      .catch(() => (totalSigns.value = 0));

    if (!auth.isLoggedIn()) return;

    boardLoading.value = true;
    chestLoading.value = true;
    try {
      leaderboard.value = await leaderboardApi('weekly', 5);
    } catch {
      leaderboard.value = []; // hỏng bảng xếp hạng thì phần còn lại vẫn xem được
    } finally {
      boardLoading.value = false;
    }
    try {
      chests.value = await rewardChestsApi();
    } catch {
      chests.value = [];
    } finally {
      chestLoading.value = false;
    }
  });
</script>

<style scoped>
  .eyebrow {
    margin: 0 0 6px;
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
    margin: 0;
    padding: 0 16px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-peach);
    color: var(--sk-amber-ink);
    font-family: var(--sk-font-head);
    font-size: 13px;
    font-weight: 700;
  }

  .pill {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    height: 24px;
    padding: 0 14px;
    border-radius: var(--sk-r-pill);
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    white-space: nowrap;
  }
  .pill--peach {
    background: var(--sk-peach);
    color: var(--sk-amber-ink);
  }
  .pill--lav {
    background: var(--sk-lavender-2);
    color: var(--sk-blue-dark);
  }

  .notice {
    margin: 0;
    padding: 14px 18px;
    border-radius: var(--sk-r-card);
    background: var(--sk-lavender);
    color: var(--sk-brown);
    font-size: 14px;
  }

  /* ===== 1. Hero ===== */
  .hero {
    position: relative;
    margin-bottom: 32px;
  }
  .hero-dot {
    position: absolute;
    border-radius: 50%;
    filter: blur(70px);
    pointer-events: none;
  }
  .hero-dot--sky {
    width: 256px;
    height: 256px;
    background: var(--sk-sky);
    opacity: 0.6;
    top: -60px;
    left: 4%;
  }
  .hero-dot--peach {
    width: 320px;
    height: 320px;
    background: var(--sk-peach);
    opacity: 0.5;
    bottom: -120px;
    right: 2%;
  }

  .hero-card {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 32px;
    flex-wrap: wrap;
    background: var(--sk-surface);
    border-radius: var(--sk-r-card);
    padding: 32px;
    box-shadow: var(--sk-shadow-card);
  }
  .hero-text {
    flex: 1 1 520px;
    min-width: 0;
  }
  .hero-text h1 {
    margin: 10px 0 8px;
    font-size: 38px;
  }
  .hero-sub {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: var(--sk-brown);
    max-width: 56ch;
  }

  /* Widget Mochi (403×152, nền #F0F3FF r32) */
  .mochi-widget {
    display: flex;
    align-items: center;
    gap: 20px;
    background: var(--sk-lavender);
    border-radius: var(--sk-r-card);
    padding: 20px;
  }
  .mochi-frame {
    position: relative;
    width: 112px;
    height: 112px;
    border-radius: var(--sk-r-block);
    background: var(--sk-peach);
    overflow: hidden;
    flex-shrink: 0;
  }
  .mochi-frame img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .mochi-tag {
    position: absolute;
    left: 50%;
    bottom: 8px;
    transform: translateX(-50%);
    padding: 2px 10px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-surface);
    color: var(--sk-amber-ink);
    font-family: var(--sk-font-head);
    font-size: 11px;
    font-weight: 700;
  }
  .mochi-stars {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 0;
    color: var(--sk-amber-ink);
  }
  .mochi-stars strong {
    font-family: var(--sk-font-head);
    font-size: 22px;
    font-weight: 700;
  }
  .mochi-level {
    margin: 4px 0 0;
    font-size: 14px;
    color: var(--sk-brown);
  }

  /* ===== 2. Bốn trò chơi ===== */
  .section-head {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 24px;
    flex-wrap: wrap;
    margin-bottom: 20px;
  }
  .section-head h2 {
    margin: 0;
    font-size: 30px;
  }
  .games {
    margin-bottom: 32px;
  }

  /* Figma xếp 2 cột lệch nhau: thẻ rộng 705 và thẻ hẹp 495 */
  .game-grid {
    display: grid;
    grid-template-columns: minmax(0, 705fr) minmax(0, 495fr);
    gap: 20px;
  }
  .game-card {
    display: flex;
    flex-direction: column;
    gap: 12px;
    background: var(--sk-surface);
    border-radius: var(--sk-r-card);
    padding: 28px;
    box-shadow: var(--sk-shadow-card);
  }
  .game-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }
  .game-reward {
    font-family: var(--sk-font-head);
    font-size: 14px;
    font-weight: 700;
    color: var(--sk-green-ink);
  }
  .game-title {
    margin: 0;
    font-size: 23px;
    font-weight: 700;
  }
  .game-desc {
    margin: 0;
    font-size: 15px;
    font-weight: 500;
    color: var(--sk-brown);
  }

  .game-board {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 12px;
    background: var(--sk-lavender);
    border-radius: var(--sk-r-card);
    padding: 18px;
    min-height: 180px;
  }
  .board-head {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    margin: 0;
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    color: var(--sk-brown);
  }
  .board-body {
    flex: 1;
    display: flex;
    flex-wrap: wrap;
    align-content: center;
    justify-content: center;
    gap: 10px;
  }
  .board-token {
    display: grid;
    place-items: center;
    width: 56px;
    height: 56px;
    border-radius: 18px;
    background: var(--sk-surface);
    font-size: 26px;
    box-shadow: var(--sk-shadow);
  }
  .board-note {
    margin: 0;
    text-align: center;
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    color: var(--sk-amber-ink);
  }

  .game-foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
  }
  .game-hint {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 14px;
    color: var(--sk-brown);
  }
  .play-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    height: 50px;
    padding: 0 26px;
    border: none;
    border-radius: var(--sk-r-pill);
    background: var(--sk-amber);
    color: var(--sk-amber-mid);
    font-family: var(--sk-font-head);
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
  }
  .play-btn:disabled {
    background: var(--sk-lavender-2);
    color: var(--sk-brown);
    cursor: not-allowed;
  }

  /* ===== 3. Xếp hạng + kho báu ===== */
  .board-grid {
    display: grid;
    grid-template-columns: minmax(0, 705fr) minmax(0, 495fr);
    gap: 20px;
  }
  .panel {
    display: flex;
    flex-direction: column;
    gap: 16px;
    background: var(--sk-surface);
    border-radius: var(--sk-r-card);
    padding: 28px;
    box-shadow: var(--sk-shadow-card);
  }
  .panel-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
  }
  .panel-head h2 {
    margin: 0;
    font-size: 23px;
  }
  .panel-sub {
    margin: 2px 0 0;
    font-size: 14px;
    color: var(--sk-brown);
  }

  .rank-rows {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .rank-row {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 12px 18px;
    border-radius: var(--sk-r-card);
    background: var(--sk-lavender);
  }
  /* Ba hạng đầu có màu riêng, đúng Figma */
  .rank-row--1 {
    background: var(--sk-peach);
  }
  .rank-row--2 {
    background: var(--sk-sky);
  }
  .rank-row--3 {
    background: var(--sk-mint);
  }
  .rank-row--me {
    outline: 3px solid var(--sk-amber);
  }
  .rank-no {
    display: grid;
    place-items: center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--sk-surface);
    font-family: var(--sk-font-head);
    font-size: 15px;
    font-weight: 700;
    flex-shrink: 0;
  }
  .rank-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
  .rank-name {
    display: flex;
    align-items: center;
    gap: 8px;
    font-family: var(--sk-font-head);
    font-size: 16px;
    font-weight: 700;
  }
  .rank-me {
    padding: 1px 10px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-amber);
    color: var(--sk-amber-mid);
    font-size: 11px;
  }
  .rank-body small {
    font-size: 13px;
    color: var(--sk-brown);
  }
  .rank-points {
    font-family: var(--sk-font-head);
    font-size: 20px;
    font-weight: 700;
  }

  .quote {
    display: flex;
    align-items: center;
    gap: 12px;
    margin: auto 0 0;
    padding: 16px 20px;
    border-radius: var(--sk-r-card);
    background: var(--sk-lavender-2);
    color: var(--sk-blue-dark);
    font-size: 14px;
    font-weight: 600;
  }

  .chest-top {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    border-radius: var(--sk-r-card);
    background: var(--sk-lavender);
  }
  .chest-mochi {
    width: 72px;
    height: 72px;
    border-radius: 20px;
    object-fit: cover;
    background: var(--sk-peach);
    flex-shrink: 0;
  }
  .chest-now {
    margin: 0;
    font-family: var(--sk-font-head);
    font-size: 16px;
    font-weight: 700;
  }
  .chest-sub {
    margin: 2px 0 0;
    font-size: 13px;
    color: var(--sk-brown);
  }

  .chest-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .chest {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    border: 2px dashed var(--sk-blue-150);
    border-radius: var(--sk-r-card);
  }
  .chest--ready {
    border-style: solid;
    border-color: var(--sk-amber);
    background: var(--sk-peach);
  }
  .chest--opened {
    border-style: solid;
    border-color: var(--sk-green-ink);
    background: var(--sk-mint);
  }
  .chest-icon {
    display: grid;
    place-items: center;
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: var(--sk-surface);
    color: var(--sk-amber-ink);
    flex-shrink: 0;
  }
  .chest-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
  .chest-body strong {
    font-family: var(--sk-font-head);
    font-size: 15px;
    font-weight: 700;
  }
  .chest-body small {
    font-size: 13px;
    color: var(--sk-brown);
  }
  .chest-state {
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    color: var(--sk-amber-ink);
    white-space: nowrap;
  }

  @media (max-width: 980px) {
    .game-grid,
    .board-grid {
      grid-template-columns: 1fr;
    }
    .hero-text h1 {
      font-size: 30px;
    }
  }
  .chest-open {
    min-height: 36px;
    padding: 0 14px;
    border: none;
    border-radius: var(--sk-r-pill);
    background: var(--sk-amber);
    color: var(--sk-brown-dark);
    font-family: var(--sk-font-head);
    font-weight: 800;
    cursor: pointer;
  }
  .chest-open:disabled {
    opacity: 0.6;
  }
  .play-btn {
    text-decoration: none;
  }
</style>
