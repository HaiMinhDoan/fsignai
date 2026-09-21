<template>
  <p v-if="loading" class="hint">Đang mở gói từ…</p>
  <p v-else-if="error" class="error" role="alert">{{ error }}</p>

  <template v-else-if="pack">
    <!-- ===== Đầu trang: tên gói + tiến độ ===== -->
    <section class="head">
      <RouterLink to="/" class="back-chip">
        <SiIcon name="back" :size="20" />
        <span>Về bản đồ</span>
      </RouterLink>

      <div class="head-row">
        <span class="head-disc" :style="{ background: pack.islandColor || 'var(--sk-sky)' }">
          <SiIcon :name="pack.iconName || 'hand'" :size="28" />
        </span>
        <div class="head-text">
          <h1>{{ pack.titleVi }}</h1>
          <p v-if="pack.descriptionVi">{{ pack.descriptionVi }}</p>
        </div>
      </div>

      <div v-if="items.length" class="progress-row">
        <div class="progress-track">
          <span class="progress-fill" :style="{ width: `${progressPercent}%` }"></span>
        </div>
        <span class="progress-num">{{ doneCount }} / {{ items.length }} từ</span>
      </div>
    </section>

    <!-- ===== Khách chưa đăng nhập ===== -->
    <section v-if="!auth.isLoggedIn()" class="panel notice-panel">
      <MascotWave :size="90" label="đang mời đăng nhập">Đăng nhập để lưu lại tiến độ nhé!</MascotWave>
      <RouterLink :to="{ path: '/dang-nhap', query: { redirect: route.fullPath } }" class="btn btn--amber">
        Đăng nhập
      </RouterLink>
    </section>

    <!-- ===== Gói đang khoá ===== -->
    <section v-else-if="pack.unlocked === false" class="panel notice-panel">
      <MascotWave :size="90" layout="column" label="đang chỉ đường">
        Học xong gói trước đã nhé, {{ pack.unlockAfterPackTitleVi || 'gói trước đó' }}!
      </MascotWave>
      <RouterLink to="/" class="btn btn--amber">Về bản đồ</RouterLink>
    </section>

    <!-- ===== Gói chưa có từ nào (chỉ xảy ra khi biên tập viên chưa soạn xong) ===== -->
    <section v-else-if="!items.length" class="panel notice-panel">
      <p>Gói này chưa có từ nào — quay lại sau nhé.</p>
    </section>

    <!-- ===== Đã học hết ===== -->
    <section v-else-if="finished" class="panel finish-panel">
      <div class="finish-stage">
        <ConfettiBurst :fire="1" />
        <SiIcon name="trophy" :size="56" />
      </div>
      <h2>Xong rồi! Bạn giỏi quá! 🎉</h2>
      <p class="stars-row">
        <SiIcon
          v-for="n in 3"
          :key="n"
          name="star"
          :size="32"
          :class="n <= (pack.myStars || 0) ? 'star-on' : 'star-off'"
        />
      </p>
      <div class="finish-actions">
        <RouterLink to="/" class="btn btn--white">Về bản đồ</RouterLink>
        <RouterLink v-if="nextPack" :to="`/goi-tu/${nextPack}`" class="btn btn--amber">
          Sang gói tiếp theo
        </RouterLink>
      </div>
    </section>

    <!-- ===== Đang học ===== -->
    <section v-else class="panel learn-panel">
      <p class="item-count">Từ {{ currentIndex + 1 }} / {{ items.length }}</p>

      <!-- Khung video trung tính — không màu, không gradient (§2.1) -->
      <div class="stage">
        <video
          v-if="currentVideo"
          ref="videoEl"
          :key="currentVideo.id"
          :src="currentVideo.videoUrl"
          :poster="currentVideo.thumbnailUrl || currentItem?.signThumbnailUrl"
          controls
          playsinline
          preload="metadata"
          @loadedmetadata="applyRate"
        ></video>
        <img
          v-else-if="currentItem?.signPrimaryVideoUrl"
          :src="currentItem.signPrimaryVideoUrl"
          :alt="`Ký hiệu của từ ${currentItem.signWordVi}`"
        />
        <span v-else class="stage-empty"><SiIcon name="hand" :size="48" /></span>
      </div>

      <h2 class="word">{{ currentItem?.signWordVi }}</h2>

      <div class="learn-actions">
        <button
          type="button"
          class="btn btn--white"
          :disabled="currentIndex === 0"
          @click="currentIndex--"
        >
          <SiIcon name="back" :size="20" />
          <span>Từ trước</span>
        </button>
        <button type="button" class="btn btn--amber" :disabled="saving" @click="markDone">
          <SiIcon name="check" :size="20" />
          <span>Bạn thuộc từ này rồi!</span>
        </button>
      </div>
    </section>
  </template>
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';
  import { useRoute } from 'vue-router';
  import {
    wordPackDetailApi,
    wordPackProgressApi,
    wordPackStartApi,
    wordPacksApi,
    type WordPack,
    type WordPackItem,
  } from '@/api/catalog';
  import { signVideosApi, type SignVideo } from '@/api/dictionary';
  import { useAuthStore } from '@/stores/auth';
  import { useA11yStore } from '@/stores/a11y';
  import ConfettiBurst from '@/components/ConfettiBurst.vue';
  import MascotWave from '@/components/MascotWave.vue';
  import SiIcon from '@/components/SiIcon.vue';

  defineOptions({ name: 'WordPackJourneyView' });

  const props = defineProps<{ id: string }>();
  const route = useRoute();
  const auth = useAuthStore();
  const a11y = useA11yStore();

  const pack = ref<WordPack | null>(null);
  const items = ref<WordPackItem[]>([]);
  const loading = ref(true);
  const error = ref('');
  const saving = ref(false);
  const nextPack = ref<string | null>(null);

  const currentIndex = ref(0);
  const currentVideo = ref<SignVideo | null>(null);
  const videoEl = ref<HTMLVideoElement>();

  const doneCount = computed(() => Math.min(pack.value?.myItemsCompleted ?? 0, items.value.length));
  const progressPercent = computed(() =>
    items.value.length ? Math.round((doneCount.value / items.value.length) * 100) : 0,
  );
  const finished = computed(
    () => items.value.length > 0 && pack.value?.myStatus === 'COMPLETED',
  );
  const currentItem = computed(() => items.value[currentIndex.value]);

  async function loadCurrentVideo() {
    currentVideo.value = null;
    const signId = currentItem.value?.signId;
    if (!signId) return;
    try {
      const videos = await signVideosApi(signId);
      currentVideo.value =
        videos.find((v) => v.region === 'COMMON' && v.isPrimary) ??
        videos.find((v) => v.isPrimary) ??
        videos[0] ??
        null;
    } catch {
      currentVideo.value = null;
    }
  }
  watch(currentIndex, loadCurrentVideo);

  /** Tốc độ lấy từ thanh trợ năng — người học chỉnh một lần, mọi video nghe theo */
  function applyRate() {
    if (videoEl.value) videoEl.value.playbackRate = a11y.playbackRate;
  }
  watch(() => a11y.playbackRate, applyRate);

  async function markDone() {
    if (!pack.value || saving.value) return;
    saving.value = true;
    try {
      const target = Math.max(doneCount.value, currentIndex.value + 1);
      pack.value = await wordPackProgressApi(pack.value.id, target);
      if (pack.value.myStatus !== 'COMPLETED' && currentIndex.value < items.value.length - 1) {
        currentIndex.value += 1;
      }
    } finally {
      saving.value = false;
    }
  }

  /** Gói kế tiếp trên bản đồ — để nút "Sang gói tiếp theo" có chỗ đi tới */
  async function findNextPack(currentId: string) {
    try {
      const all = await wordPacksApi();
      const i = all.findIndex((p) => p.id === currentId);
      nextPack.value = i >= 0 && i < all.length - 1 ? all[i + 1]!.id : null;
    } catch {
      nextPack.value = null;
    }
  }

  async function load() {
    loading.value = true;
    error.value = '';
    try {
      let detail = await wordPackDetailApi(props.id);

      // Đã đăng nhập và gói đang mở thì tự bắt đầu — người học không phải bấm thêm
      // một nút "Bắt đầu" thừa. Gọi start() an toàn dù đã bắt đầu từ trước
      // (idempotent ở backend).
      if (auth.isLoggedIn() && detail.unlocked !== false) {
        detail = await wordPackStartApi(props.id);
        // start() không trả kèm items — lấy lại chi tiết đầy đủ
        detail = { ...(await wordPackDetailApi(props.id)), ...pickProgress(detail) };
      }

      pack.value = detail;
      items.value = detail.items ?? [];
      currentIndex.value = Math.min(detail.myItemsCompleted ?? 0, Math.max(items.value.length - 1, 0));
      await loadCurrentVideo();
      findNextPack(props.id);
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  }

  /** Giữ lại 4 trường tiến độ từ response của start(), phần còn lại lấy từ detail() */
  function pickProgress(p: WordPack) {
    return {
      unlocked: p.unlocked,
      myStatus: p.myStatus,
      myItemsCompleted: p.myItemsCompleted,
      myStars: p.myStars,
    };
  }

  onMounted(load);
</script>

<style scoped>
  .hint {
    color: var(--sk-brown);
  }
  .error {
    color: #c4503f;
    font-weight: 600;
  }

  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 52px;
    padding: 0 24px;
    border-radius: var(--sk-r-pill);
    font-family: var(--sk-font-head);
    font-size: 16px;
    font-weight: 700;
    text-decoration: none;
    border: none;
    cursor: pointer;
  }
  .btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .btn--amber {
    background: var(--sk-amber);
    color: var(--sk-amber-mid);
    box-shadow: var(--sk-shadow-card);
  }
  .btn--white {
    background: var(--sk-surface);
    color: var(--sk-ink);
    box-shadow: var(--sk-shadow-card);
  }

  .back-chip {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-height: 44px;
    padding: 0 18px;
    margin-bottom: 16px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-surface);
    color: var(--sk-blue-ink);
    font-weight: 700;
    text-decoration: none;
    box-shadow: var(--sk-shadow);
  }

  .head {
    margin-bottom: 20px;
  }
  .head-row {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 14px;
  }
  .head-disc {
    display: grid;
    place-items: center;
    width: 64px;
    height: 64px;
    border-radius: 20px;
    color: #fff;
    flex-shrink: 0;
    box-shadow: var(--sk-shadow-card);
  }
  .head-text h1 {
    margin: 0;
    font-size: 30px;
  }
  .head-text p {
    margin: 4px 0 0;
    color: var(--sk-brown);
  }

  .progress-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .progress-track {
    flex: 1;
    height: 14px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-lavender-2);
    overflow: hidden;
  }
  .progress-fill {
    display: block;
    height: 100%;
    background: var(--sk-amber);
    transition: width 320ms ease;
  }
  .progress-num {
    font-family: var(--sk-font-head);
    font-weight: 700;
    color: var(--sk-brown);
    white-space: nowrap;
  }

  .panel {
    background: var(--sk-surface);
    border-radius: var(--sk-r-block);
    padding: 32px;
    box-shadow: var(--sk-shadow-card);
  }
  .notice-panel {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
    text-align: center;
    padding: 48px 24px;
  }

  .learn-panel {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }
  .item-count {
    align-self: flex-start;
    margin: 0;
    font-family: var(--sk-font-head);
    font-size: 13px;
    font-weight: 700;
    color: var(--sk-amber-ink);
  }
  .stage {
    width: 100%;
    max-width: 480px;
    aspect-ratio: 4 / 3;
    background: var(--sk-stage);
    border: 1px solid var(--sk-stage-border);
    border-radius: 16px;
    overflow: hidden;
    display: grid;
    place-items: center;
  }
  .stage video,
  .stage img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
  .stage-empty {
    color: var(--sk-brown);
  }
  .word {
    margin: 0;
    font-size: 32px;
  }
  .learn-actions {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    justify-content: center;
  }

  .finish-panel {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    text-align: center;
    padding: 48px 24px;
  }
  .finish-stage {
    position: relative;
    display: grid;
    place-items: center;
    width: 96px;
    height: 96px;
    border-radius: 50%;
    background: var(--sk-peach);
    color: var(--sk-amber-ink);
  }
  .finish-panel h2 {
    margin: 0;
    font-size: 28px;
  }
  .stars-row {
    display: flex;
    gap: 8px;
    margin: 0;
  }
  .star-on {
    color: var(--sk-amber);
  }
  .star-off {
    color: var(--sk-blue-150);
  }
  .finish-actions {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    justify-content: center;
  }
</style>
