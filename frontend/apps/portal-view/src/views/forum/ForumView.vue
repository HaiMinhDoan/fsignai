<template>
  <section class="forum-page">
    <header class="forum-head">
      <div>
        <h1>Diễn Đàn</h1>
        <p class="forum-sub">Nơi ba mẹ, thầy cô và các bạn lớn trao đổi về ký hiệu</p>
      </div>
      <button class="btn-primary" type="button" @click="composing = !composing">
        <SiIcon name="sparkles" :size="20" />
        <span>{{ composing ? 'Đóng lại' : 'Đăng bài mới' }}</span>
      </button>
    </header>

    <form v-if="composing" class="composer" @submit.prevent="submitPost">
      <label class="field">
        <span>Chuyên mục</span>
        <select v-model="draftCategoryId" required>
          <option value="" disabled>Chọn chuyên mục</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.nameVi }}</option>
        </select>
      </label>
      <label class="field">
        <span>Tiêu đề</span>
        <input v-model="draftTitle" type="text" required maxlength="255" placeholder="Tiêu đề bài viết" />
      </label>
      <label class="field">
        <span>Nội dung</span>
        <textarea v-model="draftBody" rows="4" required placeholder="Bạn muốn chia sẻ điều gì?"></textarea>
      </label>
      <p v-if="composeError" class="error-text">{{ composeError }}</p>
      <button class="btn-primary" type="submit" :disabled="posting">
        {{ posting ? 'Đang đăng…' : 'Đăng bài' }}
      </button>
    </form>

    <div class="category-pills" role="tablist">
      <button
        type="button"
        class="pill"
        :class="{ 'is-active': activeCategory === undefined }"
        @click="selectCategory(undefined)"
      >
        Tất cả
      </button>
      <button
        v-for="c in categories"
        :key="c.id"
        type="button"
        class="pill"
        :class="{ 'is-active': activeCategory === c.id }"
        @click="selectCategory(c.id)"
      >
        {{ c.nameVi }} ({{ c.postCount }})
      </button>
    </div>

    <p v-if="loading" class="hint">Đang tải bài viết…</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>
    <p v-else-if="posts.length === 0" class="hint">Chưa có bài viết nào ở đây, hãy là người đầu tiên!</p>

    <ul v-else class="post-list">
      <li v-for="p in posts" :key="p.id">
        <RouterLink :to="`/dien-dan/${p.id}`" class="post-card">
          <div class="post-card-top">
            <span v-if="p.isPinned" class="pin-badge"><SiIcon name="star" :size="14" /> Ghim</span>
            <span class="cat-tag">{{ p.categoryNameVi }}</span>
          </div>
          <h2>{{ p.titleVi }}</h2>
          <p class="post-excerpt">{{ p.bodyMd }}</p>
          <div class="post-meta">
            <span>{{ p.authorName }}</span>
            <span>·</span>
            <span>{{ formatRelative(p.lastActivityAt) }}</span>
            <span class="spacer" />
            <span><SiIcon name="user" :size="14" /> {{ p.viewCount }}</span>
            <span>💬 {{ p.commentCount }}</span>
            <span>❤️ {{ p.reactionCount }}</span>
          </div>
        </RouterLink>
      </li>
    </ul>
  </section>
</template>

<script lang="ts" setup>
  import { ref, onMounted } from 'vue';
  import SiIcon from '@/components/SiIcon.vue';
  import {
    forumCategoriesApi,
    forumPostsApi,
    forumPostCreateApi,
    type ForumCategory,
    type ForumPost,
  } from '@/api/forum';

  defineOptions({ name: 'ForumView' });

  const categories = ref<ForumCategory[]>([]);
  const posts = ref<ForumPost[]>([]);
  const activeCategory = ref<string | undefined>(undefined);
  const loading = ref(true);
  const error = ref('');

  const composing = ref(false);
  const composeError = ref('');
  const posting = ref(false);
  const draftCategoryId = ref('');
  const draftTitle = ref('');
  const draftBody = ref('');

  function formatRelative(iso: string) {
    const diffMs = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diffMs / 60000);
    if (mins < 1) return 'vừa xong';
    if (mins < 60) return `${mins} phút trước`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours} giờ trước`;
    return `${Math.floor(hours / 24)} ngày trước`;
  }

  async function loadPosts() {
    loading.value = true;
    error.value = '';
    try {
      const res = await forumPostsApi(activeCategory.value);
      posts.value = res.items;
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  }

  function selectCategory(id: string | undefined) {
    activeCategory.value = id;
    loadPosts();
  }

  async function submitPost() {
    composeError.value = '';
    posting.value = true;
    try {
      await forumPostCreateApi({
        categoryId: draftCategoryId.value,
        titleVi: draftTitle.value,
        bodyMd: draftBody.value,
      });
      draftTitle.value = '';
      draftBody.value = '';
      draftCategoryId.value = '';
      composing.value = false;
      await loadPosts();
    } catch (e) {
      composeError.value = (e as Error).message;
    } finally {
      posting.value = false;
    }
  }

  onMounted(async () => {
    try {
      categories.value = await forumCategoriesApi();
    } catch (e) {
      error.value = (e as Error).message;
    }
    await loadPosts();
  });
</script>

<style scoped>
  .forum-page {
    display: flex;
    flex-direction: column;
    gap: 18px;
  }
  .forum-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
  }
  .forum-head h1 {
    margin: 0 0 4px;
    font-size: 28px;
  }
  .forum-sub {
    margin: 0;
    color: var(--si-text-muted);
  }
  .btn-primary {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 0 18px;
    height: 44px;
    border-radius: 999px;
    border: none;
    background: var(--si-primary);
    color: #fff;
    font-weight: 700;
    cursor: pointer;
  }
  .composer {
    display: flex;
    flex-direction: column;
    gap: 12px;
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius, 16px);
    padding: 18px;
  }
  .field {
    display: flex;
    flex-direction: column;
    gap: 6px;
    font-weight: 600;
  }
  .field input,
  .field select,
  .field textarea {
    border: 2px solid var(--si-border);
    border-radius: 10px;
    padding: 10px 12px;
    font: inherit;
  }
  .error-text {
    color: var(--si-danger, #c4503f);
    font-weight: 600;
  }
  .category-pills {
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
  .post-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .post-card {
    display: block;
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius, 16px);
    padding: 16px 18px;
    text-decoration: none;
    color: inherit;
    transition: border-color 0.15s;
  }
  .post-card:hover {
    border-color: var(--si-secondary);
  }
  .post-card-top {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }
  .pin-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    border-radius: 999px;
    background: var(--si-warning, #c98a1e);
    color: #fff;
    font-size: 11px;
    font-weight: 700;
  }
  .cat-tag {
    padding: 2px 10px;
    border-radius: 999px;
    background: var(--si-primary-light);
    color: var(--si-primary);
    font-size: 12px;
    font-weight: 700;
  }
  .post-card h2 {
    margin: 0 0 6px;
    font-size: 18px;
  }
  .post-excerpt {
    margin: 0 0 10px;
    color: var(--si-text-muted);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .post-meta {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 13px;
    color: var(--si-text-muted);
  }
  .post-meta .spacer {
    flex: 1;
  }
</style>
