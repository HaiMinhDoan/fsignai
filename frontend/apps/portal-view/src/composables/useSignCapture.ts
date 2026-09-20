import type { HandLandmarker, PoseLandmarker } from '@mediapipe/tasks-vision';
import type { LandmarkClip, LandmarkFrame } from '@/api/aiCheck';

/**
 * Chụp chuỗi landmark (thân + hai bàn tay) từ webcam bằng MediaPipe chạy NGAY TRONG TRÌNH DUYỆT.
 *
 * Hình ảnh không bao giờ rời khỏi máy: chỉ toạ độ landmark được gửi đi (vài chục KB) — đó là cam kết
 * riêng tư của sản phẩm dành cho trẻ em. Vì thế WASM và model cũng tự phục vụ ở /mediapipe (xem
 * scripts/setup-mediapipe.mjs) chứ không tải từ CDN của Google.
 *
 * Dùng đúng hai model mà ai-service dùng khi sinh mẫu (Pose lite + Hand), nên phân phối landmark của
 * người học và của mẫu cùng một nguồn.
 */

interface Models {
  pose: PoseLandmarker;
  hand: HandLandmarker;
}

let modelsPromise: Promise<Models> | null = null;

/** Tải model một lần cho cả phiên (~13MB, được trình duyệt cache). Lỗi thì cho phép thử lại. */
export function loadModels(): Promise<Models> {
  if (!modelsPromise) {
    modelsPromise = createModels().catch((e) => {
      modelsPromise = null;
      throw e;
    });
  }
  return modelsPromise;
}

async function createModels(): Promise<Models> {
  const { FilesetResolver, PoseLandmarker, HandLandmarker } = await import('@mediapipe/tasks-vision');
  const fileset = await FilesetResolver.forVisionTasks('/mediapipe/wasm');
  // CPU (WASM) chứ không GPU: ai-service sinh mẫu bằng CPU, và delegate GPU trên máy yếu của học sinh hay
  // tạo được model nhưng hỏng khi suy luận. Kết quả ổn định quan trọng hơn vài mili giây.
  const [pose, hand] = await Promise.all([
    PoseLandmarker.createFromOptions(fileset, {
      baseOptions: { modelAssetPath: '/mediapipe/models/pose_landmarker_lite.task', delegate: 'CPU' },
      runningMode: 'VIDEO',
      numPoses: 1,
    }),
    HandLandmarker.createFromOptions(fileset, {
      baseOptions: { modelAssetPath: '/mediapipe/models/hand_landmarker.task', delegate: 'CPU' },
      runningMode: 'VIDEO',
      numHands: 2,
    }),
  ]);
  return { pose, hand };
}

export interface CaptureStatus {
  frames: number;
  elapsedMs: number;
  /** Số bàn tay đang thấy ở khung mới nhất (0-2) */
  hands: number;
  /** Có thấy phần thân (hai vai) không */
  body: boolean;
}

export interface CaptureHandle {
  /** Dừng và trả chuỗi đã ghi */
  stop(): LandmarkClip;
  /** Bỏ, không trả gì */
  cancel(): void;
}

/** ~15 khung/giây là đủ: ai-service nội suy về 32 bước, và tần số này khớp với lúc trích mẫu */
const TARGET_FPS = 15;
/** Trần số khung gửi lên — server từ chối quá 400 */
const MAX_FRAMES = 300;

const r4 = (n: number) => Math.round(n * 10000) / 10000;

export function startCapture(
  video: HTMLVideoElement,
  models: Models,
  opts: { maxMs: number; onStatus?: (s: CaptureStatus) => void },
): CaptureHandle {
  const frames: LandmarkFrame[] = [];
  const begin = performance.now();
  const interval = 1000 / TARGET_FPS;
  let stopped = false;
  let timer: ReturnType<typeof setTimeout> | undefined;
  let lastVideoTime = -1;
  let hands = 0;
  let body = false;

  const tick = () => {
    if (stopped) return;
    const started = performance.now();

    // Bỏ qua khi chưa có khung mới (video 30fps mà vòng lặp nhanh hơn) hoặc video chưa sẵn sàng
    if (video.readyState >= 2 && video.videoWidth > 0 && video.currentTime !== lastVideoTime) {
      lastVideoTime = video.currentTime;
      try {
        // Timestamp phải tăng dần qua MỌI lần gọi của cùng một landmarker (kể cả các lần ghi trước),
        // performance.now() đảm bảo điều đó.
        const ts = started;
        const p = models.pose.detectForVideo(video, ts);
        const h = models.hand.detectForVideo(video, ts);
        const poseLm = p.landmarks[0];
        frames.push({
          pose: poseLm ? poseLm.map((l) => [r4(l.x), r4(l.y), r4(l.z), r4(l.visibility ?? 0)]) : null,
          hands: h.landmarks.slice(0, 2).map((hand) => hand.map((l) => [r4(l.x), r4(l.y), r4(l.z)])),
        });
        hands = Math.min(2, h.landmarks.length);
        body = !!poseLm && (poseLm[11]?.visibility ?? 0) > 0.5 && (poseLm[12]?.visibility ?? 0) > 0.5;
      } catch {
        // Một khung lỗi không nên làm hỏng cả lượt ghi — bỏ khung đó
      }
    }

    const elapsed = performance.now() - begin;
    opts.onStatus?.({ frames: frames.length, elapsedMs: elapsed, hands, body });

    if (elapsed >= opts.maxMs || frames.length >= MAX_FRAMES) {
      // Hết giờ: báo trạng thái cuối rồi để chủ gọi stop()
      stopped = true;
      return;
    }
    // Đo thời gian suy luận thật rồi bù: máy chậm thì chạy liên tục, máy nhanh thì chờ đủ nhịp
    timer = setTimeout(tick, Math.max(0, interval - (performance.now() - started)));
  };

  tick();

  return {
    stop() {
      stopped = true;
      clearTimeout(timer);
      return { aspect: video.videoWidth / video.videoHeight, frames };
    },
    cancel() {
      stopped = true;
      clearTimeout(timer);
    },
  };
}
