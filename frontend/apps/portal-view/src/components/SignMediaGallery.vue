<template>
  <div v-if="items.length" class="media-gallery" :class="`media-gallery--${size}`">
    <figure v-for="m in items" :key="m.id" class="gallery-item">
      <!-- Khung video CỐ Ý trung tính, không viền màu, không gradient: mắt người
           xem phải đọc được hình bàn tay và sắc mặt, không bị màu nền kéo đi -->
      <video
        v-if="m.kind === 'VIDEO'"
        :src="m.url"
        :poster="m.thumbnailUrl"
        controls
        preload="metadata"
        playsinline
        :aria-label="`Video ký hiệu${m.durationMs ? ` dài ${Math.round(m.durationMs / 1000)} giây` : ''}`"
      ></video>
      <img v-else :src="m.url" alt="Ảnh người dùng gắn kèm" loading="lazy" />
    </figure>
  </div>
</template>

<script lang="ts" setup>
  /** Hiện video ký hiệu và ảnh của một bài viết hoặc một bình luận */
  import { computed } from 'vue';
  import type { ForumMedia } from '@/api/forum';

  defineOptions({ name: 'SignMediaGallery' });

  const props = withDefaults(
    defineProps<{ media?: ForumMedia[]; size?: 'normal' | 'small' }>(),
    { media: () => [], size: 'normal' },
  );

  const items = computed(() => props.media ?? []);
</script>

<style scoped>
  .media-gallery {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin: 12px 0;
  }
  .gallery-item {
    margin: 0;
    border-radius: var(--sk-r-card, 20px);
    overflow: hidden;
    background: var(--sk-stage, #eaf0f4);
    border: 2px solid var(--sk-stage-border, #d3e0e7);
  }
  .gallery-item video,
  .gallery-item img {
    display: block;
    width: 100%;
    height: auto;
  }

  .media-gallery--normal .gallery-item {
    flex: 1 1 320px;
    max-width: 480px;
  }
  .media-gallery--small .gallery-item {
    flex: 0 1 220px;
  }
</style>
