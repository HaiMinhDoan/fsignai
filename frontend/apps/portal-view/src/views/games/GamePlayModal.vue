<template>
  <div class="overlay" role="dialog" aria-modal="true" :aria-label="title" @keydown.esc="close">
    <div class="sheet">
      <header class="sheet-head">
        <h2>{{ title }}</h2>
        <button type="button" class="close-btn" aria-label="Đóng" @click="close">
          <SiIcon name="close" :size="20" />
        </button>
      </header>

      <p v-if="phase === 'loading'" class="msg">Mochi đang chuẩn bị ván chơi…</p>

      <div v-else-if="phase === 'error'" class="msg">
        <p class="msg-error" role="alert">{{ error }}</p>
        <button type="button" class="btn" @click="load">Thử lại</button>
      </div>

      <template v-else-if="phase === 'playing' && game">
        <MatchPairGame v-if="code === 'MATCH_PAIR'" :pairs="game.pairs!" @done="onDone" />
        <MemoryFlipGame v-else-if="code === 'MEMORY_FLIP'" :pairs="game.pairs!" @done="onDone" />
        <QuizRoundGame
          v-else
          :questions="game.questions!"
          :seconds="code === 'SPEED_GUESS' ? 10 : 7"
          :falling="code === 'FINGER_DANCE'"
          @done="onDone"
        />
      </template>

      <p v-else-if="phase === 'saving'" class="msg">Đang tính sao cho bé…</p>

      <div v-else-if="phase === 'result' && result" class="result">
        <p class="result-emoji">{{ result.score >= result.maxScore * 0.8 ? '🏆' : result.score > 0 ? '🌟' : '💪' }}</p>
        <h3>
          {{ result.score >= result.maxScore * 0.8 ? 'Tuyệt vời!' : result.score > 0 ? 'Giỏi lắm!' : 'Lần sau sẽ tốt hơn!' }}
        </h3>
        <p class="result-stars">+{{ result.starsEarned }} ⭐ <small>(tối đa {{ result.maxScore }})</small></p>
        <p class="result-detail">
          Đúng {{ result.correctCount }} — Sai {{ result.wrongCount }} — {{ result.durationSeconds }} giây
        </p>
        <p class="result-total">Tổng số sao của bé: <strong>{{ result.totalStars }} ⭐</strong></p>
        <div class="result-actions">
          <button type="button" class="btn btn--ghost" @click="close">Xong</button>
          <button type="button" class="btn" @click="load">Chơi lại</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';
  import SiIcon from '@/components/SiIcon.vue';
  import MatchPairGame from './MatchPairGame.vue';
  import MemoryFlipGame from './MemoryFlipGame.vue';
  import QuizRoundGame from './QuizRoundGame.vue';
  import { finishGameApi, startGameApi, type GameCode, type GameFinish, type GameStart } from '@/api/games';

  const props = defineProps<{ code: GameCode; title: string }>();
  const emit = defineEmits<{ (e: 'close', changed: boolean): void }>();

  const phase = ref<'loading' | 'playing' | 'saving' | 'result' | 'error'>('loading');
  const game = ref<GameStart | null>(null);
  const result = ref<GameFinish | null>(null);
  const error = ref('');
  const played = ref(false);

  const code = computed(() => props.code);

  async function load() {
    phase.value = 'loading';
    error.value = '';
    try {
      game.value = await startGameApi(props.code);
      phase.value = 'playing';
    } catch (e) {
      error.value = (e as Error).message;
      phase.value = 'error';
    }
  }

  async function onDone(r: { correct: number; wrong: number }) {
    if (!game.value) return;
    phase.value = 'saving';
    try {
      result.value = await finishGameApi(game.value.sessionId, r.correct, r.wrong);
      played.value = true;
      phase.value = 'result';
    } catch (e) {
      error.value = (e as Error).message;
      phase.value = 'error';
    }
  }

  function close() {
    emit('close', played.value);
  }

  onMounted(load);
</script>

<style scoped>
  .overlay {
    position: fixed;
    inset: 0;
    z-index: 60;
    display: grid;
    place-items: center;
    padding: 16px;
    background: rgba(17, 28, 45, 0.55);
  }
  .sheet {
    width: 100%;
    max-width: 640px;
    max-height: 92vh;
    overflow-y: auto;
    background: var(--sk-lavender);
    border-radius: var(--sk-r-card);
    padding: 20px 22px 24px;
    box-shadow: var(--sk-shadow-lift);
  }
  .sheet-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
  }
  .sheet-head h2 {
    margin: 0;
    font-family: var(--sk-font-head);
    font-size: 20px;
  }
  .close-btn {
    display: grid;
    place-items: center;
    width: 40px;
    height: 40px;
    border: none;
    border-radius: 50%;
    background: var(--sk-surface);
    color: var(--sk-ink);
    cursor: pointer;
  }
  .msg {
    text-align: center;
    color: var(--sk-brown);
    font-weight: 700;
    padding: 28px 0;
  }
  .msg-error {
    color: var(--si-danger);
  }
  .btn {
    min-height: 48px;
    padding: 0 26px;
    border: none;
    border-radius: var(--sk-r-pill);
    background: var(--sk-amber);
    color: var(--sk-brown-dark);
    font-family: var(--sk-font-head);
    font-size: 16px;
    font-weight: 800;
    cursor: pointer;
  }
  .btn--ghost {
    background: var(--sk-surface);
    color: var(--sk-blue-ink);
  }
  .result {
    text-align: center;
  }
  .result-emoji {
    margin: 0;
    font-size: 56px;
  }
  .result h3 {
    margin: 4px 0;
    font-size: 24px;
  }
  .result-stars {
    margin: 4px 0;
    font-family: var(--sk-font-head);
    font-size: 32px;
    font-weight: 800;
    color: var(--sk-amber-ink);
  }
  .result-stars small {
    font-size: 13px;
    color: var(--sk-brown);
  }
  .result-detail,
  .result-total {
    margin: 4px 0;
    color: var(--sk-brown);
  }
  .result-actions {
    display: flex;
    justify-content: center;
    gap: 12px;
    margin-top: 18px;
  }
</style>
