<template>
  <div class="auth-page">
    <div class="auth-card onboard-card">
      <MascotWave :size="80" label="đang hỏi để làm quen">Trả lời 4 câu nhé!</MascotWave>

      <div class="onboard-dots" role="progressbar" :aria-valuenow="step" aria-valuemin="1" aria-valuemax="4">
        <span v-for="n in 4" :key="n" class="onboard-dot" :class="{ 'is-on': n <= step }" />
      </div>

      <template v-if="step === 1">
        <h1>Vì sao bạn muốn học ký hiệu?</h1>
        <div class="onboard-grid">
          <button
            v-for="opt in LEARN_REASON_OPTIONS"
            :key="opt.value"
            type="button"
            class="onboard-pill"
            :class="{ 'is-active': learnReason === opt.value }"
            @click="learnReason = opt.value"
          >
            <SiIcon :name="opt.icon" :size="22" />
            <span>{{ opt.label }}</span>
          </button>
        </div>
      </template>

      <template v-else-if="step === 2">
        <h1>Bạn đã biết ký hiệu chưa?</h1>
        <div class="onboard-grid">
          <button
            v-for="opt in CURRENT_LEVEL_OPTIONS"
            :key="opt.value"
            type="button"
            class="onboard-pill"
            :class="{ 'is-active': currentLevel === opt.value }"
            @click="currentLevel = opt.value"
          >
            <SiIcon :name="opt.icon" :size="22" />
            <span>{{ opt.label }}</span>
          </button>
        </div>
      </template>

      <template v-else-if="step === 3">
        <h1>Mỗi ngày bạn muốn học bao lâu?</h1>
        <div class="onboard-grid">
          <button
            v-for="opt in DAILY_MINUTES_OPTIONS"
            :key="opt.value"
            type="button"
            class="onboard-pill"
            :class="{ 'is-active': dailyMinutes === opt.value }"
            @click="dailyMinutes = opt.value"
          >
            <SiIcon :name="opt.icon" :size="22" />
            <span>{{ opt.label }}</span>
          </button>
        </div>
      </template>

      <template v-else>
        <h1>Bạn thích chủ đề nào?</h1>
        <p class="auth-subtitle">Chọn ít nhất một chủ đề, chọn bao nhiêu cũng được</p>
        <p v-if="topicsLoading" class="hint">Đang tải danh sách chủ đề…</p>
        <div v-else class="onboard-chips">
          <button
            v-for="t in topics"
            :key="t.id"
            type="button"
            class="onboard-chip"
            :class="{ 'is-active': interestedTopics.includes(t.id) }"
            @click="toggleTopic(t.id)"
          >
            {{ t.nameVi }}
          </button>
        </div>
      </template>

      <p v-if="error" class="auth-error" role="alert">
        <SiIcon name="lock" :size="20" />
        <span>{{ error }}</span>
      </p>

      <div class="onboard-actions">
        <button v-if="step > 1" type="button" class="onboard-back" :disabled="saving" @click="back">
          <SiIcon name="back" :size="20" />
          <span>Quay lại</span>
        </button>
        <button class="auth-submit" type="button" :disabled="!canGoNext || saving" @click="next">
          <SiIcon v-if="!saving" :name="step === 4 ? 'sparkles' : 'next'" :size="22" />
          <span>{{ saving ? 'Đang lưu…' : step === 4 ? 'Xong! Vào học thôi' : 'Tiếp tục' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { useAuthStore } from '@/stores/auth';
  import MascotWave from '@/components/MascotWave.vue';
  import SiIcon from '@/components/SiIcon.vue';
  import { getOnboardingApi, saveOnboardingApi, type LearnReason, type CurrentLevel } from '@/api/onboarding';
  import { topicOptionsApi, type TopicRef } from '@/api/dictionary';

  defineOptions({ name: 'OnboardingView' });

  const LEARN_REASON_OPTIONS: { value: LearnReason; label: string; icon: string }[] = [
    { value: 'FAMILY', label: 'Có người thân khiếm thính', icon: 'user' },
    { value: 'FRIENDS', label: 'Có bạn bè khiếm thính', icon: 'sparkles' },
    { value: 'PERSONAL', label: 'Mình thích ký hiệu', icon: 'star' },
    { value: 'WORK', label: 'Học vì công việc', icon: 'book' },
    { value: 'BASIC_COMM', label: 'Giao tiếp cơ bản hằng ngày', icon: 'hand' },
    { value: 'OTHER', label: 'Lý do khác', icon: 'grid' },
  ];

  const CURRENT_LEVEL_OPTIONS: { value: CurrentLevel; label: string; icon: string }[] = [
    { value: 'BEGINNER', label: 'Mới bắt đầu, chưa biết gì', icon: 'star' },
    { value: 'BASIC', label: 'Biết vài từ cơ bản', icon: 'hand' },
    { value: 'INTERMEDIATE', label: 'Giao tiếp được', icon: 'trophy' },
    { value: 'ADVANCED', label: 'Khá thành thạo rồi', icon: 'flame' },
    { value: 'UNSURE', label: 'Chưa chắc lắm', icon: 'grid' },
  ];

  const DAILY_MINUTES_OPTIONS: { value: number; label: string; icon: string }[] = [
    { value: 5, label: '5 phút/ngày — Nhẹ nhàng', icon: 'slow' },
    { value: 10, label: '10 phút/ngày — Vừa sức', icon: 'play' },
    { value: 15, label: '15 phút/ngày — Chăm chỉ', icon: 'flame' },
    { value: 30, label: '30 phút/ngày — Quyết tâm', icon: 'trophy' },
  ];

  const auth = useAuthStore();
  const router = useRouter();

  const step = ref(1);
  const saving = ref(false);
  const error = ref('');

  const learnReason = ref<LearnReason | null>(null);
  const currentLevel = ref<CurrentLevel | null>(null);
  const dailyMinutes = ref<number | null>(null);
  const interestedTopics = ref<string[]>([]);

  const topics = ref<TopicRef[]>([]);
  const topicsLoading = ref(true);

  const canGoNext = computed(() => {
    if (step.value === 1) return !!learnReason.value;
    if (step.value === 2) return !!currentLevel.value;
    if (step.value === 3) return !!dailyMinutes.value;
    return interestedTopics.value.length > 0;
  });

  function toggleTopic(id: string) {
    const i = interestedTopics.value.indexOf(id);
    if (i >= 0) interestedTopics.value.splice(i, 1);
    else interestedTopics.value.push(id);
  }

  function back() {
    error.value = '';
    step.value -= 1;
  }

  async function next() {
    error.value = '';
    saving.value = true;
    try {
      if (step.value === 1) {
        await saveOnboardingApi({ learnReason: learnReason.value! });
        step.value = 2;
      } else if (step.value === 2) {
        await saveOnboardingApi({ currentLevel: currentLevel.value! });
        step.value = 3;
      } else if (step.value === 3) {
        await saveOnboardingApi({ dailyMinutes: dailyMinutes.value! });
        step.value = 4;
      } else {
        await saveOnboardingApi({ interestedTopics: interestedTopics.value, complete: true });
        if (auth.user) auth.user.onboardingCompleted = true;
        router.push('/');
      }
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      saving.value = false;
    }
  }

  onMounted(async () => {
    try {
      const [answers, topicList] = await Promise.all([getOnboardingApi(), topicOptionsApi()]);
      topics.value = topicList;
      // Khôi phục đúng câu đã trả lời trước đó nếu người học đóng app giữa chừng
      learnReason.value = answers.learnReason ?? null;
      currentLevel.value = answers.currentLevel ?? null;
      dailyMinutes.value = answers.dailyMinutes ?? null;
      interestedTopics.value = answers.interestedTopics ?? [];
      if (!learnReason.value) step.value = 1;
      else if (!currentLevel.value) step.value = 2;
      else if (!dailyMinutes.value) step.value = 3;
      else step.value = 4;
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      topicsLoading.value = false;
    }
  });
</script>

<style scoped>
  .onboard-card {
    max-width: 560px;
  }
  .onboard-dots {
    display: flex;
    gap: 8px;
    margin-bottom: 18px;
  }
  .onboard-dot {
    flex: 1;
    height: 8px;
    border-radius: 999px;
    background: var(--si-border);
  }
  .onboard-dot.is-on {
    background: var(--si-primary);
  }
  .onboard-card h1 {
    font-size: 22px;
    margin: 0 0 14px;
  }
  .onboard-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
    margin-bottom: 8px;
  }
  .onboard-pill {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 14px 14px;
    border-radius: var(--si-kid-radius);
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    color: var(--si-text-muted);
    font: inherit;
    font-size: 14px;
    font-weight: 600;
    text-align: left;
    cursor: pointer;
    transition: border-color 0.15s, color 0.15s, background 0.15s;
  }
  .onboard-pill:hover {
    border-color: var(--si-secondary);
  }
  .onboard-pill.is-active {
    border-color: var(--si-primary);
    background: var(--si-primary-light);
    color: var(--si-primary);
  }
  .onboard-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 8px;
  }
  .onboard-chip {
    padding: 10px 16px;
    border-radius: 999px;
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    color: var(--si-text-muted);
    font: inherit;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
  }
  .onboard-chip.is-active {
    border-color: var(--si-primary);
    background: var(--si-primary-light);
    color: var(--si-primary);
  }
  .onboard-actions {
    display: flex;
    gap: 10px;
    margin-top: 18px;
  }
  .onboard-back {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 18px;
    border-radius: 999px;
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    color: var(--si-text-muted);
    font: inherit;
    font-weight: 700;
    cursor: pointer;
  }
  .onboard-actions .auth-submit {
    flex: 1;
  }
  .hint {
    color: var(--si-text-muted);
  }
</style>
