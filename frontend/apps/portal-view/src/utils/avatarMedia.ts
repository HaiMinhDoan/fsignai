/**
 * Xử lý ảnh đại diện NGAY TRÊN TRÌNH DUYỆT trước khi tải lên.
 *
 * Máy chủ Java không giải mã được video, nên mọi việc cần "nhìn vào" video đều làm ở đây:
 *  • cắt đúng ≤ 5 giây từ một video dài hơn (người dùng chọn đoạn bắt đầu)
 *  • cắt vuông chính giữa và thu nhỏ — ảnh đại diện chỉ hiện cỡ vài chục điểm ảnh, gửi video
 *    1080p lên là bắt mọi người xem trang cá nhân tải vài MB cho một ô tròn bé xíu
 *  • cắt khung hình đầu làm ảnh tĩnh (poster) cho những chỗ không phát video
 *
 * Cách cắt: phát video (hoặc luồng camera) vào một <canvas>, ghi canvas bằng MediaRecorder.
 * Chạy đúng theo thời gian thực — cắt 5 giây mất 5 giây — nhưng không cần thư viện nào và
 * chạy được trên Chrome, Firefox, Safari 14.1+.
 */

export const AVATAR_MAX_SECONDS = 5;
const VIDEO_SIZE = 360;
const IMAGE_SIZE = 512;

export interface ProcessedVideo {
  video: Blob;
  poster: Blob;
  durationMs: number;
}

/** Codec đầu tiên trình duyệt ghi được — Chrome/Firefox ra webm, Safari ra mp4 */
function kieuGhi(): string {
  const ungVien = ['video/webm;codecs=vp9', 'video/webm;codecs=vp8', 'video/webm', 'video/mp4'];
  return ungVien.find((k) => typeof MediaRecorder !== 'undefined' && MediaRecorder.isTypeSupported(k)) ?? '';
}

export function canProcessVideo(): boolean {
  return (
    typeof MediaRecorder !== 'undefined' &&
    typeof HTMLCanvasElement !== 'undefined' &&
    'captureStream' in HTMLCanvasElement.prototype &&
    kieuGhi() !== ''
  );
}

/**
 * Thời lượng video (giây); null nếu trình duyệt không đọc được.
 *
 * WebM do MediaRecorder của Chrome ghi ra (quay từ web khác, hoặc tải về từ Zalo/Messenger bản
 * web) KHÔNG ghi thời lượng vào đầu tệp: duration = Infinity. Mẹo quen thuộc: tua tới một mốc
 * thật xa, trình duyệt buộc phải quét hết tệp và cập nhật duration thật.
 */
export function videoDuration(file: Blob): Promise<number | null> {
  return new Promise((resolve) => {
    const v = document.createElement('video');
    v.preload = 'metadata';
    v.muted = true;
    let xong = false;
    const ketThuc = (d: number | null) => {
      if (xong) return;
      xong = true;
      window.clearTimeout(henGio);
      URL.revokeObjectURL(v.src);
      resolve(d);
    };
    const henGio = window.setTimeout(() => ketThuc(null), 15000);
    v.onloadedmetadata = () => {
      if (Number.isFinite(v.duration) && v.duration > 0) return ketThuc(v.duration);
      v.ondurationchange = () => {
        if (Number.isFinite(v.duration) && v.duration > 0) ketThuc(v.duration);
      };
      v.currentTime = 1e7;
    };
    v.onerror = () => ketThuc(null);
    v.src = URL.createObjectURL(file);
  });
}

/** Vẽ phần vuông chính giữa của nguồn lên canvas */
function veVuong(ctx: CanvasRenderingContext2D, src: CanvasImageSource, w: number, h: number, size: number) {
  const canh = Math.min(w, h);
  ctx.drawImage(src, (w - canh) / 2, (h - canh) / 2, canh, canh, 0, 0, size, size);
}

function canvasToBlob(c: HTMLCanvasElement, type = 'image/jpeg', quality = 0.86): Promise<Blob> {
  return new Promise((resolve, reject) =>
    c.toBlob((b) => (b ? resolve(b) : reject(new Error('Trình duyệt không xuất được ảnh'))), type, quality),
  );
}

/** Ảnh tĩnh: cắt vuông chính giữa, thu về 512×512 JPEG */
export async function squareImage(file: Blob): Promise<Blob> {
  const url = URL.createObjectURL(file);
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const i = new Image();
      i.onload = () => resolve(i);
      i.onerror = () => reject(new Error('Không đọc được ảnh này'));
      i.src = url;
    });
    const c = document.createElement('canvas');
    c.width = c.height = Math.min(IMAGE_SIZE, img.naturalWidth, img.naturalHeight) || IMAGE_SIZE;
    veVuong(c.getContext('2d')!, img, img.naturalWidth, img.naturalHeight, c.width);
    return await canvasToBlob(c);
  } finally {
    URL.revokeObjectURL(url);
  }
}

/**
 * Ghi tối đa `maxSeconds` giây từ một <video> ĐANG PHÁT (tệp hoặc camera) thành video vuông nhỏ.
 * Dừng sớm khi video hết, khi `signal` bị huỷ, hoặc khi gọi hàm stop trả về qua onStart.
 */
export function recordFromVideo(
  el: HTMLVideoElement,
  opts: {
    maxSeconds?: number;
    onProgress?: (seconds: number) => void;
    onStart?: (stop: () => void) => void;
    signal?: AbortSignal;
  } = {},
): Promise<ProcessedVideo> {
  const maxSeconds = opts.maxSeconds ?? AVATAR_MAX_SECONDS;
  return new Promise((resolve, reject) => {
    const c = document.createElement('canvas');
    c.width = c.height = VIDEO_SIZE;
    const ctx = c.getContext('2d');
    if (!ctx) return reject(new Error('Trình duyệt không hỗ trợ vẽ video'));

    let poster: Promise<Blob> | null = null;
    let batDau = performance.now();
    let khung = 0;
    let xong = false;

    const ve = () => {
      if (xong) return;
      if (el.videoWidth && el.readyState >= 2) {
        veVuong(ctx, el, el.videoWidth, el.videoHeight, VIDEO_SIZE);
        if (!poster) poster = canvasToBlob(c);
      }
      khung = requestAnimationFrame(ve);
    };
    ve();

    // 30 khung/giây là đủ mượt cho một ô tròn; ghi theo canvas nên kích thước luôn vuông 360
    const stream = (c as HTMLCanvasElement & { captureStream(fps?: number): MediaStream }).captureStream(30);
    const mime = kieuGhi();
    const recorder = new MediaRecorder(stream, { mimeType: mime, videoBitsPerSecond: 900_000 });
    const manh: Blob[] = [];
    recorder.ondataavailable = (e) => e.data.size && manh.push(e.data);

    const dongHo = window.setInterval(() => {
      const s = (performance.now() - batDau) / 1000;
      opts.onProgress?.(Math.min(s, maxSeconds));
      if (s >= maxSeconds) dung();
    }, 100);

    const dung = () => {
      if (xong) return;
      xong = true;
      window.clearInterval(dongHo);
      cancelAnimationFrame(khung);
      el.removeEventListener('ended', dung);
      if (recorder.state !== 'inactive') recorder.stop();
    };

    recorder.onstop = async () => {
      stream.getTracks().forEach((t) => t.stop());
      const giay = Math.min((performance.now() - batDau) / 1000, maxSeconds);
      if (opts.signal?.aborted) return reject(new DOMException('Đã huỷ', 'AbortError'));
      if (!manh.length || !poster) return reject(new Error('Chưa ghi được khung hình nào, thử lại nhé'));
      try {
        resolve({
          video: new Blob(manh, { type: (mime.split(';')[0] || manh[0].type) }),
          poster: await poster,
          // Làm tròn lên một chút cho chắc không vượt 5 giây vì sai số đồng hồ
          durationMs: Math.min(Math.round(giay * 1000), maxSeconds * 1000),
        });
      } catch (e) {
        reject(e);
      }
    };

    el.addEventListener('ended', dung);
    opts.signal?.addEventListener('abort', dung);
    opts.onStart?.(dung);
    batDau = performance.now();
    recorder.start(250);
    if (opts.signal?.aborted) dung();
  });
}

/**
 * Cắt đoạn [start, start + 5s] của một tệp video. Video được phát ẩn (không tiếng) ngay trên
 * trang — Safari không chịu phát video không nằm trong DOM.
 */
export async function clipVideoFile(
  file: Blob,
  start: number,
  opts: { onProgress?: (seconds: number) => void; signal?: AbortSignal } = {},
): Promise<ProcessedVideo> {
  const url = URL.createObjectURL(file);
  const v = document.createElement('video');
  v.muted = true;
  v.playsInline = true;
  v.preload = 'auto';
  v.setAttribute('aria-hidden', 'true');
  Object.assign(v.style, { position: 'fixed', left: '-9999px', top: '0', width: '2px', height: '2px', opacity: '0' });
  document.body.appendChild(v);
  try {
    v.src = url;
    await new Promise<void>((resolve, reject) => {
      v.onloadeddata = () => resolve();
      v.onerror = () => reject(new Error('Không đọc được video này. Thử tệp MP4 hoặc WebM khác nhé.'));
    });
    if (start > 0) {
      await new Promise<void>((resolve) => {
        v.onseeked = () => resolve();
        v.currentTime = start;
      });
    }
    await v.play();
    return await recordFromVideo(v, { ...opts, maxSeconds: AVATAR_MAX_SECONDS });
  } finally {
    v.pause();
    v.removeAttribute('src');
    v.load();
    v.remove();
    URL.revokeObjectURL(url);
  }
}
