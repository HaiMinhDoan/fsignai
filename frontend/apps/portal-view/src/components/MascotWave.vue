<template>
  <div class="mascot" :class="`mascot--${layout}`">
    <svg
      class="mascot-art"
      :width="size"
      :height="size * 1.16"
      viewBox="0 0 120 140"
      role="img"
      :aria-label="`Bé Vẫy, linh vật của SignAI${label ? ': ' + label : ''}`"
    >
      <!-- Bé Vẫy là một bàn tay đang vẫy: bàn tay chính là "giọng nói" của
           ngôn ngữ ký hiệu, bé nhìn thấy là hiểu ngay đây là nơi nói bằng tay
           (docs/05-design-system.md §7.5) -->
      <g class="mascot-body">
        <!-- bốn ngón -->
        <rect x="34" y="26" width="13" height="42" rx="6.5" class="skin" />
        <rect x="49" y="18" width="13" height="50" rx="6.5" class="skin" />
        <rect x="64" y="22" width="13" height="46" rx="6.5" class="skin" />
        <rect x="79" y="32" width="13" height="36" rx="6.5" class="skin" />
        <!-- ngón cái -->
        <rect
          x="16"
          y="60"
          width="13"
          height="30"
          rx="6.5"
          class="skin"
          transform="rotate(28 22.5 75)"
        />
        <!-- lòng bàn tay + khuôn mặt -->
        <rect x="30" y="58" width="66" height="62" rx="26" class="skin" />
        <circle cx="50" cy="86" r="5" class="eye" />
        <circle cx="76" cy="86" r="5" class="eye" />
        <circle cx="41" cy="99" r="6" class="blush" />
        <circle cx="85" cy="99" r="6" class="blush" />
        <path d="M52 100q11 10 22 0" class="smile" />
      </g>
    </svg>

    <p v-if="$slots.default" class="bubble"><slot /></p>
  </div>
</template>

<script lang="ts" setup>
  /**
   * Linh vật dẫn chuyện. Vẽ bằng SVG nội tuyến để web học tập không phụ thuộc
   * file ảnh nào — khi có bộ minh hoạ chính thức thì thay ruột component này,
   * mọi chỗ gọi giữ nguyên.
   *
   * Câu thoại tối đa ~12 chữ và luôn kèm một hành động cụ thể (§7.5).
   */
  withDefaults(
    defineProps<{
      size?: number;
      /** Mô tả thêm cho trình đọc màn hình, ví dụ "đang chúc mừng" */
      label?: string;
      layout?: 'row' | 'column';
    }>(),
    { size: 96, label: '', layout: 'row' },
  );

  defineOptions({ name: 'MascotWave' });
</script>

<style scoped>
  .mascot {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  .mascot--column {
    flex-direction: column;
    text-align: center;
    gap: 10px;
  }

  .mascot-art {
    flex-shrink: 0;
    overflow: visible;
  }

  .skin {
    fill: var(--si-kid-sun);
    stroke: var(--si-kid-sun-deep);
    stroke-width: 2.5;
  }
  .eye {
    fill: var(--si-text);
  }
  .blush {
    fill: var(--si-kid-peach);
    opacity: 0.85;
  }
  .smile {
    fill: none;
    stroke: var(--si-text);
    stroke-width: 3;
    stroke-linecap: round;
  }

  /* Vẫy tay quanh cổ tay. Bé khiếm thính không nghe được lời chào, nên lời
     chào phải là một chuyển động (§7.4) */
  .mascot-body {
    transform-origin: 63px 120px;
    animation: wave 2.4s ease-in-out infinite;
  }
  @keyframes wave {
    0%,
    60%,
    100% {
      transform: rotate(0deg);
    }
    70% {
      transform: rotate(-11deg);
    }
    80% {
      transform: rotate(9deg);
    }
    90% {
      transform: rotate(-6deg);
    }
  }

  .bubble {
    position: relative;
    margin: 0;
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius);
    padding: 14px 18px;
    font-weight: 600;
    box-shadow: var(--si-kid-shadow);
  }

  /* Mũi nhọn của bóng thoại, chỉ về phía linh vật */
  .mascot--row .bubble::before {
    content: '';
    position: absolute;
    left: -10px;
    top: 50%;
    width: 16px;
    height: 16px;
    margin-top: -8px;
    background: var(--si-surface);
    border-left: 2px solid var(--si-border);
    border-bottom: 2px solid var(--si-border);
    transform: rotate(45deg);
    border-radius: 0 0 0 4px;
  }
  .mascot--column .bubble::before {
    content: '';
    position: absolute;
    top: -10px;
    left: 50%;
    width: 16px;
    height: 16px;
    margin-left: -8px;
    background: var(--si-surface);
    border-left: 2px solid var(--si-border);
    border-top: 2px solid var(--si-border);
    transform: rotate(45deg);
    border-radius: 4px 0 0 0;
  }
</style>
