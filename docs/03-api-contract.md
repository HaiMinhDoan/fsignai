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
```
POST   /ai-check/verify
  { "signId": 42,
    "landmarks": { "fps": 30, "frameCount": 48,
                   "pose": [[x,y,z,visibility], ...],
                   "leftHand": [...], "rightHand": [...] },
    "consentToStore": false }

  → { "score": 87, "passed": true,
      "feedback": { "handshape": 92, "location": 71, "movement": 88,
                    "hints": ["Hình tay chính xác.",
                              "Thử đưa tay lên cao hơn một chút, ngang cằm."] },
      "modelVersion": "verify-v1.2" }
```

Spring Boot chuyển tiếp sang FastAPI, nhận `score`, **tự quyết định `passed`** theo ngưỡng cấu hình
được cho từng từ, rồi ghi `ai_check_results` và cập nhật progress. FastAPI không biết ngưỡng là bao nhiêu.

```
GET    /ai-check/history           ?signId=&page=
GET    /ai-check/signs/{id}/readiness   → từ này đã có exemplar chưa (ẩn nút nếu chưa)
```

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
POST                 /admin/signs/{id}/exemplars/rebuild   → gọi FastAPI /embed
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
- Rate limit `/ai-check/verify`: 30 request/phút/user (Redis).
