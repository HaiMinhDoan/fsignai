<template>
  <section class="ai" aria-labelledby="ai-title">
    <h3 id="ai-title" class="ai-title"><SiIcon name="sparkles" :size="20" /><span>Chấm điểm bằng AI</span></h3>

    <!-- Chưa bắt đầu -->
    <template v-if="phase === 'idle'">
      <p class="ai-note">
        Mochi sẽ xem bạn ký hiệu rồi cho điểm từng phần. Hình ảnh của bạn
        <strong>không gửi đi đâu cả</strong> — chỉ toạ độ bàn tay được gửi để tính điểm.
      </p>
      <button type="button" class="ai-btn" @click="begin">
        <SiIcon name="camera" :size="20" />
        <span>Bắt đầu chấm điểm</span>
      </button>
    </template>

    <!-- Đang chuẩn bị: bật camera + tải bộ nhận diện (lần đầu ~13MB) -->
    <p v-else-if="phase === 'loading'" class="ai-status" role="status">
      Mochi đang chuẩn bị… (lần đầu hơi lâu một chút)
    </p>

    <!-- Đếm ngược -->
    <div v-else-if="phase === 'countdown'" class="ai-count" role="status" aria-live="assertive">
      <span class="ai-count-num">{{ countdown }}</span>
      <span>Bạn giơ hai tay lên, chuẩn bị nhé!</span>
    </div>

    <!-- Đang ghi -->
    <div v-else-if="phase === 'recording'" class="ai-rec">
      <p class="ai-status ai-status--rec" role="status">
        <span class="rec-dot" aria-hidden="true"></span>
        <span>Bạn ký hiệu đi! {{ seeText }}</span>
      </p>
      <div class="ai-progress" aria-hidden="true">
        <span :style="{ width: `${Math.min(100, (status.elapsedMs / MAX_MS) * 100)}%` }"></span>
      </div>
      <button type="button" class="ai-btn ai-btn--stop" @click="finish">
        <SiIcon name="check" :size="20" />
        <span>Xong rồi</span>
      </button>
    </div>

    <p v-else-if="phase === 'sending'" class="ai-status" role="status">Mochi đang xem bạn làm…</p>

    <!-- Kết quả -->
    <div v-else-if="phase === 'result' && result" class="ai-result">
      <div class="verdict" :class="`verdict--${verdictKind}`" role="status">
        <SiIcon :name="verdictKind === 'pass' ? 'check' : verdictKind === 'near' ? 'sparkles' : 'repeat'" :size="28" />
        <div>
          <p class="verdict-title">{{ verdictTitle }}</p>
          <p class="verdict-score">{{ Math.round(result.score) }} <small>/ 100 điểm</small></p>
        </div>
      </div>

      <ul class="parts">
        <li v-for="p in parts" :key="p.label">
          <span class="part-label">{{ p.label }}</span>
          <span class="part-bar" role="meter" :aria-label="p.label" aria-valuemin="0" aria-valuemax="100" :aria-valuenow="p.value">
            <span :style="{ width: `${p.value}%` }"></span>
          </span>
          <span class="part-num">{{ p.value }}</span>
        </li>
      </ul>

      <ul v-if="result.feedback.hints.length" class="hints">
        <li v-for="h in result.feedback.hints" :key="h">{{ h }}</li>
      </ul>

      <div class="vote" role="group" aria-label="Chấm như vậy có đúng không?">
        <template v-if="!voted">
          <span class="vote-q">Mochi chấm như vậy có đúng không?</span>
          <button type="button" class="chip" @click="vote('AGREE')">👍 Đúng rồi</button>
          <button type="button" class="chip" @click="vote('DISAGREE')">👎 Chưa đúng</button>
        </template>
        <span v-else class="vote-thanks" role="status">Cảm ơn bạn đã nói cho Mochi biết nhé!</span>
      </div>

      <button type="button" class="ai-btn" @click="begin">
        <SiIcon name="repeat" :size="20" />
        <span>Thử lại</span>
      </button>
    </div>

    <!-- Lỗi -->
    <div v-else-if="phase === 'error'" class="ai-error">
      <p role="alert">{{ error }}</p>
      <button type="button" class="ai-btn" @click="begin">
        <SiIcon name="repeat" :size="20" />
        <span>Thử lại</span>
      </button>
    </div>
  </section>
</template>

<script lang="ts" setup>
  import { computed, onBeforeUnmount, ref } from 'vue';
  import { aiFeedbackApi, aiVerifyApi, type AiVerifyResult } from '@/api/aiCheck';
  import { loadModels, startCapture, type CaptureHandle, type CaptureStatus } from '@/composables/useSignCapture';
  import SiIcon from '@/components/SiIcon.vue';

  const props = defineProps<{
    signId: string;
    /** Thẻ <video> đang phát webcam (chưa lật gương ở dữ liệu — CSS mới lật để hiển thị) */
    getVideo: () => HTMLVideoElement | undefined;
    /** Bật webcam nếu chưa bật. Trả false khi không bật được (thông báo lỗi do bên gọi hiển thị) */
    ensureCamera: () => Promise<boolean>;
  }>();
  const emit = defineEmits<{ (e: 'result', r: AiVerifyResult): void }>();

  type Phase = 'idle' | 'loading' | 'countdown' | 'recording' | 'sending' | 'result' | 'error';

  /** Người học có tối đa chừng này để ký hiệu; ký xong sớm thì bấm "Xong rồi" */
  const MAX_MS = 7000;
  /** Dưới ngưỡng này gần như chắc chắn chưa ký hiệu xong hoặc camera không thấy tay */
  const MIN_FRAMES = 8;

  const phase = ref<Phase>('idle');
  const countdown = ref(3);
  const status = ref<CaptureStatus>({ frames: 0, elapsedMs: 0, hands: 0, body: false });
  const result = ref<AiVerifyResult | null>(null);
  const error = ref('');
  const voted = ref(false);

  let handle: CaptureHandle | null = null;
  let countdownTimer: ReturnType<typeof setInterval> | undefined;
  let cancelled = false;

  /** Cho người học biết máy có thấy mình không — người học biết phải chỉnh gì thay vì đoán */
  const seeText = computed(() => {
    if (!status.value.body) return 'Mình chưa thấy vai của bạn.';
    if (status.value.hands === 0) return 'Mình chưa thấy tay.';
    return status.value.hands === 1 ? 'Mình thấy 1 tay.' : 'Mình thấy 2 tay.';
  });

  const verdictKind = computed<'pass' | 'near' | 'retry'>(() => {
    if (!result.value) return 'retry';
    if (result.value.passed) return 'pass';
    return result.value.score >= 45 ? 'near' : 'retry';
  });
  const verdictTitle = computed(
    () => ({ pass: 'Giỏi lắm!', near: 'Gần đúng rồi!', retry: 'Mình thử lại nhé!' })[verdictKind.value],
  );
  const parts = computed(() => {
    const f = result.value?.feedback;
    return f
      ? [
          { label: 'Hình bàn tay', value: f.handshape },
          { label: 'Vị trí tay', value: f.location },
          { label: 'Chuyển động', value: f.movement },
        ]
      : [];
  });

  function fail(message: string) {
    error.value = message;
    phase.value = 'error';
  }

  async function begin() {
    cancelled = false;
    result.value = null;
    voted.value = false;
    phase.value = 'loading';

    const camOk = await props.ensureCamera();
    if (cancelled) return;
    if (!camOk) {
      return fail('Chưa mở được camera. Bạn cho phép trình duyệt dùng camera nhé.');
    }

    let models;
    try {
      models = await loadModels();
    } catch {
      return fail('Chưa tải được bộ nhận diện. Bạn kiểm tra mạng rồi thử lại nhé.');
    }
    if (cancelled) return;

    const video = await waitForVideo();
    if (!video) {
      return fail('Camera chưa sẵn sàng. Bạn thử lại nhé.');
    }

    countdown.value = 3;
    phase.value = 'countdown';
    countdownTimer = setInterval(() => {
      countdown.value -= 1;
      if (countdown.value <= 0) {
        clearInterval(countdownTimer);
        if (!cancelled) record(video, models);
      }
    }, 1000);
  }

  /** Webcam vừa bật cần một chút để có khung hình đầu tiên */
  async function waitForVideo(): Promise<HTMLVideoElement | null> {
    for (let i = 0; i < 30; i++) {
      const v = props.getVideo();
      if (v && v.readyState >= 2 && v.videoWidth > 0) return v;
      await new Promise((r) => setTimeout(r, 100));
      if (cancelled) return null;
    }
    return null;
  }

  function record(video: HTMLVideoElement, models: Awaited<ReturnType<typeof loadModels>>) {
    status.value = { frames: 0, elapsedMs: 0, hands: 0, body: false };
    phase.value = 'recording';
    handle = startCapture(video, models, {
      maxMs: MAX_MS,
      onStatus: (s) => {
        status.value = s;
        if (s.elapsedMs >= MAX_MS) void finish();
      },
    });
  }

  async function finish() {
    if (!handle || phase.value !== 'recording') return;
    const clip = handle.stop();
    handle = null;

    if (clip.frames.length < MIN_FRAMES) {
      return fail('Ghi được quá ít hình. Bạn thử lại và ký hiệu lâu hơn một chút nhé.');
    }
    phase.value = 'sending';
    try {
      result.value = await aiVerifyApi(props.signId, clip);
      phase.value = 'result';
      emit('result', result.value);
    } catch (e) {
      // Lỗi 422 mang sẵn lời khuyên tiếng Việt ("chưa thấy bạn giơ tay…") — hiện nguyên văn
      fail((e as Error).message);
    }
  }

  async function vote(verdict: 'AGREE' | 'DISAGREE') {
    if (!result.value) return;
    voted.value = true;
    try {
      await aiFeedbackApi(result.value.resultId, verdict);
    } catch {
      // Góp ý không lưu được cũng không làm phiền người học; kết quả chấm vẫn còn nguyên
    }
  }

  onBeforeUnmount(() => {
    cancelled = true;
    clearInterval(countdownTimer);
    handle?.cancel();
  });
</script>

<style scoped>
  .ai {
    margin-top: 16px;
    padding: 14px 16px;
    border: 2px dashed var(--si-border);
    border-radius: var(--si-kid-radius);
    background: var(--si-surface);
  }
  .ai-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 8px;
    font-size: 17px;
  }
  .ai-note {
    margin: 0 0 12px;
    color: var(--si-text-muted);
    font-size: 15px;
  }
  .ai-status {
    margin: 0 0 10px;
    font-weight: 700;
  }
  .ai-status--rec {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .rec-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: var(--si-danger);
  }
  @media (prefers-reduced-motion: no-preference) {
    .rec-dot {
      animation: blink 1s ease-in-out infinite;
    }
  }
  @keyframes blink {
    50% {
      opacity: 0.25;
    }
  }

  .ai-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: var(--si-kid-tap);
    padding: 0 22px;
    border: 2px solid transparent;
    border-radius: 999px;
    background: var(--si-kid-sun);
    color: var(--si-text);
    font-family: inherit;
    font-size: 16px;
    font-weight: 800;
    cursor: pointer;
    box-shadow: var(--si-kid-shadow);
  }
  .ai-btn--stop {
    background: var(--si-kid-mint);
    border-color: var(--si-success);
  }

  .ai-count {
    display: flex;
    align-items: center;
    gap: 14px;
    font-weight: 700;
  }
  .ai-count-num {
    display: grid;
    place-items: center;
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: var(--si-kid-sun);
    font-size: 36px;
    font-weight: 800;
  }

  .ai-progress {
    height: 10px;
    margin: 0 0 12px;
    border-radius: 999px;
    background: var(--si-surface-2);
    overflow: hidden;
  }
  .ai-progress span {
    display: block;
    height: 100%;
    background: var(--si-primary);
    transition: width 0.15s linear;
  }

  .verdict {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius);
    margin-bottom: 12px;
  }
  .verdict--pass {
    background: var(--si-kid-mint);
    border-color: var(--si-success);
  }
  .verdict--near {
    background: var(--si-kid-sun);
    border-color: var(--si-warning);
  }
  .verdict--retry {
    background: var(--si-surface-2);
    border-color: var(--si-primary);
  }
  .verdict-title {
    margin: 0;
    font-size: 20px;
    font-weight: 800;
  }
  .verdict-score {
    margin: 0;
    font-size: 30px;
    font-weight: 800;
    line-height: 1.1;
  }
  .verdict-score small {
    font-size: 14px;
    font-weight: 600;
    color: var(--si-text-muted);
  }

  .parts {
    list-style: none;
    margin: 0 0 12px;
    padding: 0;
    display: grid;
    gap: 8px;
  }
  .parts li {
    display: grid;
    grid-template-columns: 110px 1fr 36px;
    align-items: center;
    gap: 10px;
    font-size: 15px;
    font-weight: 700;
  }
  .part-bar {
    height: 12px;
    border-radius: 999px;
    background: var(--si-surface-2);
    border: 1px solid var(--si-border);
    overflow: hidden;
  }
  .part-bar span {
    display: block;
    height: 100%;
    background: var(--si-primary);
  }
  .part-num {
    text-align: right;
  }

  .hints {
    margin: 0 0 12px;
    padding-left: 20px;
    font-size: 15px;
  }
  .hints li {
    margin-bottom: 4px;
  }

  .vote {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 14px;
    font-size: 15px;
  }
  .vote-q {
    color: var(--si-text-muted);
    font-weight: 600;
  }
  .chip {
    min-height: 40px;
    padding: 0 14px;
    border: 2px solid var(--si-border);
    border-radius: 999px;
    background: var(--si-surface);
    color: var(--si-text);
    font-family: inherit;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
  }
  .chip:hover {
    background: var(--si-kid-cream);
  }
  .vote-thanks {
    font-weight: 700;
  }

  .ai-error p {
    margin: 0 0 12px;
    color: var(--si-danger);
    font-weight: 700;
  }
</style>
