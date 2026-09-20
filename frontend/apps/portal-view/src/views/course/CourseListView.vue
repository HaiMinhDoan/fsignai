<template>
  <section class="course-list-page">
    <header class="page-head">
      <h1>Khoá Học</h1>
      <p class="page-sub">Học theo lộ trình từng bài, từ dễ đến khó</p>
    </header>

    <div class="filter-row">
      <div v-if="topics.length" class="topic-pills">
        <button
          type="button"
          class="pill"
          :class="{ 'is-active': activeTopic === undefined }"
          @click="selectTopic(undefined)"
        >
          Tất cả chủ đề
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

      <div class="level-pills">
        <button
          v-for="lv in LEVELS"
          :key="lv.value ?? 'all'"
          type="button"
          class="pill pill--level"
          :class="{ 'is-active': activeLevel === lv.value }"
          @click="selectLevel(lv.value)"
        >
          {{ lv.label }}
        </button>
      </div>
    </div>

    <p v-if="loading" class="hint">Đang tải danh sách khoá học…</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>
    <p v-else-if="courses.length === 0" class="hint">Chưa có khoá học nào ở bộ lọc này.</p>

    <ul v-else class="course-grid">
      <li v-for="c in courses" :key="c.id">
        <RouterLink :to="`/khoa-hoc/${c.id}`" class="course-card">
          <div class="course-cover" :style="c.coverUrl ? { backgroundImage: `url(${c.coverUrl})` } : {}">
            <span v-if="!c.coverUrl" class="course-cover-fallback"><SiIcon name="book" :size="32" /></span>
            <span class="level-badge">{{ LEVEL_LABEL[c.level] }}</span>
          </div>
          <div class="course-body">
            <span v-if="c.topicNameVi" class="tag">{{ c.topicNameVi }}</span>
            <h2>{{ c.titleVi }}</h2>
            <p v-if="c.descriptionVi" class="course-desc">{{ c.descriptionVi }}</p>
            <div class="course-meta">
              <span><SiIcon name="book" :size="14" /> {{ c.lessonCount }} bài học</span>
            </div>
            <div v-if="c.myProgressPercent !== undefined" class="mini-progress">
              <span class="mini-progress-fill" :style="{ width: `${c.myProgressPercent}%` }" />
            </div>
            <p v-if="c.myStatus === 'COMPLETED'" class="status-line status-line--done">
              <SiIcon name="check" :size="14" /> Đã hoàn thành
            </p>
            <p v-else-if="c.myStatus === 'IN_PROGRESS'" class="status-line">
              Đang học — {{ c.myProgressPercent }}%
            </p>
          </div>
        </RouterLink>
      </li>
    </ul>
  </section>
</template>

<script lang="ts" setup>
  import { ref, onMounted } from 'vue';
  import SiIcon from '@/components/SiIcon.vue';
  import { coursesApi, type CourseSummary, type SignLevel } from '@/api/course';
  import { topicOptionsApi, type TopicRef } from '@/api/dictionary';

  defineOptions({ name: 'CourseListView' });

  const LEVEL_LABEL: Record<SignLevel, string> = {
    BEGINNER: 'Nhập môn',
    BASIC: 'Cơ bản',
    INTERMEDIATE: 'Trung cấp',
    ADVANCED: 'Nâng cao',
  };
  const LEVELS: { label: string; value?: SignLevel }[] = [
    { label: 'Mọi cấp độ', value: undefined },
    { label: 'Nhập môn', value: 'BEGINNER' },
    { label: 'Cơ bản', value: 'BASIC' },
    { label: 'Trung cấp', value: 'INTERMEDIATE' },
    { label: 'Nâng cao', value: 'ADVANCED' },
  ];

  const topics = ref<TopicRef[]>([]);
  const activeTopic = ref<string | undefined>(undefined);
  const activeLevel = ref<SignLevel | undefined>(undefined);
  const courses = ref<CourseSummary[]>([]);
  const loading = ref(true);
  const error = ref('');

  async function loadCourses() {
    loading.value = true;
    error.value = '';
    try {
      const res = await coursesApi(activeTopic.value, activeLevel.value);
      courses.value = res.items;
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  }

  function selectTopic(id: string | undefined) {
    activeTopic.value = id;
    loadCourses();
  }
  function selectLevel(level: SignLevel | undefined) {
    activeLevel.value = level;
    loadCourses();
  }

  onMounted(async () => {
    try {
      topics.value = await topicOptionsApi();
    } catch {
      // Bo qua - bo loc chu de chi la tien loi
    }
    await loadCourses();
  });
</script>

<style scoped>
  .course-list-page {
    display: flex;
    flex-direction: column;
    gap: 18px;
  }
  .page-head h1 {
    margin: 0 0 4px;
    font-size: 28px;
  }
  .page-sub {
    margin: 0;
    color: var(--si-text-muted);
  }
  .filter-row {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .topic-pills,
  .level-pills {
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
  .hint {
    color: var(--si-text-muted);
  }
  .error-text {
    color: var(--si-danger, #c4503f);
    font-weight: 600;
  }
  .course-grid {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 16px;
  }
  .course-card {
    display: flex;
    flex-direction: column;
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius, 16px);
    overflow: hidden;
    text-decoration: none;
    color: inherit;
    transition: border-color 0.15s;
  }
  .course-card:hover {
    border-color: var(--si-secondary);
  }
  .course-cover {
    position: relative;
    height: 120px;
    background: var(--si-primary-light) center/cover no-repeat;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .course-cover-fallback {
    color: var(--si-primary);
  }
  .level-badge {
    position: absolute;
    top: 10px;
    right: 10px;
    padding: 2px 10px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.9);
    font-size: 11px;
    font-weight: 700;
    color: var(--si-primary);
  }
  .course-body {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 14px 16px 16px;
  }
  .tag {
    align-self: flex-start;
    padding: 2px 10px;
    border-radius: 999px;
    background: var(--si-primary-light);
    color: var(--si-primary);
    font-size: 11px;
    font-weight: 700;
  }
  .course-body h2 {
    margin: 0;
    font-size: 17px;
  }
  .course-desc {
    margin: 0;
    color: var(--si-text-muted);
    font-size: 13px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .course-meta {
    display: flex;
    gap: 12px;
    font-size: 12px;
    color: var(--si-text-muted);
  }
  .course-meta span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
  .mini-progress {
    height: 6px;
    border-radius: 999px;
    background: var(--si-border);
    overflow: hidden;
  }
  .mini-progress-fill {
    display: block;
    height: 100%;
    background: var(--si-primary);
  }
  .status-line {
    margin: 0;
    font-size: 12px;
    font-weight: 700;
    color: var(--si-text-muted);
  }
  .status-line--done {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: var(--si-success, #2e9e6b);
  }
</style>
