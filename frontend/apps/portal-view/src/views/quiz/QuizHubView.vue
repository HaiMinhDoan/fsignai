<template>
  <section class="quiz-hub">
    <header class="hub-head">
      <h1>Làm Bài Kiểm Tra</h1>
      <p class="hub-sub">Ôn lại từ đã học và xem mình nhớ được bao nhiêu</p>
    </header>

    <div v-if="topics.length" class="topic-pills">
      <button
        type="button"
        class="pill"
        :class="{ 'is-active': activeTopic === undefined }"
        @click="selectTopic(undefined)"
      >
        Tất cả
      </button>
      <button
        v-for="t in topics"
        :key="t.id"
        type="button"
        class="pill"
        :class="{ 'is-active': activeTopic === t.id }"
        @click="selectTopic(t.id)"
      >
        {{ t.nameVi }}
      </button>
    </div>

    <p v-if="loading" class="hint">Đang tải danh sách đề…</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>

    <template v-else>
      <section v-if="quizzes.length" class="quiz-section">
        <h2>Đề có sẵn</h2>
        <ul class="quiz-list">
          <li v-for="q in quizzes" :key="q.id" class="quiz-card">
            <div class="quiz-card-top">
              <span v-if="q.topicNameVi" class="tag">{{ q.topicNameVi }}</span>
              <span v-if="q.lessonTitleVi" class="tag tag--lesson">Bài: {{ q.lessonTitleVi }}</span>
            </div>
            <h3>{{ q.titleVi }}</h3>
            <p v-if="q.descriptionVi" class="quiz-desc">{{ q.descriptionVi }}</p>
            <div class="quiz-meta">
              <span><SiIcon name="check" :size="14" /> {{ q.questionCount }} câu</span>
              <span><SiIcon name="star" :size="14" /> Cần {{ q.passScore }}% để đạt</span>
            </div>
            <button class="btn-primary" type="button" :disabled="starting" @click="startQuiz(q.id)">
              Bắt đầu làm bài
            </button>
          </li>
        </ul>
      </section>

      <section v-if="blueprints.length" class="quiz-section">
        <h2>Đề trộn ngẫu nhiên</h2>
        <p class="section-hint">Mỗi lần bấm bắt đầu sẽ ra một đề khác nhau</p>
        <ul class="quiz-list">
          <li v-for="b in blueprints" :key="b.id" class="quiz-card">
            <div class="quiz-card-top">
              <span v-for="name in b.topicNames" :key="name" class="tag">{{ name }}</span>
            </div>
            <h3>{{ b.titleVi }}</h3>
            <p v-if="b.descriptionVi" class="quiz-desc">{{ b.descriptionVi }}</p>
            <div class="quiz-meta">
              <span><SiIcon name="check" :size="14" /> {{ b.questionCount }} câu</span>
              <span><SiIcon name="star" :size="14" /> Cần {{ b.passScore }}% để đạt</span>
            </div>
            <button class="btn-primary" type="button" :disabled="starting" @click="startBlueprint(b.id)">
              Bắt đầu làm bài
            </button>
          </li>
        </ul>
      </section>

      <p v-if="!quizzes.length && !blueprints.length" class="hint">
        Chưa có đề kiểm tra nào ở chủ đề này.
      </p>
    </template>

    <p v-if="startError" class="error-text">{{ startError }}</p>
  </section>
</template>

<script lang="ts" setup>
  import { ref, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import SiIcon from '@/components/SiIcon.vue';
  import { quizzesApi, quizBlueprintsApi, startAttemptFromQuizApi, startAttemptFromBlueprintApi, type QuizSummary, type QuizBlueprintSummary } from '@/api/quiz';
  import { topicOptionsApi } from '@/api/dictionary';
  import type { TopicRef } from '@/api/dictionary';

  defineOptions({ name: 'QuizHubView' });

  const router = useRouter();

  const topics = ref<TopicRef[]>([]);
  const activeTopic = ref<string | undefined>(undefined);
  const quizzes = ref<QuizSummary[]>([]);
  const blueprints = ref<QuizBlueprintSummary[]>([]);
  const loading = ref(true);
  const error = ref('');
  const starting = ref(false);
  const startError = ref('');

  async function loadQuizzes() {
    loading.value = true;
    error.value = '';
    try {
      const [quizRes, blueprintRes] = await Promise.all([
        quizzesApi(activeTopic.value),
        quizBlueprintsApi(),
      ]);
      quizzes.value = quizRes.items;
      blueprints.value = blueprintRes.items;
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  }

  function selectTopic(id: string | undefined) {
    activeTopic.value = id;
    loadQuizzes();
  }

  async function startQuiz(quizId: string) {
    startError.value = '';
    starting.value = true;
    try {
      const attempt = await startAttemptFromQuizApi(quizId);
      router.push(`/kiem-tra/${attempt.id}`);
    } catch (e) {
      startError.value = (e as Error).message;
    } finally {
      starting.value = false;
    }
  }

  async function startBlueprint(blueprintId: string) {
    startError.value = '';
    starting.value = true;
    try {
      const attempt = await startAttemptFromBlueprintApi(blueprintId);
      router.push(`/kiem-tra/${attempt.id}`);
    } catch (e) {
      startError.value = (e as Error).message;
    } finally {
      starting.value = false;
    }
  }

  onMounted(async () => {
    try {
      topics.value = await topicOptionsApi();
    } catch {
      // Bo qua - danh sach chu de chi la bo loc tien loi
    }
    await loadQuizzes();
  });
</script>

<style scoped>
  .quiz-hub {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  .hub-head h1 {
    margin: 0 0 4px;
    font-size: 28px;
  }
  .hub-sub {
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
  .hint,
  .section-hint {
    color: var(--si-text-muted);
  }
  .error-text {
    color: var(--si-danger, #c4503f);
    font-weight: 600;
  }
  .quiz-section h2 {
    margin: 0 0 4px;
    font-size: 20px;
  }
  .quiz-list {
    list-style: none;
    margin: 12px 0 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 14px;
  }
  .quiz-card {
    display: flex;
    flex-direction: column;
    gap: 8px;
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius, 16px);
    padding: 16px;
  }
  .quiz-card-top {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
  .tag {
    padding: 2px 10px;
    border-radius: 999px;
    background: var(--si-primary-light);
    color: var(--si-primary);
    font-size: 11px;
    font-weight: 700;
  }
  .tag--lesson {
    background: var(--si-kid-sun, #ffe1b0);
    color: var(--si-amber-ink, #855300);
  }
  .quiz-card h3 {
    margin: 0;
    font-size: 17px;
  }
  .quiz-desc {
    margin: 0;
    color: var(--si-text-muted);
    font-size: 14px;
  }
  .quiz-meta {
    display: flex;
    gap: 14px;
    font-size: 12px;
    color: var(--si-text-muted);
    margin-top: auto;
  }
  .quiz-meta span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
  .btn-primary {
    margin-top: 6px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 42px;
    border-radius: 999px;
    border: none;
    background: var(--si-primary);
    color: #fff;
    font-weight: 700;
    cursor: pointer;
  }
  .btn-primary:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
</style>
