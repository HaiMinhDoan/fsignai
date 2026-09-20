<template>
  <section class="flashcard-page">
    <!-- ===== Màn chọn chủ đề, chưa bắt đầu ===== -->
    <template v-if="phase === 'setup'">
      <header class="fc-head">
        <h1>Ôn Từ Vựng</h1>
        <p class="fc-sub">Xem video, đoán nghĩa, rồi tự đánh giá mình đã thuộc chưa</p>
      </header>

      <div v-if="topics.length" class="topic-pills">
        <button
          type="button"
          class="pill"
          :class="{ 'is-active': selectedTopic === undefined }"
          @click="selectedTopic = undefined"
        >
          Tất cả
        </button>
        <button
          v-for="t in topics"
          :key="t.id"
          type="button"
          class="pill"
          :class="{ 'is-active': selectedTopic === t.id }"
          @click="selectedTopic = t.id"
        >
          {{ t.nameVi }}
        </button>
      </div>

      <button class="btn-primary btn-block" type="button" :disabled="loading" @click="startSession">
        {{ loading ? 'Đang tải…' : 'Bắt đầu ôn tập' }}
      </button>
      <p v-if="error" class="error-text">{{ error }}</p>
    </template>

    <!-- ===== Đang ôn ===== -->
    <template v-else-if="phase === 'session'">
      <div class="progress-track">
        <span class="progress-fill" :style="{ width: `${((currentIndex) / cards.length) * 100}%` }" />
      </div>
      <p class="progress-label">Thẻ {{ currentIndex + 1 }} / {{ cards.length }}</p>

      <article class="flip-card" :class="{ 'is-flipped': flipped }">
        <span v-if="currentCard.isNew" class="new-badge">Từ mới</span>

        <div class="fc-video">
          <video
            :key="currentCard.signId"
            :src="currentCard.videoUrl"
            :poster="currentCard.thumbnailUrl"
            controls
            playsinline
            autoplay
          ></video>
        </div>

        <template v-if="!flipped">
          <button class="btn-primary btn-block" type="button" @click="flipped = true">
            Lật thẻ — Xem nghĩa
          </button>
        </template>

        <template v-else>
          <h2 class="fc-word">{{ currentCard.wordVi }}</h2>
          <p v-if="currentCard.descriptionVi" class="fc-desc">{{ currentCard.descriptionVi }}</p>

          <div class="fc-actions">
            <button type="button" class="btn-needs" :disabled="grading" @click="grade('NEEDS_PRACTICE')">
              <SiIcon name="repeat" :size="20" /> Cần luyện thêm
            </button>
            <button type="button" class="btn-known" :disabled="grading" @click="grade('KNOWN')">
              <SiIcon name="check" :size="20" /> Đã thuộc
            </button>
          </div>
        </template>
      </article>
    </template>

    <!-- ===== Xong phiên ôn ===== -->
    <template v-else-if="phase === 'done'">
      <div class="done-banner">
        <SiIcon name="trophy" :size="32" />
        <div>
          <h1>Xong rồi!</h1>
          <p>Bạn vừa ôn {{ cards.length }} thẻ — {{ knownCount }} đã thuộc, {{ needsPracticeCount }} cần luyện thêm.</p>
        </div>
      </div>
      <div class="done-actions">
        <button class="btn-primary" type="button" @click="phase = 'setup'">Ôn tiếp</button>
        <RouterLink to="/" class="btn-secondary">Về trang chủ</RouterLink>
      </div>
    </template>
  </section>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted } from 'vue';
  import SiIcon from '@/components/SiIcon.vue';
  import { dueFlashcardsApi, reviewFlashcardApi, type Flashcard, type FlashcardResult } from '@/api/flashcard';
  import { topicOptionsApi, type TopicRef } from '@/api/dictionary';

  defineOptions({ name: 'FlashcardView' });

  const phase = ref<'setup' | 'session' | 'done'>('setup');
  const topics = ref<TopicRef[]>([]);
  const selectedTopic = ref<string | undefined>(undefined);
  const loading = ref(false);
  const error = ref('');

  const cards = ref<Flashcard[]>([]);
  const currentIndex = ref(0);
  const flipped = ref(false);
  const grading = ref(false);
  const results = ref<FlashcardResult[]>([]);

  const currentCard = computed(() => cards.value[currentIndex.value]!);
  const knownCount = computed(() => results.value.filter((r) => r === 'KNOWN').length);
  const needsPracticeCount = computed(() => results.value.filter((r) => r === 'NEEDS_PRACTICE').length);

  async function startSession() {
    error.value = '';
    loading.value = true;
    try {
      cards.value = await dueFlashcardsApi(selectedTopic.value);
      if (cards.value.length === 0) {
        error.value = 'Chưa có từ nào để ôn ở chủ đề này.';
        return;
      }
      currentIndex.value = 0;
      flipped.value = false;
      results.value = [];
      phase.value = 'session';
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  }

  async function grade(result: FlashcardResult) {
    grading.value = true;
    try {
      await reviewFlashcardApi(currentCard.value.signId, result);
      results.value.push(result);
      if (currentIndex.value + 1 < cards.value.length) {
        currentIndex.value += 1;
        flipped.value = false;
      } else {
        phase.value = 'done';
      }
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      grading.value = false;
    }
  }

  onMounted(async () => {
    try {
      topics.value = await topicOptionsApi();
    } catch {
      // Bo qua - danh sach chu de chi la bo loc tien loi
    }
  });
</script>

<style scoped>
  .flashcard-page {
    display: flex;
    flex-direction: column;
    gap: 18px;
    max-width: 560px;
    margin: 0 auto;
  }
  .fc-head h1 {
    margin: 0 0 4px;
    font-size: 28px;
  }
  .fc-sub {
    margin: 0;
    color: var(--si-text-muted);
  }
  .topic-pills {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  .pill {
    padding: 8px 16px;
    border-radius: 999px;
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    color: var(--si-text-muted);
    font-weight: 700;
    font-size: 13px;
    cursor: pointer;
  }
  .pill.is-active {
    border-color: var(--si-primary);
    background: var(--si-primary-light);
    color: var(--si-primary);
  }
  .error-text {
    color: var(--si-danger, #c4503f);
    font-weight: 600;
  }
  .btn-primary,
  .btn-secondary {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 48px;
    border-radius: 999px;
    font-weight: 700;
    cursor: pointer;
    text-decoration: none;
    border: none;
  }
  .btn-primary {
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
  .btn-block {
    width: 100%;
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
    margin: 0;
    font-size: 13px;
    color: var(--si-text-muted);
  }
  .flip-card {
    position: relative;
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius, 16px);
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 14px;
  }
  .new-badge {
    position: absolute;
    top: 14px;
    right: 14px;
    padding: 2px 10px;
    border-radius: 999px;
    background: var(--si-secondary);
    color: #fff;
    font-size: 11px;
    font-weight: 700;
  }
  .fc-video video {
    width: 100%;
    max-height: 320px;
    border-radius: 12px;
    background: #000;
  }
  .fc-word {
    margin: 0;
    font-size: 26px;
    text-align: center;
  }
  .fc-desc {
    margin: 0;
    color: var(--si-text-muted);
    text-align: center;
  }
  .fc-actions {
    display: flex;
    gap: 10px;
  }
  .btn-needs,
  .btn-known {
    flex: 1;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    height: 48px;
    border-radius: 999px;
    font-weight: 700;
    cursor: pointer;
    border: 2px solid transparent;
  }
  .btn-needs {
    background: #fbe9e7;
    color: var(--si-danger, #c4503f);
  }
  .btn-known {
    background: var(--si-success-light, #dff5e8);
    color: var(--si-success, #2e9e6b);
  }
  .btn-needs:disabled,
  .btn-known:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
  .done-banner {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px;
    border-radius: var(--si-kid-radius, 16px);
    background: var(--si-primary-light);
    color: var(--si-primary);
  }
  .done-banner h1 {
    margin: 0 0 4px;
    font-size: 20px;
  }
  .done-banner p {
    margin: 0;
  }
  .done-actions {
    display: flex;
    gap: 10px;
  }
  .done-actions > * {
    flex: 1;
  }
</style>
