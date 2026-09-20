<template>
  <section class="post-page">
    <RouterLink to="/dien-dan" class="back-link"><SiIcon name="back" :size="18" /> Về Diễn Đàn</RouterLink>

    <p v-if="loading" class="hint">Đang tải bài viết…</p>
    <p v-else-if="error" class="error-text" role="alert">{{ error }}</p>

    <template v-else-if="post">
      <article class="post-detail">
        <div class="post-top">
          <span class="cat-tag">{{ post.categoryNameVi }}</span>
          <span v-if="post.isPinned" class="pin-badge"><SiIcon name="star" :size="14" /> Ghim</span>
        </div>
        <h1>{{ post.titleVi }}</h1>
        <div class="post-meta">
          <span>{{ post.authorName }}</span>
          <span>·</span>
          <span>{{ new Date(post.createdAt).toLocaleString('vi-VN') }}</span>
        </div>
        <p class="post-body">{{ post.bodyMd }}</p>

        <div class="post-actions">
          <button type="button" class="action-btn" :class="{ 'is-on': post.myReaction }" @click="toggleLike">
            <SiIcon name="star" :size="18" /> {{ post.reactionCount }} Thích
          </button>
          <span class="action-static"><SiIcon name="user" :size="16" /> {{ post.viewCount }} lượt xem</span>

          <template v-if="isOwner">
            <button type="button" class="action-btn" @click="editing = !editing">
              {{ editing ? 'Huỷ sửa' : 'Sửa bài' }}
            </button>
            <button type="button" class="action-btn action-danger" @click="removePost">Gỡ bài</button>
          </template>
          <button v-else type="button" class="action-btn" @click="openReport('POST', post.id)">
            Báo cáo
          </button>
        </div>

        <form v-if="editing" class="composer" @submit.prevent="saveEdit">
          <label class="field">
            <span>Tiêu đề</span>
            <input v-model="editTitle" type="text" required maxlength="255" />
          </label>
          <label class="field">
            <span>Nội dung</span>
            <textarea v-model="editBody" rows="4" required></textarea>
          </label>
          <button class="btn-primary" type="submit" :disabled="saving">
            {{ saving ? 'Đang lưu…' : 'Lưu thay đổi' }}
          </button>
        </form>
      </article>

      <section class="comments">
        <h2>Bình luận ({{ comments.length }})</h2>

        <form class="comment-form" @submit.prevent="submitComment()">
          <textarea v-model="newComment" rows="2" placeholder="Viết bình luận…" required></textarea>
          <button class="btn-primary" type="submit" :disabled="commenting">Gửi</button>
        </form>

        <ul class="comment-list">
          <li v-for="c in rootComments" :key="c.id" class="comment-item">
            <CommentRow
              :comment="c"
              :is-owner="c.authorId === auth.user?.userId"
              @like="likeComment(c.id)"
              @reply="replyTo = replyTo === c.id ? null : c.id"
              @delete="removeComment(c.id)"
              @report="openReport('COMMENT', c.id)"
            />

            <form v-if="replyTo === c.id" class="comment-form comment-form--reply" @submit.prevent="submitComment(c.id)">
              <textarea v-model="replyText" rows="2" placeholder="Trả lời…" required></textarea>
              <button class="btn-primary" type="submit" :disabled="commenting">Gửi</button>
            </form>

            <ul class="comment-replies">
              <li v-for="r in repliesOf(c.id)" :key="r.id">
                <CommentRow
                  :comment="r"
                  :is-owner="r.authorId === auth.user?.userId"
                  @like="likeComment(r.id)"
                  @delete="removeComment(r.id)"
                  @report="openReport('COMMENT', r.id)"
                />
              </li>
            </ul>
          </li>
        </ul>
      </section>
    </template>

    <div v-if="reportTarget" class="report-modal" role="dialog" aria-label="Báo cáo vi phạm">
      <div class="report-card">
        <h3>Báo cáo nội dung này</h3>
        <label class="field">
          <span>Lý do</span>
          <select v-model="reportReason">
            <option value="SPAM">Spam / quảng cáo</option>
            <option value="ABUSE">Ngôn từ xúc phạm</option>
            <option value="WRONG_SIGN">Ký hiệu sai</option>
            <option value="OFF_TOPIC">Lạc chủ đề</option>
            <option value="SENSITIVE">Nội dung nhạy cảm</option>
            <option value="OTHER">Khác</option>
          </select>
        </label>
        <label class="field">
          <span>Ghi chú (không bắt buộc)</span>
          <textarea v-model="reportNote" rows="2"></textarea>
        </label>
        <div class="report-actions">
          <button type="button" class="action-btn" @click="reportTarget = null">Huỷ</button>
          <button type="button" class="btn-primary" :disabled="reporting" @click="submitReport">
            {{ reporting ? 'Đang gửi…' : 'Gửi báo cáo' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted, defineComponent, h } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { useAuthStore } from '@/stores/auth';
  import SiIcon from '@/components/SiIcon.vue';
  import {
    forumPostDetailApi,
    forumPostUpdateApi,
    forumPostDeleteApi,
    forumPostLikeApi,
    forumCommentsApi,
    forumCommentCreateApi,
    forumCommentDeleteApi,
    forumCommentLikeApi,
    forumReportApi,
    type ForumPost,
    type ForumComment,
    type ReportReason,
  } from '@/api/forum';

  defineOptions({ name: 'ForumPostView' });

  const CommentRow = defineComponent({
    props: { comment: { type: Object as () => ForumComment, required: true }, isOwner: Boolean },
    emits: ['like', 'reply', 'delete', 'report'],
    setup(props, { emit }) {
      return () =>
        h('div', { class: 'comment-row' }, [
          h('div', { class: 'comment-meta' }, [
            h('strong', props.comment.authorName),
            h('span', new Date(props.comment.createdAt).toLocaleString('vi-VN')),
          ]),
          h('p', { class: 'comment-body' }, props.comment.bodyText),
          h('div', { class: 'comment-actions' }, [
            h(
              'button',
              { type: 'button', class: ['action-btn', 'action-btn--sm', props.comment.myReaction ? 'is-on' : ''], onClick: () => emit('like') },
              `❤️ ${props.comment.reactionCount}`,
            ),
            props.comment.depth === 0
              ? h('button', { type: 'button', class: 'action-btn action-btn--sm', onClick: () => emit('reply') }, 'Trả lời')
              : null,
            props.isOwner
              ? h('button', { type: 'button', class: 'action-btn action-btn--sm action-danger', onClick: () => emit('delete') }, 'Xoá')
              : h('button', { type: 'button', class: 'action-btn action-btn--sm', onClick: () => emit('report') }, 'Báo cáo'),
          ]),
        ]);
    },
  });

  const route = useRoute();
  const router = useRouter();
  const auth = useAuthStore();

  const postId = computed(() => route.params.id as string);
  const post = ref<ForumPost | null>(null);
  const comments = ref<ForumComment[]>([]);
  const loading = ref(true);
  const error = ref('');

  const editing = ref(false);
  const editTitle = ref('');
  const editBody = ref('');
  const saving = ref(false);

  const newComment = ref('');
  const replyTo = ref<string | null>(null);
  const replyText = ref('');
  const commenting = ref(false);

  const reportTarget = ref<{ type: 'POST' | 'COMMENT'; id: string } | null>(null);
  const reportReason = ref<ReportReason>('SPAM');
  const reportNote = ref('');
  const reporting = ref(false);

  const isOwner = computed(() => post.value?.authorId === auth.user?.userId);
  const rootComments = computed(() => comments.value.filter((c) => c.depth === 0));
  const repliesOf = (parentId: string) => comments.value.filter((c) => c.parentId === parentId);

  async function load() {
    loading.value = true;
    error.value = '';
    try {
      const [p, c] = await Promise.all([forumPostDetailApi(postId.value), forumCommentsApi(postId.value)]);
      post.value = p;
      comments.value = c;
      editTitle.value = p.titleVi;
      editBody.value = p.bodyMd;
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  }

  async function toggleLike() {
    if (!post.value) return;
    const res = await forumPostLikeApi(post.value.id);
    post.value.myReaction = res.liked;
    post.value.reactionCount += res.liked ? 1 : -1;
  }

  async function saveEdit() {
    if (!post.value) return;
    saving.value = true;
    try {
      post.value = await forumPostUpdateApi(post.value.id, {
        categoryId: post.value.categoryId,
        titleVi: editTitle.value,
        bodyMd: editBody.value,
      });
      editing.value = false;
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      saving.value = false;
    }
  }

  async function removePost() {
    if (!post.value) return;
    await forumPostDeleteApi(post.value.id);
    router.push('/dien-dan');
  }

  async function submitComment(parentId?: string) {
    const text = parentId ? replyText.value : newComment.value;
    if (!text.trim()) return;
    commenting.value = true;
    try {
      const created = await forumCommentCreateApi(postId.value, text, parentId);
      comments.value.push(created);
      if (post.value) post.value.commentCount += 1;
      if (parentId) {
        replyText.value = '';
        replyTo.value = null;
      } else {
        newComment.value = '';
      }
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      commenting.value = false;
    }
  }

  async function likeComment(id: string) {
    const res = await forumCommentLikeApi(id);
    const c = comments.value.find((x) => x.id === id);
    if (c) {
      c.myReaction = res.liked;
      c.reactionCount += res.liked ? 1 : -1;
    }
  }

  async function removeComment(id: string) {
    await forumCommentDeleteApi(id);
    comments.value = comments.value.filter((c) => c.id !== id && c.parentId !== id);
    if (post.value) post.value.commentCount -= 1;
  }

  function openReport(type: 'POST' | 'COMMENT', id: string) {
    reportTarget.value = { type, id };
    reportReason.value = 'SPAM';
    reportNote.value = '';
  }

  async function submitReport() {
    if (!reportTarget.value) return;
    reporting.value = true;
    try {
      await forumReportApi({
        targetType: reportTarget.value.type,
        targetId: reportTarget.value.id,
        reason: reportReason.value,
        note: reportNote.value || undefined,
      });
      reportTarget.value = null;
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      reporting.value = false;
    }
  }

  onMounted(load);
</script>

<style scoped>
  .post-page {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  .back-link {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: var(--si-text-muted);
    text-decoration: none;
    font-weight: 600;
    width: fit-content;
  }
  .hint,
  .error-text {
    color: var(--si-text-muted);
  }
  .error-text {
    color: var(--si-danger, #c4503f);
    font-weight: 600;
  }
  .post-detail {
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius, 16px);
    padding: 24px;
  }
  .post-top {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }
  .cat-tag {
    padding: 2px 10px;
    border-radius: 999px;
    background: var(--si-primary-light);
    color: var(--si-primary);
    font-size: 12px;
    font-weight: 700;
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
  .post-detail h1 {
    margin: 0 0 8px;
    font-size: 26px;
  }
  .post-meta {
    display: flex;
    gap: 8px;
    color: var(--si-text-muted);
    font-size: 14px;
    margin-bottom: 16px;
  }
  .post-body {
    white-space: pre-wrap;
    line-height: 1.6;
    margin: 0 0 18px;
  }
  .post-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
  .action-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 14px;
    border-radius: 999px;
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    color: var(--si-text-muted);
    font-weight: 700;
    cursor: pointer;
  }
  .action-btn--sm {
    padding: 4px 10px;
    font-size: 12px;
  }
  .action-btn.is-on {
    border-color: var(--si-primary);
    color: var(--si-primary);
    background: var(--si-primary-light);
  }
  .action-danger {
    color: var(--si-danger, #c4503f);
    border-color: var(--si-danger, #c4503f);
  }
  .action-static {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: var(--si-text-muted);
    font-size: 13px;
  }
  .composer {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-top: 16px;
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
  .btn-primary {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 0 18px;
    height: 40px;
    border-radius: 999px;
    border: none;
    background: var(--si-primary);
    color: #fff;
    font-weight: 700;
    cursor: pointer;
    align-self: flex-start;
  }
  .comments h2 {
    font-size: 20px;
  }
  .comment-form {
    display: flex;
    gap: 10px;
    align-items: flex-start;
    margin-bottom: 16px;
  }
  .comment-form textarea {
    flex: 1;
    border: 2px solid var(--si-border);
    border-radius: 10px;
    padding: 10px 12px;
    font: inherit;
  }
  .comment-form--reply {
    margin: 8px 0 8px 24px;
  }
  .comment-list,
  .comment-replies {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .comment-replies {
    margin: 8px 0 0 24px;
    border-left: 2px solid var(--si-border);
    padding-left: 14px;
  }
  .comment-item :deep(.comment-row) {
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: 12px;
    padding: 12px 14px;
  }
  .comment-item :deep(.comment-meta) {
    display: flex;
    gap: 8px;
    font-size: 13px;
    color: var(--si-text-muted);
    margin-bottom: 4px;
  }
  .comment-item :deep(.comment-body) {
    margin: 0 0 8px;
    white-space: pre-wrap;
  }
  .comment-item :deep(.comment-actions) {
    display: flex;
    gap: 8px;
  }
  .report-modal {
    position: fixed;
    inset: 0;
    background: rgba(17, 28, 45, 0.4);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px;
    z-index: 50;
  }
  .report-card {
    background: var(--si-surface);
    border-radius: var(--si-kid-radius, 16px);
    padding: 20px;
    width: 100%;
    max-width: 400px;
    display: flex;
    flex-direction: column;
    gap: 14px;
  }
  .report-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
</style>
