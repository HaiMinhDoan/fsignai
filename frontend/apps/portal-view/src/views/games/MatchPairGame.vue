<template>
  <div class="match">
    <p class="match-hint">Chọn một video ở bên trái, rồi chọn chữ đúng ở bên phải.</p>
    <div class="cols">
      <ul class="col">
        <li v-for="p in pairs" :key="p.signId">
          <button
            type="button"
            class="tile tile--video"
            :class="{
              'is-picked': pickedVideo === p.signId,
              'is-done': matched.has(p.signId),
            }"
            :disabled="matched.has(p.signId)"
            :aria-label="`Video ký hiệu ${matched.has(p.signId) ? p.wordVi : ''}`"
            @click="pickedVideo = p.signId"
          >
            <video :src="p.videoUrl" :poster="p.thumbnailUrl" muted loop playsinline autoplay></video>
          </button>
        </li>
      </ul>
      <ul class="col">
        <li v-for="p in shuffledWords" :key="p.signId">
          <button
            type="button"
            class="tile tile--word"
            :class="{ 'is-done': matched.has(p.signId), 'is-wrong': wrongFlash === p.signId }"
            :disabled="matched.has(p.signId)"
            @click="pickWord(p.signId)"
          >
            {{ p.wordVi }}
          </button>
        </li>
      </ul>
    </div>
    <p class="match-count">Đã nối {{ matched.size }} / {{ pairs.length }}</p>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import type { GameSign } from '@/api/games';

  const props = defineProps<{ pairs: GameSign[] }>();
  const emit = defineEmits<{ (e: 'done', result: { correct: number; wrong: number }): void }>();

  const pickedVideo = ref<string | null>(null);
  const matched = ref<Set<string>>(new Set());
  const wrongFlash = ref<string | null>(null);
  const wrong = ref(0);

  const shuffledWords = computed(() => [...props.pairs].sort(() => Math.random() - 0.5));

  function pickWord(signId: string) {
    if (!pickedVideo.value) return;
    if (pickedVideo.value === signId) {
      matched.value = new Set(matched.value).add(signId);
      pickedVideo.value = null;
      if (matched.value.size === props.pairs.length) {
        emit('done', { correct: matched.value.size, wrong: wrong.value });
      }
    } else {
      wrong.value += 1;
      wrongFlash.value = signId;
      setTimeout(() => (wrongFlash.value = null), 500);
    }
  }
</script>

<style scoped>
  .match-hint,
  .match-count {
    margin: 0 0 10px;
    text-align: center;
    color: var(--sk-brown);
    font-weight: 700;
  }
  .cols {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }
  .col {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .tile {
    width: 100%;
    min-height: 72px;
    border-radius: 20px;
    border: 3px solid var(--sk-blue-150);
    background: var(--sk-surface);
    font-family: var(--sk-font-head);
    font-size: 18px;
    font-weight: 700;
    color: var(--sk-ink);
    cursor: pointer;
    padding: 0;
    overflow: hidden;
  }
  .tile--video video {
    display: block;
    width: 100%;
    height: 96px;
    object-fit: cover;
    background: #000;
  }
  .tile.is-picked {
    border-color: var(--sk-amber);
    box-shadow: 0 0 0 3px var(--sk-peach);
  }
  .tile.is-done {
    border-color: var(--sk-green);
    background: var(--sk-mint);
    opacity: 0.75;
    cursor: default;
  }
  .tile.is-wrong {
    border-color: var(--si-danger);
    animation: shake 0.4s;
  }
  @keyframes shake {
    25% {
      transform: translateX(-6px);
    }
    75% {
      transform: translateX(6px);
    }
  }
</style>
