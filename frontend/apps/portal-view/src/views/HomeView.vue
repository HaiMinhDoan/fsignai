<template>
  <!-- ===================================================================
       Dựng theo Figma 1:1154 "SignKids - Trang Chủ Phiêu Lưu Ký Hiệu".
       Số đo trong ngoặc ở mỗi khối là kích thước khung gốc trên Figma.
       =================================================================== -->

  <!-- ===== 1. Hero Welcome Banner (1232×544, r48) ===== -->
  <section class="hero">
    <span class="hero-dot hero-dot--amber" aria-hidden="true"></span>
    <span class="hero-dot hero-dot--sky" aria-hidden="true"></span>

    <div class="hero-inner">
      <div class="hero-text">
        <p class="eyebrow-pill">
          <SiIcon name="sparkles" :size="16" />
          <span>HÀNH TRÌNH HÔM NAY</span>
        </p>

        <div class="hero-heading">
          <h1>Chào {{ displayName }}! 👋</h1>
          <p class="hero-sub">
            Hôm nay đôi bàn tay của bạn sẽ kể câu chuyện gì nào? Khám phá thêm những cử chỉ mới
            cùng Mochi nhé!
          </p>
        </div>

        <div class="hero-buttons">
          <RouterLink
            :to="challenge ? { name: 'practice-sign', params: { id: challenge.id } } : '/phong-luyen'"
            class="btn btn--amber btn--tall"
          >
            <SiIcon name="play" :size="22" />
            <span>Tiếp tục: {{ challenge?.wordVi ?? 'luyện ký hiệu' }}</span>
          </RouterLink>
          <a href="#nhiem-vu" class="btn btn--white btn--tall">
            <SiIcon name="star" :size="20" />
            <span>Xem nhiệm vụ hôm nay</span>
          </a>
        </div>

        <!-- "Mini Visual Sign Cue Tip" (465×64, trắng r32) — khối tôi bỏ sót ở bản trước -->
        <div class="cue-tip">
          <span class="cue-dot"><SiIcon name="hand" :size="18" /></span>
          <div class="cue-body">
            <p class="cue-title">Ký hiệu {{ challenge ? `“${challenge.wordVi}”` : 'yêu thương' }}:</p>
            <p class="cue-desc">
              {{
                challenge?.descriptionVi ||
                'Hai tay bắt chéo áp nhẹ lên lồng ngực kèm nụ cười rạng rỡ!'
              }}
            </p>
          </div>
        </div>
      </div>

      <!-- "Mascot Mochi Showcase" (448×448 trắng r48) — linh vật đã tách nền
           nên thả thẳng lên một quầng pastel, không còn khung ảnh chữ nhật -->
      <div class="mochi-showcase">
        <span class="mochi-glow" aria-hidden="true"></span>
        <img
          :src="mochiHero"
          alt="Mochi — bàn tay làm ký hiệu “yêu thương” đang vẫy chào"
          class="mochi-img"
        />
        <span class="float-badge float-badge--green">
          <SiIcon name="check" :size="14" />
          <span>Mochi Chào Bạn!</span>
        </span>
        <span class="float-badge float-badge--blue">
          <SiIcon name="hand" :size="14" />
          <span>Ký hiệu: “{{ challenge?.wordVi ?? 'Yêu Thương' }}” ❤️</span>
        </span>
      </div>
    </div>
  </section>

  <!-- ===== 2. Player Level & Achievement Badges Bar (1232×144, trắng r48) ===== -->
  <section class="level-bar" aria-labelledby="level-heading">
    <div class="level-main">
      <span class="level-disc">
        <strong>{{ stats.level }}</strong>
        <span class="level-disc-tag">CẤP ĐỘ</span>
      </span>

      <div class="level-body">
        <div class="level-row">
          <h2 id="level-heading" class="level-title">{{ levelTitle }}</h2>
          <span class="level-xp">
            {{ stats.levelPoints }} / {{ stats.levelTarget }} XP ({{ stats.levelPercent }}%)
          </span>
        </div>

        <!-- "Chunky Candy Bar": rãnh 20px #E7EEFF, ruột 12px gradient 3 chặng -->
        <div
          class="xp-track"
          role="progressbar"
          :aria-valuenow="stats.levelPercent"
          aria-valuemin="0"
          aria-valuemax="100"
        >
          <span class="xp-fill" :style="{ width: `${Math.max(stats.levelPercent, 2)}%` }"></span>
        </div>

        <p class="level-next">
          Chỉ còn {{ stats.levelTarget - stats.levelPoints }} XP nữa để lên Cấp
          {{ stats.level + 1 }}: “{{ nextLevelTitle }}”!
        </p>
      </div>
    </div>

    <ul class="badge-row">
      <li v-if="!recentBadges.length" class="badge-empty">
        Chưa có huy hiệu nào — học xong trạm đầu là có ngay.
      </li>
      <li
        v-for="(badge, i) in recentBadges"
        :key="badge.id"
        class="badge"
      >
        <span class="badge-dot" :class="`badge-dot--${BADGE_TONES[i % 3]}`">
          <SiIcon name="trophy" :size="18" />
        </span>
        <span class="badge-lines">
          <strong>{{ badge.nameVi }}</strong>
          <small :class="`badge-note--${BADGE_TONES[i % 3]}`">Đã đạt ✨</small>
        </span>
      </li>
    </ul>
  </section>

  <!-- ===== 3. Daily Sign Quest Highlight (710×264 + 502×386) ===== -->
  <section id="nhiem-vu" class="quest-grid">
    <article class="quest-card">
      <div class="quest-tags">
        <span class="pill pill--amber">
          <SiIcon name="star" :size="13" />
          <span>THỬ THÁCH MỖI NGÀY</span>
        </span>
        <span class="pill pill--white">
          <SiIcon name="sparkles" :size="15" />
          <span>+20 Sao Vàng</span>
        </span>
      </div>

      <div class="quest-heading">
        <h2>
          Thử thách:
          <template v-if="challenge">“{{ challenge.wordVi }}”</template>
          <template v-else>đang chuẩn bị…</template>
        </h2>
        <p>
          Mở camera hoặc quan sát mô hình bàn tay để làm theo ký hiệu kết nối tình bạn thật
          ngọt ngào!
        </p>
      </div>

      <div class="quest-foot">
        <span class="quest-done">
          <span class="quest-check"><SiIcon name="check" :size="14" /></span>
          <strong>Đã xem mẫu</strong>
          <small>(1/2 bước)</small>
        </span>
        <RouterLink
          :to="challenge ? { name: 'practice-sign', params: { id: challenge.id } } : '/phong-luyen'"
          class="btn btn--brown"
        >
          <SiIcon name="play" :size="17" />
          <span>Bắt đầu luyện ngay</span>
        </RouterLink>
      </div>
    </article>

    <aside class="camera-card">
      <div class="camera-head">
        <h2>
          <SiIcon name="camera" :size="22" />
          <span>Gương Soi AI Tương Tác</span>
        </h2>
        <span class="pill pill--sky">Chưa bật</span>
      </div>
      <p class="camera-desc">
        Bạn hãy giơ tay lên trước camera để vừa nhìn mẫu vừa nhìn tay mình nhé!
      </p>
      <div class="camera-shot">
        <img :src="cameraTeaser" alt="Màn hình gương soi khi bạn luyện ký hiệu trước camera" />
        <RouterLink to="/phong-luyen" class="camera-play" aria-label="Mở gương luyện tập">
          <SiIcon name="camera" :size="22" />
        </RouterLink>
      </div>
      <!-- Nói thẳng là chưa chấm điểm. Figma ghi "AI chính xác 98%" nhưng hứa
           thế khi chưa có gì chạy là mất lòng tin ngay lần thử đầu -->
      <p class="camera-note">Máy chấm điểm động tác sẽ bật ở bản sau.</p>
    </aside>
  </section>

  <!-- ===== 4. Adventure Island Map (1232×356) ===== -->
  <section class="map-section" aria-labelledby="map-heading">
    <div class="section-head">
      <div>
        <p class="eyebrow">BẢN ĐỒ KHÁM PHÁ</p>
        <h2 id="map-heading">Hành Trình “Đảo Ký Hiệu Kỳ Diệu” 🏝️</h2>
        <p class="section-sub">Chạm vào từng trạm dừng chân để mở khóa kho báu ký hiệu mới!</p>
      </div>
      <span class="pill pill--amber pill--lg">Tổng sao thu thập: {{ stats.stars }}</span>
    </div>

    <div class="map-board">
      <!-- Đường cong nối các trạm, nằm dưới thẻ (Figma: Wavy Decorative Path) -->
      <svg class="map-path" viewBox="0 0 986 78" preserveAspectRatio="none" aria-hidden="true">
        <path
          d="M0 58 C 120 8, 220 8, 330 46 S 560 92, 680 34 S 880 6, 986 44"
          fill="none"
          stroke="var(--sk-surface)"
          stroke-width="8"
          stroke-linecap="round"
          stroke-dasharray="2 22"
          opacity="0.95"
        />
      </svg>

      <p v-if="packsLoading" class="hint">Đang vẽ bản đồ…</p>
      <p v-else-if="packsError" class="error" role="alert">{{ packsError }}</p>

      <ol v-else class="station-row">
        <li v-for="st in stations" :key="st.key">
          <component
            :is="st.to ? 'RouterLink' : 'div'"
            v-bind="st.to ? { to: st.to } : {}"
            class="station"
            :class="{ 'station--locked': !st.to }"
          >
            <span class="station-disc" :style="{ background: st.discColor }">
              <SiIcon :name="st.icon" :size="28" />
            </span>
            <span class="station-stars" :aria-label="`${st.stars} trên 3 sao`">
              <SiIcon
                v-for="n in 3"
                :key="n"
                name="star"
                :size="15"
                :class="n <= st.stars ? 'star-on' : 'star-off'"
              />
            </span>
            <strong class="station-name">{{ st.name }}</strong>
            <span class="station-desc">{{ st.desc }}</span>
            <!-- Trạng thái nói bằng cả icon, chữ lẫn màu nền (§2.2) -->
            <span class="station-state" :style="{ background: st.stateBg, color: st.stateInk }">
              {{ st.state }}
            </span>
          </component>
        </li>
      </ol>
    </div>
  </section>

  <!-- ===== 5. Animated Gesture Video Gallery (3 thẻ 389×381) ===== -->
  <section class="gallery-section" aria-labelledby="gallery-heading">
    <div class="section-head">
      <div>
        <p class="eyebrow">GÓC HOẠT HỌA TRỰC QUAN</p>
        <h2 id="gallery-heading">Học Ký Hiệu Qua Video 🎬</h2>
        <p class="section-sub">
          Video bàn tay quay chính diện, kèm phụ đề chữ to rõ ràng giúp bạn dễ dàng học theo.
        </p>
      </div>
      <RouterLink to="/tu-dien" class="btn btn--white">
        <span>Xem tất cả thư viện ký hiệu</span>
        <SiIcon name="next" :size="20" />
      </RouterLink>
    </div>

    <div class="gallery-grid">
      <article v-for="(item, i) in gallery" :key="item.sign.id" class="gcard">
        <div class="gcard-media">
          <video
            v-if="item.video"
            :ref="(el) => setCardVideo(el, i)"
            :src="item.video.videoUrl"
            :poster="item.video.thumbnailUrl || item.sign.thumbnailUrl"
            muted
            loop
            playsinline
            preload="none"
          ></video>
          <img
            v-else
            :src="item.sign.thumbnailUrl || FALLBACK_CARDS[i % 3]"
            :alt="`Ký hiệu của từ ${item.sign.wordVi}`"
          />

          <span class="speed-badge">
            <SiIcon name="slow" :size="12" />
            <span>{{ a11y.playbackRate }}x</span>
          </span>

          <button
            type="button"
            class="gcard-play"
            :aria-label="`Phát video ký hiệu ${item.sign.wordVi}`"
            @click="toggleCard(i)"
          >
            <SiIcon :name="playingCard === i ? 'check' : 'play'" :size="20" />
          </button>

          <!-- "Accessible Large Subtitle Banner": nền #263143, chữ 24/700.
               Đây là phần trợ năng quan trọng nhất của thẻ — người học đọc được từ
               ngay cả khi chưa hiểu hình -->
          <p class="subtitle-banner">“{{ item.sign.wordVi.toUpperCase() }}”</p>
        </div>

        <div class="gcard-body">
          <div class="gcard-meta">
            <span class="meta-topic">Chủ đề: {{ item.sign.primaryTopicNameVi || 'Chưa xếp' }}</span>
            <span class="meta-level">
              <SiIcon name="star" :size="13" />
              <span>{{ levelLabel(item.sign.level) }}</span>
            </span>
          </div>

          <h3 class="gcard-title">Ký hiệu: {{ item.sign.wordVi }}</h3>
          <p class="gcard-desc">
            {{ item.sign.descriptionVi || 'Bạn xem video và làm theo Mochi nhé.' }}
          </p>

          <div class="gcard-actions">
            <button type="button" class="text-btn" @click="slowCard(i)">
              <SiIcon name="slow" :size="14" />
              <span>Xem lại chậm</span>
            </button>
            <RouterLink
              :to="{ name: 'practice-sign', params: { id: item.sign.id } }"
              class="text-btn"
            >
              <SiIcon name="hand" :size="14" />
              <span>Thực hành ngay</span>
            </RouterLink>
          </div>
        </div>
      </article>
    </div>
  </section>

  <!-- ===== 6. Parent & Teacher Corner (1232×132, #E7EEFF r48) ===== -->
  <section class="parent-callout">
    <div class="parent-left">
      <span class="parent-dot"><SiIcon name="user" :size="26" /></span>
      <div>
        <h2>Góc Đồng Hành Cùng Người Học</h2>
        <p>Xem báo cáo học tập, thời lượng và chuỗi ngày học của người học ở một chỗ.</p>
      </div>
    </div>
    <div class="parent-actions">
      <RouterLink to="/ba-me-thay-co" class="btn btn--white">
        <SiIcon name="grid" :size="18" />
        <span>Xem báo cáo học tập</span>
      </RouterLink>
    </div>
  </section>
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';
  import {
    myAchievementsApi,
    wordPacksApi,
    type Achievement,
    type WordPack,
  } from '@/api/catalog';
  import {
    dictionarySearchApi,
    signVideosApi,
    type SignSummary,
    type SignVideo,
  } from '@/api/dictionary';
  import { useAuthStore } from '@/stores/auth';
  import { useA11yStore } from '@/stores/a11y';
  import { useLearnerStatsStore } from '@/stores/learnerStats';
  import SiIcon from '@/components/SiIcon.vue';

  // Ảnh thiết kế lấy thẳng từ Figma — xem src/assets/figma/README.md
  import mochiHero from '@/assets/mascot/mochi-hand.png';
  import cameraTeaser from '@/assets/figma/home-camera-teaser.png';
  import card1 from '@/assets/figma/video-card-1.png';
  import card2 from '@/assets/figma/video-card-2.png';
  import card3 from '@/assets/figma/video-card-3.png';

  defineOptions({ name: 'HomeView' });

  const FALLBACK_CARDS = [card1, card2, card3];
  const BADGE_TONES = ['mint', 'sky', 'peach'] as const;

  const auth = useAuthStore();
  const a11y = useA11yStore();
  const statsStore = useLearnerStatsStore();
  const stats = computed(() => statsStore.stats);

  const LEVEL_TITLES = [
    'Mới Tập Ký Hiệu 🌱',
    'Bàn Tay Chăm Chỉ 🌟',
    'Nhà Thám Hiểm Ký Hiệu 🧭',
    'Bậc Thầy Ngón Tay ✋',
    'Siêu Sao Ký Hiệu 🏆',
  ];
  const LEVEL_LABELS: Record<string, string> = {
    BEGINNER: 'Cực dễ',
    BASIC: 'Cơ bản',
    INTERMEDIATE: 'Trung cấp',
    ADVANCED: 'Nâng cao',
  };
  const levelLabel = (v: string) => LEVEL_LABELS[v] ?? v;
  const pick = (i: number) => LEVEL_TITLES[Math.min(i, LEVEL_TITLES.length - 1)]!;

  const displayName = computed(() => auth.user?.realName || 'bạn');
  const levelTitle = computed(() => pick(stats.value.level - 1));
  const nextLevelTitle = computed(() => pick(stats.value.level));

  const packsLoading = ref(true);
  const packsError = ref('');
  const packs = ref<WordPack[]>([]);
  const totalSigns = ref(0);

  const challenge = ref<SignSummary | null>(null);
  const gallery = ref<{ sign: SignSummary; video: SignVideo | null }[]>([]);
  const recentBadges = ref<Achievement[]>([]);

  const cardVideos = ref<(HTMLVideoElement | null)[]>([]);
  const playingCard = ref<number | null>(null);

  /** Màu đĩa và pill trạng thái của từng trạm — khớp bảng màu Figma */
  const DISC_COLORS = ['#006C4A', '#006398', '#855300', '#5BB8FE', '#50C594'];
  const LOCK_BG = 'var(--sk-blue-150)';

  interface Station {
    key: string;
    name: string;
    desc: string;
    icon: string;
    stars: number;
    discColor: string;
    state: string;
    stateBg: string;
    stateInk: string;
    to?: object | string;
  }

  /**
   * Trạm đầu luôn có thật (cả kho từ, xem tự do — không cần mở khoá).
   * Các trạm sau là GÓI TỪ thật lấy từ /api/v1/word-packs: khoá/mở, tiến độ
   * và số sao đều do backend tính (xem WordPackServiceImpl.isUnlocked) —
   * trang này chỉ hiển thị, không tự suy đoán trạng thái.
   */
  const stations = computed<Station[]>(() => {
    const list: Station[] = [];

    if (totalSigns.value) {
      list.push({
        key: 'kho',
        name: 'Trạm 1: Kho Từ',
        desc: `${totalSigns.value} từ, xem tự do`,
        icon: 'book',
        stars: 3,
        discColor: DISC_COLORS[0]!,
        state: 'Mở tự do ✔️',
        stateBg: 'var(--sk-mint)',
        stateInk: 'var(--sk-green-dark)',
        to: '/tu-dien',
      });
    }

    packs.value.forEach((p, i) => {
      const open = p.unlocked !== false; // undefined (khách chưa đăng nhập, không điều kiện) coi như mở
      const done = p.myStatus === 'COMPLETED';
      list.push({
        key: p.id,
        name: `Trạm ${list.length + 1}: ${p.titleVi}`,
        desc: p.descriptionVi || 'Đang soạn nội dung',
        icon: p.iconName || ['hand', 'star', 'sparkles', 'grid'][i % 4]!,
        stars: p.myStars ?? (done ? 3 : 0),
        discColor: open ? p.islandColor || DISC_COLORS[(i + 1) % DISC_COLORS.length]! : LOCK_BG,
        state: !open
          ? 'Sắp mở'
          : done
            ? `${p.itemCount ?? 0} từ · Đã xong ✔️`
            : `${p.itemCount ?? 0} từ · Vào học`,
        stateBg: !open ? 'var(--sk-blue-150)' : done ? 'var(--sk-mint)' : 'var(--sk-sky)',
        stateInk: !open ? 'var(--sk-brown)' : done ? 'var(--sk-green-dark)' : 'var(--sk-blue-dark)',
        to: open ? `/goi-tu/${p.id}` : undefined,
      });
    });

    return list;
  });

  function setCardVideo(el: unknown, i: number) {
    cardVideos.value[i] = (el as HTMLVideoElement) ?? null;
  }

  function toggleCard(i: number) {
    const v = cardVideos.value[i];
    if (!v) return;
    if (playingCard.value === i) {
      v.pause();
      playingCard.value = null;
      return;
    }
    // Chỉ một thẻ chạy một lúc — ba video cùng nhảy sẽ làm người học hoa mắt
    cardVideos.value.forEach((other) => other?.pause());
    v.playbackRate = a11y.playbackRate;
    void v.play();
    playingCard.value = i;
  }

  /** "Xem lại chậm": hạ tốc độ toàn cục xuống 0.5x rồi phát lại thẻ này */
  function slowCard(i: number) {
    a11y.setRate(0.5);
    const v = cardVideos.value[i];
    if (v) {
      v.currentTime = 0;
      v.playbackRate = 0.5;
      void v.play();
      playingCard.value = i;
    }
  }

  watch(
    () => a11y.playbackRate,
    (r) => cardVideos.value.forEach((v) => v && (v.playbackRate = r)),
  );

  /** Thử thách phải giống nhau suốt cả ngày và đổi sang hôm sau */
  function dayIndex(): number {
    const now = new Date();
    const start = Date.UTC(now.getFullYear(), 0, 0);
    const today = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
    return Math.floor((today - start) / 86400000);
  }

  async function primaryVideo(signId: string): Promise<SignVideo | null> {
    try {
      const videos = await signVideosApi(signId);
      return (
        videos.find((v) => v.region === 'COMMON' && v.isPrimary) ??
        videos.find((v) => v.isPrimary) ??
        videos[0] ??
        null
      );
    } catch {
      return null;
    }
  }

  async function loadSigns() {
    try {
      const probe = await dictionarySearchApi({ page: 0, size: 1 });
      totalSigns.value = probe.total;
      if (!probe.total) return;

      const picked = await dictionarySearchApi({ page: dayIndex() % probe.total, size: 1 });
      challenge.value = picked.items[0] ?? null;

      // Ba thẻ thư viện: lấy trang kế tiếp để mỗi ngày bộ thẻ cũng đổi theo
      const page = await dictionarySearchApi({
        page: (dayIndex() + 1) % Math.max(Math.floor(probe.total / 3), 1),
        size: 3,
      });
      gallery.value = await Promise.all(
        page.items.map(async (sign) => ({ sign, video: await primaryVideo(sign.id) })),
      );
    } catch {
      challenge.value = null; // hỏng phần này thì cả trang vẫn phải dùng được
    }
  }

  async function loadPacks() {
    try {
      packs.value = await wordPacksApi();
    } catch (e) {
      packsError.value = (e as Error).message;
    } finally {
      packsLoading.value = false;
    }
  }

  async function loadBadges() {
    if (!auth.isLoggedIn()) return;
    try {
      const all = await myAchievementsApi();
      recentBadges.value = all.filter((b) => b.earned).slice(0, 3);
    } catch {
      recentBadges.value = [];
    }
  }

  onMounted(() => {
    loadSigns();
    loadPacks();
    loadBadges();
    statsStore.load();
  });
</script>

<style scoped>
  .hint {
    color: var(--sk-brown);
  }
  .error {
    color: #c4503f;
    font-weight: 600;
  }

  /* ===== Mảnh dùng lại ===== */
  .eyebrow {
    margin: 0 0 6px;
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 1.2px;
    color: var(--sk-amber-ink);
  }

  .pill {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 26px;
    padding: 0 12px;
    border-radius: var(--sk-r-pill);
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    white-space: nowrap;
  }
  .pill--lg {
    height: 40px;
    padding: 0 20px;
    font-size: 14px;
  }
  .pill--amber {
    background: var(--sk-amber);
    color: var(--sk-amber-mid);
  }
  .pill--white {
    background: var(--sk-surface);
    color: var(--sk-amber-ink);
    box-shadow: var(--sk-shadow);
  }
  .pill--sky {
    background: var(--sk-sky);
    color: var(--sk-blue-dark);
  }

  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    min-height: 42px;
    padding: 0 22px;
    border-radius: var(--sk-r-pill);
    font-family: var(--sk-font-head);
    font-size: 14px;
    font-weight: 700;
    text-decoration: none;
    border: none;
    cursor: pointer;
    transition: transform 160ms ease;
  }
  .btn:hover {
    transform: translateY(-2px);
  }
  /* Nút chính trong hero cao 62px theo Figma, không phải 42 */
  .btn--tall {
    min-height: 62px;
    padding: 0 28px;
    font-size: 16px;
  }
  .btn--amber {
    background: var(--sk-amber);
    color: var(--sk-brown-dark);
    box-shadow: var(--sk-shadow-card);
  }
  .btn--white {
    background: var(--sk-surface);
    color: var(--sk-blue-ink);
    box-shadow: var(--sk-shadow-card);
  }
  .btn--brown {
    background: var(--sk-amber-ink);
    color: #fff;
  }

  /* ===== 1. Hero ===== */
  .hero {
    position: relative;
    overflow: hidden;
    border-radius: var(--sk-r-block);
    background: linear-gradient(120deg, #dee8ff 0%, #f0f3ff 52%, #cce5ff 100%);
    padding: 48px;
    margin-bottom: 24px;
  }
  .hero-dot {
    position: absolute;
    border-radius: 50%;
    filter: blur(70px);
    pointer-events: none;
  }
  .hero-dot--amber {
    width: 256px;
    height: 256px;
    background: var(--sk-amber);
    opacity: 0.3;
    top: -70px;
    right: 22%;
  }
  .hero-dot--sky {
    width: 320px;
    height: 320px;
    background: var(--sk-sky-strong);
    opacity: 0.32;
    bottom: -150px;
    right: -70px;
  }

  .hero-inner {
    position: relative;
    display: grid;
    grid-template-columns: minmax(0, 649fr) 455fr;
    gap: 32px;
    align-items: center;
  }
  .hero-text {
    display: flex;
    flex-direction: column;
    gap: 20px;
    align-items: flex-start;
  }

  /* Nhãn là VIÊN THUỐC nâu chữ trắng, không phải chữ thường */
  .eyebrow-pill {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 30px;
    margin: 0;
    padding: 0 14px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-amber-ink);
    color: #fff;
    font-family: var(--sk-font-head);
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 0.4px;
  }

  .hero-heading {
    display: flex;
    flex-direction: column;
    gap: 6px;
    max-width: 533px;
  }
  .hero-heading h1 {
    margin: 0;
    color: var(--sk-ink);
  }
  .hero-sub {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: var(--sk-brown);
  }

  .hero-buttons {
    display: flex;
    gap: 20px;
    flex-wrap: wrap;
  }

  /* "Mini Visual Sign Cue Tip" — thẻ trắng r32, vòng mint 40px */
  .cue-tip {
    display: flex;
    align-items: center;
    gap: 12px;
    max-width: 480px;
    padding: 12px 16px;
    border-radius: var(--sk-r-card);
    background: var(--sk-surface);
    box-shadow: var(--sk-shadow-card);
  }
  .cue-dot {
    display: grid;
    place-items: center;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: var(--sk-mint);
    color: var(--sk-green-dark);
    flex-shrink: 0;
  }
  .cue-body {
    min-width: 0;
  }
  .cue-title {
    margin: 0;
    font-family: var(--sk-font-head);
    font-size: 14px;
    font-weight: 700;
    color: var(--sk-ink);
  }
  .cue-desc {
    margin: 0;
    font-size: 14px;
    color: var(--sk-brown);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  /* Khung linh vật 448×448 trắng r48; bên trong là quầng pastel + bàn tay Mochi */
  .mochi-showcase {
    position: relative;
    aspect-ratio: 1 / 1;
    max-width: 448px;
    justify-self: end;
    width: 100%;
    background: var(--sk-surface);
    border-radius: var(--sk-r-block);
    box-shadow: var(--sk-shadow-card);
    display: grid;
    place-items: center;
    padding: 20px;
  }
  /* Quầng sáng mờ sau bàn tay: nền khung vốn trắng, thiếu quầng thì linh vật
     trông như bị dán lên. Bo tròn + blur nên không tạo mép cứng nào. */
  .mochi-glow {
    position: absolute;
    inset: 12%;
    border-radius: 50%;
    background: radial-gradient(circle at 32% 26%, var(--sk-peach), var(--sk-sky) 70%);
    filter: blur(14px);
    opacity: 0.7;
    animation: mochi-glow 4.2s ease-in-out infinite;
  }
  .mochi-img {
    position: relative;
    width: 88%;
    height: 88%;
    object-fit: contain;
    /* Vẫy quanh cổ tay = mép dưới của ảnh */
    transform-origin: 50% 92%;
    animation: mochi-wave 3.4s ease-in-out infinite;
  }
  @keyframes mochi-wave {
    0%,
    62%,
    100% {
      transform: rotate(0deg);
    }
    72% {
      transform: rotate(-9deg);
    }
    82% {
      transform: rotate(7deg);
    }
    91% {
      transform: rotate(-4deg);
    }
  }
  @keyframes mochi-glow {
    0%,
    100% {
      transform: scale(1);
      opacity: 0.7;
    }
    50% {
      transform: scale(1.07);
      opacity: 0.85;
    }
  }
  .float-badge {
    position: absolute;
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 5px 12px;
    border-radius: var(--sk-r-pill);
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    color: #fff;
    box-shadow: var(--sk-shadow-card);
    white-space: nowrap;
    max-width: 92%;
  }
  .float-badge--green {
    background: var(--sk-green-ink);
    top: 26px;
    left: -12px;
  }
  .float-badge--blue {
    background: var(--sk-blue-ink);
    bottom: 26px;
    right: -12px;
  }

  /* ===== 2. Thanh cấp độ ===== */
  .level-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 32px;
    flex-wrap: wrap;
    background: var(--sk-surface);
    border-radius: var(--sk-r-block);
    padding: 32px 40px;
    box-shadow: var(--sk-shadow-card);
    margin-bottom: 24px;
  }
  .level-main {
    display: flex;
    align-items: center;
    gap: 20px;
    flex: 1 1 560px;
    min-width: 0;
  }

  /* Đĩa cấp độ: nền ĐÀO, số Quicksand rất đậm, nhãn là pill nâu riêng */
  .level-disc {
    position: relative;
    display: grid;
    place-items: center;
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: var(--sk-peach);
    flex-shrink: 0;
    box-shadow: var(--sk-shadow-card);
  }
  .level-disc strong {
    font-family: var(--sk-font-head);
    font-size: 32px;
    font-weight: 700;
    line-height: 1;
    color: var(--sk-brown-dark);
    transform: translateY(-6px);
  }
  .level-disc-tag {
    position: absolute;
    bottom: 8px;
    padding: 2px 10px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-amber-ink);
    color: #fff;
    font-family: var(--sk-font-head);
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.4px;
  }

  .level-body {
    flex: 1;
    min-width: 0;
  }
  .level-row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
  }
  .level-title {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    color: var(--sk-amber-ink);
  }
  .level-xp {
    font-family: var(--sk-font-head);
    font-size: 14px;
    font-weight: 700;
    color: var(--sk-brown);
  }

  .xp-track {
    height: 20px;
    margin: 6px 0;
    padding: 4px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-lavender-2);
  }
  .xp-fill {
    display: block;
    height: 12px;
    border-radius: var(--sk-r-pill);
    background: linear-gradient(90deg, #f59e0b 0%, #ffb95f 55%, #855300 100%);
    transition: width 400ms ease;
  }
  .level-next {
    margin: 0;
    font-size: 14px;
    color: var(--sk-brown);
  }

  .badge-row {
    display: flex;
    gap: 12px;
    list-style: none;
    margin: 0;
    padding: 0;
    flex-wrap: wrap;
  }
  /* Huy hiệu là thẻ r32 nền lavender, KHÔNG phải viên thuốc tròn */
  .badge {
    display: flex;
    align-items: center;
    gap: 8px;
    height: 52px;
    padding: 0 14px 0 6px;
    border-radius: var(--sk-r-card);
    background: var(--sk-lavender);
  }
  .badge-dot {
    display: grid;
    place-items: center;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    flex-shrink: 0;
  }
  .badge-dot--mint {
    background: var(--sk-mint);
    color: var(--sk-green-dark);
  }
  .badge-dot--sky {
    background: var(--sk-sky);
    color: var(--sk-blue-dark);
  }
  .badge-dot--peach {
    background: var(--sk-peach);
    color: var(--sk-amber-ink);
  }
  .badge-lines {
    display: flex;
    flex-direction: column;
    line-height: 1.25;
  }
  .badge-lines strong {
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    color: var(--sk-ink);
  }
  .badge-note--mint {
    color: var(--sk-green-ink);
    font-size: 13px;
  }
  .badge-note--sky {
    color: var(--sk-blue-ink);
    font-size: 13px;
  }
  .badge-note--peach {
    color: var(--sk-amber-ink);
    font-size: 13px;
  }
  .badge-empty {
    font-size: 14px;
    color: var(--sk-brown);
    max-width: 30ch;
  }

  /* ===== 3. Nhiệm vụ hôm nay ===== */
  .quest-grid {
    display: grid;
    grid-template-columns: minmax(0, 710fr) 502fr;
    gap: 20px;
    margin-bottom: 24px;
    align-items: start;
  }

  /* Thẻ nhiệm vụ KHÔNG chứa video — chỉ chữ, tiến độ và nút */
  .quest-card {
    display: flex;
    flex-direction: column;
    gap: 22px;
    background: linear-gradient(120deg, #ffddb8 0%, #f0f3ff 100%);
    border-radius: var(--sk-r-block);
    padding: 32px;
    min-height: 264px;
  }
  .quest-tags {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }
  .quest-heading h2 {
    margin: 0 0 6px;
    font-size: 32px;
    font-weight: 700;
    color: var(--sk-brown-dark);
  }
  .quest-heading p {
    margin: 0;
    font-size: 16px;
    font-weight: 500;
    color: #653e00;
    max-width: 56ch;
  }
  .quest-foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    flex-wrap: wrap;
    margin-top: auto;
  }
  .quest-done {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-family: var(--sk-font-head);
    font-size: 14px;
    font-weight: 700;
    color: var(--sk-ink);
  }
  .quest-check {
    display: grid;
    place-items: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: var(--sk-green-ink);
    color: #fff;
  }
  .quest-done small {
    font-size: 12px;
    color: var(--sk-brown);
  }

  .camera-card {
    display: flex;
    flex-direction: column;
    gap: 12px;
    background: var(--sk-surface);
    border-radius: var(--sk-r-block);
    padding: 24px;
    box-shadow: var(--sk-shadow-card);
  }
  .camera-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }
  .camera-head h2 {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0;
    font-size: 20px;
    font-weight: 700;
  }
  .camera-desc {
    margin: 0;
    font-size: 14px;
    color: var(--sk-brown);
  }
  .camera-shot {
    position: relative;
    border-radius: var(--sk-r-card);
    overflow: hidden;
    background: var(--sk-lavender-2);
    aspect-ratio: 462 / 260;
  }
  .camera-shot img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .camera-play {
    position: absolute;
    inset: auto 50% 16px auto;
    transform: translateX(50%);
    display: grid;
    place-items: center;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: var(--sk-amber);
    color: var(--sk-amber-mid);
    box-shadow: var(--sk-shadow-lift);
  }
  .camera-note {
    margin: 0;
    font-size: 13px;
    font-weight: 700;
    color: var(--sk-amber-ink);
  }

  /* ===== 4. Bản đồ ===== */
  .section-head {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 24px;
    flex-wrap: wrap;
    margin-bottom: 24px;
  }
  .section-head h2 {
    margin: 0;
    font-size: 36px;
  }
  .section-sub {
    margin: 6px 0 0;
    font-size: 18px;
    font-weight: 600;
    color: var(--sk-brown);
    max-width: 62ch;
  }

  .map-section,
  .gallery-section {
    margin-bottom: 32px;
  }
  .gallery-section {
    background: var(--sk-lavender);
    border-radius: var(--sk-r-block);
    padding: 32px;
  }

  .map-board {
    position: relative;
    border-radius: var(--sk-r-block);
    background: linear-gradient(120deg, #f0f3ff 0%, #e7eeff 50%, #cce5ff 100%);
    padding: 48px 48px 40px;
  }
  /* Đường cong nối các trạm, nằm DƯỚI thẻ */
  .map-path {
    position: absolute;
    top: 96px;
    left: 5%;
    width: 90%;
    height: 78px;
    pointer-events: none;
  }

  .station-row {
    position: relative;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
    gap: 15px;
    list-style: none;
    margin: 0;
    padding: 0;
  }
  /* Trạm là thẻ TRẮNG r48, đổi trạng thái bằng màu đĩa tròn bên trong */
  .station {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    height: 100%;
    padding: 24px 16px 20px;
    border-radius: var(--sk-r-block);
    background: var(--sk-surface);
    box-shadow: var(--sk-shadow-card);
    text-align: center;
    text-decoration: none;
    color: var(--sk-ink);
    transition:
      transform 160ms ease,
      box-shadow 160ms ease;
  }
  a.station:hover {
    transform: translateY(-5px);
    box-shadow: var(--sk-shadow-lift);
  }
  .station--locked {
    cursor: not-allowed;
    opacity: 0.85;
  }
  .station-disc {
    display: grid;
    place-items: center;
    width: 64px;
    height: 64px;
    border-radius: 50%;
    color: #fff;
    box-shadow: var(--sk-shadow-card);
  }
  .station-stars {
    display: inline-flex;
    gap: 2px;
  }
  .star-on {
    color: var(--sk-amber);
  }
  .star-off {
    color: var(--sk-blue-150);
  }
  .station-name {
    font-family: var(--sk-font-head);
    font-size: 18px;
    font-weight: 700;
  }
  .station-desc {
    font-size: 14px;
    color: var(--sk-brown);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .station-state {
    margin-top: auto;
    padding: 6px 16px;
    border-radius: var(--sk-r-pill);
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
  }

  /* ===== 5. Thư viện video ===== */
  .gallery-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 32px;
  }
  .gcard {
    display: flex;
    flex-direction: column;
    background: var(--sk-surface);
    border-radius: var(--sk-r-block);
    overflow: hidden;
    box-shadow: var(--sk-shadow-card);
  }
  .gcard-media {
    position: relative;
    aspect-ratio: 389 / 219;
    /* Khung video vẫn trung tính — pastel dừng ở mép khung (§2.1) */
    background: var(--sk-stage);
  }
  .gcard-media video,
  .gcard-media img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .speed-badge {
    position: absolute;
    top: 10px;
    left: 10px;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 3px 10px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-surface);
    color: var(--sk-ink);
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
  }
  .gcard-play {
    position: absolute;
    top: 42%;
    left: 50%;
    transform: translate(-50%, -50%);
    display: grid;
    place-items: center;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    border: none;
    background: var(--sk-amber);
    color: var(--sk-amber-mid);
    cursor: pointer;
    box-shadow: var(--sk-shadow-lift);
  }
  /* Banner phụ đề to — phần trợ năng quan trọng nhất của thẻ */
  .subtitle-banner {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    margin: 0;
    padding: 8px 14px;
    background: #263143;
    color: #ecf1ff;
    font-family: var(--sk-font-head);
    font-size: 22px;
    font-weight: 700;
    text-align: center;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .gcard-body {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 20px;
  }
  .gcard-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
  }
  .meta-topic {
    color: var(--sk-blue-ink);
  }
  .meta-level {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    color: var(--sk-green-ink);
  }
  .gcard-title {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
  }
  .gcard-desc {
    margin: 0;
    font-size: 14px;
    color: var(--sk-brown);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .gcard-actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-top: 4px;
  }
  /* Hai hành động là NÚT CHỮ nhỏ màu nâu, không phải viên thuốc to */
  .text-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 6px 2px;
    border: none;
    background: none;
    color: var(--sk-amber-ink);
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    text-decoration: none;
    cursor: pointer;
  }
  .text-btn:hover {
    text-decoration: underline;
  }

  /* ===== 6. Góc phụ huynh ===== */
  .parent-callout {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
    flex-wrap: wrap;
    background: var(--sk-lavender-2);
    border-radius: var(--sk-r-block);
    padding: 28px 40px;
  }
  .parent-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  .parent-dot {
    display: grid;
    place-items: center;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: var(--sk-surface);
    color: var(--sk-blue-ink);
    flex-shrink: 0;
  }
  .parent-left h2 {
    margin: 0 0 2px;
    font-size: 20px;
  }
  .parent-left p {
    margin: 0;
    font-size: 14px;
    color: var(--sk-brown);
    max-width: 62ch;
  }

  @media (max-width: 980px) {
    .hero-inner,
    .quest-grid {
      grid-template-columns: 1fr;
    }
    .hero {
      padding: 28px;
    }
    .mochi-showcase {
      justify-self: center;
    }
    .map-board {
      padding: 28px 20px;
    }
    .map-path {
      display: none;
    }
    .section-head h2 {
      font-size: 28px;
    }
  }
</style>
