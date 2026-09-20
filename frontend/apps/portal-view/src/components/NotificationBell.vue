<template>
  <div class="bell-wrap">
    <button
      type="button"
      class="icon-btn"
      title="Thông báo"
      :aria-expanded="open"
      @click="toggle"
    >
      <SiIcon name="bell" :size="20" />
      <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 9 ? '9+' : unreadCount }}</span>
      <span class="si-visually-hidden">Thông báo</span>
    </button>

    <div v-if="open" class="bell-panel" role="menu">
      <div class="bell-head">
        <strong>Thông báo</strong>
        <button type="button" class="bell-mark-all" @click="markAll">Đánh dấu tất cả đã đọc</button>
      </div>
      <p v-if="loading" class="bell-hint">Đang tải…</p>
      <p v-else-if="items.length === 0" class="bell-hint">Chưa có thông báo nào.</p>
      <ul v-else class="bell-list">
        <li
          v-for="n in items"
          :key="n.id"
          class="bell-item"
          :class="{ 'is-unread': !n.readAt }"
          @click="onClickItem(n)"
        >
          <p class="bell-item-title">{{ n.titleVi }}</p>
          <p v-if="n.bodyVi" class="bell-item-body">{{ n.bodyVi }}</p>
          <span class="bell-item-time">{{ new Date(n.createdAt).toLocaleString('vi-VN') }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, onMounted, onBeforeUnmount } from 'vue';
  import SiIcon from './SiIcon.vue';
  import {
    notificationsApi,
    unreadCountApi,
    markNotificationReadApi,
    markAllNotificationsReadApi,
    type AppNotification,
  } from '@/api/notifications';

  defineOptions({ name: 'NotificationBell' });

  const open = ref(false);
  const loading = ref(false);
  const items = ref<AppNotification[]>([]);
  const unreadCount = ref(0);

  async function refreshCount() {
    try {
      unreadCount.value = (await unreadCountApi()).count;
    } catch {
      // Bo qua - so chua doc chi la tien loi, khong lam gian doan trai nghiem
    }
  }

  async function toggle() {
    open.value = !open.value;
    if (open.value) {
      loading.value = true;
      try {
        items.value = (await notificationsApi()).items;
      } finally {
        loading.value = false;
      }
    }
  }

  async function onClickItem(n: AppNotification) {
    if (!n.readAt) {
      await markNotificationReadApi(n.id);
      n.readAt = new Date().toISOString();
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
    if (n.actionUrl) window.location.href = n.actionUrl;
  }

  async function markAll() {
    await markAllNotificationsReadApi();
    items.value.forEach((n) => (n.readAt = n.readAt ?? new Date().toISOString()));
    unreadCount.value = 0;
  }

  function onDocClick(e: MouseEvent) {
    if (!(e.target as HTMLElement).closest('.bell-wrap')) open.value = false;
  }

  let poll: ReturnType<typeof setInterval> | undefined;

  onMounted(() => {
    refreshCount();
    poll = setInterval(refreshCount, 60000);
    document.addEventListener('click', onDocClick);
  });
  onBeforeUnmount(() => {
    if (poll) clearInterval(poll);
    document.removeEventListener('click', onDocClick);
  });
</script>

<style scoped>
  .bell-wrap {
    position: relative;
  }
  .icon-btn {
    position: relative;
  }
  .bell-badge {
    position: absolute;
    top: -4px;
    right: -4px;
    min-width: 16px;
    height: 16px;
    padding: 0 3px;
    border-radius: 999px;
    background: var(--sk-amber);
    color: #fff;
    font-size: 10px;
    font-weight: 800;
    display: grid;
    place-items: center;
  }
  .bell-panel {
    position: absolute;
    top: calc(100% + 8px);
    right: 0;
    width: 320px;
    max-height: 400px;
    overflow-y: auto;
    background: var(--sk-surface);
    border-radius: 16px;
    box-shadow: var(--sk-shadow-lift);
    padding: 12px;
    z-index: 30;
  }
  .bell-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }
  .bell-mark-all {
    border: none;
    background: none;
    color: var(--sk-blue-ink);
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
  }
  .bell-hint {
    color: var(--sk-brown);
    font-size: 13px;
    margin: 8px 0;
  }
  .bell-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .bell-item {
    padding: 8px 10px;
    border-radius: 10px;
    cursor: pointer;
  }
  .bell-item:hover {
    background: var(--sk-lavender);
  }
  .bell-item.is-unread {
    background: var(--sk-blue-100);
  }
  .bell-item-title {
    margin: 0;
    font-weight: 700;
    font-size: 13px;
  }
  .bell-item-body {
    margin: 2px 0 0;
    font-size: 12px;
    color: var(--sk-brown);
  }
  .bell-item-time {
    display: block;
    margin-top: 4px;
    font-size: 11px;
    color: var(--sk-brown);
    opacity: 0.8;
  }
</style>
