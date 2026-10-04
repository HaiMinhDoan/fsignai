<template>
  <div class="ae">
    <!-- ===== Chọn nguồn ===== -->
    <div v-if="buoc === 'chon'" class="ae-row">
      <button type="button" class="ae-btn ae-btn--main" @click="oTep?.click()">
        <SiIcon name="grid" :size="18" />
        <span>Chọn ảnh hoặc video</span>
      </button>
      <button v-if="coCamera" type="button" class="ae-btn" @click="moCamera">
        <SiIcon name="camera" :size="18" />
        <span>Quay video {{ AVATAR_MAX_SECONDS }} giây</span>
      </button>
      <button v-if="hasAvatar" type="button" class="ae-btn ae-btn--danger" :disabled="dangLuu" @click="goAnh">
        <SiIcon name="close" :size="18" />
        <span>Gỡ ảnh hiện tại</span>
      </button>
      <input
        ref="oTep"
        type="file"
        class="si-visually-hidden"
        accept="image/jpeg,image/png,image/webp,video/*"
        @change="nhanTep"
      />
      <p class="ae-hint">
        Ảnh JPG/PNG/WebP, hoặc video — chỉ giữ tối đa {{ AVATAR_MAX_SECONDS }} giây, phát lặp và không có tiếng.
        Video dài hơn thì bạn chọn đoạn muốn giữ.
      </p>
    </div>

    <!-- ===== Chọn đoạn 5 giây của video dài ===== -->
    <div v-else-if="buoc === 'cat'" class="ae-trim">
      <div class="ae-stage">
        <video
          ref="oXemCat"
          class="ae-trim-video"
          :src="nguonUrl"
          muted
          playsinline
          @loadedmetadata="onTrimMeta"
          @timeupdate="giuTrongDoan"
        ></video>
      </div>
      <label class="ae-range">
        <span>
          Bắt đầu từ giây <strong>{{ batDau.toFixed(1) }}</strong> — giữ đến giây
          <strong>{{ Math.min(batDau + AVATAR_MAX_SECONDS, doDai).toFixed(1) }}</strong>
        </span>
        <input
          v-model.number="batDau"
          type="range"
          min="0"
          :max="Math.max(0, doDai - AVATAR_MAX_SECONDS)"
          step="0.1"
          @input="xemDoan"
        />
      </label>
      <p class="ae-hint">Khung vuông bạn thấy ở trên chính là phần được giữ lại.</p>
      <div class="ae-row">
        <button type="button" class="ae-btn ae-btn--main" @click="catDoan">
          <SiIcon name="check" :size="18" /> <span>Dùng đoạn này</span>
        </button>
        <button type="button" class="ae-btn" @click="xemDoan">
          <SiIcon name="play" :size="18" /> <span>Xem thử</span>
        </button>
        <button type="button" class="ae-btn" @click="lamLai">Huỷ</button>
      </div>
    </div>

    <!-- ===== Quay camera ===== -->
    <div v-else-if="buoc === 'camera'" class="ae-trim">
      <div class="ae-stage">
        <!-- Lật gương chỉ ở khung xem trước, video ghi lại vẫn đúng chiều tay ký hiệu -->
        <video ref="oCamera" class="ae-trim-video ae-mirror" autoplay muted playsinline></video>
        <span v-if="demNguoc > 0" class="ae-countdown" aria-live="assertive">{{ demNguoc }}</span>
      </div>
      <div class="ae-row">
        <button v-if="!dangGhi && demNguoc === 0" type="button" class="ae-btn ae-btn--main" @click="batDauGhi">
          <span class="ae-rec-dot" aria-hidden="true"></span> <span>Bắt đầu quay</span>
        </button>
        <button v-if="dangGhi" type="button" class="ae-btn ae-btn--main" @click="dungGhi?.()">Dừng sớm</button>
        <button type="button" class="ae-btn" @click="lamLai">Huỷ</button>
      </div>
    </div>

    <!-- ===== Đang xử lý ===== -->
    <div v-else-if="buoc === 'xuly'" class="ae-progress" role="status">
      <span>{{ nhanXuLy }}</span>
      <progress :value="tienDo" :max="AVATAR_MAX_SECONDS"></progress>
    </div>

    <!-- ===== Xem lại trước khi lưu ===== -->
    <div v-else-if="buoc === 'xem'" class="ae-review">
      <div class="ae-preview">
        <video v-if="ketQuaVideoUrl" :src="ketQuaVideoUrl" muted loop autoplay playsinline aria-label="Video ảnh đại diện mới"></video>
        <img v-else-if="ketQuaAnhUrl" :src="ketQuaAnhUrl" alt="Ảnh đại diện mới" />
      </div>
      <div class="ae-review-side">
        <p v-if="ketQua?.durationMs" class="ae-hint">
          Video {{ (ketQua.durationMs / 1000).toFixed(1) }} giây · phát lặp lại, không tiếng
        </p>
        <div class="ae-row">
          <button type="button" class="ae-btn ae-btn--main" :disabled="dangLuu" @click="luu">
            <SiIcon name="check" :size="18" />
            <span>{{ dangLuu ? 'Đang lưu…' : 'Lưu ảnh đại diện' }}</span>
          </button>
          <button type="button" class="ae-btn" :disabled="dangLuu" @click="lamLai">Chọn lại</button>
        </div>
      </div>
    </div>

    <p v-if="loi" class="ae-error" role="alert">{{ loi }}</p>
  </div>
</template>

<script lang="ts" setup>
  import { onBeforeUnmount, ref } from 'vue';
  import SiIcon from '@/components/SiIcon.vue';
  import { removeAvatarApi, uploadAvatarApi, type Profile } from '@/api/profile';
  import {
    AVATAR_MAX_SECONDS,
    canProcessVideo,
    clipVideoFile,
    recordFromVideo,
    squareImage,
    videoDuration,
  } from '@/utils/avatarMedia';

  defineOptions({ name: 'AvatarEditor' });

  defineProps<{ hasAvatar?: boolean }>();
  const emit = defineEmits<{ (e: 'saved', p: Profile): void }>();

  type Buoc = 'chon' | 'cat' | 'camera' | 'xuly' | 'xem';
  interface KetQua {
    file: Blob;
    poster?: Blob;
    durationMs?: number;
  }

  // Camera chỉ có trong ngữ cảnh an toàn (HTTPS/localhost) — và cần ghi được canvas để cắt vuông
  const coCamera = typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getUserMedia && canProcessVideo();

  const buoc = ref<Buoc>('chon');
  const loi = ref('');
  const dangLuu = ref(false);
  const tienDo = ref(0);
  const nhanXuLy = ref('');

  const oTep = ref<HTMLInputElement>();
  const oXemCat = ref<HTMLVideoElement>();
  const oCamera = ref<HTMLVideoElement>();

  const nguon = ref<File | null>(null);
  const nguonUrl = ref('');
  const doDai = ref(0);
  const batDau = ref(0);

  const demNguoc = ref(0);
  const dangGhi = ref(false);
  const dungGhi = ref<(() => void) | null>(null);
  let camStream: MediaStream | null = null;
  let huy: AbortController | null = null;

  const ketQua = ref<KetQua | null>(null);
  const ketQuaVideoUrl = ref('');
  const ketQuaAnhUrl = ref('');

  // ------------------------------------------------------------ chọn tệp

  async function nhanTep(e: Event) {
    const input = e.target as HTMLInputElement;
    const tep = input.files?.[0];
    input.value = '';
    if (!tep) return;
    loi.value = '';

    if (tep.type.startsWith('image/')) {
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(tep.type)) {
        loi.value = 'Ảnh đại diện nhận JPG, PNG hoặc WebP. Ảnh động GIF thì đổi sang video nhé.';
        return;
      }
      try {
        datKetQua({ file: await squareImage(tep) });
      } catch (err) {
        loi.value = (err as Error).message;
      }
      return;
    }

    if (!tep.type.startsWith('video/')) {
      loi.value = 'Chỉ nhận ảnh hoặc video';
      return;
    }
    const giay = await videoDuration(tep);
    if (!giay) {
      loi.value = 'Không đọc được video này. Thử tệp MP4 hoặc WebM khác nhé.';
      return;
    }

    if (!canProcessVideo()) {
      // Trình duyệt quá cũ để cắt video: chỉ nhận video vốn đã ngắn
      if (giay > AVATAR_MAX_SECONDS + 0.3) {
        loi.value = `Trình duyệt này không cắt được video. Hãy chọn video dài tối đa ${AVATAR_MAX_SECONDS} giây.`;
        return;
      }
      const poster = await squareImageFromVideo(tep);
      if (!poster) {
        loi.value = 'Không lấy được khung hình đầu của video này';
        return;
      }
      datKetQua({ file: tep, poster, durationMs: Math.round(giay * 1000) });
      return;
    }

    nguon.value = tep;
    doDai.value = giay;
    batDau.value = 0;
    if (giay <= AVATAR_MAX_SECONDS + 0.3) {
      // Đã ngắn sẵn: vẫn chạy qua bước cắt để thu về ô vuông nhỏ, nhẹ
      await catDoan();
    } else {
      nguonUrl.value = URL.createObjectURL(tep);
      buoc.value = 'cat';
    }
  }

  /** Lấy khung hình đầu (vuông) của video cho trình duyệt không ghi được canvas */
  async function squareImageFromVideo(tep: File): Promise<Blob | null> {
    const url = URL.createObjectURL(tep);
    try {
      const v = document.createElement('video');
      v.muted = true;
      v.playsInline = true;
      v.src = url;
      await new Promise<void>((resolve, reject) => {
        v.onloadeddata = () => resolve();
        v.onerror = () => reject();
      });
      const c = document.createElement('canvas');
      const canh = Math.min(v.videoWidth, v.videoHeight);
      c.width = c.height = Math.min(512, canh);
      c.getContext('2d')!.drawImage(v, (v.videoWidth - canh) / 2, (v.videoHeight - canh) / 2, canh, canh, 0, 0, c.width, c.height);
      return await new Promise((resolve) => c.toBlob((b) => resolve(b), 'image/jpeg', 0.86));
    } catch {
      return null;
    } finally {
      URL.revokeObjectURL(url);
    }
  }

  // ------------------------------------------------------------ cắt đoạn

  function onTrimMeta() {
    xemDoan();
  }

  function xemDoan() {
    const v = oXemCat.value;
    if (!v) return;
    v.currentTime = batDau.value;
    v.play().catch(() => undefined);
  }

  /** Xem thử: chỉ phát trong đoạn đã chọn, hết đoạn thì quay lại đầu đoạn */
  function giuTrongDoan() {
    const v = oXemCat.value;
    if (v && v.currentTime >= batDau.value + AVATAR_MAX_SECONDS) v.currentTime = batDau.value;
  }

  async function catDoan() {
    if (!nguon.value) return;
    oXemCat.value?.pause();
    loi.value = '';
    buoc.value = 'xuly';
    nhanXuLy.value = 'Đang cắt video…';
    tienDo.value = 0;
    huy = new AbortController();
    try {
      const kq = await clipVideoFile(nguon.value, batDau.value, {
        onProgress: (s) => (tienDo.value = s),
        signal: huy.signal,
      });
      datKetQua({ file: kq.video, poster: kq.poster, durationMs: kq.durationMs });
    } catch (err) {
      if ((err as DOMException).name === 'AbortError') return;
      loi.value = (err as Error).message || 'Cắt video không được, thử lại nhé';
      lamLai();
    }
  }

  // ------------------------------------------------------------ camera

  async function moCamera() {
    loi.value = '';
    try {
      camStream = await navigator.mediaDevices.getUserMedia({
        // Không lấy tiếng: ảnh đại diện luôn phát không tiếng
        video: { width: { ideal: 720 }, height: { ideal: 720 }, facingMode: 'user' },
        audio: false,
      });
    } catch {
      loi.value = 'Chưa mở được camera. Bạn cho phép trình duyệt dùng camera rồi thử lại nhé.';
      return;
    }
    buoc.value = 'camera';
    // Chờ khung <video> được dựng ra rồi mới gắn luồng
    requestAnimationFrame(() => {
      if (oCamera.value && camStream) {
        oCamera.value.srcObject = camStream;
        oCamera.value.play().catch(() => undefined);
      }
    });
  }

  async function batDauGhi() {
    // Đếm ngược 3 giây để kịp chuẩn bị tay — với ký hiệu tên riêng, giây đầu tiên là quan trọng nhất
    for (let i = 3; i > 0; i--) {
      demNguoc.value = i;
      await new Promise((r) => setTimeout(r, 1000));
      if (buoc.value !== 'camera') return;
    }
    demNguoc.value = 0;
    const el = oCamera.value;
    if (!el) return;
    dangGhi.value = true;
    huy = new AbortController();
    try {
      const kq = await recordFromVideo(el, {
        onStart: (stop) => (dungGhi.value = stop),
        signal: huy.signal,
      });
      tatCamera();
      datKetQua({ file: kq.video, poster: kq.poster, durationMs: kq.durationMs });
    } catch (err) {
      if ((err as DOMException).name !== 'AbortError') {
        loi.value = (err as Error).message || 'Quay không được, thử lại nhé';
      }
    } finally {
      dangGhi.value = false;
      dungGhi.value = null;
    }
  }

  function tatCamera() {
    camStream?.getTracks().forEach((t) => t.stop());
    camStream = null;
    if (oCamera.value) oCamera.value.srcObject = null;
  }

  // ------------------------------------------------------------ xem lại & lưu

  function datKetQua(kq: KetQua) {
    donUrlKetQua();
    ketQua.value = kq;
    if (kq.file.type.startsWith('video/')) {
      ketQuaVideoUrl.value = URL.createObjectURL(kq.file);
    } else {
      ketQuaAnhUrl.value = URL.createObjectURL(kq.file);
    }
    buoc.value = 'xem';
  }

  async function luu() {
    if (!ketQua.value) return;
    dangLuu.value = true;
    loi.value = '';
    try {
      const p = await uploadAvatarApi(ketQua.value.file, {
        poster: ketQua.value.poster,
        durationMs: ketQua.value.durationMs,
      });
      emit('saved', p);
      lamLai();
    } catch (err) {
      loi.value = (err as Error).message || 'Lưu ảnh đại diện không được, thử lại nhé';
    } finally {
      dangLuu.value = false;
    }
  }

  async function goAnh() {
    if (!window.confirm('Gỡ ảnh đại diện hiện tại?')) return;
    dangLuu.value = true;
    loi.value = '';
    try {
      emit('saved', await removeAvatarApi());
    } catch (err) {
      loi.value = (err as Error).message;
    } finally {
      dangLuu.value = false;
    }
  }

  function donUrlKetQua() {
    if (ketQuaVideoUrl.value) URL.revokeObjectURL(ketQuaVideoUrl.value);
    if (ketQuaAnhUrl.value) URL.revokeObjectURL(ketQuaAnhUrl.value);
    ketQuaVideoUrl.value = '';
    ketQuaAnhUrl.value = '';
  }

  function lamLai() {
    huy?.abort();
    huy = null;
    tatCamera();
    demNguoc.value = 0;
    donUrlKetQua();
    ketQua.value = null;
    if (nguonUrl.value) URL.revokeObjectURL(nguonUrl.value);
    nguonUrl.value = '';
    nguon.value = null;
    buoc.value = 'chon';
  }

  onBeforeUnmount(lamLai);
</script>

<style scoped>
  .ae {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .ae-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
  }
  .ae-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-height: var(--sk-tap);
    padding: 0 18px;
    border-radius: var(--sk-r-pill);
    border: 2px solid var(--sk-blue-150);
    background: var(--sk-surface);
    color: var(--sk-blue-ink);
    font-family: var(--sk-font-head);
    font-weight: 700;
    font-size: 15px;
    cursor: pointer;
  }
  .ae-btn:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
  .ae-btn--main {
    background: var(--sk-amber);
    border-color: var(--sk-amber);
    color: var(--sk-brown-dark);
  }
  .ae-btn--danger {
    color: #a33a2b;
    border-color: #f1c7bf;
  }
  .ae-hint {
    margin: 0;
    width: 100%;
    font-size: 14px;
    color: var(--sk-brown);
  }
  .ae-error {
    margin: 0;
    color: #a33a2b;
    font-weight: 700;
  }

  .ae-trim {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  /* Khung VUÔNG + object-fit: cover = đúng phần vuông chính giữa mà bước cắt giữ lại.
     Thấy gì được nấy, không cần vẽ thêm ô đánh dấu. */
  .ae-stage {
    position: relative;
    width: min(100%, 320px);
    aspect-ratio: 1;
    border-radius: 24px;
    overflow: hidden;
    background: var(--sk-stage);
    border: 2px solid var(--sk-stage-border);
  }
  .ae-trim-video {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
  .ae-mirror {
    transform: scaleX(-1);
  }
  .ae-countdown {
    position: absolute;
    inset: 0;
    display: grid;
    place-items: center;
    font-family: var(--sk-font-head);
    font-size: 96px;
    font-weight: 700;
    color: #fff;
    text-shadow: 0 4px 16px rgba(0, 0, 0, 0.5);
  }
  .ae-rec-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: #d93025;
  }
  .ae-range {
    display: flex;
    flex-direction: column;
    gap: 6px;
    width: min(100%, 320px);
    font-size: 14px;
    color: var(--sk-brown);
  }
  .ae-range input {
    width: 100%;
    min-height: 32px;
  }

  .ae-progress {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: min(100%, 420px);
    font-weight: 700;
    color: var(--sk-blue-ink);
  }
  .ae-progress progress {
    width: 100%;
    height: 12px;
  }

  .ae-review {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 20px;
  }
  .ae-preview {
    width: 140px;
    height: 140px;
    border-radius: 50%;
    overflow: hidden;
    background: var(--sk-stage);
    box-shadow: var(--sk-shadow-card);
  }
  .ae-preview video,
  .ae-preview img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
  .ae-review-side {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
</style>
