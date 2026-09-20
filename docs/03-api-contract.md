# SignAI — Hợp đồng API giữa 3 service

Ba ranh giới cần định nghĩa rõ:

1. **Vue → Spring Boot** — REST, JWT Bearer. Toàn bộ nghiệp vụ.
2. **Spring Boot → FastAPI** — REST nội bộ, xác thực M2M. Chỉ suy luận ML.
3. **Vue → FastAPI** — WebSocket, **chỉ cho chế độ luyện tập không tính điểm**.

---

## 1. Vue → Spring Boot

Base: `/api/v1`. Mọi response bọc trong envelope thống nhất:

```json
{ "success": true, "data": { }, "error": null, "timestamp": "2026-09-14T10:00:00Z" }
```

```json
{ "success": false, "data": null,
  "error": { "code": "SIGN_NOT_FOUND", "message": "Không tìm thấy từ này", "details": {} } }
```

`error.message` viết sẵn **tiếng Việt** ở backend — frontend hiển thị trực tiếp, không tự map mã lỗi.
Tránh việc mỗi màn hình tự chế một câu thông báo khác nhau cho cùng một lỗi.

### Auth
```
POST   /auth/register              { email, password, fullName, ageRange, userType }
POST   /auth/login                 { email, password }  → { accessToken, refreshToken, user }
POST   /auth/google                { idToken }          → như trên
POST   /auth/refresh               { refreshToken }
POST   /auth/logout
POST   /auth/forgot-password       { email }
POST   /auth/reset-password        { token, newPassword }
POST   /auth/verify-email          { token }
```

### Onboarding & người dùng
```
GET    /onboarding/questions
POST   /onboarding/submit          { learnReason, currentLevel, dailyMinutes, interestedTopics[] }
                                   → { learningPath }
GET    /me                         → profile + settings + streak tóm tắt
PATCH  /me                         { fullName, avatarUrl, address, ageRange, userType }
PATCH  /me/settings                { dailyGoalMinutes, notify*, reminderTime, timezone }
POST   /me/password                { currentPassword, newPassword }
POST   /me/avatar                  (multipart) → { avatarUrl }
```

### Trang chủ & nội dung
```
GET    /home                       → hero, topics nổi bật, learning path, blog mới nhất  (public)
GET    /blog?page=&size=                                                                 (public)
GET    /blog/{slug}                                                                      (public)
GET    /topics                     ?category=SIMPLE_SIGN|COMPLEX_SIGN|SITUATION
GET    /topics/{slug}
GET    /courses                    ?level=
GET    /courses/{slug}             → gồm lessons + tiến độ của user
GET    /lessons/{id}               → lesson_items đã resolve sẵn video URL
POST   /lessons/{id}/start
POST   /lessons/{id}/progress      { lastItemId, progressPercent, timeSpentSeconds }
POST   /lessons/{id}/complete
```

### Từ điển VSL
```
GET    /dictionary/search          ?q=me&level=&topic=&page=&size=
GET    /dictionary/signs/{gloss}   → video, category, level, related signs
GET    /dictionary/signs/{gloss}/videos   ?angle=FRONT|LEFT|RIGHT
POST   /dictionary/signs/{id}/save
DELETE /dictionary/signs/{id}/save
GET    /me/saved-signs             ?page=&size=
```

Response `search` (khớp đúng ví dụ trong tài liệu gốc):
```json
{ "items": [{
    "id": 42, "gloss": "ME", "wordVi": "mẹ", "wordEn": "mother",
    "level": "BEGINNER",
    "category": { "slug": "family", "nameVi": "Gia đình" },
    "primaryVideo": { "url": "https://cdn/...", "thumbnailUrl": "...", "durationMs": 2400, "captionVi": "..." },
    "related": [ { "gloss": "CHA", "wordVi": "cha" }, { "gloss": "BO", "wordVi": "bố" } ]
  }],
  "page": 0, "size": 20, "total": 137 }
```

### Luyện tập
```
GET    /practice/flashcards        ?topicId=&limit=20   → thẻ đến hạn ôn (SRS)
POST   /practice/flashcards/{signId}/review  { result: "KNOWN" | "NEEDS_PRACTICE" }
                                   → { nextDueAt, intervalDays }
GET    /practice/matching          ?topicId=&size=8
GET    /quizzes/{id}               → câu hỏi, KHÔNG kèm đáp án đúng
POST   /quizzes/{id}/attempts      → { attemptId, startedAt }
POST   /quizzes/attempts/{id}/answer   { questionId, selectedOptionIndex }
POST   /quizzes/attempts/{id}/submit   → { score, maxScore, passed, review[] }
GET    /me/quiz-history            ?page=&size=
```

Đáp án đúng **không bao giờ** xuất hiện trong response `GET /quizzes/{id}`. Chấm điểm ở server.

### AI Checking

> **Cập nhật 20.09.2026** — khớp với bản đã cài đặt (Mức A: chỉ luyện tập, không cộng sao). Khác bản nháp cũ:
> landmark gửi theo **khung hình** (không tách `pose/leftHand/rightHand` thành ba mảng), bàn tay **không gắn nhãn
> trái/phải** (nhãn của MediaPipe đảo theo việc ảnh có lật gương hay không — service tự gán theo cổ tay của pose),
> có `aspect`, và dùng Pose + Hand Landmarker chứ không phải Holistic (xem [01-ai-model-research.md](01-ai-model-research.md)).

```
POST   /ai-check/verify                                  (đăng nhập; 30 lượt/phút/người → 429)
  { "signId": "<uuid>",
    "landmarks": {
      "aspect": 1.7778,                     // rộng / cao của khung hình đã quay (chưa lật gương)
      "frames": [                           // 8–400 khung, ~15 khung/giây
        { "pose":  [[x,y,z,visibility] × 33] | null,
          "hands": [[[x,y,z] × 21], ...] }  // 0–2 bàn tay
      ] },
    "context": "PRACTICE" }                 // tuỳ chọn

  → { "resultId": "<uuid>", "signId": "...", "score": 87.4, "passed": true,
      "feedback": { "handshape": 92, "location": 71, "movement": 88,
                    "hints": ["Hình tay chính xác.", "Thử đưa tay lên cao hơn một chút."],
                    "hintCodes": ["HANDSHAPE_OK", "LOCATION_TOO_LOW"] },
      "modelVersion": "verify-dtw-v1", "checkedAt": "..." }
```

Toạ độ x, y chuẩn hoá `[0,1]` theo khung hình **chưa lật gương**. Video của người học **không rời khỏi máy** —
MediaPipe chạy trong trình duyệt, chỉ landmark được gửi (vài chục KB).

Spring Boot chuyển tiếp sang ai-service cùng exemplar của từ, nhận về **khoảng cách** (không phải điểm), **tự tra
ngưỡng và quyết định `passed`**, quy ra điểm rồi ghi `ai_check_results`. ai-service không biết ngưỡng là bao nhiêu.
- `passed ⇔ distance ≤ ngưỡng`; `score = 100 × 0,6^(distance / ngưỡng)` (đúng ngưỡng = 60 điểm).
- Ngưỡng: `verify_thresholds` của từ nếu có, không thì `verify_threshold_groups` theo (unit_type × số tay). Số tay lấy
  `max(signs.hand_count, số tay đo được từ mẫu)` vì cột `hand_count` đang là 1 cho cả kho (mặc định chưa ai nhập).
- Lỗi do người học có lời khuyên tiếng Việt trong `message`: `422` `NO_BODY` (không thấy hai vai), `NO_SIGN`
  (chưa giơ tay); `409` `NO_EXEMPLAR` (từ chưa có mẫu); `429` `RATE_LIMITED`; `503` `AI_SERVICE_DOWN`.
- Mức A: **không cộng sao**; chỉ ghi `ai_check_results` và tính vào chuỗi ngày học (`daily_activity.ai_checks_done`).
- Chưa hỗ trợ `consentToStore` (lưu landmark người học): `consent_to_store` luôn `false`, không lưu file nào.

```
GET    /ai-check/signs/{id}/readiness   → { ready, exemplarCount }   (công khai; ẩn nút nếu ready = false)
GET    /ai-check/history                ?signId=&page=&size=          (của chính mình)
POST   /ai-check/results/{id}/feedback  { verdict: "AGREE" | "DISAGREE", note? }
```

Góp ý 👍/👎 là một dòng cho mỗi (người, kết quả), gửi lại thì cập nhật. `weight_snapshot` chụp lại lúc góp ý:
chuyên gia VSL **đã xác minh** = 5, người học = 1, chuyên gia chưa xác minh = 0 (lưu, kích hoạt khi được duyệt).

### Tiến độ
```
GET    /me/dashboard      → welcome, todayLesson, progress, dailyGoal, streak, recentLessons[]
GET    /me/progress       → courses 2/5, lessons 28/50, quizAverage 87%, learningTime, streak
GET    /me/activity       ?from=&to=   → heatmap theo ngày
GET    /me/achievements
```

### Thông báo
```
GET    /notifications              ?unreadOnly=true&page=
POST   /notifications/{id}/read
POST   /notifications/read-all
POST   /push/subscribe             { endpoint, keys: { p256dh, auth } }
DELETE /push/subscribe
```

### Admin (ROLE_ADMIN)
```
GET|POST|PUT|DELETE  /admin/signs
POST                 /admin/signs/{id}/videos        (multipart)
GET                  /admin/signs/{id}/exemplars                   → các mẫu chấm điểm của từ
POST                 /admin/signs/{id}/exemplars/rebuild           → chạy MediaPipe (ai-service /extract) trên từng video của từ
PATCH                /admin/exemplars/{id}/active?active=          → tắt/bật một mẫu
POST                 /admin/ai/exemplars/build-missing?limit=&retryFailed=   → job nền cho video còn thiếu mẫu
GET                  /admin/ai/exemplars/status                    → tiến độ job + độ phủ toàn kho
GET|POST|PUT|DELETE  /admin/topics | /admin/courses | /admin/lessons | /admin/quizzes
GET                  /admin/users        ?q=&userType=
GET                  /admin/stats
```

---

## 2. Spring Boot → FastAPI

Base: `http://sign-service:8000`. Xác thực: header `X-Internal-Token` (shared secret qua env)
hoặc mTLS ở production. **Không bao giờ expose service này ra Internet.**

### `POST /verify` — chấm một lần thực hiện ký hiệu
```json
{ "signId": 42,
  "landmarks": { "fps": 30, "frameCount": 48, "pose": [...], "leftHand": [...], "rightHand": [...] } }
```
```json
{ "score": 87.3,
  "components": { "handshape": 92.1, "location": 71.4, "movement": 88.0 },
  "hints": ["HAND_SHAPE_OK", "LOCATION_TOO_LOW"],
  "matchedExemplarId": 311,
  "modelVersion": "verify-v1.2",
  "processingMs": 34 }
```

`hints` trả về **mã**, không phải câu tiếng Việt. Spring Boot dịch mã sang câu hiển thị. Như vậy
đổi cách diễn đạt không phải deploy lại service Python, và sau này thêm ngôn ngữ cũng không phải sửa ML.

### `POST /embed` — tạo exemplar từ video mẫu
```json
{ "signId": 42, "videoUrl": "https://minio/...", "signVideoId": 311 }
```
```json
{ "embedding": "base64...", "dim": 256, "landmarkKey": "landmarks/311.npz",
  "qualityScore": 0.93, "modelVersion": "verify-v1.2" }
```

Chạy bất đồng bộ khi admin upload video mới. Nếu `qualityScore` thấp (tay bị che, thiếu frame,
phát hiện không ổn định) thì cảnh báo admin thay vì lặng lẽ thêm một mẫu xấu.

### `POST /recognize` — nhận diện mở (Phase 6)
```json
{ "landmarks": {...}, "topK": 5, "candidateSignIds": [1,2,3] }
```
```json
{ "predictions": [ { "signId": 42, "gloss": "ME", "confidence": 0.81 } ],
  "modelVersion": "classify-v0.9" }
```

`candidateSignIds` thu hẹp không gian tìm kiếm (ví dụ chỉ trong chủ đề đang học) — độ chính xác
tăng đáng kể so với mở toàn bộ 400 lớp.

### `GET /health` · `GET /models`
```json
{ "status": "ok", "modelVersion": "verify-v1.2",
  "exemplarsLoaded": 1240, "signsCovered": 142, "uptimeSeconds": 88213 }
```

---

## 3. Vue → FastAPI (WebSocket, chỉ luyện tập)

`wss://api.signai.vn/ai/ws/practice?token=<short-lived-token>`

Token do Spring Boot cấp, TTL 60 giây, nhúng `signId`. Dùng cho feedback realtime khi người dùng
đang tập — **không dùng cho bài có tính điểm**.

Client gửi mỗi ~100ms:
```json
{ "type": "frame", "t": 1234, "pose": [...], "leftHand": [...], "rightHand": [...] }
```

Server đẩy về:
```json
{ "type": "feedback", "partialScore": 62, "phase": "MOVEMENT",
  "hint": "HANDS_TOO_CLOSE", "trackingQuality": 0.88 }
```
```json
{ "type": "result", "score": 87.3, "components": {...}, "hints": [...] }
```

`trackingQuality` cho phép UI báo "camera không thấy rõ tay bạn" thay vì chấm điểm thấp một cách khó hiểu —
người dùng sẽ tưởng mình làm sai trong khi vấn đề là ánh sáng hoặc góc máy.

---

## 4. Định dạng landmark — chốt một lần, dùng chung

Thống nhất giữa cả ba service. Đổi định dạng sau này sẽ làm hỏng toàn bộ exemplar đã tạo.

```typescript
interface LandmarkSequence {
  fps: number;
  frameCount: number;
  pose:      number[][];  // [frameCount][33 * 4]  x,y,z,visibility
  leftHand:  number[][];  // [frameCount][21 * 3]  x,y,z
  rightHand: number[][];  // [frameCount][21 * 3]
  face?:     number[][];  // [frameCount][10 * 3]  chỉ vùng miệng
}
```

**Quy ước bắt buộc:**
- Toạ độ đã chuẩn hoá về `[0, 1]` theo kích thước khung hình (MediaPipe trả sẵn như vậy).
- Frame không phát hiện được tay → mảng toàn `0`, **không phải `null`**. Giữ độ dài mảng cố định.
- Giữ nguyên `fps` gốc; việc resample về 32 frame do FastAPI làm, không làm ở client.
- Bỏ 468 trong 478 face landmark trước khi gửi. Chỉ giữ ~10 điểm quanh miệng
  (khẩu hình có nghĩa trong VSL). Giảm payload khoảng 95%.
- Giới hạn 150 frame (~5 giây) mỗi lần verify.

Payload sau khi nén gzip: khoảng **20–50 KB** cho một lần thực hiện ký hiệu 2 giây.

---

## 5. Mã lỗi

| Mã | HTTP | Ý nghĩa |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Dữ liệu vào sai định dạng |
| `UNAUTHORIZED` | 401 | Thiếu hoặc hết hạn token |
| `EMAIL_ALREADY_EXISTS` | 409 | Email đã đăng ký |
| `INVALID_CREDENTIALS` | 401 | Sai email hoặc mật khẩu |
| `ONBOARDING_REQUIRED` | 403 | Chưa hoàn tất onboarding |
| `SIGN_NOT_FOUND` | 404 | |
| `QUIZ_ATTEMPT_EXPIRED` | 410 | Quá thời gian làm bài |
| `AI_SERVICE_UNAVAILABLE` | 503 | FastAPI không phản hồi |
| `NO_EXEMPLAR_FOR_SIGN` | 422 | Từ này chưa có mẫu để chấm |
| `LANDMARK_TOO_SHORT` | 422 | Chuỗi dưới 10 frame |
| `TRACKING_QUALITY_TOO_LOW` | 422 | Không thấy rõ tay, yêu cầu làm lại |
| `RATE_LIMITED` | 429 | |

Hai mã cuối là lỗi "người dùng cần làm lại", không phải lỗi hệ thống — UI phải xử lý chúng
bằng hướng dẫn cụ thể (chỉnh ánh sáng, lùi ra xa camera), không phải bằng toast đỏ chung chung.

---

## 6. Nguyên tắc chống gian lận

Mọi thứ có tính điểm phải đi qua Spring Boot:

- `GET /quizzes/{id}` không trả đáp án đúng.
- Chấm quiz ở server, so với `quiz_questions.correct_option_index`.
- Bài kiểm tra có `time_limit_seconds` → server kiểm tra `submitted_at - started_at`.
- Với `AI_PERFORM`: client **không** được gọi thẳng FastAPI; phải qua `POST /ai-check/verify`.
- Token WebSocket TTL 60 giây, gắn với một `signId` cụ thể, dùng một lần.
- Rate limit `/ai-check/verify`: 30 request/phút/user. Hiện là bộ đếm cửa sổ trượt TRONG BỘ NHỚ (đủ cho một instance); nhiều instance thì chuyển sang Redis (`RedisService` đã có) vì mỗi instance sẽ đếm riêng.
