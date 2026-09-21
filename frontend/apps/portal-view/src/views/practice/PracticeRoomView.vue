<template>
  <!-- ===================================================================
       Dựng theo Figma 1:1668 "SignKids - Phòng Luyện Ký Hiệu Tương Tác".
       =================================================================== -->
  <p v-if="loading" class="hint">Đang mở phòng luyện tập…</p>
  <p v-else-if="error" class="error" role="alert">{{ error }}</p>

  <template v-else-if="sign">
    <!-- ===== 1. Top Mission Control (1232×96, trắng r32) ===== -->
    <section class="mission">
      <div class="mission-left">
        <span class="mission-disc"><SiIcon name="hand" :size="28" /></span>
        <div class="mission-body">
          <div class="mission-tags">
            <span class="pill pill--sky">{{ levelLabel }}: {{ sign.primaryTopicNameVi || 'Kho từ chung' }}</span>
            <span class="live-tag">
              <span class="live-dot" :class="{ 'live-dot--off': !mirrorOn }"></span>
              <span>{{ mirrorOn ? 'Camera đang bật' : 'Camera đang tắt' }}</span>
            </span>
          </div>
          <h1 class="mission-title">Ký Hiệu: “{{ sign.wordVi.toUpperCase() }}”</h1>
        </div>
      </div>

      <div class="mission-right">
        <div class="quest-meter">
          <div class="quest-meter-row">
            <span>Tiến độ thử thách</span>
            <strong>{{ doneSteps }} / {{ totalSteps }} bước</strong>
          </div>
          <div
            class="quest-track"
            role="progressbar"
            :aria-valuenow="questPercent"
            aria-valuemin="0"
            aria-valuemax="100"
          >
            <span class="quest-fill" :style="{ width: `${questPercent}%` }"></span>
          </div>
        </div>

        <span class="treasure">
          <SiIcon name="trophy" :size="22" />
          <span class="treasure-lines">
            <small>Kho Báu</small>
            <strong>{{ stats.stars }} ⭐</strong>
          </span>
        </span>
      </div>
    </section>

    <!-- ===== 2. Sign Switcher Pills (cao 40) ===== -->
    <div v-if="switcher.length > 1" class="switcher" role="group" aria-label="Chọn ký hiệu để luyện">
      <RouterLink
        v-for="(s, i) in switcher"
        :key="s.id"
        :to="{ name: 'practice-sign', params: { id: s.id } }"
        class="switch-pill"
        :class="{ 'switch-pill--on': s.id === sign.id }"
      >
        {{ SWITCH_EMOJI[i % SWITCH_EMOJI.length] }} {{ i + 1 }}. {{ s.wordVi }}
      </RouterLink>
    </div>

    <!-- ===== 3. Dual-Pane Practice Arena (2 × 600×471, trắng r32) ===== -->
    <section class="arena">
      <!-- --- Khung A: mẫu chuẩn --- -->
      <article class="pane">
        <header class="pane-head">
          <h2 class="pane-title">
            <span class="pane-letter pane-letter--blue">A</span>
            <span>Ký Hiệu Chuẩn: Thầy Cô &amp; Mochi</span>
          </h2>
          <span class="pill pill--sky">
            <SiIcon name="check" :size="14" />
            <span>Video gốc</span>
          </span>
        </header>

        <!-- Thanh tốc độ (560×56, nền #F0F3FF) -->
        <div class="tool-bar">
          <div class="tool-left">
            <button type="button" class="round-btn round-btn--brown" @click="replay">
              <SiIcon name="repeat" :size="18" />
              <span class="si-visually-hidden">Xem lại từ đầu</span>
            </button>
            <span class="tool-label">Tốc độ xem:</span>
          </div>
          <div class="speed-group">
            <button
              v-for="r in RATES"
              :key="r.value"
              type="button"
              class="speed-btn"
              :class="{ 'speed-btn--on': a11y.playbackRate === r.value }"
              :aria-pressed="a11y.playbackRate === r.value"
              @click="a11y.setRate(r.value)"
            >
              {{ r.label }}
            </button>
          </div>
        </div>

        <!-- Khung video: nền trung tính, pastel dừng ở mép khung (§2.1) -->
        <div class="port">
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
          <img v-else :src="teacherPane" alt="Khung video mẫu, từ này chưa có video" />

          <!-- "Large High Contrast Visual Subtitle" — phụ đề tương phản cao -->
          <p class="port-subtitle">{{ sign.wordVi.toUpperCase() }}</p>
        </div>

        <div v-if="regions.length > 1" class="region-row">
          <button
            v-for="r in regions"
            :key="r"
            type="button"
            class="chip"
            :class="{ 'chip--on': activeRegion === r }"
            :aria-pressed="activeRegion === r"
            @click="activeRegion = r"
          >
            {{ regionLabel(r) }}
          </button>
        </div>
      </article>

      <!-- --- Khung B: gương của người học --- -->
      <article class="pane">
        <header class="pane-head">
          <h2 class="pane-title">
            <span class="pane-letter pane-letter--green">B</span>
            <span>Gương Thần Của Bạn</span>
          </h2>
          <span class="pill" :class="mirrorOn ? 'pill--mint' : 'pill--grey'">
            <SiIcon name="camera" :size="14" />
            <span>{{ mirrorOn ? 'Gương đang bật' : 'Gương đang tắt' }}</span>
          </span>
        </header>

        <div class="tool-bar">
          <div class="tool-left">
            <button type="button" class="mini-btn" @click="flipped = !flipped">
              <SiIcon name="repeat" :size="14" />
              <span>Lật Gương</span>
            </button>
            <!-- Nói thật: chưa có AI nên nút khung xương chưa bật được -->
            <button type="button" class="mini-btn mini-btn--off" disabled>
              <SiIcon name="hand" :size="14" />
              <span>Khung Xương: Chưa bật</span>
            </button>
          </div>
          <button
            type="button"
            class="capture-btn"
            :disabled="!mirrorOn"
            @click="capture"
          >
            <SiIcon name="camera" :size="15" />
            <span>Chụp Kỷ Niệm 📸</span>
          </button>
        </div>

        <div class="port port--mirror">
          <video
            v-show="mirrorOn"
            ref="mirrorVideo"
            class="mirror"
            :class="{ 'mirror--flipped': flipped }"
            autoplay
            playsinline
            muted
          ></video>

          <template v-if="!mirrorOn">
            <img :src="mirrorPane" alt="Xem trước màn hình gương soi khi bật camera" />
            <button type="button" class="port-cta" @click="toggleMirror">
              <SiIcon name="camera" :size="20" />
              <span>Bật gương của bạn</span>
            </button>
          </template>

          <ConfettiBurst :fire="celebrate" />
        </div>

        <p v-if="mirrorError" class="mirror-error">{{ mirrorError }}</p>
        <button v-else-if="mirrorOn" type="button" class="mini-btn mini-btn--wide" @click="toggleMirror">
          <SiIcon name="camera" :size="14" />
          <span>Tắt gương</span>
        </button>
      </article>
    </section>

    <!-- ===== 4. Step-by-Step Tactile Visual Cues ===== -->
    <section class="steps" aria-labelledby="steps-heading">
      <div class="steps-head">
        <h2 id="steps-heading">
          <SiIcon name="hand" :size="22" />
          <span>{{ steps.length || 3 }} Bước Thực Hiện Thần Tốc</span>
        </h2>
        <p class="steps-hint">Chạm vào từng bước để Mochi làm mẫu chậm</p>
      </div>

      <div class="step-row">
        <!-- Có dữ liệu thật thì dựng từ dữ liệu -->
        <article
          v-for="(step, i) in steps"
          :key="step.id"
          class="step-card"
          :class="{ 'step-card--on': activeStep === step.stepOrder }"
          tabindex="0"
          @click="pickStep(step.stepOrder)"
          @keydown.enter="pickStep(step.stepOrder)"
        >
          <div class="step-top">
            <span class="step-no" :class="`step-no--${STEP_TONES[i % 3]}`">{{ step.stepOrder }}</span>
            <SiIcon v-if="seenSteps.has(step.stepOrder)" name="check" :size="20" class="step-tick" />
          </div>
          <div class="step-shot">
            <img
              v-if="step.imageUrl"
              :src="step.imageUrl"
              :alt="`Thế tay ở bước ${step.stepOrder}: ${step.descriptionVi}`"
              loading="lazy"
            />
            <span v-else class="step-shot-empty"><SiIcon name="hand" :size="34" /></span>
          </div>
          <h3 class="step-title">Bước {{ step.stepOrder }}: {{ step.titleVi || 'Làm theo mẫu' }}</h3>
          <!-- Chữ luôn hiện: ảnh không được là kênh thông tin duy nhất (§2.2) -->
          <p class="step-desc">{{ step.descriptionVi }}</p>
        </article>

        <!-- Chưa soạn bước nào: giữ nguyên hình dáng 3 thẻ của Figma nhưng
             nói rõ là chưa có nội dung, KHÔNG mượn ảnh mẫu để giả vờ có.
             Vẫn bấm được để tính tiến độ + phát chậm mẫu — thiếu ảnh/mô tả
             không có nghĩa là người học không luyện được bước đó. -->
        <article
          v-for="n in (steps.length ? 0 : 3)"
          :key="`ph-${n}`"
          class="step-card step-card--empty"
          :class="{ 'step-card--on': activeStep === n }"
          tabindex="0"
          @click="pickStep(n)"
          @keydown.enter="pickStep(n)"
        >
          <div class="step-top">
            <span class="step-no" :class="`step-no--${STEP_TONES[(n - 1) % 3]}`">{{ n }}</span>
            <SiIcon v-if="seenSteps.has(n)" name="check" :size="20" class="step-tick" />
          </div>
          <div class="step-shot">
            <span class="step-shot-empty"><SiIcon name="hand" :size="34" /></span>
          </div>
          <h3 class="step-title">Bước {{ n }}: chưa soạn</h3>
          <p class="step-desc">
            {{
              n === 1 && sign.descriptionVi
                ? sign.descriptionVi
                : 'Biên tập viên chưa soạn hướng dẫn từng bước cho từ này.'
            }}
          </p>
        </article>
      </div>
    </section>

    <!-- ===== 5. Main Tactical Floating Control Panel (1232×96, trắng r48) ===== -->
    <section class="control-panel">
      <div class="control-left">
        <button type="button" class="big-btn big-btn--white" @click="replay">
          <SiIcon name="repeat" :size="18" />
          <span>Thử lại lần nữa 🔄</span>
        </button>
        <button type="button" class="big-btn big-btn--sky" @click="a11y.setRate(0.5)">
          <SiIcon name="slow" :size="20" />
          <span>Làm chậm hơn 🐢</span>
        </button>
      </div>

      <RouterLink
        v-if="nextSign"
        :to="{ name: 'practice-sign', params: { id: nextSign.id } }"
        class="big-btn big-btn--amber"
        @click="cheer"
      >
        <SiIcon name="sparkles" :size="22" />
        <span>Nhận Thưởng &amp; Sang Ký Hiệu Tiếp ➡️</span>
      </RouterLink>
      <button v-else type="button" class="big-btn big-btn--amber" @click="cheer">
        <SiIcon name="sparkles" :size="22" />
        <span>Bạn làm được rồi! ⭐</span>
      </button>
    </section>

    <p v-if="celebrate" class="cheer-note">
      <SiIcon name="check" :size="22" />
      <span>Giỏi lắm! Sao sẽ được cộng khi phần tính điểm bật ở bản sau.</span>
    </p>
  </template>
</template>

<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import {
    dictionarySearchApi,
    signDetailApi,
    signStepsApi,
    signVideosApi,
    type SignStep,
    type SignSummary,
    type SignVideo,
  } from '@/api/dictionary';
  import { useA11yStore } from '@/stores/a11y';
  import { useLearnerStatsStore } from '@/stores/learnerStats';
  import ConfettiBurst from '@/components/ConfettiBurst.vue';
  import SiIcon from '@/components/SiIcon.vue';

  // Ảnh Figma dùng làm nền chờ khi chưa có video / chưa bật camera
  import teacherPane from '@/assets/figma/practice-teacher-pane.png';
  import mirrorPane from '@/assets/figma/practice-mirror-pane.png';

  defineOptions({ name: 'PracticeRoomView' });

  const props = defineProps<{ id?: string }>();

  const a11y = useA11yStore();
  const statsStore = useLearnerStatsStore();
  const stats = computed(() => statsStore.stats);

  const RATES = [
    { value: 0.5, label: '0.5x 🐢' },
    { value: 0.75, label: '0.75x' },
    { value: 1, label: '1.0x Chuẩn' },
  ];
  const SWITCH_EMOJI = ['👋', '🙏', '🤝', '❤️'];
  const STEP_TONES = ['peach', 'sky', 'mint'] as const;

  const LEVEL_LABELS: Record<string, string> = {
    BEGINNER: 'Cấp độ 1',
    BASIC: 'Cấp độ 2',
    INTERMEDIATE: 'Cấp độ 3',
    ADVANCED: 'Cấp độ 4',
  };
  const REGION_LABELS: Record<string, string> = {
    NORTH: 'Miền Bắc',
    CENTRAL: 'Miền Trung',
    SOUTH: 'Miền Nam',
    COMMON: 'Dùng chung',
  };
  const regionLabel = (r: string) => REGION_LABELS[r] ?? r;

  const sign = ref<SignSummary | null>(null);
  const videos = ref<SignVideo[]>([]);
  const steps = ref<SignStep[]>([]);
  const switcher = ref<SignSummary[]>([]);
  const activeRegion = ref('COMMON');
  const activeStep = ref(1);
  const seenSteps = ref<Set<number>>(new Set());
  const loading = ref(true);
  const error = ref('');

  const sampleVideo = ref<HTMLVideoElement>();
  const mirrorVideo = ref<HTMLVideoElement>();
  const mirrorOn = ref(false);
  const flipped = ref(true); // mặc định lật: người học giơ tay phải phải thấy tay phải
  const mirrorError = ref('');
  let mirrorStream: MediaStream | null = null;

  const celebrate = ref(0);

  const levelLabel = computed(() =>
    sign.value ? (LEVEL_LABELS[sign.value.level] ?? sign.value.level) : '',
  );
  const regions = computed(() => Array.from(new Set(videos.value.map((v) => v.region))));
  const activeVideo = computed(
    () =>
      videos.value.find((v) => v.region === activeRegion.value && v.isPrimary) ??
      videos.value.find((v) => v.region === activeRegion.value) ??
      videos.value[0],
  );
  const nextSign = computed(() => {
    const i = switcher.value.findIndex((s) => s.id === sign.value?.id);
    return i >= 0 ? switcher.value[i + 1] : undefined;
  });

  /** Tiến độ = số bước người học đã chạm vào, trên tổng số bước của từ này */
  const totalSteps = computed(() => steps.value.length || 3);
  const doneSteps = computed(() => seenSteps.value.size);
  const questPercent = computed(() =>
    Math.round((doneSteps.value / Math.max(totalSteps.value, 1)) * 100),
  );

  function pickStep(order: number) {
    activeStep.value = order;
    seenSteps.value = new Set(seenSteps.value).add(order);
    // Chạm vào bước nào thì phát chậm lại mẫu để người học nhìn kỹ bước đó
    a11y.setRate(0.5);
    replay();
  }

  function applyRate() {
    if (sampleVideo.value) sampleVideo.value.playbackRate = a11y.playbackRate;
  }
  watch(() => a11y.playbackRate, applyRate);
  watch(activeVideo, applyRate);

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
      if (mirrorVideo.value) mirrorVideo.value.srcObject = mirrorStream;
      mirrorOn.value = true;
    } catch {
      // Người học từ chối quyền, máy không có webcam, hoặc trang chạy trên http không
      // phải localhost — cả ba đều rơi vào đây
      mirrorError.value = 'Chưa mở được camera. Bạn cho phép trình duyệt dùng camera nhé.';
      mirrorOn.value = false;
    }
  }

  function stopMirror() {
    mirrorStream?.getTracks().forEach((t) => t.stop());
    mirrorStream = null;
    if (mirrorVideo.value) mirrorVideo.value.srcObject = null;
    mirrorOn.value = false;
  }

  /**
   * Chụp lại khung hình đang soi gương rồi tải về máy. Ảnh KHÔNG rời khỏi máy
   * người học — không gửi lên máy chủ, vì đây là ảnh riêng tư.
   */
  function capture() {
    const v = mirrorVideo.value;
    if (!v || !v.videoWidth) return;
    const canvas = document.createElement('canvas');
    canvas.width = v.videoWidth;
    canvas.height = v.videoHeight;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    if (flipped.value) {
      ctx.translate(canvas.width, 0);
      ctx.scale(-1, 1); // chụp đúng cái người học đang nhìn thấy trong gương
    }
    ctx.drawImage(v, 0, 0);
    canvas.toBlob((blob) => {
      if (!blob) return;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `signai-${sign.value?.gloss ?? 'ky-hieu'}.png`;
      a.click();
      URL.revokeObjectURL(url);
    }, 'image/png');
  }

  function cheer() {
    celebrate.value += 1;
  }

  function dayIndex(): number {
    const now = new Date();
    const start = Date.UTC(now.getFullYear(), 0, 0);
    const today = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
    return Math.floor((today - start) / 86400000);
  }

  async function resolveSignId(): Promise<string | null> {
    if (props.id) return props.id;
    const probe = await dictionarySearchApi({ page: 0, size: 1 });
    if (!probe.total) return null;
    const picked = await dictionarySearchApi({ page: dayIndex() % probe.total, size: 1 });
    return picked.items[0]?.id ?? null;
  }

  async function load() {
    loading.value = true;
    error.value = '';
    celebrate.value = 0;
    seenSteps.value = new Set();
    stopMirror();
    try {
      const id = await resolveSignId();
      if (!id) {
        error.value = 'Kho từ vựng đang trống.';
        return;
      }
      const [detail, vids, stepList] = await Promise.all([
        signDetailApi(id),
        signVideosApi(id),
        signStepsApi(id),
      ]);
      sign.value = detail;
      videos.value = vids;
      steps.value = stepList;
      activeStep.value = stepList[0]?.stepOrder ?? 1;
      activeRegion.value = regions.value.includes('COMMON')
        ? 'COMMON'
        : (regions.value[0] ?? 'COMMON');
      await loadSwitcher(detail);
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  }

  /** Vài từ cùng chủ đề để người học luyện liền mạch; kho chưa gán chủ đề thì lấy từ kề */
  async function loadSwitcher(current: SignSummary) {
    try {
      const near = await dictionarySearchApi({
        topicId: current.primaryTopicId || undefined,
        page: 0,
        size: 4,
      });
      switcher.value = [current, ...near.items.filter((s) => s.id !== current.id).slice(0, 3)];
    } catch {
      switcher.value = [current];
    }
  }

  watch(() => props.id, load);
  onMounted(() => {
    load();
    statsStore.load();
  });
  // Rời trang mà quên tắt thì đèn webcam vẫn sáng — phải tự dọn
  onBeforeUnmount(stopMirror);
</script>

<style scoped>
  .hint {
    color: var(--sk-brown);
  }
  .error {
    color: #c4503f;
    font-weight: 600;
  }

  .pill {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    height: 24px;
    padding: 0 12px;
    border-radius: var(--sk-r-pill);
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    white-space: nowrap;
  }
  .pill--sky {
    background: var(--sk-sky);
    color: var(--sk-blue-dark);
  }
  .pill--mint {
    background: var(--sk-mint);
    color: var(--sk-green-dark);
  }
  .pill--grey {
    background: var(--sk-lavender-2);
    color: var(--sk-brown);
  }

  /* ===== 1. Mission control ===== */
  .mission {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 32px;
    flex-wrap: wrap;
    background: var(--sk-surface);
    border-radius: var(--sk-r-card);
    padding: 20px 32px;
    box-shadow: var(--sk-shadow-card);
    margin-bottom: 16px;
  }
  .mission-left {
    display: flex;
    align-items: center;
    gap: 20px;
    min-width: 0;
  }
  .mission-disc {
    display: grid;
    place-items: center;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: var(--sk-amber);
    color: var(--sk-amber-mid);
    flex-shrink: 0;
  }
  .mission-body {
    min-width: 0;
  }
  .mission-tags {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    margin-bottom: 4px;
  }
  .live-tag {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    color: var(--sk-green-ink);
  }
  .live-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--sk-green-ink);
  }
  .live-dot--off {
    background: var(--sk-brown);
  }
  .live-tag:has(.live-dot--off) {
    color: var(--sk-brown);
  }
  .mission-title {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
  }

  .mission-right {
    display: flex;
    align-items: center;
    gap: 20px;
    flex-wrap: wrap;
  }
  .quest-meter {
    min-width: 192px;
  }
  .quest-meter-row {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    color: var(--sk-brown);
    margin-bottom: 4px;
  }
  .quest-meter-row strong {
    color: var(--sk-amber-ink);
  }
  .quest-track {
    height: 14px;
    padding: 2px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-lavender-2);
  }
  .quest-fill {
    display: block;
    height: 10px;
    border-radius: var(--sk-r-pill);
    background: linear-gradient(90deg, #f59e0b 0%, #50c594 100%);
    transition: width 320ms ease;
  }

  .treasure {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 49px;
    padding: 0 18px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-peach);
    color: var(--sk-amber-ink);
  }
  .treasure-lines {
    display: flex;
    flex-direction: column;
    line-height: 1.15;
  }
  .treasure-lines small {
    font-family: var(--sk-font-head);
    font-size: 11px;
    font-weight: 700;
    color: var(--sk-brown-dark);
  }
  .treasure-lines strong {
    font-family: var(--sk-font-head);
    font-size: 15px;
    font-weight: 700;
    color: var(--sk-brown-dark);
  }

  /* ===== 2. Pill đổi ký hiệu ===== */
  .switcher {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 16px;
  }
  .switch-pill {
    display: inline-flex;
    align-items: center;
    height: 40px;
    padding: 0 20px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-surface);
    color: var(--sk-brown);
    font-family: var(--sk-font-head);
    font-size: 17px;
    font-weight: 700;
    text-decoration: none;
    box-shadow: var(--sk-shadow);
  }
  .switch-pill--on {
    background: var(--sk-amber);
    color: var(--sk-amber-mid);
  }

  /* ===== 3. Đấu trường hai khung ===== */
  .arena {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
    gap: 32px;
    margin-bottom: 20px;
  }
  .pane {
    background: var(--sk-surface);
    border-radius: var(--sk-r-card);
    padding: 20px;
    box-shadow: var(--sk-shadow-card);
  }
  .pane-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 12px;
  }
  .pane-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0;
    font-size: 19px;
    font-weight: 700;
  }
  /* Chữ A / B trong vòng tròn — nhãn khung, giúp nói "nhìn khung A" */
  .pane-letter {
    display: grid;
    place-items: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    color: #fff;
    font-family: var(--sk-font-body);
    font-size: 13px;
    font-weight: 700;
    flex-shrink: 0;
  }
  .pane-letter--blue {
    background: var(--sk-blue-ink);
  }
  .pane-letter--green {
    background: var(--sk-green-ink);
  }

  .tool-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    background: var(--sk-lavender);
    border-radius: var(--sk-r-pill);
    padding: 6px 8px 6px 6px;
    margin-bottom: 12px;
  }
  .tool-left {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }
  .round-btn {
    display: grid;
    place-items: center;
    width: 44px;
    height: 44px;
    border-radius: 50%;
    border: none;
    cursor: pointer;
  }
  .round-btn--brown {
    background: var(--sk-amber-ink);
    color: #fff;
  }
  .tool-label {
    font-family: var(--sk-font-head);
    font-size: 14px;
    font-weight: 700;
    color: var(--sk-brown);
  }
  .speed-group {
    display: flex;
    gap: 4px;
    padding: 4px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-lavender-2);
  }
  .speed-btn {
    height: 24px;
    padding: 0 12px;
    border: none;
    border-radius: var(--sk-r-pill);
    background: transparent;
    color: var(--sk-brown);
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
  }
  .speed-btn--on {
    background: var(--sk-amber-ink);
    color: #fff;
  }

  .mini-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    height: 32px;
    padding: 0 12px;
    border: none;
    border-radius: var(--sk-r-pill);
    background: var(--sk-surface);
    color: var(--sk-ink);
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
  }
  .mini-btn--off {
    background: var(--sk-lavender-2);
    color: var(--sk-brown);
    cursor: not-allowed;
  }
  .mini-btn--wide {
    margin-top: 10px;
    height: 36px;
  }
  .capture-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    height: 34px;
    padding: 0 14px;
    border: none;
    border-radius: var(--sk-r-pill);
    background: var(--sk-blue-ink);
    color: #fff;
    font-family: var(--sk-font-head);
    font-size: 13px;
    font-weight: 700;
    cursor: pointer;
  }
  .capture-btn:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }

  /* Khung video/gương: nền trung tính, không pastel, không gradient (§2.1) */
  .port {
    position: relative;
    aspect-ratio: 560 / 315;
    border-radius: var(--sk-r-card);
    overflow: hidden;
    background: var(--sk-stage);
    display: grid;
    place-items: center;
  }
  .port video,
  .port img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
  .port img {
    object-fit: cover;
    opacity: 0.55;
  }
  .mirror {
    object-fit: cover !important;
  }
  .mirror--flipped {
    transform: scaleX(-1);
  }
  /* Phụ đề tương phản cao, nền tối chữ sáng — đọc được ở mọi độ sáng màn hình */
  .port-subtitle {
    position: absolute;
    left: 12px;
    right: 12px;
    bottom: 12px;
    margin: 0;
    padding: 10px;
    border-radius: 14px;
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
  .port-cta {
    position: absolute;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    height: 48px;
    padding: 0 22px;
    border: none;
    border-radius: var(--sk-r-pill);
    background: var(--sk-amber);
    color: var(--sk-amber-mid);
    font-family: var(--sk-font-head);
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
    box-shadow: var(--sk-shadow-lift);
  }
  .mirror-error {
    margin: 10px 0 0;
    font-size: 13px;
    font-weight: 700;
    color: var(--sk-amber-ink);
  }

  .region-row {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
    margin-top: 10px;
  }
  .chip {
    height: 32px;
    padding: 0 14px;
    border: 2px solid var(--sk-blue-150);
    border-radius: var(--sk-r-pill);
    background: var(--sk-surface);
    color: var(--sk-brown);
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
  }
  .chip--on {
    background: var(--sk-sky);
    border-color: var(--sk-blue-ink);
    color: var(--sk-blue-dark);
  }

  /* ===== 4. Ba bước ===== */
  .steps {
    margin-bottom: 20px;
  }
  .steps-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    flex-wrap: wrap;
    margin-bottom: 12px;
  }
  .steps-head h2 {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0;
    font-size: 24px;
    font-weight: 700;
    color: var(--sk-ink);
  }
  .steps-head h2 :deep(svg) {
    color: var(--sk-amber-ink);
  }
  .steps-hint {
    margin: 0;
    font-family: var(--sk-font-head);
    font-size: 12px;
    font-weight: 700;
    color: var(--sk-brown);
  }

  .step-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 20px;
  }
  .step-card {
    display: flex;
    flex-direction: column;
    gap: 4px;
    background: var(--sk-surface);
    border: 3px solid transparent;
    border-radius: var(--sk-r-card);
    padding: 20px;
    box-shadow: var(--sk-shadow-card);
    cursor: pointer;
  }
  .step-card--on {
    border-color: var(--sk-amber);
  }
  .step-card--empty {
    opacity: 0.92;
  }
  .step-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 4px;
  }
  .step-no {
    display: grid;
    place-items: center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    font-family: var(--sk-font-head);
    font-size: 18px;
    font-weight: 700;
  }
  .step-no--peach {
    background: var(--sk-peach);
    color: var(--sk-brown-dark);
  }
  .step-no--sky {
    background: var(--sk-sky);
    color: var(--sk-blue-dark);
  }
  .step-no--mint {
    background: var(--sk-mint);
    color: var(--sk-green-dark);
  }
  .step-tick {
    color: var(--sk-green-ink);
  }
  .step-shot {
    aspect-ratio: 357 / 130;
    border-radius: 16px;
    overflow: hidden;
    background: var(--sk-lavender-2);
    display: grid;
    place-items: center;
    margin-bottom: 8px;
  }
  .step-shot img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .step-shot-empty {
    color: var(--sk-brown);
  }
  .step-title {
    margin: 0;
    font-size: 19px;
    font-weight: 700;
  }
  .step-desc {
    margin: 0;
    font-size: 14px;
    color: var(--sk-brown);
  }

  /* ===== 5. Thanh điều khiển ===== */
  .control-panel {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    flex-wrap: wrap;
    background: var(--sk-surface);
    border: 4px solid var(--sk-blue-100);
    border-radius: var(--sk-r-block);
    padding: 18px 28px;
    box-shadow: var(--sk-shadow-card);
  }
  .control-left {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }
  .big-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    height: 56px;
    padding: 0 24px;
    border: none;
    border-radius: var(--sk-r-pill);
    font-family: var(--sk-font-head);
    font-size: 17px;
    font-weight: 700;
    text-decoration: none;
    cursor: pointer;
    transition: transform 160ms ease;
  }
  .big-btn:hover {
    transform: translateY(-2px);
  }
  .big-btn--white {
    background: var(--sk-surface);
    color: var(--sk-ink);
    box-shadow: var(--sk-shadow-card);
  }
  .big-btn--sky {
    background: var(--sk-sky);
    color: var(--sk-blue-dark);
  }
  .big-btn--amber {
    background: var(--sk-amber);
    color: var(--sk-amber-mid);
    box-shadow: var(--sk-shadow-card);
  }

  .cheer-note {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 16px 0 0;
    padding: 14px 20px;
    border-radius: var(--sk-r-card);
    background: var(--sk-mint);
    color: var(--sk-green-dark);
    font-weight: 700;
  }
</style>
