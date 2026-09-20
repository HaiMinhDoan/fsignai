# SignAI AI service

Service Python (FastAPI) chấm ký hiệu bằng **DTW + exemplar** — Mức A trong
[docs/01-ai-model-research.md](../docs/01-ai-model-research.md): không train model, exemplar sinh tự
động bằng MediaPipe từ chính video từ điển đã nạp.

**Stateless.** Không có CSDL, không biết người dùng là ai, không biết ngưỡng "đạt" là bao nhiêu.
Chỉ Spring Boot gọi nó (bí mật dùng chung `AI_SERVICE_SECRET`, header `X-AI-Secret`). Trình duyệt
không bao giờ gọi thẳng.

```
Trình duyệt ──landmark (JSON)──► Spring Boot ──landmark + exemplar──► ai-service /verify
                                    │  tra ngưỡng, quyết `passed`,            │
                                    ▼  ghi ai_check_results                   ▼
                                  người học ◄────────────── khoảng cách + gợi ý
```

Video của người học **không rời khỏi máy**: MediaPipe chạy trong trình duyệt, chỉ toạ độ landmark
(vài chục KB) được gửi đi.

## Chạy

```powershell
cd ai-service
python -m venv .venv
.venv\Scripts\python -m pip install -r requirements.txt
.venv\Scripts\python scripts\fetch_models.py        # tải 2 model MediaPipe (~13MB), chỉ cần một lần
.venv\Scripts\python -m uvicorn app.main:app --port 8001
```

Biến môi trường: `AI_SERVICE_SECRET` (bỏ trống = không kiểm tra, chỉ nên khi chạy local),
`AI_MODEL_DIR`, `AI_MAX_FRAMES`, `AI_EXTRACT_FPS`, `AI_MAX_CONCURRENCY` (số video trích song song,
mặc định 4). Spring Boot đọc `ai.service.url`, `ai.service.secret` và `ai.exemplar-job.workers`
trong `application.properties` — nên đặt số luồng của job ≤ `AI_MAX_CONCURRENCY`.

## Endpoint

| | |
|---|---|
| `GET /health` | tình trạng + `featureVersion` |
| `POST /extract` | multipart `video` → chuỗi đặc trưng chuẩn hoá (dùng khi sinh exemplar) |
| `POST /verify` | `{aspect, frames[], exemplars[]}` → `{distance, components, hints, ...}` |

Định dạng khung hình (trình duyệt và `/extract` dùng chung):
`{ "pose": [[x,y,z,visibility] × 33] | null, "hands": [[[x,y,z] × 21], ...] }` — toạ độ x, y chuẩn hoá
`[0,1]` theo khung hình **chưa lật gương**; `aspect` = rộng/cao. Bàn tay không gắn nhãn trái/phải
(nhãn của MediaPipe đảo theo việc ảnh có lật hay không) — service tự gán theo vị trí cổ tay của pose.

## Cách hoạt động

1. **Chuẩn hoá** (`features.py`): gốc toạ độ ở giữa hai vai, chia cho độ rộng vai, bỏ đoạn nghỉ đầu/cuối
   (cổ tay chưa giơ lên khỏi y ≈ 1,10 độ rộng vai), nội suy về 32 bước. Vị trí tay lấy từ *pose*
   (bám ổn định), hình dạng ngón từ *Hand Landmarker*.
2. **So khớp** (`matcher.py`): DTW với chi phí = hình tay + vị trí + vận tốc + tay nào đang giơ, băng
   Sakoe-Chiba ±10. Thử cả hai chiều thuận tay (người ký thuận tay trái vẫn khớp mẫu thuận tay phải),
   lấy exemplar gần nhất.
3. **Trả về** khoảng cách cùng thang với `verify_threshold_groups` (0,24 cho từ 1 tay …), điểm từng
   phần và **mã** gợi ý (không phải câu tiếng Việt — đổi cách diễn đạt không phải deploy lại).

## Hiệu chỉnh và giới hạn — đọc trước khi tin điểm số

Hằng số `K_*` trong `app/matcher.py` đo bằng `tools/calibrate.py`. Đo trên **120 video từ điển ngẫu
nhiên**, người học **giả lập** (đổi tốc độ, tỉ lệ, xoay, nhiễu, mất khung tay) với ngưỡng nhóm đã seed
trong CSDL (0,24 cho từ 1 tay, 0,30 cho từ 2 tay):

| | Từ chối oan người học | Nhận nhầm từ khác |
|---|---|---|
| Từ 1 tay (ngưỡng 0,24) | 1,9% | 0,33% |
| Từ 2 tay (ngưỡng 0,30) | 0,0% | 0,94% |

Nhận đúng từ trong 360/360 lượt (so người học giả lập với mọi mẫu). 120/120 video dựng được mẫu,
chất lượng theo dõi thấp nhất 0,76 — MediaPipe không trượt video nào trong bộ dữ liệu này.

Đây vẫn là **cận trên lạc quan**: chưa có người học thật nào được đo. Người học thật khác ở tỉ lệ cơ thể,
cỡ bàn tay, độ nhoè và độ sáng webcam, nên tỉ lệ từ chối oan thực tế sẽ cao hơn. Nút 👍/👎 "chấm như vậy
có đúng không?" ở giao diện là nguồn dữ liệu để chỉnh ngưỡng (`ai_check_feedback`) khi có người dùng thật.

Chưa làm: khẩu hình (khuôn mặt), ký hiệu nhiều bước/câu, nhận diện mở (M2), encoder học (M1).

## Test

```powershell
.venv\Scripts\python -m pytest tests -q          # 22 test, dữ liệu tổng hợp, không cần MediaPipe
```
