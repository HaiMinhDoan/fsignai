<template>
  <div class="confetti" aria-hidden="true">
    <span
      v-for="p in pieces"
      :key="p.id"
      class="piece"
      :style="{
        background: p.color,
        left: `${p.left}%`,
        transform: `rotate(${p.spin}deg)`,
        animationDelay: `${p.delay}ms`,
        animationDuration: `${p.duration}ms`,
        borderRadius: p.round ? '50%' : '2px',
      }"
    ></span>
  </div>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';

  /**
   * Pháo hoa ăn mừng.
   *
   * Bé khiếm thính không nhận được tiếng vỗ tay hay tiếng "ting", nên phần
   * thưởng phải nhìn thấy được (docs/05-design-system.md §7.4). Nhưng hiệu ứng
   * này luôn là PHẦN CỘNG THÊM: dấu ✓ và câu chữ khen mới là thứ mang thông
   * tin, pháo hoa tắt đi thì màn hình vẫn nói đủ ý (§2.2).
   */
  const props = defineProps<{ fire: number }>();

  interface Piece {
    id: number;
    left: number;
    color: string;
    spin: number;
    delay: number;
    duration: number;
    round: boolean;
  }

  const COLORS = [
    'var(--si-kid-sun)',
    'var(--si-kid-peach)',
    'var(--si-kid-mint)',
    'var(--si-kid-sky)',
    'var(--si-kid-lilac)',
  ];

  const pieces = ref<Piece[]>([]);
  let seq = 0;
  let clearTimer: number | undefined;

  // Có bé nhạy cảm tiền đình — hệ điều hành bật giảm chuyển động thì không bắn
  const reducedMotion = () =>
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  watch(
    () => props.fire,
    (n) => {
      if (!n || reducedMotion()) return;
      window.clearTimeout(clearTimer);
      pieces.value = Array.from({ length: 28 }, () => ({
        id: seq++,
        left: Math.random() * 100,
        color: COLORS[Math.floor(Math.random() * COLORS.length)]!,
        spin: Math.random() * 360,
        delay: Math.random() * 180,
        duration: 800 + Math.random() * 500,
        round: Math.random() > 0.5,
      }));
      // Dọn khỏi DOM sau khi rơi xong, để không tích lại hàng trăm phần tử
      clearTimer = window.setTimeout(() => (pieces.value = []), 1600);
    },
  );

  defineOptions({ name: 'ConfettiBurst' });
</script>

<style scoped>
  .confetti {
    position: absolute;
    inset: 0;
    overflow: hidden;
    pointer-events: none;
    z-index: 5;
  }

  .piece {
    position: absolute;
    top: -14px;
    width: 10px;
    height: 14px;
    animation-name: fall;
    animation-timing-function: cubic-bezier(0.25, 0.75, 0.5, 1);
    animation-fill-mode: forwards;
  }

  @keyframes fall {
    0% {
      opacity: 0;
      transform: translateY(0) rotate(0deg);
    }
    12% {
      opacity: 1;
    }
    100% {
      opacity: 0;
      transform: translateY(320px) rotate(540deg);
    }
  }
</style>
