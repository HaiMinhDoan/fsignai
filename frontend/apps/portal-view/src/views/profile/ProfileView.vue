<template>
  <section class="pf-page">
    <p v-if="loading" class="pf-hint">Đang tải trang cá nhân…</p>
    <p v-else-if="error" class="pf-error" role="alert">{{ error }}</p>

    <template v-else-if="profile">
      <!-- ===== Đầu trang ===== -->
      <header class="pf-hero">
        <UserAvatar
          :name="profile.fullName"
          :avatar-url="profile.avatarUrl"
          :video-url="profile.avatarVideoUrl"
          :size="128"
          class="pf-avatar"
        />
        <div class="pf-hero-body">
          <div class="pf-name-row">
            <h1>{{ profile.fullName }}</h1>
            <span v-if="kindLabel" class="pf-badge">{{ kindLabel }}</span>
            <span v-if="vslLabel" class="pf-badge pf-badge--verified" :title="`Đã xác minh: ${vslLabel}`">
              <SiIcon name="check" :size="14" /> {{ vslLabel }}
            </span>
          </div>
          <p class="pf-joined">Tham gia từ {{ formatDate(profile.joinedAt) }}</p>
          <p v-if="profile.bio" class="pf-bio">{{ profile.bio }}</p>
          <p v-else-if="profile.isMe" class="pf-bio pf-bio--empty">Bạn chưa viết vài dòng giới thiệu.</p>

          <button v-if="profile.isMe" type="button" class="pf-btn" @click="editing = !editing">
            <SiIcon :name="editing ? 'close' : 'sparkles'" :size="18" />
            <span>{{ editing ? 'Đóng chỉnh sửa' : 'Chỉnh sửa trang cá nhân' }}</span>
          </button>
        </div>
      </header>

      <ul class="pf-stats" aria-label="Thành tích">
        <li><strong>{{ profile.stats.posts }}</strong><span>Bài viết</span></li>
        <li><strong>{{ profile.stats.comments }}</strong><span>Bình luận</span></li>
        <li><strong>❤️ {{ profile.stats.reactionsReceived }}</strong><span>Lượt thích nhận được</span></li>
        <li><strong>🔥 {{ profile.stats.streakDays }}</strong><span>Ngày học liên tiếp</span></li>
        <li><strong>⭐ {{ profile.stats.stars }}</strong><span>Sao</span></li>
        <li><strong>{{ profile.stats.level }}</strong><span>Cấp độ</span></li>
      </ul>

      <!-- ===== Chỉnh sửa (chỉ chủ trang) ===== -->
      <section v-if="profile.isMe && editing" class="pf-card pf-edit" aria-labelledby="pf-edit-title">
        <h2 id="pf-edit-title">Chỉnh sửa trang cá nhân</h2>

        <div class="pf-edit-block">
          <h3>Ảnh đại diện</h3>
          <AvatarEditor :has-avatar="!!profile.avatarUrl" @saved="onSaved" />
        </div>

        <form class="pf-edit-block" @submit.prevent="saveInfo">
          <h3>Thông tin</h3>
          <label class="pf-field">
            <span>Tên hiển thị</span>
            <input v-model="draftName" type="text" maxlength="150" required />
          </label>
          <label class="pf-field">
            <span>Giới thiệu bản thân</span>
            <textarea
              v-model="draftBio"
              rows="4"
              maxlength="500"
              placeholder="Bạn đang học ký hiệu vì ai? Bạn thích chủ đề nào?"
            ></textarea>
            <small class="pf-counter" :class="{ 'is-near': draftBio.length > 450 }">{{ draftBio.length }}/500</small>
          </label>
          <p v-if="saveError" class="pf-error" role="alert">{{ saveError }}</p>
          <p v-if="saveOk" class="pf-ok" role="status"><SiIcon name="check" :size="16" /> Đã lưu</p>
          <button type="submit" class="pf-btn pf-btn--main" :disabled="saving">
            {{ saving ? 'Đang lưu…' : 'Lưu thông tin' }}
          </button>
        </form>
      </section>

      <!-- ===== Thẻ: bài viết / giao dịch ===== -->
      <div class="pf-tabs" role="tablist" aria-label="Nội dung trang cá nhân">
        <button
          id="tab-posts"
          type="button"
          role="tab"
          class="pf-tab"
          :class="{ 'is-active': tab === 'posts' }"
          :aria-selected="tab === 'posts'"
          aria-controls="panel-posts"
          @click="tab = 'posts'"
        >
          Bài đã đăng
        </button>
        <button
          v-if="profile.isMe"
          id="tab-payments"
          type="button"
          role="tab"
          class="pf-tab"
          :class="{ 'is-active': tab === 'payments' }"
          :aria-selected="tab === 'payments'"
          aria-controls="panel-payments"
          @click="openPayments"
        >
          Lịch sử giao dịch
        </button>
      </div>

      <!-- Bài viết -->
      <section v-show="tab === 'posts'" id="panel-posts" role="tabpanel" aria-labelledby="tab-posts">
        <p v-if="postsLoading && !posts.length" class="pf-hint">Đang tải bài viết…</p>
        <p v-else-if="postsError" class="pf-error">{{ postsError }}</p>
        <div v-else-if="!posts.length" class="pf-empty">
          <p>{{ profile.isMe ? 'Bạn chưa đăng bài nào.' : 'Chưa có bài viết nào.' }}</p>
          <RouterLink v-if="profile.isMe" to="/dien-dan" class="pf-btn pf-btn--main">Viết bài đầu tiên</RouterLink>
        </div>
        <ul v-else class="pf-posts">
          <li v-for="p in posts" :key="p.id">
            <RouterLink :to="`/dien-dan/${p.id}`" class="pf-post">
              <div class="pf-post-top">
                <span class="pf-cat">{{ p.categoryNameVi }}</span>
                <span v-if="p.status !== 'PUBLISHED'" class="pf-status" :class="`pf-status--${p.status}`">
                  {{ POST_STATUS[p.status] ?? p.status }}
                </span>
                <span class="pf-post-date">{{ formatDate(p.createdAt) }}</span>
              </div>
              <div v-if="!p.titleVi && p.titleMedia" class="pf-post-sign">
                <img v-if="p.titleMedia.thumbnailUrl" :src="p.titleMedia.thumbnailUrl" alt="" loading="lazy" />
                <h3>Bài bằng ký hiệu</h3>
              </div>
              <h3 v-else>{{ p.titleVi || 'Bài không tiêu đề' }}</h3>
              <p v-if="p.bodyMd" class="pf-excerpt">{{ p.bodyMd }}</p>
              <div class="pf-post-meta">
                <span><SiIcon name="user" :size="14" /> {{ p.viewCount }}</span>
                <span>💬 {{ p.commentCount }}</span>
                <span>❤️ {{ p.reactionCount }}</span>
              </div>
            </RouterLink>
          </li>
        </ul>
        <button v-if="postsHasMore" type="button" class="pf-btn pf-more" :disabled="postsLoading" @click="loadPosts(false)">
          {{ postsLoading ? 'Đang tải…' : 'Xem thêm bài' }}
        </button>
      </section>

      <!-- Giao dịch (chỉ chủ trang) -->
      <section
        v-if="profile.isMe"
        v-show="tab === 'payments'"
        id="panel-payments"
        role="tabpanel"
        aria-labelledby="tab-payments"
      >
        <div class="pf-filter" role="group" aria-label="Lọc theo trạng thái">
          <button
            v-for="f in PAY_FILTERS"
            :key="f.value"
            type="button"
            class="pf-chip"
            :class="{ 'is-active': payFilter === f.value }"
            :aria-pressed="payFilter === f.value"
            @click="setPayFilter(f.value)"
          >
            {{ f.label }}
          </button>
        </div>

        <p v-if="payLoading && !payments.length" class="pf-hint">Đang tải lịch sử giao dịch…</p>
        <p v-else-if="payError" class="pf-error">{{ payError }}</p>
        <div v-else-if="!payments.length" class="pf-empty">
          <p>Chưa có giao dịch nào{{ payFilter ? ' ở trạng thái này' : '' }}.</p>
          <RouterLink to="/goi-dich-vu" class="pf-btn pf-btn--main">Xem gói Premium</RouterLink>
        </div>
        <div v-else class="pf-table-wrap">
          <table class="pf-table">
            <caption class="si-visually-hidden">Lịch sử giao dịch</caption>
            <thead>
              <tr>
                <th scope="col">Ngày tạo</th>
                <th scope="col">Nội dung chuyển khoản</th>
                <th scope="col">Gói</th>
                <th scope="col" class="num">Số tiền</th>
                <th scope="col">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in payments" :key="o.id">
                <td data-label="Ngày tạo">{{ formatDateTime(o.createdAt) }}</td>
                <td data-label="Nội dung"><code>{{ o.paymentCode }}</code></td>
                <td data-label="Gói">Premium {{ o.durationDays ? `${o.durationDays} ngày` : '' }}</td>
                <td data-label="Số tiền" class="num">{{ formatMoney(o.paidAmount ?? o.amount) }}</td>
                <td data-label="Trạng thái">
                  <div class="pf-pay-cell">
                    <span class="pf-pay" :class="`pf-pay--${o.status}`">{{ PAY_STATUS[o.status] ?? o.status }}</span>
                    <small v-if="o.status === 'PAID' && o.paidAt" class="pf-pay-when">lúc {{ formatDateTime(o.paidAt) }}</small>
                    <RouterLink v-else-if="o.status === 'PENDING'" to="/goi-dich-vu" class="pf-pay-when">
                      Tiếp tục thanh toán
                    </RouterLink>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <button v-if="payHasMore" type="button" class="pf-btn pf-more" :disabled="payLoading" @click="loadPayments(false)">
          {{ payLoading ? 'Đang tải…' : 'Xem thêm giao dịch' }}
        </button>
      </section>
    </template>
  </section>
</template>

<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';
  import SiIcon from '@/components/SiIcon.vue';
  import UserAvatar from '@/components/UserAvatar.vue';
  import AvatarEditor from '@/components/AvatarEditor.vue';
  import { useAuthStore } from '@/stores/auth';
  import { myProfileApi, profileApi, profilePostsApi, updateProfileApi, type Profile } from '@/api/profile';
  import { myPaymentHistoryApi, type PaymentHistoryItem } from '@/api/payment';
  import type { ForumPost } from '@/api/forum';

  defineOptions({ name: 'ProfileView' });

  /** Không có id = trang của chính mình (/trang-ca-nhan) */
  const props = defineProps<{ id?: string }>();

  const PAGE_SIZE = 10;
  const KIND_LABEL: Record<string, string> = {
    CHILD: 'Học sinh',
    PARENT: 'Phụ huynh',
    TEACHER: 'Giáo viên',
  };
  const VSL_LABEL: Record<string, string> = {
    DEAF_NATIVE: 'Người Điếc bản ngữ',
    TEACHER: 'Giáo viên ký hiệu',
    INTERPRETER: 'Phiên dịch viên',
  };
  const POST_STATUS: Record<string, string> = {
    DRAFT: 'Nháp',
    PENDING_REVIEW: 'Chờ duyệt',
    HIDDEN: 'Đang bị ẩn',
  };
  const PAY_STATUS: Record<string, string> = {
    PAID: 'Đã thanh toán',
    PENDING: 'Chờ chuyển khoản',
    EXPIRED: 'Hết hạn',
    CANCELLED: 'Đã huỷ',
  };
  const PAY_FILTERS = [
    { value: '', label: 'Tất cả' },
    { value: 'PAID', label: 'Đã thanh toán' },
    { value: 'PENDING', label: 'Đang chờ' },
    { value: 'EXPIRED', label: 'Hết hạn' },
  ];

  const auth = useAuthStore();

  const profile = ref<Profile | null>(null);
  const loading = ref(true);
  const error = ref('');

  const editing = ref(false);
  const draftName = ref('');
  const draftBio = ref('');
  const saving = ref(false);
  const saveError = ref('');
  const saveOk = ref(false);

  const tab = ref<'posts' | 'payments'>('posts');

  const posts = ref<ForumPost[]>([]);
  const postsPage = ref(0);
  const postsHasMore = ref(false);
  const postsLoading = ref(false);
  const postsError = ref('');

  const payments = ref<PaymentHistoryItem[]>([]);
  const payPage = ref(0);
  const payHasMore = ref(false);
  const payLoading = ref(false);
  const payError = ref('');
  const payFilter = ref('');
  const payLoaded = ref(false);

  const kindLabel = computed(() => KIND_LABEL[profile.value?.accountKind ?? ''] ?? '');
  const vslLabel = computed(() => VSL_LABEL[profile.value?.verifiedVslRole ?? ''] ?? '');

  async function load() {
    loading.value = true;
    error.value = '';
    editing.value = false;
    tab.value = 'posts';
    payLoaded.value = false;
    payments.value = [];
    try {
      const isMine = !props.id || props.id === auth.user?.userId;
      profile.value = isMine ? await myProfileApi() : await profileApi(props.id!);
      draftName.value = profile.value.fullName;
      draftBio.value = profile.value.bio ?? '';
      document.title = `${profile.value.fullName} · SignAI`;
      await loadPosts(true);
    } catch (e) {
      error.value = (e as Error).message || 'Không tải được trang cá nhân';
    } finally {
      loading.value = false;
    }
  }

  async function loadPosts(reset: boolean) {
    if (!profile.value) return;
    postsLoading.value = true;
    postsError.value = '';
    try {
      const page = reset ? 0 : postsPage.value + 1;
      const res = await profilePostsApi(profile.value.userId, page, PAGE_SIZE);
      posts.value = reset ? res.items : [...posts.value, ...res.items];
      postsPage.value = page;
      postsHasMore.value = page + 1 < res.totalPages;
    } catch (e) {
      postsError.value = (e as Error).message;
    } finally {
      postsLoading.value = false;
    }
  }

  function openPayments() {
    tab.value = 'payments';
    if (!payLoaded.value) loadPayments(true);
  }

  function setPayFilter(v: string) {
    payFilter.value = v;
    loadPayments(true);
  }

  async function loadPayments(reset: boolean) {
    payLoading.value = true;
    payError.value = '';
    try {
      const page = reset ? 0 : payPage.value + 1;
      const res = await myPaymentHistoryApi(page, PAGE_SIZE, payFilter.value || undefined);
      payments.value = reset ? res.items : [...payments.value, ...res.items];
      payPage.value = page;
      payHasMore.value = page + 1 < res.totalPages;
      payLoaded.value = true;
    } catch (e) {
      payError.value = (e as Error).message;
    } finally {
      payLoading.value = false;
    }
  }

  async function saveInfo() {
    saving.value = true;
    saveError.value = '';
    saveOk.value = false;
    try {
      await onSaved(await updateProfileApi({ fullName: draftName.value.trim(), bio: draftBio.value.trim() }));
      saveOk.value = true;
      setTimeout(() => (saveOk.value = false), 2500);
    } catch (e) {
      saveError.value = (e as Error).message;
    } finally {
      saving.value = false;
    }
  }

  /** Lưu xong ảnh hoặc thông tin: cập nhật trang, và nạp lại hồ sơ để header đổi theo ngay */
  async function onSaved(p: Profile) {
    profile.value = p;
    draftName.value = p.fullName;
    draftBio.value = p.bio ?? '';
    // Bài viết trong danh sách mang ảnh đại diện cũ của tác giả
    posts.value = posts.value.map((x) => ({ ...x, authorName: p.fullName, authorAvatarUrl: p.avatarUrl }));
    await auth.refreshProfile().catch(() => undefined);
  }

  function formatDate(iso: string) {
    return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  function formatDateTime(iso: string) {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  function formatMoney(v: number) {
    return `${v.toLocaleString('vi-VN')}đ`;
  }

  watch(() => props.id, load, { immediate: true });
</script>

<style scoped>
  .pf-page {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  .pf-hint {
    color: var(--sk-brown);
  }
  .pf-error {
    margin: 0;
    color: #a33a2b;
    font-weight: 700;
  }
  .pf-ok {
    margin: 0;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: var(--sk-green-ink);
    font-weight: 700;
  }

  .pf-hero {
    display: flex;
    gap: 28px;
    align-items: flex-start;
    padding: 28px;
    border-radius: var(--sk-r-card);
    background: linear-gradient(135deg, var(--sk-lavender) 0%, var(--sk-sky) 100%);
    box-shadow: var(--sk-shadow-card);
  }
  .pf-avatar {
    border: 4px solid var(--sk-surface);
    box-shadow: var(--sk-shadow-card);
  }
  .pf-hero-body {
    display: flex;
    flex-direction: column;
    gap: 8px;
    min-width: 0;
    flex: 1;
  }
  .pf-name-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
  }
  .pf-name-row h1 {
    margin: 0;
    font-family: var(--sk-font-head);
    font-size: 30px;
    color: var(--sk-ink);
    overflow-wrap: anywhere;
  }
  .pf-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 3px 12px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-surface);
    color: var(--sk-blue-ink);
    font-size: 13px;
    font-weight: 700;
  }
  .pf-badge--verified {
    background: var(--sk-mint);
    color: var(--sk-green-ink);
  }
  .pf-joined {
    margin: 0;
    font-size: 14px;
    color: var(--sk-brown);
  }
  .pf-bio {
    margin: 4px 0;
    white-space: pre-line;
    overflow-wrap: anywhere;
    color: var(--sk-ink);
    line-height: 1.55;
  }
  .pf-bio--empty {
    color: var(--sk-brown);
    font-style: italic;
  }

  .pf-btn {
    align-self: flex-start;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: var(--sk-tap);
    padding: 0 20px;
    border-radius: var(--sk-r-pill);
    border: 2px solid var(--sk-blue-150);
    background: var(--sk-surface);
    color: var(--sk-blue-ink);
    font-family: var(--sk-font-head);
    font-weight: 700;
    font-size: 15px;
    text-decoration: none;
    cursor: pointer;
  }
  .pf-btn:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
  .pf-btn--main {
    background: var(--sk-amber);
    border-color: var(--sk-amber);
    color: var(--sk-brown-dark);
  }
  .pf-more {
    align-self: center;
    margin-top: 12px;
  }

  .pf-stats {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 12px;
    margin: 0;
    padding: 0;
    list-style: none;
  }
  .pf-stats li {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    padding: 14px 8px;
    border-radius: 20px;
    background: var(--sk-surface);
    box-shadow: var(--sk-shadow);
    text-align: center;
  }
  .pf-stats strong {
    font-family: var(--sk-font-head);
    font-size: 22px;
    color: var(--sk-ink);
  }
  .pf-stats span {
    font-size: 13px;
    color: var(--sk-brown);
  }

  .pf-card {
    padding: 24px;
    border-radius: var(--sk-r-card);
    background: var(--sk-surface);
    box-shadow: var(--sk-shadow-card);
  }
  .pf-edit h2 {
    margin: 0 0 8px;
    font-family: var(--sk-font-head);
  }
  .pf-edit-block {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 16px 0;
    border-top: 1px solid var(--sk-blue-150);
  }
  .pf-edit-block h3 {
    margin: 0;
    font-family: var(--sk-font-head);
    font-size: 18px;
  }
  .pf-field {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 6px;
    max-width: 560px;
    font-weight: 700;
    color: var(--sk-brown);
  }
  .pf-field input,
  .pf-field textarea {
    min-height: var(--sk-tap);
    padding: 10px 14px;
    border-radius: 16px;
    border: 2px solid var(--sk-blue-150);
    background: var(--sk-surface);
    color: var(--sk-ink);
    font-size: 16px;
    font-weight: 400;
    resize: vertical;
  }
  .pf-counter {
    align-self: flex-end;
    font-weight: 400;
    color: var(--sk-brown);
  }
  .pf-counter.is-near {
    color: var(--sk-amber-ink);
    font-weight: 700;
  }

  .pf-tabs {
    display: flex;
    gap: 6px;
    padding: 4px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-lavender);
    align-self: flex-start;
  }
  .pf-tab {
    min-height: 44px;
    padding: 0 20px;
    border: none;
    border-radius: var(--sk-r-pill);
    background: transparent;
    color: var(--sk-brown);
    font-family: var(--sk-font-head);
    font-weight: 700;
    font-size: 15px;
    cursor: pointer;
  }
  .pf-tab.is-active {
    background: var(--sk-surface);
    color: var(--sk-blue-ink);
    box-shadow: var(--sk-shadow);
  }

  .pf-empty {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding: 24px;
    border-radius: 24px;
    background: var(--sk-lavender);
    color: var(--sk-brown);
  }
  .pf-empty p {
    margin: 0;
  }

  .pf-posts {
    display: grid;
    gap: 12px;
    margin: 0;
    padding: 0;
    list-style: none;
  }
  .pf-post {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 18px 20px;
    border-radius: 24px;
    background: var(--sk-surface);
    box-shadow: var(--sk-shadow);
    color: var(--sk-ink);
    text-decoration: none;
    transition: box-shadow 160ms ease;
  }
  .pf-post:hover {
    box-shadow: var(--sk-shadow-lift);
  }
  .pf-post h3 {
    margin: 0;
    font-family: var(--sk-font-head);
    font-size: 18px;
    overflow-wrap: anywhere;
  }
  .pf-post-top {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    font-size: 13px;
  }
  .pf-cat {
    padding: 2px 10px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-sky);
    color: var(--sk-blue-ink);
    font-weight: 700;
  }
  .pf-status {
    padding: 2px 10px;
    border-radius: var(--sk-r-pill);
    background: var(--sk-peach);
    color: var(--sk-amber-ink);
    font-weight: 700;
  }
  .pf-post-date {
    margin-left: auto;
    color: var(--sk-brown);
  }
  .pf-post-sign {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .pf-post-sign img {
    width: 72px;
    height: 54px;
    object-fit: cover;
    border-radius: 12px;
    background: var(--sk-stage);
  }
  .pf-excerpt {
    margin: 0;
    color: var(--sk-brown);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .pf-post-meta {
    display: flex;
    gap: 14px;
    font-size: 13px;
    color: var(--sk-brown);
  }
  .pf-post-meta span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  .pf-filter {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 14px;
  }
  .pf-chip {
    min-height: 40px;
    padding: 0 16px;
    border-radius: var(--sk-r-pill);
    border: 2px solid var(--sk-blue-150);
    background: var(--sk-surface);
    color: var(--sk-brown);
    font-weight: 700;
    cursor: pointer;
  }
  .pf-chip.is-active {
    border-color: var(--sk-blue-ink);
    background: var(--sk-blue-100);
    color: var(--sk-blue-ink);
  }

  .pf-table-wrap {
    border-radius: 24px;
    background: var(--sk-surface);
    box-shadow: var(--sk-shadow);
    overflow: hidden;
  }
  .pf-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 15px;
  }
  .pf-table th,
  .pf-table td {
    padding: 14px 16px;
    text-align: left;
    vertical-align: top;
    border-bottom: 1px solid var(--sk-lavender-2);
  }
  .pf-table th {
    background: var(--sk-lavender);
    color: var(--sk-brown);
    font-size: 13px;
    font-weight: 700;
  }
  .pf-table tr:last-child td {
    border-bottom: none;
  }
  .pf-table .num {
    text-align: right;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }
  .pf-table code {
    font-family: var(--sk-font-mono);
    font-size: 14px;
  }
  .pf-pay {
    display: inline-block;
    padding: 2px 10px;
    border-radius: var(--sk-r-pill);
    font-size: 13px;
    font-weight: 700;
    white-space: nowrap;
  }
  .pf-pay--PAID {
    background: var(--sk-mint);
    color: var(--sk-green-ink);
  }
  .pf-pay--PENDING {
    background: var(--sk-peach);
    color: var(--sk-amber-ink);
  }
  .pf-pay--EXPIRED,
  .pf-pay--CANCELLED {
    background: var(--sk-lavender-2);
    color: var(--sk-brown);
  }
  .pf-pay-when {
    display: block;
    margin-top: 4px;
    font-size: 13px;
    color: var(--sk-brown);
  }
  a.pf-pay-when {
    color: var(--sk-blue-ink);
    font-weight: 700;
  }

  @media (max-width: 900px) {
    .pf-stats {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
  }

  @media (max-width: 640px) {
    .pf-hero {
      flex-direction: column;
      align-items: center;
      text-align: center;
      padding: 22px 16px;
    }
    .pf-name-row {
      justify-content: center;
    }
    .pf-btn {
      align-self: center;
    }
    .pf-name-row h1 {
      font-size: 24px;
    }
    .pf-stats {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    /* Bảng giao dịch thành thẻ xếp dọc: điện thoại không có chỗ cho 5 cột */
    .pf-table thead {
      display: none;
    }
    .pf-table,
    .pf-table tbody,
    .pf-table tr,
    .pf-table td {
      display: block;
      width: 100%;
    }
    .pf-table tr {
      padding: 10px 0;
      border-bottom: 1px solid var(--sk-lavender-2);
    }
    .pf-table td {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      padding: 6px 16px;
      border: none;
      text-align: right;
    }
    .pf-table td::before {
      content: attr(data-label);
      color: var(--sk-brown);
      font-size: 13px;
      font-weight: 700;
      text-align: left;
    }
    .pf-table .num {
      text-align: right;
    }
    .pf-pay-cell {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
    }
  }
</style>
