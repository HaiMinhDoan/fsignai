<template>
  <div class="round">
    <div class="round-top">
      <span class="round-no">Câu {{ index + 1 }}/{{ questions.length }}</span>
      <span class="round-time" :class="{ 'is-low': secondsLeft <= 3 }">⏱ {{ secondsLeft }}s</span>
    </div>
    <div class="timer-track"><span class="timer-fill" :style="{ width: `${(msLeft / totalMs) * 100}%` }" /></div>

    <div class="stage" :class="{ 'stage--falling': falling }">
      <video
        :key="current.signId + index"
        class="prompt"
        :src="current.videoUrl"
        :poster="current.thumbnailUrl"
        muted
        loop
        playsinline
        autoplay
      ></video>

      <ul class="options" :class="{ 'options--falling': falling }">
        <li v-for="o in current.options" :key="index + o.signId">
          <button
            type="button"
            class="option"
            :class="{
              'is-correct': answered && o.signId === current.signId,
              'is-wrong': answered && picked === o.signId && o.signId !== current.signId,
            }"
            :style="falling ? { animationDuration: `${totalMs / 1000}s` } : undefined"
            :disabled="answered"
            @click="answer(o.signId)"
          >
            {{ o.label }}
          </button>
        </li>
      </ul>
    </div>

    <p v-if="answered" class="feedback" :class="lastCorrect ? 'is-ok' : 'is-bad'" role="status">
      {{ feedbackText }}
    </p>
  </div>
</template>

<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
  import type { GameQuestion } from '@/api/games';

  const props = defineProps<{ questions: GameQuestion[]; seconds: number; falling?: boolean }>();
  const emit = defineEmits<{ (e: 'done', result: { correct: number; wrong: number }): void }>();

  const index = ref(0);
  const correct = ref(0);
  const wrong = ref(0);
  const answered = ref(false);
  const picked = ref<string | null>(null);
  const lastCorrect = ref(false);
  const timedOut = ref(false);

  const totalMs = props.seconds * 1000;
  const msLeft = ref(totalMs);
  const secondsLeft = computed(() => Math.ceil(msLeft.value / 1000));
  const current = computed(() => props.questions[index.value]!);
  const feedbackText = computed(() => {
    if (lastCorrect.value) return 'Đúng rồi! Giỏi lắm 🎉';
    const right = current.value.options.find((o) => o.signId === current.value.signId)?.label;
    return timedOut.value ? `Hết giờ! Đáp án là "${right}"` : `Chưa đúng, đáp án là "${right}"`;
  });

  let timer: ReturnType<typeof setInterval> | undefined;
  let nextTimer: ReturnType<typeof setTimeout> | undefined;

  function startTimer() {
    msLeft.value = totalMs;
    clearInterval(timer);
    timer = setInterval(() => {
      msLeft.value -= 100;
      if (msLeft.value <= 0) {
        clearInterval(timer);
        if (!answered.value) settle(null);
      }
    }, 100);
  }

  function answer(signId: string) {
    if (answered.value) return;
    settle(signId);
  }

  function settle(signId: string | null) {
    clearInterval(timer);
    answered.value = true;
    picked.value = signId;
    timedOut.value = signId === null;
    lastCorrect.value = signId === current.value.signId;
    if (lastCorrect.value) correct.value += 1;
    else wrong.value += 1;

    nextTimer = setTimeout(() => {
      if (index.value + 1 >= props.questions.length) {
        emit('done', { correct: correct.value, wrong: wrong.value });
        return;
      }
      index.value += 1;
      answered.value = false;
      picked.value = null;
      startTimer();
    }, 1400);
  }

  onMounted(startTimer);
  onBeforeUnmount(() => {
    clearInterval(timer);
    clearTimeout(nextTimer);
  });
</script>

<style scoped>
  .round-top {
    display: flex;
    justify-content: space-between;
    font-weight: 800;
    color: var(--sk-brown);
    margin-bottom: 6px;
  }
  .round-time.is-low {
    color: var(--si-danger);
  }
  .timer-track {
    height: 8px;
    border-radius: 999px;
    background: var(--sk-blue-150);
    overflow: hidden;
    margin-bottom: 12px;
  }
  .timer-fill {
    display: block;
    height: 100%;
    background: var(--sk-amber);
    transition: width 0.1s linear;
  }
  .stage {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .prompt {
    width: 100%;
    max-height: 240px;
    border-radius: 16px;
    background: #000;
  }
  .options {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
  .option {
    width: 100%;
    min-height: var(--sk-tap);
    padding: 10px 14px;
    border-radius: 18px;
    border: 3px solid var(--sk-blue-150);
    background: var(--sk-surface);
    font-family: var(--sk-font-head);
    font-size: 17px;
    font-weight: 700;
    color: var(--sk-ink);
    cursor: pointer;
  }
  .option.is-correct {
    border-color: var(--sk-green);
    background: var(--sk-mint);
  }
  .option.is-wrong {
    border-color: var(--si-danger);
    background: #fbe9e7;
  }

  /* Vũ Điệu Ngón Tay: ba thẻ chữ rơi từ trên xuống, người học chạm thẻ đúng trước khi chạm đáy */
  .stage--falling {
    position: relative;
    height: 420px;
  }
  .stage--falling .prompt {
    max-height: 150px;
  }
  .options--falling {
    position: absolute;
    left: 0;
    right: 0;
    top: 160px;
    bottom: 0;
    grid-template-columns: repeat(3, 1fr);
    align-items: start;
  }
  .options--falling .option {
    animation: fall linear forwards;
  }
  @keyframes fall {
    from {
      transform: translateY(0);
    }
    to {
      transform: translateY(190px);
    }
  }
  @media (prefers-reduced-motion: reduce) {
    .options--falling .option {
      animation: none;
    }
  }

  .feedback {
    margin: 12px 0 0;
    padding: 10px 14px;
    border-radius: 14px;
    font-weight: 800;
    text-align: center;
  }
  .feedback.is-ok {
    background: var(--sk-mint);
    color: var(--sk-green-dark);
  }
  .feedback.is-bad {
    background: #fbe9e7;
    color: var(--si-danger);
  }
</style>
