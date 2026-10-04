<template>
  <section v-if="steps.length" class="steps" aria-labelledby="steps-heading">
    <div class="steps-head">
      <h2 id="steps-heading">
        <SiIcon name="hand" :size="22" />
        <span>Các bước thực hiện</span>
      </h2>
      <p class="steps-hint">Chạm vào một bước để xem lại mẫu ở tốc độ chậm</p>
    </div>

    <ol class="step-row">
      <li
        v-for="(step, i) in steps"
        :key="step.id"
        class="step-card"
        :class="{ 'step-card--on': activeStep === step.stepOrder }"
        tabindex="0"
        @click="chon(step)"
        @keydown.enter="chon(step)"
      >
        <span class="step-no" :class="`step-no--${TONES[i % TONES.length]}`">{{ step.stepOrder }}</span>

        <!-- Khung ảnh trung tính và giữ NGUYÊN tỉ lệ ảnh: cắt mất đầu ngón tay hay
             khuôn mặt là mất đúng phần người học cần nhìn nhất -->
        <div class="step-shot">
          <img
            v-if="step.imageUrl"
            :src="step.imageUrl"
            :alt="`Thế tay ở bước ${step.stepOrder}: ${step.descriptionVi}`"
            loading="lazy"
          />
          <span v-else class="step-shot-empty"><SiIcon name="hand" :size="40" /></span>
        </div>

        <h3 class="step-title">Bước {{ step.stepOrder }}: {{ step.titleVi || 'Làm theo mẫu' }}</h3>
        <!-- Chữ luôn hiện: ảnh không được là kênh thông tin duy nhất (§2.2) -->
        <p class="step-desc">{{ step.descriptionVi }}</p>
      </li>
    </ol>
  </section>
</template>

<script lang="ts" setup>
  /**
   * Hướng dẫn từng bước của một từ ký hiệu: ảnh cắt từ video mẫu + câu mô tả.
   *
   * Từ chưa có bước nào thì không hiện gì cả — không dựng thẻ trống "chưa soạn",
   * vì trang chi tiết từ đã có video mẫu, thêm ba ô rỗng chỉ làm rối.
   */
  import { ref, watch } from 'vue';
  import SiIcon from '@/components/SiIcon.vue';
  import { signStepsApi, type SignStep } from '@/api/dictionary';

  defineOptions({ name: 'SignStepsSection' });

  const props = defineProps<{ signId: string }>();
  const emit = defineEmits<{ (e: 'pick', step: SignStep): void }>();

  const TONES = ['peach', 'sky', 'mint'] as const;
  const steps = ref<SignStep[]>([]);
  const activeStep = ref<number>();

  watch(
    () => props.signId,
    async (id) => {
      steps.value = [];
      activeStep.value = undefined;
      if (!id) return;
      try {
        steps.value = await signStepsApi(id);
      } catch {
        // Thiếu hướng dẫn từng bước không làm hỏng trang: video mẫu vẫn còn đó
        steps.value = [];
      }
    },
    { immediate: true },
  );

  function chon(step: SignStep) {
    activeStep.value = step.stepOrder;
    emit('pick', step);
  }
</script>

<style scoped>
  .steps {
    margin-bottom: 8px;
  }
  .steps-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
    margin-bottom: 14px;
  }
  .steps-head h2 {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0;
    font-size: 24px;
    font-weight: 700;
    color: var(--sk-ink);
  }
  .steps-head h2 :deep(svg) {
    color: var(--sk-amber-ink);
  }
  .steps-hint {
    margin: 0;
    font-family: var(--sk-font-head);
    font-size: 13px;
    font-weight: 700;
    color: var(--sk-brown);
  }

  .step-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 20px;
    margin: 0;
    padding: 0;
    list-style: none;
  }
  .step-card {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 6px;
    background: var(--sk-surface);
    border: 3px solid transparent;
    border-radius: var(--sk-r-card);
    padding: 18px;
    box-shadow: var(--sk-shadow-card);
    cursor: pointer;
  }
  .step-card--on {
    border-color: var(--sk-amber);
  }

  .step-no {
    position: absolute;
    top: 28px;
    left: 28px;
    z-index: 1;
    display: grid;
    place-items: center;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    font-family: var(--sk-font-head);
    font-size: 18px;
    font-weight: 700;
    box-shadow: var(--sk-shadow);
  }
  .step-no--peach {
    background: var(--sk-peach);
    color: var(--sk-brown-dark);
  }
  .step-no--sky {
    background: var(--sk-sky);
    color: var(--sk-blue-dark);
  }
  .step-no--mint {
    background: var(--sk-mint);
    color: var(--sk-green-dark);
  }

  /* Ảnh bước được cắt quanh nửa người trên, tỉ lệ ~17:15. Khung theo đúng tỉ lệ đó
     và dùng contain: thẻ cao lên nhưng không mất đầu, mất tay như khung dẹt cũ (357:130) */
  .step-shot {
    aspect-ratio: 17 / 15;
    border-radius: 18px;
    overflow: hidden;
    background: var(--sk-stage);
    border: 2px solid var(--sk-stage-border);
    display: grid;
    place-items: center;
    margin-bottom: 6px;
  }
  .step-shot img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
  .step-shot-empty {
    color: var(--sk-brown);
  }

  .step-title {
    margin: 0;
    font-size: 19px;
    font-weight: 700;
  }
  .step-desc {
    margin: 0;
    font-size: 15px;
    line-height: 1.5;
    color: var(--sk-brown);
  }
</style>
