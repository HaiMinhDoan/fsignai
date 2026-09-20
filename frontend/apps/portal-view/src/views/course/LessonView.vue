<template>
  <section class="lesson-page">
    <RouterLink v-if="lesson" :to="`/khoa-hoc/${lesson.courseId}`" class="back-link">
      <SiIcon name="back" :size="18" /> {{ lesson.courseTitleVi ?? 'Về khoá học' }}
    </RouterLink>

    <p v-if="loading" class="hint">Đang tải bài học…</p>
    <p v-else-if="error" class="error-text" role="alert">{{ error }}</p>

    <template v-else-if="lesson && lesson.items.length">
      <header class="lesson-head">
        <h1>{{ lesson.titleVi }}</h1>
        <div class="progress-track">
          <span class="progress-fill" :style="{ width: `${((currentIndex + 1) / lesson.items.length) * 100}%` }" />
        </div>
        <p class="progress-label">Nội dung {{ currentIndex + 1 }} / {{ lesson.items.length }}</p>
      </header>

      <article class="item-card">
        <template v-if="currentItem.itemType === 'SIGN'">
          <div class="item-video">
            <video
              :key="currentItem.id"
              :src="currentItem.signPrimaryVideoUrl"
              :poster="currentItem.signThumbnailUrl"
              controls
              playsinline
            ></video>
          </div>
          <h2 class="item-word">{{ currentItem.signWordVi }}</h2>
        </template>

        <template v-else-if="currentItem.itemType === 'VIDEO'">
          <div class="item-video">
            <video
              :key="currentItem.id"
              :src="String(currentItem.contentJson?.videoUrl ?? '')"
              controls
              playsinline
            ></video>
          </div>
        </template>

        <template v-else-if="currentItem.itemType === 'TEXT'">
          <p class="item-text">{{ currentItem.contentJson?.text ?? '(chưa có nội dung)' }}</p>
        </template>

        <template v-else-if="currentItem.itemType === 'QUIZ'">
          <div class="item-quiz">
            <SiIcon name="star" :size="28" />
            <h2>{{ currentItem.quizTitleVi ?? 'Bài kiểm tra' }}</h2>
            <p>Làm bài để kiểm tra lại những gì vừa học trong bài này.</p>
            <button class="btn-primary" type="button" :disabled="startingQuiz" @click="goToQuiz">
              {{ startingQuiz ? 'Đang mở đề…' : 'Làm bài kiểm tra' }}
            </button>
          </div>
        </template>

        <template v-else>
          <p class="hint">Nội dung dạng "{{ currentItem.itemType }}" đang được biên soạn.</p>
        </template>
      </article>

      <div class="nav-actions">
        <button type="button" class="btn-secondary" :disabled="currentIndex === 0" @click="currentIndex--">
          Trước
        </button>
        <button
          v-if="currentIndex < lesson.items.length - 1"
          type="button"
          class="btn-primary"
          :disabled="tracking"
          @click="next"
        >
          {{ tracking ? 'Đang lưu…' : 'Tiếp theo' }}
        </button>
        <button v-else type="button" class="btn-primary" :disabled="tracking" @click="finish">
          {{ tracking ? 'Đang lưu…' : lesson.myStatus === 'COMPLETED' ? 'Đã hoàn thành ✓' : 'Hoàn thành bài học' }}
        </button>
      </div>

      <p v-if="justCompleted" class="done-banner">
        <SiIcon name="trophy" :size="20" /> Chúc mừng, bạn đã hoàn thành bài học này!
      </p>
    </template>
  </section>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import SiIcon from '@/components/SiIcon.vue';
  import { lessonDetailApi, trackLessonProgressApi, type LessonDetail } from '@/api/course';
  import { startAttemptFromQuizApi } from '@/api/quiz';

  defineOptions({ name: 'LessonView' });

  const route = useRoute();
  const router = useRouter();

  const lesson = ref<LessonDetail | null>(null);
  const loading = ref(true);
  const error = ref('');
  const currentIndex = ref(0);
  const tracking = ref(false);
  const startingQuiz = ref(false);
  const justCompleted = ref(false);

  const currentItem = computed(() => lesson.value!.items[currentIndex.value]!);

  async function trackCurrent() {
    if (!lesson.value) return;
    tracking.value = true;
    try {
      const updated = await trackLessonProgressApi(lesson.value.id, currentItem.value.id);
      lesson.value.myStatus = updated.myStatus;
      lesson.value.myProgressPercent = updated.myProgressPercent;
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      tracking.value = false;
    }
  }

  async function next() {
    await trackCurrent();
    currentIndex.value += 1;
  }

  async function finish() {
    await trackCurrent();
    if (lesson.value?.myStatus === 'COMPLETED') justCompleted.value = true;
  }

  async function goToQuiz() {
    if (!currentItem.value.quizId) return;
    startingQuiz.value = true;
    try {
      const attempt = await startAttemptFromQuizApi(currentItem.value.quizId);
      router.push(`/kiem-tra/${attempt.id}`);
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      startingQuiz.value = false;
    }
  }

  onMounted(async () => {
    try {
      lesson.value = await lessonDetailApi(route.params.id as string);
      // Tiep tuc tu cho da dung lan truoc, khong bat hoc lai tu dau
      if (lesson.value.myLastItemId) {
        const idx = lesson.value.items.findIndex((i) => i.id === lesson.value!.myLastItemId);
        if (idx >= 0 && idx < lesson.value.items.length - 1) currentIndex.value = idx + 1;
      }
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  });
</script>

<style scoped>
  .lesson-page {
    display: flex;
    flex-direction: column;
    gap: 20px;
    max-width: 640px;
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
  .lesson-head h1 {
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
  .item-card {
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius, 16px);
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    align-items: center;
    text-align: center;
  }
  .item-video {
    width: 100%;
  }
  .item-video video {
    width: 100%;
    max-height: 320px;
    border-radius: 12px;
    background: #000;
  }
  .item-word {
    margin: 0;
    font-size: 24px;
  }
  .item-text {
    font-size: 17px;
    line-height: 1.6;
    white-space: pre-wrap;
  }
  .item-quiz {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: var(--si-primary);
  }
  .item-quiz h2 {
    margin: 0;
    font-size: 20px;
  }
  .item-quiz p {
    margin: 0;
    color: var(--si-text-muted);
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
  .btn-secondary:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .done-banner {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 14px;
    border-radius: 12px;
    background: var(--si-success-light, #dff5e8);
    color: var(--si-success, #2e9e6b);
    font-weight: 700;
  }
</style>
