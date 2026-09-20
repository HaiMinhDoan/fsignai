<template>
  <svg
    class="si-icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <path v-for="(d, i) in paths" :key="i" :d="d" />
    <circle
      v-for="(c, i) in circles"
      :key="`c${i}`"
      :cx="c[0]"
      :cy="c[1]"
      :r="c[2]"
    />
  </svg>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';

  /**
   * Một cửa duy nhất cho toàn bộ icon của web học tập.
   *
   * Vẽ nội tuyến thay vì gọi thư viện icon: app này cố ý không có dependency
   * nào ngoài vue/router/pinia/axios, và bộ icon riêng của dự án sẽ được cung
   * cấp sau (docs/05-design-system.md §5). Khi có bộ đó, chỉ thay bảng dưới đây
   * — hơn ba chục chỗ gọi <SiIcon> không phải sửa dòng nào.
   *
   * Quy cách: khung 24×24, nét 1.8, dùng currentColor để ăn theo màu chữ.
   */
  type IconDef = { paths: string[]; circles?: [number, number, number][] };

  const ICONS: Record<string, IconDef> = {
    home: { paths: ['M3 11l9-8 9 8v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z'] },
    search: { paths: ['M16.5 16.5L21 21'], circles: [[11, 11, 7]] },
    book: { paths: ['M4 5.5A2.5 2.5 0 0 1 6.5 3H19v15H6.5A2.5 2.5 0 0 0 4 20.5z', 'M4 20.5A2.5 2.5 0 0 1 6.5 18H19v3H6.5A2.5 2.5 0 0 1 4 20.5z'] },
    map: { paths: ['M9 4L3 7v13l6-3 6 3 6-3V4l-6 3z', 'M9 4v13', 'M15 7v13'] },
    star: { paths: ['M12 3.2l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17.2l-5.4 2.9 1-6.1-4.4-4.3 6.1-.9z'] },
    trophy: { paths: ['M7 4h10v5a5 5 0 0 1-10 0z', 'M7 5H4.5A2.5 2.5 0 0 0 7 10.5', 'M17 5h2.5A2.5 2.5 0 0 1 17 10.5', 'M12 14v4', 'M8.5 21h7'] },
    flame: { paths: ['M12 3c0 3-3 3.5-3 6.5 0 1.2.8 2 1.6 2 .9 0 1.4-.8 1.4-1.8 2 1.3 3 3 3 4.6a5 5 0 1 1-10 0C5 10 12 8.5 12 3z'] },
    lock: { paths: ['M5.5 11.5a1.5 1.5 0 0 1 1.5-1.5h10a1.5 1.5 0 0 1 1.5 1.5v7A1.5 1.5 0 0 1 17 20H7a1.5 1.5 0 0 1-1.5-1.5z', 'M8.5 10V7a3.5 3.5 0 0 1 7 0v3'] },
    check: { paths: ['M4.5 12.5l5 5 10-10'] },
    play: { paths: ['M8 5.2l11 6.8-11 6.8z'] },
    repeat: { paths: ['M4 12a8 8 0 0 1 13.7-5.6L20 8', 'M20 4v4h-4', 'M20 12a8 8 0 0 1-13.7 5.6L4 16', 'M4 20v-4h4'] },
    slow: { paths: ['M12 7v5l3 2'], circles: [[12, 12, 8.5]] },
    camera: { paths: ['M3 8.5A1.5 1.5 0 0 1 4.5 7h3L9 5h6l1.5 2h3A1.5 1.5 0 0 1 21 8.5v9a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17.5z'], circles: [[12, 13, 3.5]] },
    sparkles: { paths: ['M12 3l1.5 4.2L18 8.7l-4.5 1.5L12 14.4l-1.5-4.2L6 8.7l4.5-1.5z', 'M18.5 15l.8 2.2 2.2.8-2.2.8-.8 2.2-.8-2.2-2.2-.8 2.2-.8z'] },
    user: { paths: ['M4.5 20.5c0-3.6 3.4-5.5 7.5-5.5s7.5 1.9 7.5 5.5'], circles: [[12, 8, 4]] },
    logout: { paths: ['M15 12H4.5', 'M8.5 7.5L4 12l4.5 4.5', 'M13 4h5.5A1.5 1.5 0 0 1 20 5.5v13a1.5 1.5 0 0 1-1.5 1.5H13'] },
    back: { paths: ['M20 12H4.5', 'M11 5l-7 7 7 7'] },
    next: { paths: ['M4 12h15.5', 'M13 5l7 7-7 7'] },
    filter: { paths: ['M3.5 5h17l-6.5 7.5v6l-4 2.5v-8.5z'] },
    hand: { paths: ['M9 11V5.5a1.5 1.5 0 0 1 3 0V11', 'M12 11V4.5a1.5 1.5 0 0 1 3 0V11', 'M15 11V6.5a1.5 1.5 0 0 1 3 0V14a7 7 0 0 1-7 7h-.5a6 6 0 0 1-5.1-2.9L3 14.5a1.6 1.6 0 0 1 2.7-1.7L9 16.5V11'] },
    grid: { paths: ['M4 4h7v7H4z', 'M13 4h7v7h-7z', 'M4 13h7v7H4z', 'M13 13h7v7h-7z'] },
    bell: { paths: ['M6 10a6 6 0 0 1 12 0v4.5l1.5 3H4.5L6 14.5z', 'M10 20.5a2 2 0 0 0 4 0'] },
    close: { paths: ['M6 6l12 12', 'M18 6L6 18'] },
  };

  const props = withDefaults(defineProps<{ name: string; size?: number | string }>(), {
    size: 24,
  });

  const def = computed<IconDef>(() => ICONS[props.name] ?? ICONS.sparkles!);
  const paths = computed(() => def.value.paths);
  const circles = computed(() => def.value.circles ?? []);
</script>

<style scoped>
  .si-icon {
    display: block;
    flex-shrink: 0;
  }
</style>
