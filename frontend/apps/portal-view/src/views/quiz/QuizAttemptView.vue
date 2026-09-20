<template>
  <section class="attempt-page">
    <RouterLink to="/kiem-tra" class="back-link"><SiIcon name="back" :size="18" /> Về danh sách đề</RouterLink>

    <p v-if="loading" class="hint">Đang tải đề…</p>
    <p v-else-if="error" class="error-text" role="alert">{{ error }}</p>

    <!-- ===== Đang làm bài ===== -->
    <template v-else-if="attempt && attempt.status === 'IN_PROGRESS'">
      <header class="attempt-head">
        <h1>{{ attempt.quizTitleVi ?? attempt.blueprintTitleVi }}</h1>
        <div class="progress-track">
          <span class="progress-fill" :style="{ width: `${((currentIndex + 1) / attempt.questions.length) * 100}%` }" />
        </div>
        <p class="progress-label">Câu {{ currentIndex + 1 }} / {{ attempt.questions.length }}</p>
      </header>

      <article class="question-card">
        <template v-if="currentQuestion.questionType === 'AI_PERFORM'">
          <p class="ai-placeholder">
            <SiIcon name="camera" :size="24" />
            Câu này chấm bằng camera AI — tính năng đang được phát triển, tạm thời bỏ qua câu này khi nộp bài.
          </p>
        </template>

        <template v-else>
          <div v-if="currentQuestion.signVideoUrl" class="prompt-video">
            <video
              :key="currentQuestion.id"
              :src="currentQuestion.signVideoUrl"
              :poster="currentQuestion.signThumbnailUrl"
              controls
              playsinline
            ></video>
          </div>
          <p v-if="currentQuestion.promptVi" class="prompt-text">{{ currentQuestion.promptVi }}</p>

          <div class="option-grid" :class="{ 'option-grid--video': hasVideoOptions }">
            <button
              v-for="(opt, i) in currentQuestion.options"
              :key="i"
              type="button"
              class="option-btn"
              :class="{ 'is-selected': answers[currentIndex] === i }"
              @click="answers[currentIndex] = i"
            >
              <video v-if="opt.videoUrl" :src="opt.videoUrl" muted playsinline @mouseenter="replay($event)"></video>
              <span>{{ opt.label }}</span>
            </button>
          </div>
        </template>
      </article>

      <div class="nav-actions">
        <button type="button" class="btn-secondary" :disabled="currentIndex === 0" @click="currentIndex--">
          Câu trước
        </button>
        <button
          v-if="currentIndex < attempt.questions.length - 1"
          type="button"
          class="btn-primary"
          @click="currentIndex++"
        >
          Câu tiếp theo
        </button>
        <button v-else type="button" class="btn-primary" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? 'Đang nộp…' : 'Nộp bài' }}
        </button>
      </div>

      <div class="jump-row">
        <button
          v-for="(q, i) in attempt.questions"
          :key="q.id"
          type="button"
          class="jump-dot"
          :class="{ 'is-current': i === currentIndex, 'is-answered': answers[i] !== null }"
          @click="currentIndex = i"
        >
          {{ i + 1 }}
        </button>
      </div>
    </template>

    <!-- ===== Đã nộp - xem lại ===== -->
    <template v-else-if="attempt && attempt.status === 'SUBMITTED'">
      <div class="result-banner" :class="attempt.passed ? 'is-pass' : 'is-fail'">
        <SiIcon :name="attempt.passed ? 'trophy' : 'close'" :size="32" />
        <div>
          <h1>{{ attempt.passed ? 'Chúc mừng, bạn đã đạt!' : 'Chưa đạt, luyện thêm nhé!' }}</h1>
          <p>
            Điểm: {{ attempt.score }} / {{ attempt.maxScore }}
            ({{ percentScore }}% — cần {{ attempt.passScoreRequired }}% để đạt)
          </p>
        </div>
      </div>

      <div v-if="attempt.newAchievements.length" class="achievement-banner">
        <p class="achievement-title"><SiIcon name="sparkles" :size="18" /> Huy hiệu mới!</p>
        <div class="achievement-list">
          <span v-for="a in attempt.newAchievements" :key="a.id" class="achievement-chip">
            {{ a.nameVi }}
          </span>
        </div>
      </div>

      <h2 class="review-title">Xem lại từng câu</h2>
      <ul class="review-list">
        <li v-for="(q, i) in attempt.questions" :key="q.id" class="review-card">
          <div class="review-top">
            <span class="review-index">Câu {{ i + 1 }}</span>
            <span
              v-if="q.questionType !== 'AI_PERFORM'"
              class="review-status"
              :class="isCorrect(i) ? 'is-correct' : 'is-wrong'"
            >
              <SiIcon :name="isCorrect(i) ? 'check' : 'close'" :size="14" />
              {{ isCorrect(i) ? 'Đúng' : 'Sai' }}
            </span>
            <span v-else class="review-status">Chưa chấm</span>
          </div>

          <div v-if="q.signVideoUrl" class="prompt-video prompt-video--sm">
            <video :src="q.signVideoUrl" :poster="q.signThumbnailUrl" controls playsinline></video>
          </div>
          <p v-if="q.promptVi" class="prompt-text">{{ q.promptVi }}</p>

          <ul class="review-options">
            <li
              v-for="(opt, oi) in q.options"
              :key="oi"
              class="review-option"
              :class="{
                'is-correct-answer': oi === q.correctOptionIndex,
                'is-your-wrong-pick': oi === attempt.selectedOptionIndexes?.[i] && oi !== q.correctOptionIndex,
              }"
            >
              <span>{{ opt.label }}</span>
              <span v-if="oi === attempt.selectedOptionIndexes?.[i]" class="pick-tag">Bạn chọn</span>
              <span v-if="oi === q.correctOptionIndex" class="pick-tag pick-tag--correct">Đáp án đúng</span>
            </li>
          </ul>
        </li>
      </ul>

      <RouterLink to="/kiem-tra" class="btn-primary btn-block">Làm đề khác</RouterLink>
    </template>
  </section>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRoute } from 'vue-router';
  import SiIcon from '@/components/SiIcon.vue';
  import { getAttemptApi, submitAttemptApi, type QuizAttempt } from '@/api/quiz';

  defineOptions({ name: 'QuizAttemptView' });

  const route = useRoute();
  const attemptId = route.params.attemptId as string;

  const attempt = ref<QuizAttempt | null>(null);
  const loading = ref(true);
  const error = ref('');
  const submitting = ref(false);
  const currentIndex = ref(0);
  const answers = ref<(number | null)[]>([]);

  const currentQuestion = computed(() => attempt.value!.questions[currentIndex.value]!);
  const hasVideoOptions = computed(() => currentQuestion.value.options.some((o) => o.videoUrl));
  const percentScore = computed(() => {
    if (!attempt.value?.maxScore) return 0;
    return Math.round(((attempt.value.score ?? 0) * 100) / attempt.value.maxScore);
  });

  function isCorrect(i: number) {
    const q = attempt.value!.questions[i]!;
    const picked = attempt.value!.selectedOptionIndexes?.[i];
    return q.correctOptionIndex !== undefined && picked === q.correctOptionIndex;
  }

  function replay(e: Event) {
    const el = e.target as HTMLVideoElement;
    el.currentTime = 0;
    void el.play();
  }

  async function handleSubmit() {
    if (!attempt.value) return;
    submitting.value = true;
    error.value = '';
    try {
      attempt.value = await submitAttemptApi(
        attemptId,
        answers.value.map((selectedOptionIndex, questionIndex) => ({ questionIndex, selectedOptionIndex })),
      );
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      submitting.value = false;
    }
  }

  onMounted(async () => {
    try {
      attempt.value = await getAttemptApi(attemptId);
      answers.value = attempt.value.questions.map(() => null);
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  });
</script>

<style scoped>
  .attempt-page {
    display: flex;
    flex-direction: column;
    gap: 20px;
    max-width: 720px;
    margin: 0 auto;
  }
  .back-link {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: var(--si-text-muted);
    text-decoration: none;
    font-weight: 600;
    width: fit-content;
  }
  .hint,
  .error-text {
    color: var(--si-text-muted);
  }
  .error-text {
    color: var(--si-danger, #c4503f);
    font-weight: 600;
  }
  .attempt-head h1 {
    margin: 0 0 10px;
    font-size: 22px;
  }
  .progress-track {
    height: 10px;
    border-radius: 999px;
    background: var(--si-border);
    overflow: hidden;
  }
  .progress-fill {
    display: block;
    height: 100%;
    background: var(--si-primary);
    transition: width 0.2s;
  }
  .progress-label {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--si-text-muted);
  }
  .question-card {
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius, 16px);
    padding: 20px;
  }
  .ai-placeholder {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--si-text-muted);
    font-weight: 600;
  }
  .prompt-video video {
    width: 100%;
    max-height: 320px;
    border-radius: 12px;
    background: #000;
  }
  .prompt-video--sm video {
    max-height: 200px;
  }
  .prompt-text {
    font-size: 18px;
    font-weight: 700;
    margin: 12px 0;
  }
  .option-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
    margin-top: 12px;
  }
  .option-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 12px;
    border-radius: 12px;
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    font: inherit;
    font-weight: 700;
    cursor: pointer;
    text-align: center;
  }
  .option-btn video {
    width: 100%;
    border-radius: 8px;
  }
  .option-btn.is-selected {
    border-color: var(--si-primary);
    background: var(--si-primary-light);
    color: var(--si-primary);
  }
  .nav-actions {
    display: flex;
    gap: 10px;
  }
  .btn-primary,
  .btn-secondary {
    flex: 1;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 46px;
    border-radius: 999px;
    font-weight: 700;
    cursor: pointer;
    text-decoration: none;
  }
  .btn-primary {
    border: none;
    background: var(--si-primary);
    color: #fff;
  }
  .btn-primary:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
  .btn-secondary {
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    color: var(--si-text-muted);
  }
  .btn-secondary:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .btn-block {
    width: 100%;
  }
  .jump-row {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    justify-content: center;
  }
  .jump-dot {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
  }
  .jump-dot.is-answered {
    background: var(--si-primary-light);
    border-color: var(--si-secondary);
  }
  .jump-dot.is-current {
    border-color: var(--si-primary);
    color: var(--si-primary);
  }
  .result-banner {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px;
    border-radius: var(--si-kid-radius, 16px);
  }
  .result-banner h1 {
    margin: 0 0 4px;
    font-size: 20px;
  }
  .result-banner p {
    margin: 0;
  }
  .result-banner.is-pass {
    background: var(--si-success-light, #dff5e8);
    color: var(--si-success, #2e9e6b);
  }
  .result-banner.is-fail {
    background: #fbe9e7;
    color: var(--si-danger, #c4503f);
  }
  .achievement-banner {
    background: var(--si-kid-sun, #fff4e0);
    border-radius: 12px;
    padding: 12px 16px;
  }
  .achievement-title {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 0 0 8px;
    font-weight: 700;
  }
  .achievement-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  .achievement-chip {
    padding: 4px 12px;
    border-radius: 999px;
    background: #fff;
    font-size: 13px;
    font-weight: 700;
  }
  .review-title {
    font-size: 18px;
    margin: 0;
  }
  .review-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 14px;
  }
  .review-card {
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: 14px;
    padding: 16px;
  }
  .review-top {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;
  }
  .review-index {
    font-weight: 700;
    color: var(--si-text-muted);
  }
  .review-status {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 10px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 700;
  }
  .review-status.is-correct {
    background: var(--si-success-light, #dff5e8);
    color: var(--si-success, #2e9e6b);
  }
  .review-status.is-wrong {
    background: #fbe9e7;
    color: var(--si-danger, #c4503f);
  }
  .review-options {
    list-style: none;
    margin: 10px 0 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .review-option {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    border-radius: 8px;
    border: 2px solid var(--si-border);
    font-size: 14px;
  }
  .review-option.is-correct-answer {
    border-color: var(--si-success, #2e9e6b);
    background: var(--si-success-light, #dff5e8);
  }
  .review-option.is-your-wrong-pick {
    border-color: var(--si-danger, #c4503f);
    background: #fbe9e7;
  }
  .pick-tag {
    margin-left: auto;
    font-size: 11px;
    font-weight: 700;
    color: var(--si-text-muted);
  }
  .pick-tag--correct {
    color: var(--si-success, #2e9e6b);
  }
</style>
