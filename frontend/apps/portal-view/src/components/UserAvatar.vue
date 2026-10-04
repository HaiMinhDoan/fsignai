<template>
  <span
    ref="root"
    class="ua"
    :style="{ width: `${size}px`, height: `${size}px`, fontSize: `${Math.round(size * 0.42)}px` }"
    @mouseenter="hover = true"
    @mouseleave="hover = false"
    @focusin="hover = true"
    @focusout="hover = false"
  >
    <video
      v-if="videoUrl && !videoBroken"
      ref="vid"
      class="ua-media"
      :src="videoUrl ?? undefined"
      :poster="avatarUrl ?? undefined"
      muted
      loop
      playsinline
      preload="metadata"
      disablepictureinpicture
      aria-hidden="true"
      @timeupdate="giuTrong5Giay"
      @error="videoBroken = true"
    ></video>
    <img v-else-if="avatarUrl && !imageBroken" class="ua-media" :src="avatarUrl ?? undefined" alt="" loading="lazy" @error="imageBroken = true" />
    <span v-else class="ua-initial" aria-hidden="true">{{ initial }}</span>
    <span class="si-visually-hidden">Ảnh đại diện của {{ name }}</span>
  </span>
</template>

<script lang="ts" setup>
  /**
   * Ảnh đại diện dùng chung: video ≤ 5 giây phát lặp, ảnh tĩnh, hoặc chữ cái đầu tên.
   *
   * Video chỉ phát khi ô ảnh đang nằm trong màn hình — trang diễn đàn có thể có hàng chục ô,
   * phát cùng lúc là máy yếu giật cả trang. Người bật "giảm chuyển động" ở hệ điều hành thì chỉ
   * thấy khung hình đầu, rê chuột hoặc Tab tới mới phát.
   *
   * Giới hạn 5 giây được giữ cả ở ĐÂY chứ không chỉ lúc tải lên: video có lách được kiểm tra
   * thời lượng thì người xem vẫn chỉ thấy 5 giây đầu lặp lại.
   */
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

  defineOptions({ name: 'UserAvatar' });

  const MAX_SECONDS = 5;

  const props = withDefaults(
    defineProps<{
      name?: string;
      avatarUrl?: string | null;
      videoUrl?: string | null;
      size?: number;
    }>(),
    { name: '', avatarUrl: null, videoUrl: null, size: 40 },
  );

  const root = ref<HTMLElement>();
  const vid = ref<HTMLVideoElement>();
  const hover = ref(false);
  const visible = ref(false);
  const videoBroken = ref(false);
  const imageBroken = ref(false);
  const reduceMotion =
    typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;

  const initial = computed(() => (props.name?.trim().split(/\s+/).pop()?.[0] ?? '?').toUpperCase());

  let observer: IntersectionObserver | null = null;

  function giuTrong5Giay() {
    const v = vid.value;
    if (v && v.currentTime >= MAX_SECONDS) v.currentTime = 0;
  }

  function capNhatPhat() {
    const v = vid.value;
    if (!v) return;
    const nenPhat = visible.value && (!reduceMotion || hover.value);
    if (nenPhat) {
      // Trình duyệt chặn tự phát thì thôi, vẫn còn ảnh poster
      v.play().catch(() => undefined);
    } else if (!v.paused) {
      v.pause();
    }
  }

  watch([visible, hover, vid], capNhatPhat);
  watch(
    () => props.videoUrl,
    () => (videoBroken.value = false),
  );
  watch(
    () => props.avatarUrl,
    () => (imageBroken.value = false),
  );

  onMounted(() => {
    if (typeof IntersectionObserver === 'undefined') {
      visible.value = true;
      return;
    }
    observer = new IntersectionObserver(([e]) => (visible.value = e.isIntersecting), { rootMargin: '80px' });
    if (root.value) observer.observe(root.value);
  });

  onBeforeUnmount(() => observer?.disconnect());
</script>

<style scoped>
  .ua {
    position: relative;
    display: inline-grid;
    place-items: center;
    flex-shrink: 0;
    border-radius: 50%;
    overflow: hidden;
    background: var(--sk-amber-ink);
    color: #fff;
    font-family: var(--sk-font-head);
    font-weight: 700;
    line-height: 1;
  }
  .ua-media {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
  .ua-initial {
    user-select: none;
  }
</style>
