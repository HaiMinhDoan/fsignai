<template>
  <p v-if="loading" class="hint">Đang mở phòng luyện tập…</p>
  <p v-else-if="error" class="error" role="alert">{{ error }}</p>

  <article v-else-if="sign" class="detail">
    <RouterLink to="/tu-dien" class="back-chip">
      <SiIcon name="back" :size="20" />
      <span>Về kho từ vựng</span>
    </RouterLink>

    <header class="word-head">
      <div>
        <h1>{{ sign.wordVi }}</h1>
        <p v-if="sign.wordEn" class="word-en">{{ sign.wordEn }}</p>
        <div class="pills">
          <span class="pill" :style="{ background: levelColor }">{{ levelLabel }}</span>
          <span v-if="sign.primaryTopicNameVi" class="pill pill-plain">
            {{ sign.primaryTopicNameVi }}
          </span>
          <span v-if="regions.length > 1" class="pill pill-plain">
            {{ regions.length }} vùng miền
          </span>
        </div>
      </div>
      <div class="word-head-right">
        <button
          type="button"
          class="save-btn"
          :class="{ 'is-saved': saved }"
          :disabled="savingToggle"
          @click="toggleSave"
        >
          <SiIcon name="star" :size="20" />
          <span>{{ saved ? 'Đã lưu' : 'Lưu từ này' }}</span>
        </button>
        <MascotWave :size="96" label="đang hướng dẫn">Nhìn mẫu rồi làm theo nhé!</MascotWave>
      </div>
    </header>

    <!-- Chọn vùng miền: chip to, có dấu ✓ khi đang chọn chứ không chỉ đổi màu -->
    <div v-if="regions.length > 1" class="chip-row" role="group" aria-label="Chọn vùng miền">
      <button
        v-for="r in regions"
        :key="r"
        type="button"
        class="chip"
        :class="{ 'chip--on': activeRegion === r }"
        :aria-pressed="activeRegion === r"
        @click="activeRegion = r"
      >
        <SiIcon v-if="activeRegion === r" name="check" :size="18" />
        <span>{{ regionLabel(r) }}</span>
      </button>
    </div>

    <!-- ===== Phòng luyện tập ===== -->
    <section class="room" aria-labelledby="room-heading">
      <h2 id="room-heading" class="si-visually-hidden">Phòng luyện tập</h2>

      <!-- Cột mẫu -->
      <div class="stage">
        <p class="stage-title"><SiIcon name="play" :size="20" /><span>Mẫu</span></p>

        <!-- Khung video tuyệt đối trung tính: không viền màu, không nền pastel,
             không gradient. Mắt bé phải đọc được hình bàn tay và sắc mặt
             (docs/05-design-system.md §2.1) -->
        <div class="media-frame">
          <video
            v-if="activeVideo"
            ref="sampleVideo"
            :key="activeVideo.id"
            :src="activeVideo.videoUrl"
            :poster="activeVideo.thumbnailUrl || sign.thumbnailUrl"
            controls
            playsinline
            preload="metadata"
            @loadedmetadata="applyRate"
          ></video>
          <p v-else class="frame-empty">Từ này chưa có video.</p>
        </div>

        <div v-if="activeVideo" class="controls">
          <!-- Tốc độ chậm là công cụ học thật sự, không phải trang trí: bàn tay
               đi nhanh thì bé không kịp thấy khẩu hình và điểm chạm -->
          <div class="control-group" role="group" aria-label="Tốc độ phát">
            <span class="control-label"><SiIcon name="slow" :size="18" /><span>Tốc độ</span></span>
            <button
              v-for="r in RATES"
              :key="r"
              type="button"
              class="chip chip--sm"
              :class="{ 'chip--on': rate === r }"
              :aria-pressed="rate === r"
              @click="setRate(r)"
            >
              {{ r }}×
            </button>
          </div>

          <div class="control-group">
            <button type="button" class="chip chip--sm" @click="replay">
              <SiIcon name="repeat" :size="18" />
              <span>Xem lại</span>
            </button>
            <button
              type="button"
              class="chip chip--sm"
              :class="{ 'chip--on': looping }"
              :aria-pressed="looping"
              @click="toggleLoop"
            >
              <SiIcon v-if="looping" name="check" :size="18" />
              <span>Lặp lại mãi</span>
            </button>
          </div>
        </div>

        <p v-if="activeVideo?.captionVi" class="caption">{{ activeVideo.captionVi }}</p>
      </div>

      <!-- Cột gương -->
      <div class="stage">
        <p class="stage-title"><SiIcon name="camera" :size="20" /><span>Gương của bé</span></p>

        <div class="media-frame mirror-frame">
          <!-- Lật ngang để bé thấy mình như soi gương: không lật thì bé giơ tay
               phải lại thấy hình giơ tay trái và bắt chước ngược -->
          <video v-show="mirrorOn" ref="mirrorVideo" class="mirror" autoplay playsinline muted></video>

          <div v-if="!mirrorOn" class="mirror-off">
            <SiIcon name="camera" :size="48" />
            <p v-if="mirrorError" class="mirror-error">{{ mirrorError }}</p>
            <p v-else class="mirror-hint">Bật gương để vừa nhìn mẫu vừa nhìn tay mình.</p>
          </div>

          <ConfettiBurst :fire="celebrate" />
        </div>

        <div class="controls">
          <button type="button" class="btn btn-soft" @click="toggleMirror">
            <SiIcon name="camera" :size="20" />
            <span>{{ mirrorOn ? 'Tắt gương' : 'Bật gương' }}</span>
          </button>
          <button type="button" class="btn btn-sun" @click="cheer">
            <SiIcon name="sparkles" :size="20" />
            <span>Bé làm được rồi!</span>
          </button>
        </div>

        <!-- Dấu ✓ + chữ mới là thứ mang thông tin; pháo hoa chỉ là phần thêm -->
        <p v-if="celebrate" class="cheer-note">
          <SiIcon name="check" :size="22" />
          <span>Giỏi lắm! Thử thêm một lần ở tốc độ nhanh hơn nhé.</span>
        </p>

        <!-- Chấm điểm bằng AI: chỉ hiện khi từ này đã có mẫu (readiness) -->
        <AiCheckPanel
          v-if="aiReady && auth.isLoggedIn()"
          :sign-id="sign.id"
          :get-video="() => mirrorVideo"
          :ensure-camera="ensureCamera"
          @result="onAiResult"
        />
        <p v-else-if="aiReady" class="ai-login">
          <SiIcon name="sparkles" :size="20" />
          <span>
            <RouterLink to="/dang-nhap">Đăng nhập</RouterLink> để Mochi chấm điểm ký hiệu của bé bằng AI nhé.
          </span>
        </p>
      </div>
    </section>

    <section v-if="sign.descriptionVi" class="how-to">
      <h2>Cách làm ký hiệu này</h2>
      <p>{{ sign.descriptionVi }}</p>
    </section>
  </article>
</template>

<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import {
    signDetailApi,
    signVideosApi,
    isSignSavedApi,
    saveSignApi,
    unsaveSignApi,
    type SignSummary,
    type SignVideo,
  } from '@/api/dictionary';
  import { aiReadinessApi, type AiVerifyResult } from '@/api/aiCheck';
  import { useAuthStore } from '@/stores/auth';
  import AiCheckPanel from '@/components/AiCheckPanel.vue';
  import ConfettiBurst from '@/components/ConfettiBurst.vue';
  import MascotWave from '@/components/MascotWave.vue';
  import SiIcon from '@/components/SiIcon.vue';

  defineOptions({ name: 'SignDetailView' });

  const props = defineProps<{ id: string }>();

  const LEVEL_LABELS: Record<string, string> = {
    BEGINNER: 'Người mới',
    BASIC: 'Cơ bản',
    INTERMEDIATE: 'Trung cấp',
    ADVANCED: 'Nâng cao',
  };
  const LEVEL_COLORS: Record<string, string> = {
    BEGINNER: 'var(--si-kid-mint)',
    BASIC: 'var(--si-kid-sky)',
    INTERMEDIATE: 'var(--si-kid-sun)',
    ADVANCED: 'var(--si-kid-lilac)',
  };
  const REGION_LABELS: Record<string, string> = {
    NORTH: 'Miền Bắc',
    CENTRAL: 'Miền Trung',
    SOUTH: 'Miền Nam',
    COMMON: 'Dùng chung',
  };
  const RATES = [0.5, 0.75, 1];

  const sign = ref<SignSummary | null>(null);
  const videos = ref<SignVideo[]>([]);
  const activeRegion = ref<string>('COMMON');
  const loading = ref(true);
  const error = ref('');

  const sampleVideo = ref<HTMLVideoElement>();
  const rate = ref(1);
  const looping = ref(false);

  const mirrorVideo = ref<HTMLVideoElement>();
  const mirrorOn = ref(false);
  const mirrorError = ref('');
  let mirrorStream: MediaStream | null = null;

  /** Tăng lên mỗi lần ăn mừng — ConfettiBurst theo dõi số này để bắn lại */
  const celebrate = ref(0);

  const saved = ref(false);
  const savingToggle = ref(false);

  const auth = useAuthStore();
  /** Từ này đã có mẫu để AI chấm chưa — chưa có thì ẩn hẳn nút, không hứa điều chưa làm được */
  const aiReady = ref(false);

  /** Panel AI cần webcam đang chạy: bật giúp bé nếu bé chưa bật gương */
  async function ensureCamera(): Promise<boolean> {
    if (!mirrorOn.value) await toggleMirror();
    return mirrorOn.value;
  }

  function onAiResult(r: AiVerifyResult) {
    if (r.passed) celebrate.value += 1;
  }

  async function toggleSave() {
    if (!sign.value) return;
    savingToggle.value = true;
    try {
      if (saved.value) {
        await unsaveSignApi(sign.value.id);
        saved.value = false;
      } else {
        await saveSignApi(sign.value.id);
        saved.value = true;
      }
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      savingToggle.value = false;
    }
  }

  const levelLabel = computed(() =>
    sign.value ? (LEVEL_LABELS[sign.value.level] ?? sign.value.level) : '',
  );
  const levelColor = computed(() =>
    sign.value ? (LEVEL_COLORS[sign.value.level] ?? 'var(--si-surface-2)') : 'var(--si-surface-2)',
  );
  const regionLabel = (r: string) => REGION_LABELS[r] ?? r;

  const regions = computed(() => Array.from(new Set(videos.value.map((v) => v.region))));

  const activeVideo = computed(
    () =>
      videos.value.find((v) => v.region === activeRegion.value && v.isPrimary) ??
      videos.value.find((v) => v.region === activeRegion.value) ??
      videos.value[0],
  );

  /** Thẻ <video> mới dựng lại mỗi lần đổi vùng miền nên phải đặt lại tốc độ */
  function applyRate() {
    if (sampleVideo.value) {
      sampleVideo.value.playbackRate = rate.value;
      sampleVideo.value.loop = looping.value;
    }
  }

  function setRate(value: number) {
    rate.value = value;
    applyRate();
  }

  function toggleLoop() {
    looping.value = !looping.value;
    applyRate();
  }

  function replay() {
    const el = sampleVideo.value;
    if (!el) return;
    el.currentTime = 0;
    void el.play();
  }

  async function toggleMirror() {
    if (mirrorOn.value) {
      stopMirror();
      return;
    }
    mirrorError.value = '';
    try {
      mirrorStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
      // Thẻ <video> dùng v-show chứ không v-if, nên nó đã nằm sẵn trong DOM và
      // gán được luồng ngay — v-if sẽ phải chờ nextTick mới có ref
      if (mirrorVideo.value) mirrorVideo.value.srcObject = mirrorStream;
      mirrorOn.value = true;
    } catch {
      // Bé từ chối quyền, máy không có webcam, hoặc trang chạy trên http không
      // phải localhost — cả ba đều rơi vào đây và cần một câu nói rõ việc
      mirrorError.value = 'Chưa mở được camera. Bé hỏi người lớn cho phép dùng camera nhé.';
      mirrorOn.value = false;
    }
  }

  function stopMirror() {
    mirrorStream?.getTracks().forEach((t) => t.stop());
    mirrorStream = null;
    if (mirrorVideo.value) mirrorVideo.value.srcObject = null;
    mirrorOn.value = false;
  }

  function cheer() {
    celebrate.value += 1;
  }

  async function load(id: string) {
    loading.value = true;
    error.value = '';
    celebrate.value = 0;
    try {
      const [detail, list, savedStatus, readiness] = await Promise.all([
        signDetailApi(id),
        signVideosApi(id),
        isSignSavedApi(id).catch(() => ({ saved: false })),
        aiReadinessApi(id).catch(() => ({ ready: false })),
      ]);
      sign.value = detail;
      videos.value = list;
      saved.value = savedStatus.saved;
      aiReady.value = readiness.ready;
      // Ưu tiên video dùng chung; không có thì lấy vùng đầu tiên có video
      activeRegion.value = regions.value.includes('COMMON') ? 'COMMON' : (regions.value[0] ?? 'COMMON');
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  }

  watch(activeVideo, applyRate);
  watch(() => props.id, load);
  onMounted(() => load(props.id));
  // Rời trang mà quên tắt thì đèn webcam vẫn sáng — phải tự dọn
  onBeforeUnmount(stopMirror);
</script>

<style scoped>
  .hint {
    color: var(--si-text-muted);
  }
  .error {
    color: var(--si-danger);
    font-weight: 600;
  }

  .back-chip {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-height: 44px;
    padding: 0 18px;
    margin-bottom: 18px;
    border-radius: 999px;
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    color: var(--si-primary);
    font-weight: 700;
    text-decoration: none;
  }
  .back-chip:hover {
    background: var(--si-primary-light);
  }

  .word-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
    flex-wrap: wrap;
    margin-bottom: 20px;
  }
  .word-head h1 {
    margin: 0;
    font-size: 42px;
  }
  .word-en {
    margin: 2px 0 12px;
    color: var(--si-text-muted);
    font-size: 19px;
  }

  .word-head-right {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
  }
  .save-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    min-height: 40px;
    padding: 0 16px;
    border-radius: 999px;
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    color: var(--si-text-muted);
    font-family: inherit;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
  }
  .save-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
  .save-btn.is-saved {
    border-color: var(--si-warning);
    background: var(--si-kid-sun);
    color: var(--si-text);
  }

  .pills {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }
  .pill {
    display: inline-flex;
    padding: 5px 14px;
    border-radius: 999px;
    font-size: 14px;
    font-weight: 700;
    color: var(--si-text);
  }
  .pill-plain {
    background: var(--si-surface-2);
    border: 1px solid var(--si-border);
  }

  .chip-row {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    margin-bottom: 20px;
  }
  .chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    min-height: 44px;
    padding: 0 18px;
    border-radius: 999px;
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    color: var(--si-text);
    font-family: inherit;
    font-size: 16px;
    font-weight: 700;
    cursor: pointer;
  }
  .chip--sm {
    min-height: 40px;
    padding: 0 14px;
    font-size: 15px;
  }
  .chip:hover {
    background: var(--si-kid-cream);
  }
  .chip--on {
    background: var(--si-kid-sky);
    border-color: var(--si-primary);
  }

  /* ===== Phòng luyện tập: mẫu bên trái, gương bên phải ===== */
  .room {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(330px, 1fr));
    gap: 24px;
    margin-bottom: 40px;
  }

  .stage {
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius-lg);
    padding: 18px;
    box-shadow: var(--si-kid-shadow);
  }
  .stage-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 12px;
    font-weight: 800;
    font-size: 18px;
  }

  .media-frame {
    position: relative;
    aspect-ratio: 4 / 3;
    background: var(--si-surface-2);
    border: 1px solid var(--si-border);
    border-radius: var(--si-radius);
    overflow: hidden;
    display: grid;
    place-items: center;
  }
  .media-frame video {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
  .frame-empty {
    color: var(--si-text-muted);
    margin: 0;
  }

  .mirror {
    transform: scaleX(-1);
    object-fit: cover;
  }
  .mirror-off {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 20px;
    text-align: center;
    color: var(--si-text-muted);
  }
  .mirror-hint,
  .mirror-error {
    margin: 0;
    font-size: 15px;
    max-width: 34ch;
  }
  .mirror-error {
    color: var(--si-warning);
    font-weight: 600;
  }

  .controls {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-top: 14px;
  }
  .control-group {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }
  .control-label {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 15px;
    font-weight: 700;
    color: var(--si-text-muted);
  }

  .caption {
    margin: 12px 0 0;
    color: var(--si-text-muted);
    font-size: 15px;
  }

  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: var(--si-kid-tap);
    padding: 0 20px;
    border-radius: 999px;
    border: 2px solid transparent;
    font-family: inherit;
    font-size: 16px;
    font-weight: 700;
    cursor: pointer;
  }
  .btn-soft {
    background: var(--si-surface);
    color: var(--si-primary);
    border-color: var(--si-border);
  }
  .btn-soft:hover {
    background: var(--si-primary-light);
  }
  .btn-sun {
    background: var(--si-kid-sun);
    color: var(--si-text);
    box-shadow: var(--si-kid-shadow);
  }

  .cheer-note {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 14px 0 0;
    padding: 10px 14px;
    border: 2px solid var(--si-success);
    border-radius: var(--si-kid-radius);
    background: var(--si-kid-mint);
    color: var(--si-text);
    font-weight: 700;
  }

  .ai-login {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 16px 0 0;
    color: var(--si-text-muted);
    font-size: 15px;
  }

  .how-to {
    max-width: 68ch;
  }
  .how-to h2 {
    margin: 0 0 8px;
  }
  .how-to p {
    margin: 0;
  }
</style>
