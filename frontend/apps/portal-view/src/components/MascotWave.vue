<template>
  <div class="mascot" :class="`mascot--${layout}`">
    <span
      class="mascot-art"
      :style="{ width: `${size}px`, height: `${size}px` }"
      role="img"
      :aria-label="`Mochi, bàn tay linh vật của SignAI${label ? ': ' + label : ''}`"
    >
      <!-- Quầng sáng pastel nằm dưới bàn tay: nền trang chỗ nào cũng sáng,
           thiếu quầng này linh vật trông như bị dán lên. -->
      <span class="mascot-halo" aria-hidden="true"></span>
      <img :src="mascotHand" alt="" class="mascot-img" />
    </span>

    <p v-if="$slots.default" class="bubble"><slot /></p>
  </div>
</template>

<script lang="ts" setup>
  /**
   * Linh vật dẫn chuyện: bàn tay Mochi đang làm ký hiệu “I love you”.
   *
   * Bàn tay chính là "giọng nói" của ngôn ngữ ký hiệu — nhìn là hiểu ngay đây
   * là nơi nói bằng tay (docs/05-design-system.md §7.5). Ảnh đã tách nền sẵn
   * nên đặt được lên mọi nền màu; đổi ảnh thì mọi chỗ gọi giữ nguyên.
   *
   * Câu thoại tối đa ~12 chữ và luôn kèm một hành động cụ thể (§7.5).
   */
  import mascotHand from '@/assets/mascot/mochi-hand-solo.png';

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
    position: relative;
    flex-shrink: 0;
    display: grid;
    place-items: center;
  }

  .mascot-halo {
    position: absolute;
    inset: 4%;
    border-radius: 50%;
    background: radial-gradient(circle at 32% 28%, var(--sk-peach), var(--sk-sky) 72%);
    opacity: 0.55;
    animation: halo-pulse 3.6s ease-in-out infinite;
  }

  .mascot-img {
    position: relative;
    width: 82%;
    height: 82%;
    object-fit: contain;
    /* Vẫy quanh cổ tay, tức là quanh mép dưới của ảnh */
    transform-origin: 50% 92%;
    animation: wave 2.4s ease-in-out infinite;
  }

  /* Bàn tay vẫy chào. Người khiếm thính không nghe được lời chào, nên lời
     chào phải là một chuyển động (§7.4) */
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

  @keyframes halo-pulse {
    0%,
    100% {
      transform: scale(1);
      opacity: 0.55;
    }
    50% {
      transform: scale(1.06);
      opacity: 0.72;
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
