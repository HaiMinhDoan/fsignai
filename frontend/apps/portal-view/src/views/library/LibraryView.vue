<template>
  <section class="library-page">
    <header class="page-head">
      <h1>Thư Viện Của Tôi</h1>
      <p class="page-sub">Những từ bạn đã lưu lại để xem sau</p>
    </header>

    <p v-if="loading" class="hint">Đang tải…</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>
    <p v-else-if="signs.length === 0" class="hint">
      Chưa lưu từ nào. Vào <RouterLink to="/tu-dien">Thư Viện Cử Chỉ</RouterLink> và bấm "Lưu từ này" ở từ bạn thích.
    </p>

    <ul v-else class="saved-grid">
      <li v-for="s in signs" :key="s.id" class="saved-card">
        <RouterLink :to="`/tu-dien/${s.signId}`" class="saved-video">
          <video v-if="s.videoUrl" :src="s.videoUrl" :poster="s.thumbnailUrl" muted playsinline></video>
          <span v-else class="saved-video-empty"><SiIcon name="hand" :size="28" /></span>
        </RouterLink>
        <div class="saved-body">
          <RouterLink :to="`/tu-dien/${s.signId}`" class="saved-word">{{ s.wordVi }}</RouterLink>
          <p v-if="s.note" class="saved-note">{{ s.note }}</p>
          <p class="saved-date">Lưu lúc {{ new Date(s.savedAt).toLocaleDateString('vi-VN') }}</p>
        </div>
      </li>
    </ul>
  </section>
</template>

<script lang="ts" setup>
  import { ref, onMounted } from 'vue';
  import SiIcon from '@/components/SiIcon.vue';
  import { mySavedSignsApi, type SavedSign } from '@/api/savedSigns';

  defineOptions({ name: 'LibraryView' });

  const signs = ref<SavedSign[]>([]);
  const loading = ref(true);
  const error = ref('');

  onMounted(async () => {
    try {
      signs.value = (await mySavedSignsApi(0, 100)).items;
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  });
</script>

<style scoped>
  .library-page {
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
  .hint {
    color: var(--si-text-muted);
  }
  .error-text {
    color: var(--si-danger, #c4503f);
    font-weight: 600;
  }
  .saved-grid {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 14px;
  }
  .saved-card {
    display: flex;
    flex-direction: column;
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius, 16px);
    overflow: hidden;
  }
  .saved-video {
    display: block;
    aspect-ratio: 4 / 3;
    background: #000;
  }
  .saved-video video {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .saved-video-empty {
    display: grid;
    place-items: center;
    width: 100%;
    height: 100%;
    color: var(--si-text-muted);
  }
  .saved-body {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 12px 14px 14px;
  }
  .saved-word {
    font-size: 17px;
    font-weight: 700;
    color: var(--si-text);
    text-decoration: none;
  }
  .saved-note {
    margin: 2px 0 0;
    font-size: 13px;
    color: var(--si-text-muted);
  }
  .saved-date {
    margin: 4px 0 0;
    font-size: 11px;
    color: var(--si-text-muted);
  }
</style>
