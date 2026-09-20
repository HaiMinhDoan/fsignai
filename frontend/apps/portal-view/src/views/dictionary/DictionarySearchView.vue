<template>
  <!-- ===== Băng tìm kiếm ===== -->
  <section class="finder">
    <div class="finder-intro">
      <MascotWave :size="104" label="đang chỉ vào ô tìm kiếm" />
      <div>
        <h1>Kho từ vựng</h1>
        <p class="finder-sub">
          Gõ tiếng Việt <strong>không cần bỏ dấu</strong> cũng tìm được, ví dụ
          <em>gia dinh</em>.
        </p>
      </div>
    </div>

    <div class="search-box">
      <span class="search-icon" aria-hidden="true"><SiIcon name="search" :size="26" /></span>
      <label class="si-visually-hidden" for="keyword">Tìm từ vựng</label>
      <input
        id="keyword"
        ref="keywordInput"
        v-model="keyword"
        type="search"
        autocomplete="off"
        placeholder="Bé muốn tìm từ gì?"
        @input="onKeywordInput"
      />
      <button v-if="keyword" type="button" class="search-clear" @click="clearKeyword">
        Xoá
      </button>
    </div>

    <!-- Bộ lọc là các nút tròn bấm được, không phải thẻ select: tay bé chưa
         chính xác và trẻ chưa đọc thạo khó thao tác với danh sách xổ xuống -->
    <div class="chip-row" role="group" aria-label="Lọc theo cấp độ">
      <button
        v-for="opt in visibleLevels"
        :key="opt.value"
        type="button"
        class="chip"
        :class="{ 'chip--on': level === opt.value }"
        :aria-pressed="level === opt.value"
        @click="pickLevel(opt.value)"
      >
        <!-- Trạng thái đang chọn có cả dấu ✓ lẫn màu, không chỉ màu (§2.2) -->
        <SiIcon v-if="level === opt.value" name="check" :size="18" />
        <span>{{ opt.label }}</span>
      </button>
    </div>

    <div v-if="visibleTopics.length" class="chip-row" role="group" aria-label="Lọc theo chủ đề">
      <button
        type="button"
        class="chip chip--topic"
        :class="{ 'chip--on': !topicId }"
        :aria-pressed="!topicId"
        @click="pickTopic('')"
      >
        <SiIcon v-if="!topicId" name="check" :size="18" />
        <span>Tất cả chủ đề</span>
      </button>
      <button
        v-for="topic in visibleTopics"
        :key="topic.id"
        type="button"
        class="chip chip--topic"
        :class="{ 'chip--on': topicId === topic.id }"
        :aria-pressed="topicId === topic.id"
        @click="pickTopic(topic.id)"
      >
        <SiIcon v-if="topicId === topic.id" name="check" :size="18" />
        <span>{{ topic.nameVi }}</span>
      </button>
    </div>
  </section>

  <!-- ===== Kết quả ===== -->
  <p v-if="error" class="error" role="alert">{{ error }}</p>

  <p class="result-count" aria-live="polite">
    <template v-if="loading">Đang tìm…</template>
    <template v-else-if="total">
      Tìm thấy <strong>{{ total }}</strong> từ<template v-if="activeTopicName">
        trong chủ đề <strong>{{ activeTopicName }}</strong></template
      >
    </template>
  </p>

  <!-- Khung giữ nguyên tỉ lệ trong lúc tải để lưới không nhảy (§6) -->
  <div v-if="loading" class="grid" aria-hidden="true">
    <div v-for="n in 12" :key="n" class="card card--skeleton">
      <div class="card-frame skeleton"></div>
      <div class="skeleton-line"></div>
      <div class="skeleton-line skeleton-line--short"></div>
    </div>
  </div>

  <div v-else-if="!items.length" class="empty">
    <MascotWave :size="120" layout="column" label="đang tìm giúp">
      Không tìm thấy từ nào cả.
    </MascotWave>
    <p class="empty-hint">
      Thử gõ ngắn hơn, hoặc bỏ bớt bộ lọc đi nhé.
    </p>
    <button type="button" class="btn btn-primary" @click="resetAll">
      <SiIcon name="repeat" :size="22" />
      <span>Xem lại tất cả từ</span>
    </button>
  </div>

  <div v-else class="grid">
    <RouterLink
      v-for="sign in items"
      :key="sign.id"
      :to="{ name: 'dictionary-detail', params: { id: sign.id } }"
      class="card"
    >
      <!-- Khung ảnh trung tính, không viền màu, không gradient: bé phải đọc
           được hình bàn tay (docs/05-design-system.md §2.1) -->
      <div class="card-frame">
        <img
          v-if="sign.thumbnailUrl"
          :src="sign.thumbnailUrl"
          :alt="`Ký hiệu của từ ${sign.wordVi}`"
          loading="lazy"
        />
        <span v-else class="card-frame-empty">
          <SiIcon name="hand" :size="44" />
          <span>Chưa có hình</span>
        </span>

        <span class="card-play" aria-hidden="true"><SiIcon name="play" :size="24" /></span>
      </div>

      <div class="card-body">
        <span class="card-word">{{ sign.wordVi }}</span>
        <span v-if="sign.wordEn" class="card-en">{{ sign.wordEn }}</span>
        <span class="card-meta">
          <span class="pill" :style="{ background: levelColor(sign.level) }">
            {{ levelLabel(sign.level) }}
          </span>
          <span v-if="sign.availableRegions && sign.availableRegions.length > 1" class="pill pill-plain">
            {{ sign.availableRegions.length }} vùng miền
          </span>
        </span>
      </div>
    </RouterLink>
  </div>

  <nav v-if="!loading && totalPages > 1" class="pager" aria-label="Chuyển trang">
    <button type="button" class="pager-btn" :disabled="page === 0" @click="goPage(page - 1)">
      <SiIcon name="back" :size="22" />
      <span>Trước</span>
    </button>
    <span class="pager-now">Trang {{ page + 1 }} / {{ totalPages }}</span>
    <button
      type="button"
      class="pager-btn"
      :disabled="page >= totalPages - 1"
      @click="goPage(page + 1)"
    >
      <span>Sau</span>
      <SiIcon name="next" :size="22" />
    </button>
  </nav>
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { dictionarySearchApi, type SignSummary } from '@/api/dictionary';
  import { topicsApi, type Topic } from '@/api/catalog';
  import MascotWave from '@/components/MascotWave.vue';
  import SiIcon from '@/components/SiIcon.vue';

  defineOptions({ name: 'DictionarySearchView' });

  const route = useRoute();
  const router = useRouter();

  const LEVELS = [
    { value: '', label: 'Mọi cấp độ' },
    { value: 'BEGINNER', label: 'Người mới' },
    { value: 'BASIC', label: 'Cơ bản' },
    { value: 'INTERMEDIATE', label: 'Trung cấp' },
    { value: 'ADVANCED', label: 'Nâng cao' },
  ];

  /** Nền pastel cho nhãn cấp độ. Chữ luôn là --si-text nên tương phản vẫn >7:1 */
  const LEVEL_COLORS: Record<string, string> = {
    BEGINNER: 'var(--si-kid-mint)',
    BASIC: 'var(--si-kid-sky)',
    INTERMEDIATE: 'var(--si-kid-sun)',
    ADVANCED: 'var(--si-kid-lilac)',
  };

  const keyword = ref('');
  const level = ref('');
  const topicId = ref((route.query.topicId as string) || '');
  const page = ref(0);

  const items = ref<SignSummary[]>([]);
  const topics = ref<Topic[]>([]);
  const total = ref(0);
  const totalPages = ref(0);
  const loading = ref(true);
  const error = ref('');

  const keywordInput = ref<HTMLInputElement>();
  const levelCounts = ref<Record<string, number>>({});

  const activeTopicName = computed(
    () => topics.value.find((t) => t.id === topicId.value)?.nameVi ?? '',
  );

  /**
   * Chỉ bày ra những bộ lọc thực sự lọc ra được từ.
   *
   * Kho nhập từ qipedc chưa gán chủ đề và chưa phân cấp độ, nên hôm nay hàng
   * chip chủ đề sẽ ẩn hẳn và chỉ còn một cấp độ. Bày đủ 4 cấp độ + 12 chủ đề mà
   * bấm cái nào cũng ra "không tìm thấy từ nào" thì bé sẽ nghĩ là máy hỏng.
   * Gán chủ đề / phân cấp độ xong là các chip tự hiện ra, không phải sửa code.
   */
  const visibleLevels = computed(() =>
    LEVELS.filter((l) => !l.value || (levelCounts.value[l.value] ?? 0) > 0),
  );
  const visibleTopics = computed(() => topics.value.filter((t) => (t.signCount ?? 0) > 0));

  const levelLabel = (v: string) => LEVELS.find((l) => l.value === v)?.label ?? v;
  const levelColor = (v: string) => LEVEL_COLORS[v] ?? 'var(--si-surface-2)';

  let debounceTimer: number | undefined;

  function onKeywordInput() {
    window.clearTimeout(debounceTimer);
    debounceTimer = window.setTimeout(reload, 350);
  }

  function clearKeyword() {
    keyword.value = '';
    keywordInput.value?.focus();
    reload();
  }

  function pickLevel(value: string) {
    level.value = value;
    reload();
  }

  /** Chủ đề nằm trên URL để bé chia sẻ được đường dẫn và nút Back hoạt động đúng */
  function pickTopic(id: string) {
    topicId.value = id;
    router.replace({ path: '/tu-dien', query: id ? { topicId: id } : {} });
    reload();
  }

  function resetAll() {
    keyword.value = '';
    level.value = '';
    pickTopic('');
  }

  async function reload() {
    page.value = 0;
    await fetchResults();
  }

  async function goPage(next: number) {
    page.value = next;
    await fetchResults();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  async function fetchResults() {
    loading.value = true;
    error.value = '';
    try {
      const result = await dictionarySearchApi({
        keyword: keyword.value || undefined,
        level: level.value || undefined,
        topicId: topicId.value || undefined,
        page: page.value,
        size: 24,
      });
      items.value = result.items;
      total.value = result.total;
      totalPages.value = result.totalPages;
    } catch (e) {
      error.value = (e as Error).message;
      items.value = [];
      total.value = 0;
      totalPages.value = 0;
    } finally {
      loading.value = false;
    }
  }

  // Vào thẳng /tu-dien?topicId=... từ bản đồ hành trình cũng phải lọc đúng
  watch(
    () => route.query.topicId,
    (next) => {
      const id = (next as string) || '';
      if (id !== topicId.value) {
        topicId.value = id;
        reload();
      }
    },
  );

  /** Đếm một lần lúc mở trang, trên toàn kho — không đếm lại theo từ khoá đang gõ */
  async function loadLevelCounts() {
    const real = LEVELS.filter((l) => l.value);
    const results = await Promise.all(
      real.map((l) =>
        dictionarySearchApi({ level: l.value, page: 0, size: 1 })
          .then((r) => r.total)
          .catch(() => 0),
      ),
    );
    const counts: Record<string, number> = {};
    real.forEach((l, i) => (counts[l.value] = results[i] ?? 0));
    levelCounts.value = counts;
  }

  onMounted(async () => {
    fetchResults();
    loadLevelCounts();
    try {
      topics.value = await topicsApi();
    } catch {
      topics.value = []; // mất hàng chip chủ đề thì ô tìm kiếm vẫn dùng được
    }
  });
</script>

<style scoped>
  /* ===== Băng tìm kiếm ===== */
  .finder {
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius-lg);
    padding: 24px 26px 22px;
    box-shadow: var(--si-kid-shadow);
    margin-bottom: 28px;
  }

  .finder-intro {
    display: flex;
    align-items: center;
    gap: 18px;
    margin-bottom: 20px;
  }
  .finder-intro h1 {
    margin: 0;
  }
  .finder-sub {
    margin: 4px 0 0;
    color: var(--si-text-muted);
  }
  .finder-sub em {
    font-style: normal;
    font-family: var(--si-font-mono);
    background: var(--si-surface-2);
    padding: 1px 8px;
    border-radius: 6px;
  }

  .search-box {
    display: flex;
    align-items: center;
    gap: 12px;
    background: var(--si-kid-cream);
    border: 3px solid var(--si-primary);
    border-radius: 999px;
    padding: 0 10px 0 20px;
    height: 64px;
    margin-bottom: 18px;
  }
  .search-box:focus-within {
    box-shadow: 0 0 0 4px var(--si-primary-light);
  }
  .search-icon {
    color: var(--si-primary);
    display: grid;
    place-items: center;
  }
  .search-box input {
    flex: 1;
    border: 0;
    background: transparent;
    font-family: inherit;
    font-size: 20px;
    font-weight: 600;
    color: var(--si-text);
    min-width: 0;
  }
  .search-box input:focus {
    outline: none; /* viền sáng đã nằm ở .search-box:focus-within */
  }
  .search-box input::placeholder {
    color: var(--si-text-muted);
    font-weight: 500;
  }
  .search-clear {
    height: 44px;
    padding: 0 18px;
    border-radius: 999px;
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    color: var(--si-text);
    font-weight: 700;
    cursor: pointer;
  }

  .chip-row {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
  .chip-row + .chip-row {
    margin-top: 12px;
    padding-top: 14px;
    border-top: 2px dashed var(--si-border);
  }

  .chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    min-height: 44px;
    padding: 0 18px;
    border-radius: 999px;
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    color: var(--si-text);
    font-family: inherit;
    font-size: 16px;
    font-weight: 700;
    cursor: pointer;
    transition:
      transform 140ms ease,
      background 140ms ease;
  }
  .chip:hover {
    transform: translateY(-2px);
    background: var(--si-kid-cream);
  }
  .chip--on {
    background: var(--si-kid-sky);
    border-color: var(--si-primary);
  }
  .chip--topic.chip--on {
    background: var(--si-kid-mint);
    border-color: var(--si-success);
  }

  /* ===== Kết quả ===== */
  .result-count {
    margin: 0 0 18px;
    color: var(--si-text-muted);
    font-size: 17px;
    min-height: 24px;
  }
  .error {
    color: var(--si-danger);
    font-weight: 600;
  }

  .grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
    gap: 20px;
  }

  .card {
    display: flex;
    flex-direction: column;
    background: var(--si-surface);
    border: 2px solid var(--si-border);
    border-radius: var(--si-kid-radius);
    overflow: hidden;
    text-decoration: none;
    color: var(--si-text);
    box-shadow: var(--si-kid-shadow);
    transition:
      transform 160ms ease,
      box-shadow 160ms ease,
      border-color 160ms ease;
  }
  a.card:hover {
    transform: translateY(-4px);
    border-color: var(--si-primary);
    box-shadow: var(--si-kid-shadow-lift);
  }

  .card-frame {
    position: relative;
    aspect-ratio: 4 / 3;
    background: var(--si-surface-2);
    display: grid;
    place-items: center;
  }
  .card-frame img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .card-frame-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    color: var(--si-text-muted);
    font-size: 13px;
    font-weight: 600;
  }

  /* Nút play nổi trên góc khung — dấu hiệu "bấm vào là có video xem" */
  .card-play {
    position: absolute;
    right: 10px;
    bottom: 10px;
    display: grid;
    place-items: center;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: var(--si-surface);
    border: 2px solid var(--si-primary);
    color: var(--si-primary);
    opacity: 0.92;
    transition: transform 160ms ease;
  }
  a.card:hover .card-play {
    transform: scale(1.12);
    opacity: 1;
  }

  .card-body {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 14px 16px 16px;
  }
  .card-word {
    font-size: 20px;
    font-weight: 800;
    line-height: 1.35;
  }
  .card-en {
    color: var(--si-text-muted);
    font-size: 14px;
  }
  .card-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 10px;
  }
  .pill {
    display: inline-flex;
    padding: 3px 12px;
    border-radius: 999px;
    font-size: 13px;
    font-weight: 700;
    color: var(--si-text);
  }
  .pill-plain {
    background: var(--si-surface-2);
    border: 1px solid var(--si-border);
  }

  /* ===== Đang tải ===== */
  .card--skeleton {
    padding-bottom: 16px;
    box-shadow: none;
  }
  .skeleton,
  .skeleton-line {
    background: var(--si-surface-2);
    animation: pulse 1.4s ease-in-out infinite;
  }
  .card--skeleton .card-frame {
    width: 100%;
  }
  .skeleton-line {
    height: 16px;
    border-radius: 6px;
    margin: 14px 16px 0;
  }
  .skeleton-line--short {
    width: 45%;
    margin-top: 8px;
  }
  @keyframes pulse {
    0%,
    100% {
      opacity: 1;
    }
    50% {
      opacity: 0.45;
    }
  }

  /* ===== Không tìm thấy gì ===== */
  .empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 14px;
    padding: 48px 20px 60px;
    text-align: center;
  }
  .empty-hint {
    margin: 0;
    color: var(--si-text-muted);
    font-size: 17px;
  }

  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: var(--si-kid-tap);
    padding: 0 22px;
    border-radius: 999px;
    font-weight: 700;
    font-size: 17px;
    font-family: inherit;
    border: 2px solid transparent;
    cursor: pointer;
  }
  .btn-primary {
    background: var(--si-primary);
    color: #fff;
    box-shadow: var(--si-kid-shadow);
  }
  .btn-primary:hover {
    background: var(--si-primary-dark);
  }

  /* ===== Chuyển trang ===== */
  .pager {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 18px;
    margin-top: 36px;
  }
  .pager-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-height: var(--si-kid-tap);
    padding: 0 22px;
    border-radius: 999px;
    border: 2px solid var(--si-border);
    background: var(--si-surface);
    color: var(--si-text);
    font-family: inherit;
    font-size: 16px;
    font-weight: 700;
    cursor: pointer;
    box-shadow: var(--si-kid-shadow);
  }
  .pager-btn:disabled {
    opacity: 0.45;
    cursor: not-allowed;
    box-shadow: none;
  }
  .pager-now {
    font-weight: 700;
    color: var(--si-text-muted);
  }

  @media (max-width: 620px) {
    .finder-intro {
      flex-direction: column;
      text-align: center;
    }
    .search-box {
      height: 56px;
    }
  }
</style>
