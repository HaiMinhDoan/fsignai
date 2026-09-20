<template>
  <div class="memory">
    <p class="memory-hint">Lật hai thẻ để tìm cặp: video ký hiệu và chữ tương ứng.</p>
    <ul class="deck">
      <li v-for="(c, i) in cards" :key="c.key">
        <button
          type="button"
          class="card"
          :class="{ 'is-open': isOpen(i), 'is-done': matched.has(c.signId) }"
          :disabled="isOpen(i) || busy"
          :aria-label="isOpen(i) ? c.label : 'Thẻ úp'"
          @click="flip(i)"
        >
          <template v-if="isOpen(i)">
            <video v-if="c.kind === 'video'" :src="c.videoUrl" :poster="c.thumbnailUrl" muted loop playsinline autoplay></video>
            <span v-else class="card-word">{{ c.label }}</span>
          </template>
          <span v-else class="card-back">?</span>
        </button>
      </li>
    </ul>
    <p class="memory-count">Đã tìm {{ matched.size }} / {{ pairs.length }} cặp</p>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import type { GameSign } from '@/api/games';

  const props = defineProps<{ pairs: GameSign[] }>();
  const emit = defineEmits<{ (e: 'done', result: { correct: number; wrong: number }): void }>();

  interface Card {
    key: string;
    signId: string;
    kind: 'video' | 'word';
    label: string;
    videoUrl?: string;
    thumbnailUrl?: string;
  }

  const cards = computed<Card[]>(() =>
    props.pairs
      .flatMap((p) => [
        { key: `${p.signId}-v`, signId: p.signId, kind: 'video' as const, label: 'Video ký hiệu', videoUrl: p.videoUrl, thumbnailUrl: p.thumbnailUrl },
        { key: `${p.signId}-w`, signId: p.signId, kind: 'word' as const, label: p.wordVi },
      ])
      .sort(() => Math.random() - 0.5),
  );

  const open = ref<number[]>([]);
  const matched = ref<Set<string>>(new Set());
  const wrong = ref(0);
  const busy = ref(false);

  const isOpen = (i: number) => open.value.includes(i) || matched.value.has(cards.value[i]!.signId);

  function flip(i: number) {
    open.value = [...open.value, i];
    if (open.value.length < 2) return;

    const [a, b] = open.value.map((idx) => cards.value[idx]!) as [Card, Card];
    // Cùng từ nhưng phải khác loại (một video + một chữ) mới là một cặp
    if (a.signId === b.signId && a.kind !== b.kind) {
      matched.value = new Set(matched.value).add(a.signId);
      open.value = [];
      if (matched.value.size === props.pairs.length) {
        emit('done', { correct: matched.value.size, wrong: wrong.value });
      }
    } else {
      wrong.value += 1;
      busy.value = true;
      setTimeout(() => {
        open.value = [];
        busy.value = false;
      }, 900);
    }
  }
</script>

<style scoped>
  .memory-hint,
  .memory-count {
    margin: 0 0 10px;
    text-align: center;
    color: var(--sk-brown);
    font-weight: 700;
  }
  .deck {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
  }
  .card {
    width: 100%;
    aspect-ratio: 1 / 1;
    border-radius: 18px;
    border: 3px solid var(--sk-blue-150);
    background: var(--sk-sky);
    overflow: hidden;
    cursor: pointer;
    padding: 0;
    display: grid;
    place-items: center;
  }
  .card-back {
    font-family: var(--sk-font-head);
    font-size: 32px;
    font-weight: 800;
    color: var(--sk-blue-ink);
  }
  .card.is-open {
    background: var(--sk-surface);
  }
  .card.is-done {
    border-color: var(--sk-green);
    background: var(--sk-mint);
  }
  .card video {
    width: 100%;
    height: 100%;
    object-fit: cover;
    background: #000;
  }
  .card-word {
    padding: 6px;
    font-family: var(--sk-font-head);
    font-size: 16px;
    font-weight: 700;
    color: var(--sk-ink);
    text-align: center;
  }
</style>
