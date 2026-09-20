// Chuẩn bị MediaPipe để trình duyệt tự phục vụ, KHÔNG gọi CDN của Google lúc chạy.
//
//   node scripts/setup-mediapipe.mjs      (hoặc: pnpm setup:mediapipe)
//
// Vì sao tự phục vụ: người học là trẻ em, và cam kết trong docs là hình ảnh không rời khỏi máy. Nếu để
// MediaPipe tự tải WASM + model từ storage.googleapis.com thì mỗi lần mở trang chấm điểm lại có một request
// tới bên thứ ba, và trang chấm điểm hỏng khi mất mạng ngoài.
//
// Chạy lại được, bỏ qua file đã có. Kết quả nằm trong public/mediapipe (đã .gitignore, ~20MB).

import { copyFileSync, existsSync, mkdirSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const out = join(root, 'public', 'mediapipe');

const wasmSrc = join(root, 'node_modules', '@mediapipe', 'tasks-vision', 'wasm');
if (!existsSync(wasmSrc)) {
  console.error('Chưa cài @mediapipe/tasks-vision — chạy `pnpm install` trước.');
  process.exit(1);
}
mkdirSync(join(out, 'wasm'), { recursive: true });
for (const f of readdirSync(wasmSrc)) {
  copyFileSync(join(wasmSrc, f), join(out, 'wasm', f));
}
console.log(`đã chép wasm (${readdirSync(wasmSrc).length} file)`);

const BASE = 'https://storage.googleapis.com/mediapipe-models';
const MODELS = {
  'pose_landmarker_lite.task': `${BASE}/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task`,
  'hand_landmarker.task': `${BASE}/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task`,
};

mkdirSync(join(out, 'models'), { recursive: true });
for (const [name, url] of Object.entries(MODELS)) {
  const target = join(out, 'models', name);
  if (existsSync(target) && statSync(target).size > 1_000_000) {
    console.log(`đã có   ${name}`);
    continue;
  }
  console.log(`đang tải ${name} ...`);
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Tải ${name} thất bại: HTTP ${res.status}`);
  writeFileSync(target, Buffer.from(await res.arrayBuffer()));
  console.log(`xong    ${name} (${Math.round(statSync(target).size / 1024)} KB)`);
}
