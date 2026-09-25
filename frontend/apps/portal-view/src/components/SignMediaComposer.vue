<template>
  <div class="composer-media">
    <div class="tool-row">
      <button
        v-if="coCamera"
        type="button"
        class="tool-btn tool-btn--rec"
        :disabled="dayRoi || dangQuay || dangTai"
        @click="batDauQuay"
      >
        <SiIcon name="camera" :size="18" />
        <span>{{ onlyVideo ? 'Quay tiêu đề bằng ký hiệu' : 'Quay ký hiệu' }}</span>
      </button>

      <button type="button" class="tool-btn" :disabled="dayRoi || dangQuay || dangTai" @click="chonTep">
        <SiIcon name="grid" :size="18" />
        <span>{{ onlyVideo ? 'Tải video lên' : 'Tải ảnh / video lên' }}</span>
      </button>

      <input
        ref="oTep"
        type="file"
        class="sr-only"
        :accept="onlyVideo ? 'video/*' : 'video/*,image/*'"
        :multiple="!onlyVideo"
        @change="nhanTep"
      />

      <span v-if="dangTai" class="tool-note">Đang tải lên…</span>
      <span v-else-if="dayRoi" class="tool-note">Đã đủ {{ max }} tệp</span>
    </div>

    <!-- Khung quay: chỉ dựng khi thật sự quay, để không giữ camera sáng đèn vô cớ -->
    <div v-if="dangQuay || dangChuanBi" class="rec-box">
      <!-- Lật gương: người ký hiệu phải thấy mình như soi gương thì mới làm đúng tay -->
      <video ref="oXemTruoc" class="rec-video" autoplay muted playsinline></video>
      <div class="rec-bar">
        <span class="rec-dot" aria-hidden="true"></span>
        <span class="rec-time">{{ dem }}s / {{ maxSeconds }}s</span>
        <button type="button" class="rec-stop" :disabled="!dangQuay" @click="dungQuay(true)">Xong</button>
        <button type="button" class="rec-cancel" @click="dungQuay(false)">Huỷ</button>
      </div>
    </div>

    <p v-if="loi" class="error-text">{{ loi }}</p>

    <ul v-if="danhSach.length" class="media-list">
      <li v-for="m in danhSach" :key="m.id" class="media-item">
        <img v-if="m.thumbnailUrl" :src="m.thumbnailUrl" :alt="moTa(m)" loading="lazy" />
        <span v-else class="media-blank"><SiIcon name="play" :size="22" /></span>

        <span v-if="m.kind === 'VIDEO'" class="media-badge">
          <SiIcon name="play" :size="12" />
          <span>{{ giay(m.durationMs) }}</span>
        </span>

        <button type="button" class="media-del" :title="`Bỏ ${moTa(m)}`" @click="bo(m)">×</button>
      </li>
    </ul>

    <p v-if="hint" class="tool-hint">{{ hint }}</p>
  </div>
</template>

<script lang="ts" setup>
  /**
   * Ô soạn nội dung bằng NGÔN NGỮ KÝ HIỆU: quay thẳng từ camera hoặc chọn tệp có sẵn,
   * dùng chung cho tiêu đề bài, nội dung bài và bình luận.
   *
   * Tệp được tải lên NGAY khi quay xong chứ không đợi bấm Đăng: người dùng cần xem lại,
   * quay lại nếu chưa ưng, và một video 60 giây tải lên mất vài giây — dồn tất cả vào
   * lúc bấm Đăng là bắt họ ngồi chờ trước một nút đơ.
   *
   * Ảnh đại diện của video do CHÍNH TRÌNH DUYỆT cắt rồi gửi kèm: máy chủ Java không
   * giải mã được video, thiếu ảnh này thì danh sách bài hiện một ô đen.
   */
  import { computed, onBeforeUnmount, ref } from 'vue';
  import SiIcon from '@/components/SiIcon.vue';
  import { forumMediaDeleteApi, forumMediaUploadApi, type ForumMedia } from '@/api/forum';

  defineOptions({ name: 'SignMediaComposer' });

  const props = withDefaults(
    defineProps<{
      modelValue: ForumMedia[];
      max?: number;
      maxSeconds?: number;
      /** Chế độ tiêu đề: đúng MỘT video, không nhận ảnh */
      onlyVideo?: boolean;
      hint?: string;
    }>(),
    { max: 6, maxSeconds: 60, onlyVideo: false, hint: '' },
  );

  const emit = defineEmits<{ (e: 'update:modelValue', v: ForumMedia[]): void }>();

  const danhSach = computed(() => props.modelValue ?? []);
  const gioiHan = computed(() => (props.onlyVideo ? 1 : props.max));
  const dayRoi = computed(() => danhSach.value.length >= gioiHan.value);

  const oTep = ref<HTMLInputElement>();
  const oXemTruoc = ref<HTMLVideoElement>();
  const dangQuay = ref(false);
  const dangChuanBi = ref(false);
  const dangTai = ref(false);
  const dem = ref(0);
  const loi = ref('');

  // Camera chỉ chạy trong ngữ cảnh an toàn (HTTPS hoặc localhost). Vào bằng
  // http://<ip> thì trình duyệt giấu luôn navigator.mediaDevices.
  const coCamera = typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getUserMedia;

  let dong: MediaStream | null = null;
  let mayGhi: MediaRecorder | null = null;
  let manh: Blob[] = [];
  let dongHo: number | undefined;

  function giay(ms?: number) {
    return ms ? `${Math.round(ms / 1000)}s` : '';
  }

  function moTa(m: ForumMedia) {
    return m.kind === 'VIDEO' ? 'video ký hiệu' : 'ảnh';
  }

  function capNhat(v: ForumMedia[]) {
    emit('update:modelValue', v);
  }

  // ---------------------------------------------------------------- quay

  async function batDauQuay() {
    loi.value = '';
    dangChuanBi.value = true;
    try {
      dong = await navigator.mediaDevices.getUserMedia({
        // Không lấy tiếng: đây là nội dung ký hiệu, thu thêm âm thanh chỉ làm
        // tệp nặng gấp đôi và ghi lại những gì nói quanh người dùng
        video: { width: { ideal: 1280 }, height: { ideal: 720 }, facingMode: 'user' },
        audio: false,
      });
    } catch {
      dangChuanBi.value = false;
      loi.value = 'Chưa mở được camera. Bạn cho phép trình duyệt dùng camera rồi thử lại nhé.';
      return;
    }

    if (oXemTruoc.value) {
      oXemTruoc.value.srcObject = dong;
    }
    manh = [];
    mayGhi = new MediaRecorder(dong, { mimeType: kieuGhi() });
    mayGhi.ondataavailable = (e) => e.data.size && manh.push(e.data);
    mayGhi.start();

    dangChuanBi.value = false;
    dangQuay.value = true;
    dem.value = 0;
    dongHo = window.setInterval(() => {
      dem.value += 1;
      if (dem.value >= props.maxSeconds) dungQuay(true);
    }, 1000);
  }

  /** Chrome và Firefox không cùng bộ codec — chọn cái đầu tiên máy hiểu */
  function kieuGhi() {
    const ungVien = ['video/webm;codecs=vp9', 'video/webm;codecs=vp8', 'video/webm', 'video/mp4'];
    return ungVien.find((k) => MediaRecorder.isTypeSupported(k)) ?? '';
  }

  async function dungQuay(giuLai: boolean) {
    window.clearInterval(dongHo);
    const soGiay = dem.value;
    dangQuay.value = false;

    const xong = new Promise<Blob | null>((resolve) => {
      if (!mayGhi || mayGhi.state === 'inactive') return resolve(null);
      mayGhi.onstop = () => resolve(manh.length ? new Blob(manh, { type: manh[0].type }) : null);
      mayGhi.stop();
    });
    const blob = await xong;
    dongCamera();

    if (!giuLai || !blob) return;
    const duoi = blob.type.includes('mp4') ? 'mp4' : 'webm';
    const tep = new File([blob], `ky-hieu-${Date.now()}.${duoi}`, { type: blob.type });
    await taiLen(tep, 'WEBCAM_RECORDED', Math.max(1, soGiay) * 1000);
  }

  function dongCamera() {
    dong?.getTracks().forEach((t) => t.stop());
    dong = null;
    mayGhi = null;
    if (oXemTruoc.value) oXemTruoc.value.srcObject = null;
  }

  // ---------------------------------------------------------------- chọn tệp

  function chonTep() {
    loi.value = '';
    oTep.value?.click();
  }

  async function nhanTep(e: Event) {
    const input = e.target as HTMLInputElement;
    const tep = [...(input.files ?? [])];
    input.value = '';
    for (const t of tep) {
      if (dayRoi.value) {
        loi.value = `Chỉ gắn được tối đa ${gioiHan.value} tệp`;
        break;
      }
      if (t.type.startsWith('video/')) {
        const so = await doDaiVideo(t);
        if (so && so > props.maxSeconds + 1) {
          loi.value = `Video "${t.name}" dài ${Math.round(so)} giây, quá ${props.maxSeconds} giây cho phép`;
          continue;
        }
        await taiLen(t, 'FILE_UPLOAD', so ? Math.round(so * 1000) : undefined);
      } else if (t.type.startsWith('image/')) {
        await taiLen(t, 'FILE_UPLOAD');
      } else {
        loi.value = 'Chỉ nhận video hoặc ảnh';
      }
    }
  }

  function doDaiVideo(tep: File): Promise<number | null> {
    return new Promise((resolve) => {
      const v = document.createElement('video');
      v.preload = 'metadata';
      v.onloadedmetadata = () => {
        const d = Number.isFinite(v.duration) ? v.duration : null;
        URL.revokeObjectURL(v.src);
        resolve(d);
      };
      v.onerror = () => resolve(null);
      v.src = URL.createObjectURL(tep);
    });
  }

  /** Cắt một khung hình làm ảnh đại diện. Trả null nếu trình duyệt không cho vẽ. */
  function catKhungHinh(tep: File): Promise<Blob | null> {
    return new Promise((resolve) => {
      const v = document.createElement('video');
      v.preload = 'metadata';
      v.muted = true;
      v.playsInline = true;
      const xong = (b: Blob | null) => {
        URL.revokeObjectURL(v.src);
        resolve(b);
      };
      v.onloadeddata = () => {
        // Nhảy qua khung đầu: nhiều video mở màn bằng một khung tối thui
        v.currentTime = Math.min(0.4, (v.duration || 1) / 3);
      };
      v.onseeked = () => {
        try {
          const c = document.createElement('canvas');
          c.width = v.videoWidth || 640;
          c.height = v.videoHeight || 480;
          c.getContext('2d')?.drawImage(v, 0, 0, c.width, c.height);
          c.toBlob((b) => xong(b), 'image/jpeg', 0.82);
        } catch {
          xong(null);
        }
      };
      v.onerror = () => xong(null);
      v.src = URL.createObjectURL(tep);
    });
  }

  async function taiLen(tep: File, nguon: 'WEBCAM_RECORDED' | 'FILE_UPLOAD', durationMs?: number) {
    dangTai.value = true;
    loi.value = '';
    try {
      const laVideo = tep.type.startsWith('video/');
      const poster = laVideo ? await catKhungHinh(tep) : undefined;
      const m = await forumMediaUploadApi(tep, {
        poster: poster ?? undefined,
        source: nguon,
        durationMs,
      });
      capNhat(props.onlyVideo ? [m] : [...danhSach.value, m]);
    } catch (e) {
      loi.value = (e as Error).message || 'Tải tệp lên không được, thử lại nhé';
    } finally {
      dangTai.value = false;
    }
  }

  async function bo(m: ForumMedia) {
    capNhat(danhSach.value.filter((x) => x.id !== m.id));
    try {
      await forumMediaDeleteApi(m.id);
    } catch {
      // Tệp đã gắn vào bài cũ hoặc mạng lỗi: bỏ khỏi danh sách là đủ, dọn rác
      // phía máy chủ lo — không dựng cảnh báo cho việc người dùng không sửa được
    }
  }

  onBeforeUnmount(() => {
    window.clearInterval(dongHo);
    dongCamera();
  });
</script>

<style scoped>
  .composer-media {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .tool-row {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
  .tool-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-height: var(--sk-tap);
    padding: 0 16px;
    border-radius: var(--sk-r-pill);
    border: 2px solid var(--sk-blue-150);
    background: var(--sk-surface);
    color: var(--sk-blue-ink);
    font-family: var(--sk-font-head);
    font-weight: 700;
    cursor: pointer;
  }
  .tool-btn:hover:not(:disabled) {
    background: var(--sk-lavender);
  }
  .tool-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .tool-btn--rec {
    border-color: var(--sk-amber);
    color: var(--sk-amber-ink);
  }
  .tool-note,
  .tool-hint {
    font-size: 13px;
    color: var(--sk-brown);
  }

  .sr-only {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip: rect(0 0 0 0);
  }

  .rec-box {
    position: relative;
    border-radius: var(--sk-r-card);
    overflow: hidden;
    background: var(--sk-stage);
    border: 2px solid var(--sk-stage-border);
  }
  .rec-video {
    display: block;
    width: 100%;
    max-height: 320px;
    object-fit: cover;
    transform: scaleX(-1);
  }
  .rec-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 14px;
    background: var(--sk-surface);
  }
  .rec-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: #c4503f;
    animation: nhay 1s steps(2, end) infinite;
  }
  @keyframes nhay {
    50% {
      opacity: 0.25;
    }
  }
  .rec-time {
    font-family: var(--sk-font-mono);
    font-weight: 700;
  }
  .rec-stop,
  .rec-cancel {
    min-height: 40px;
    padding: 0 18px;
    border-radius: var(--sk-r-pill);
    border: none;
    font-weight: 700;
    cursor: pointer;
  }
  .rec-stop {
    margin-left: auto;
    background: var(--sk-amber);
    color: var(--sk-brown-dark);
  }
  .rec-cancel {
    background: var(--sk-lavender-2);
    color: var(--sk-brown);
  }

  .media-list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    list-style: none;
    margin: 0;
    padding: 0;
  }
  .media-item {
    position: relative;
    width: 104px;
    height: 104px;
    border-radius: 16px;
    overflow: hidden;
    background: var(--sk-stage);
    border: 2px solid var(--sk-blue-150);
  }
  .media-item img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .media-blank {
    display: grid;
    place-items: center;
    width: 100%;
    height: 100%;
    color: var(--sk-brown);
  }
  .media-badge {
    position: absolute;
    left: 6px;
    bottom: 6px;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    border-radius: var(--sk-r-pill);
    background: rgba(17, 28, 45, 0.78);
    color: #fff;
    font-size: 11px;
    font-weight: 700;
  }
  .media-del {
    position: absolute;
    top: 4px;
    right: 4px;
    width: 26px;
    height: 26px;
    border-radius: 50%;
    border: none;
    background: rgba(17, 28, 45, 0.78);
    color: #fff;
    font-size: 17px;
    line-height: 1;
    cursor: pointer;
  }

  .error-text {
    margin: 0;
    color: #c4503f;
    font-size: 14px;
  }
</style>
