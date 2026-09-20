<template>
  <section class="course-detail-page">
    <RouterLink to="/khoa-hoc" class="back-link"><SiIcon name="back" :size="18" /> Về danh sách khoá học</RouterLink>

    <p v-if="loading" class="hint">Đang tải…</p>
    <p v-else-if="error" class="error-text" role="alert">{{ error }}</p>

    <template v-else-if="course">
      <header class="course-head">
        <span v-if="course.topicNameVi" class="tag">{{ course.topicNameVi }}</span>
        <h1>{{ course.titleVi }}</h1>
        <p v-if="course.descriptionVi" class="course-desc">{{ course.descriptionVi }}</p>
        <div v-if="course.myProgressPercent !== undefined" class="progress-track">
          <span class="progress-fill" :style="{ width: `${course.myProgressPercent}%` }" />
        </div>
        <p v-if="course.myProgressPercent !== undefined" class="progress-label">
          Đã học {{ course.myProgressPercent }}%
        </p>
      </header>

      <ol class="lesson-list">
        <li v-for="(l, i) in course.lessons" :key="l.id">
          <RouterLink :to="`/bai-hoc/${l.id}`" class="lesson-card">
            <span class="lesson-index" :class="{ 'is-done': l.myStatus === 'COMPLETED' }">
              <SiIcon v-if="l.myStatus === 'COMPLETED'" name="check" :size="16" />
              <template v-else>{{ i + 1 }}</template>
            </span>
            <div class="lesson-body">
              <h3>{{ l.titleVi }}</h3>
              <p v-if="l.descriptionVi" class="lesson-desc">{{ l.descriptionVi }}</p>
              <div class="lesson-meta">
                <span><SiIcon name="slow" :size="13" /> {{ l.estimatedMinutes }} phút</span>
                <span><SiIcon name="grid" :size="13" /> {{ l.itemCount }} nội dung</span>
                <span v-if="l.myStatus === 'IN_PROGRESS'" class="in-progress-tag">
                  Đang học {{ l.myProgressPercent }}%
                </span>
              </div>
            </div>
            <SiIcon name="next" :size="18" />
          </RouterLink>
        </li>
      </ol>
    </template>
  </section>
</template>

<script lang="ts" setup>
  import { ref, onMounted } from 'vue';
  import { useRoute } from 'vue-router';
  import SiIcon from '@/components/SiIcon.vue';
  import { courseDetailApi, type CourseDetail } from '@/api/course';

  defineOptions({ name: 'CourseDetailView' });

  const route = useRoute();
  const course = ref<CourseDetail | null>(null);
  const loading = ref(true);
  const error = ref('');

  onMounted(async () => {
    try {
      course.value = await courseDetailApi(route.params.id as string);
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  });
</script>

<style scoped>
  .course-detail-page {
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
  .tag {
    display: inline-block;
    padding: 2px 10px;
    border-radius: 999px;
    background: var(--si-primary-light);
    color: var(--si-primary);
    font-size: 11px;
    font-weight: 700;
    margin-bottom: 8px;
  }
  .course-head h1 {
    margin: 0 0 6px;
    font-size: 26px;
  }
  .course-desc {
    margin: 0 0 12px;
    color: var(--si-text-muted);
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
  }
  .progress-label {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--si-text-muted);
  }
  .lesson-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .lesson-card {
    display: flex;
    align-items: center;
    gap: 14px;
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: 14px;
    padding: 14px 16px;
    text-decoration: none;
    color: inherit;
    transition: border-color 0.15s;
  }
  .lesson-card:hover {
    border-color: var(--si-secondary);
  }
  .lesson-index {
    display: grid;
    place-items: center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--si-primary-light);
    color: var(--si-primary);
    font-weight: 700;
    flex-shrink: 0;
  }
  .lesson-index.is-done {
    background: var(--si-success-light, #dff5e8);
    color: var(--si-success, #2e9e6b);
  }
  .lesson-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .lesson-body h3 {
    margin: 0;
    font-size: 16px;
  }
  .lesson-desc {
    margin: 0;
    color: var(--si-text-muted);
    font-size: 13px;
  }
  .lesson-meta {
    display: flex;
    gap: 12px;
    font-size: 12px;
    color: var(--si-text-muted);
  }
  .lesson-meta span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
  .in-progress-tag {
    color: var(--si-primary);
    font-weight: 700;
  }
</style>
