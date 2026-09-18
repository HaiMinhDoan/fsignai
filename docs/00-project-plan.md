# SignAI — Kế hoạch dự án

> Nguồn yêu cầu: Google Doc "SIGN AI – WEBSITE FEATURE REQUIREMENT"
> Stack: **Spring Boot (backend)** · **Vue 3 (frontend)** · **Python FastAPI (AI sign service)**
> Cập nhật: 2026-09-15

---

## 1. Hiểu đúng phạm vi sản phẩm

Tài liệu nói rõ một điều quyết định toàn bộ kiến trúc AI:

> "Website tập trung vào **dạy và luyện** ngôn ngữ ký hiệu, **không phải nền tảng dịch trực tiếp** giữa ngôn ngữ ký hiệu và tiếng nói."

Nghĩa là AI trong SignAI **không** phải bài toán "dịch video ký hiệu bất kỳ sang tiếng Việt". Nó là mục
`Cộng đồng + AI checking (?)` trong sơ đồ: **chấm điểm xem người học làm ký hiệu có đúng không**.

Đây là khác biệt cực lớn về độ khó:

| | Bài toán | Độ khó | Dữ liệu cần |
|---|---|---|---|
| ❌ Không làm (MVP) | Nhận diện mở: video → từ nào trong 2000 từ? | SOTA thế giới mới ~60–68% top-1 | Hàng chục nghìn clip có nhãn |
| ✅ Làm (MVP) | Verification: người dùng đang học từ "MẸ", họ làm có đúng không? | Khả thi >90% | **3–10 clip mẫu cho mỗi từ** |

Vì đã biết trước từ mục tiêu, bài toán rút từ *phân loại 400 lớp* xuống *so khớp 1-1*. Đây là đòn bẩy
kỹ thuật quan trọng nhất của dự án — chi tiết ở [01-ai-model-research.md](01-ai-model-research.md).

### Hai nhóm người dùng

1. **Người khiếm thính** — học VSL, củng cố vốn từ, học theo chủ đề, luyện tập, kiểm tra, xem lịch sử.
2. **Người giao tiếp với người khiếm thính** (cha mẹ, anh chị em, bạn bè, người chăm sóc) — học ký hiệu
   cơ bản, học theo tình huống, luyện giao tiếp.

### Ngôn ngữ giao diện

Toàn bộ web **tiếng Việt** (yêu cầu ghi ngay dòng đầu tài liệu). Vẫn giữ i18n từ đầu để sau thêm EN, nhưng
`vi` là locale mặc định và duy nhất khi release.

---

## 2. Kiến trúc hệ thống

```
                    ┌──────────────────────────────────────────┐
                    │  Browser (Vue 3)                         │
                    │  ┌────────────────────────────────────┐  │
                    │  │ MediaPipe Holistic Landmarker      │  │
                    │  │ (WASM/WebGPU) — chạy NGAY TRÊN MÁY │  │
                    │  │ webcam → 553 landmark/frame        │  │
                    │  └──────────────┬─────────────────────┘  │
                    └────────┬────────┼────────────────────────┘
                             │ REST   │ WebSocket (chỉ toạ độ landmark,
                             │        │  KHÔNG gửi video → riêng tư + nhẹ)
                             ▼        ▼
        ┌────────────────────────┐   ┌─────────────────────────────┐
        │ Spring Boot API        │◄─►│ FastAPI — sign-service      │
        │ (Java 21)              │M2M│ (Python 3.11)               │
        │ • Auth / OAuth2 Google │   │ • Chấm ký hiệu (verify)     │
        │ • Bài học, chủ đề      │   │ • Nhận diện mở (phase 2)    │
        │ • Từ điển VSL          │   │ • Trích embedding exemplar  │
        │ • Quiz, flashcard SRS  │   │ • ONNX Runtime              │
        │ • Progress, streak     │   └───────────┬─────────────────┘
        │ • Notification         │               │
        └───┬──────────┬─────────┘               │
            │          │                         │
        ┌───▼───┐  ┌───▼────┐              ┌─────▼──────┐
        │Postgre│  │ Redis  │              │ MinIO / S3 │
        │  SQL  │  │ cache, │              │ video ký   │
        │       │  │ streak │              │ hiệu + CDN │
        └───────┘  └────────┘              └────────────┘
```

### Vì sao MediaPipe chạy ở **trình duyệt**, không ở server

- **Riêng tư** — video webcam của người khiếm thính không rời khỏi máy họ. Chỉ toạ độ khớp được gửi đi.
- **Băng thông** — video 1080p ≈ 2–5 Mbps; landmark stream ≈ **15–40 KB/s**. Rẻ hơn khoảng hai bậc.
- **Chi phí server** — FastAPI chỉ chạy model nhỏ trên vector, không decode video → CPU là đủ cho MVP, chưa cần GPU.
- **Độ trễ** — phản hồi được ngay trong lúc người dùng đang làm ký hiệu.

Vẫn giữ đường dự phòng: upload video → FastAPI tự trích landmark (dùng cho bài kiểm tra chấm bất đồng bộ,
và cho máy yếu không chạy nổi WASM).

### Phân chia trách nhiệm

Nguyên tắc: không chồng lấn.

| Service | Sở hữu | Không được làm |
|---|---|---|
| **Spring Boot** | Toàn bộ dữ liệu nghiệp vụ, quyền, điểm, tiến độ. Là **nguồn sự thật duy nhất**. | Không xử lý ML. |
| **FastAPI** | Suy luận model. **Stateless** — không DB riêng, không biết user là ai. | Không ghi tiến độ học. Không tự quyết "đạt/không đạt" — nó trả `score`, Spring Boot quyết định. |
| **Vue** | UI, thu landmark từ webcam, phát video. | Không gọi thẳng FastAPI cho luồng có tính điểm (phải qua Spring Boot để chống gian lận). |

---

## 3. Tech stack chi tiết

### 3.1 Backend — Spring Boot

| Thành phần | Lựa chọn | Ghi chú |
|---|---|---|
| Ngôn ngữ | **Java 21 (LTS)** | Virtual threads hợp với nhiều I/O chờ FastAPI |
| Framework | **Spring Boot 3.3+** | |
| Bảo mật | Spring Security + **JWT** (access 15 phút, refresh 7 ngày) + **OAuth2 Client** cho Google Login | Doc yêu cầu Google login |
| ORM | Spring Data JPA + Hibernate | |
| DB | **PostgreSQL 16** | Cần `jsonb` cho onboarding answers, `tsvector` + `unaccent` cho search tiếng Việt có dấu |
| Migration | **Flyway** | Bắt buộc — schema sẽ đổi nhiều |
| Cache / realtime | **Redis** | streak counter, rate-limit, cache từ điển, session quiz |
| Lưu video | **MinIO** (dev) / S3 + CloudFront (prod) | Presigned URL, không stream qua Spring |
| Mail | Spring Mail + template Thymeleaf | Nhắc học, nhắc streak |
| Job định kỳ | `@Scheduled` + ShedLock (chống chạy trùng khi scale nhiều instance) | Tính streak, gửi nhắc nhở |
| API doc | springdoc-openapi | |
| Test | JUnit 5 + Testcontainers | |
| Build | Gradle (Kotlin DSL) | |

**Cấu trúc module** — modular monolith, đừng microservice hoá sớm:

```
backend/src/main/java/vn/signai/
├── auth/          # register, login, google oauth, refresh, forgot password
├── user/          # profile, user_type, settings, onboarding
├── catalog/       # topic, course, lesson, lesson_item
├── dictionary/    # sign, sign_video, sign_relation, search
├── practice/      # flashcard (SRS), quiz, matching, bài kiểm tra
├── aicheck/       # client gọi FastAPI, chấm & lưu kết quả
├── progress/      # user_progress, streak, daily_activity, achievement
├── notification/  # email + web push
├── admin/         # CMS: quản lý từ vựng, video, bài học
└── common/        # config, exception, security, storage, i18n
```

### 3.2 Frontend — Vue 3

**Hiện trạng repo — đã kiểm tra kỹ 15.09.2026.** Thư mục [frontend/](../frontend/) là
[vue-vben-admin](https://github.com/vbenjs/vue-vben-admin) **v2.11.5** (Vue 3.4 + Vite 5 + Ant Design Vue 4 +
Pinia), và về cơ bản **lành lặn**. `internal/*` (4 package), `packages/hooks`, `packages/types`,
`apps/test-server` đều là thành phần chính thức của v2.11.5. Cả 6 phụ thuộc `@vben/*` khai trong
`package.json` đều phân giải đúng → **`pnpm install` và build chạy bình thường.**

Thứ thừa ra chỉ là rác, không phải kiến trúc thứ hai:

| Thừa | Nội dung |
|---|---|
| `packages/constants`, `effects`, `icons`, `locales`, `preferences`, `stores`, `styles`, `utils`, `internal/tsconfig` | **Rỗng hoàn toàn — 0 file** |
| `packages/@core` | Chỉ **24 file `dist/`** (build output), không source, không `package.json` |
| `internal/lint-configs`, `node-utils`, `tailwind-config` | Vài KB, không được tham chiếu |

`src/` không import thư mục nào trong số đó, `tsconfig.json` cũng không map tới
(`paths` chỉ có `@/*` → `src/*`). pnpm bỏ qua thư mục không có `package.json`, nên chúng
**không gây lỗi gì** — chỉ gây hiểu nhầm cho người đọc repo.

> **Việc cần làm (Tuần 0, ~30 phút):** xoá 9 thư mục rỗng + `packages/@core` + 3 thư mục `internal/` thừa.
> Giữ nguyên `src/`, `internal/{eslint-config,stylelint-config,ts-config,vite-config}`,
> `packages/{hooks,types}`, `apps/test-server`.

**Quyết định thật cần chốt: portal đặt ở đâu.** [frontend/apps/portal-view/](../frontend/apps/portal-view/)
hiện **rỗng**. `pnpm-workspace.yaml` đã glob sẵn `apps/*`, nên cách rẻ nhất là scaffold `apps/portal-view`
thành một app Vite riêng trong workspace, **giữ nguyên `src/` ở root làm app admin**. Không phải tái cấu
trúc gì thêm — hợp với thời hạn 5 tuần.

**Hai app riêng biệt** — đây là quyết định quan trọng:

| App | Mục đích | Người dùng | UI |
|---|---|---|---|
| `apps/portal-view` | Web học VSL: trang chủ, chủ đề, từ điển, luyện tập, thư viện, diễn đàn, tài khoản | Người khiếm thính & người thân | **Tự thiết kế** — Tailwind + component riêng. Không dùng layout admin. |
| `apps/admin` | CMS quản lý ~4.000 từ vựng, video, bài học, quiz, diễn đàn, người dùng | Nội bộ | **Dùng tối đa vben-admin** |

**Lý do tách:** vben-admin là layout sidebar/table cho dashboard nội bộ — cực mạnh ở đó, và đó chính là nơi
phải khai thác hết. Nhưng người học VSL cần giao diện nhiều hình ảnh, video lớn, ít chữ, tương phản cao;
ép portal vào layout admin sẽ phải gỡ bỏ gần hết những gì template cho sẵn.

**Khai thác tối đa vben cho `apps/admin`.** Đã rà [frontend/src/components/](../frontend/src/components/) —
`BasicTable`, `VxeTable`, `BasicForm`, `Upload`, `Tinymce`, `Excel` (nhập/xuất hàng loạt), `Tree`, `Cropper`,
`CountDown`, `Authority`, `sortablejs`, `echarts` đều dùng được ngay. **Chỉ ba thứ phải tự viết**: trình phát
so sánh hai video, ô quay webcam, và lớp phủ khung xương. Chi tiết ở [04-admin-cms.md](04-admin-cms.md).

Phần hạ tầng của vben (`@core`, request layer, store, i18n, preferences, dark mode) dùng chung được cho cả
portal — không việc gì dựng lại.

| Thành phần | Lựa chọn |
|---|---|
| Core | Vue 3.4 + TypeScript + Vite 5 |
| State | Pinia (+ `pinia-plugin-persistedstate` cho onboarding draft) |
| Router | Vue Router 4, lazy route theo section |
| UI portal | Tailwind CSS + headless component (Reka UI / Radix Vue) |
| UI admin | Ant Design Vue 4 (đã có sẵn) |
| HTTP | Axios + interceptor refresh token |
| Video | `<video>` thuần, thêm HLS.js nếu cần adaptive streaming |
| Webcam AI | **`@mediapipe/tasks-vision`** — Holistic Landmarker chạy WASM/WebGPU |
| i18n | vue-i18n, locale `vi` mặc định |
| Test | Vitest + Playwright (E2E luồng onboarding → học → quiz) |

**Accessibility — không phải mục tuỳ chọn ở dự án này.** Người dùng chính là người khiếm thính:

- Mọi video ký hiệu phải có **phụ đề tiếng Việt và mô tả văn bản**.
- Không dùng âm thanh làm kênh thông tin duy nhất. Mọi feedback phải nhìn thấy được.
- Tương phản tối thiểu WCAG AA (4.5:1), hỗ trợ phóng to 200%.
- Điều hướng đầy đủ bằng bàn phím, focus ring rõ ràng.
- Video phát chậm được (0.5x / 0.75x) và tua lại — người học ký hiệu rất cần điều này.

### 3.3 AI Service — Python FastAPI

| Thành phần | Lựa chọn |
|---|---|
| Python | 3.11 |
| Web | FastAPI + Uvicorn, WebSocket cho chấm realtime |
| Trích landmark (fallback phía server) | `mediapipe` Tasks Python |
| Suy luận | **ONNX Runtime** — không kéo cả PyTorch vào image production |
| Train (offline, thư mục `ml/`) | PyTorch + PyTorch Lightning |
| Xử lý chuỗi | NumPy + `dtaidistance` hoặc `fastdtw` cho DTW |
| Quản lý gói | `uv` hoặc Poetry |
| Đóng gói | Docker multi-stage |

```
ai-service/
├── app/
│   ├── main.py
│   ├── api/          # /verify, /recognize, /embed, /health, ws /verify-stream
│   ├── core/         # config, auth M2M (shared secret hoặc mTLS)
│   ├── pipeline/     # normalize landmark, temporal resample, augment
│   ├── models/       # encoder ONNX, dtw matcher, classifier
│   └── exemplars/    # loader embedding mẫu, cache trong RAM
└── ml/               # notebook + script train — KHÔNG deploy
```

---

## 4. Lộ trình triển khai — bản 3 ngày

> **Cập nhật 15.09.2026.** Đã chốt: xây bằng AI — **toàn bộ chức năng trừ AI checking trong 3 ngày**.
> Dữ liệu nạp sau qua Excel + nạp video hàng loạt, vẫn giữ thêm từ mới thủ công.
> AI checking (DTW) làm sau, theo [01-ai-model-research.md](01-ai-model-research.md).

### 4.0. Nguyên tắc nền: tự động hoá thay cho soạn tay

Áp cho luồng dữ liệu ở §4.2. **Không soạn tay bất cứ thứ gì có 4.000 bản.**

| Hạng mục | Cách làm thủ công | Cách làm ở đây | Công sức |
|---|---|---|---|
| Video 4.000 từ | Tự quay | **Crawl có phép từ Bộ GD&ĐT** | 1 script |
| Vùng miền B/T/N | Gán tay từng video | **Parse hậu tố tên file** `W00665B/T/N` | 1 hàm |
| Exemplar cho AI | Quay 3 clip mỗi từ | **Chạy MediaPipe hàng loạt trên video đã crawl** | 1 job batch, vài giờ máy |
| Bài học | Viết 4.000 bài | **Sinh tự động**: gom 10–15 từ cùng chủ đề + cấp độ thành một bài | 1 job |
| Quiz | Soạn từng câu | **`quiz_blueprints` sinh đề tại chỗ** — đã thiết kế sẵn | đã có |
| Chủ đề, từ loại | Gán tay 2.877 dòng | Hạt giống từ `label.csv` của photienanh + gán theo quy tắc, soát tay nhóm 200 từ dùng nhiều nhất | 1–2 ngày |

**Điểm mấu chốt về AI checking:** vì mỗi từ trong dữ liệu Bộ GD&ĐT đã có sẵn 1–3 video, chạy MediaPipe một
lượt là **cả 4.000 từ đều chấm được ngay** — không phải quay thêm clip nào. Và 83% số từ chỉ có 1 video lại
đúng là tình huống DTW xử lý được còn classifier thì không (xem [01-ai-model-research.md](01-ai-model-research.md)).
Chọn DTW hoá ra là lựa chọn duy nhất khớp với dữ liệu thực tế.

**Cả ba vùng miền làm thế nào cho rẻ:** không dựng ba bài học song song. Chỉ là **trình phát video có ba tab
Bắc / Trung / Nam**, mặc định mở tab vùng người học đã chọn lúc onboarding, và hiện huy hiệu "Từ này có biến
thể vùng miền" khi `sign_videos` có nhiều hơn một `region`. Một component, không phải ba luồng nội dung.

### 4.0.1. Cái KHÔNG nằm trong 3 ngày — nói trước

- **Người thạo VSL duyệt tính đúng đắn của 4.000 từ.** Không kịp, và **không cần**: nội dung lấy từ từ điển
  chính thức của Bộ GD&ĐT, vốn đã do người điếc ba miền thẩm định. Ta xuất bản nguyên trạng và ghi rõ nguồn.
  Vai trò `VSL_REVIEWER` vẫn dựng sẵn để duyệt dần về sau.
- **`description_vi` viết tay cho từng từ.** Để trống, bổ sung dần. Không chặn gì.
- **Toàn bộ AI checking.** Đã tách khỏi 3 ngày theo đúng quyết định — làm sau, và chỉ chạy được khi
  video thật đã nạp xong. Ba tầng ngưỡng ở [02-data-model.md §6](02-data-model.md).
- **Test tự động phủ rộng.** Ưu tiên luồng auth và luồng tính điểm; phần còn lại test tay.

Ở nhịp 3 ngày, thứ quyết định không phải tốc độ sinh code mà là **thứ tự**: mốc nghiệm thu cuối Ngày 1
phải đạt trước khi sang Ngày 2, và năm việc ở bảng dưới phải xong trước khi bắt đầu.

### Việc phải làm TRƯỚC Ngày 1 — không nén được bằng AI

Đây là những việc chỉ gồm bấm trong console bên thứ ba và chờ. Không sinh code được, và nếu
để đến lúc cần mới làm thì mỗi việc chặn nửa ngày.

| # | Việc | Mất bao lâu | Chặn cái gì |
|---|---|---|---|
| 1 | **Tạo OAuth Client trên Google Cloud Console** — consent screen, redirect URI, client id/secret | ~30 phút | Đăng nhập Google |
| 2 | **SMTP gửi mail** — tài khoản app password hoặc dịch vụ gửi mail | ~20 phút | Xác thực email, quên mật khẩu, nhắc streak |
| 3 | **MinIO + cấu hình CORS** cho presigned URL | ~30 phút | Mọi thứ liên quan video |
| 4 | **`ffmpeg` trong Docker image** của worker | ~30 phút | Bình luận video ở diễn đàn |
| 5 | **Văn bản xin phép Bộ GD&ĐT** | ngoài tầm kiểm soát | Nạp dữ liệu thật |

Năm việc này làm song song được và nên xong trước khi bắt đầu Ngày 1.

---

### Ngày 1 — Nền tảng + xương sống chạy được đầu-cuối

**Hạ tầng**
- Xoá thư mục rỗng trong `frontend/`, scaffold `apps/portal-view`
- `docker-compose`: postgres · redis · minio · backend · ai-service
- Khởi tạo Spring Boot + FastAPI

**Backend**
- **Toàn bộ Flyway migration** — dựng hết schema một lần, đừng chia nhỏ ([02-data-model.md](02-data-model.md))
- Auth: đăng ký (kèm `vsl_role`), đăng nhập, Google OAuth2, refresh token, quên mật khẩu
- Entity + repository + service + controller cho `users`, `signs`, `topics`, `sign_videos`

**Frontend**
- Layout portal + layout admin, router, axios interceptor, auth store
- Hệ màu theo [05-design-system.md](05-design-system.md)
- Trang đăng ký / đăng nhập / onboarding

> **Mốc nghiệm thu Ngày 1:** đăng nhập bằng Google được, gọi một API thật có JWT, đọc được một bản ghi từ
> PostgreSQL ra màn hình. Chưa đạt mốc này thì đừng sang Ngày 2 — mọi thứ phía sau đều dựng trên nó.

### Ngày 2 — Chức năng học + admin

**Portal**
- Từ điển: search `unaccent`, lọc chủ đề · cấp độ · đơn vị · từ loại · vùng miền
- **Trình phát 3 tab Bắc/Trung/Nam**
- Chủ đề, bài học, `lesson_items`
- Flashcard + SRS (SM-2)
- Quiz sinh đề trộn theo chủ đề + vùng miền ([02-data-model.md §4.1](02-data-model.md))
- Progress, streak, thư viện của tôi
- Dashboard

**Admin** — dùng `BasicTable` + `BasicForm` khai báo theo schema, đây là chỗ AI nhanh nhất
- CRUD từ vựng · chủ đề · bài học · ngân hàng câu hỏi · cấu hình đề trộn
- **Nhập Excel** + **thêm từ mới thủ công** + **tải video** (xem §4.2)

### Ngày 3 — Diễn đàn + hoàn thiện

- Diễn đàn: chuyên mục, bài viết, bình luận 2 cấp, cảm xúc
- **Bình luận video**: `MediaRecorder` quay webcam + tải file lên
- Worker `ffmpeg` qua hàng đợi Redis → H.264 720p + thumbnail
- Hàng đợi kiểm duyệt + báo cáo vi phạm
- Notification email + Web Push
- Profile, cài đặt, duyệt vai trò chuyên môn

### Phần gần như chắc chắn tràn sang ngày 4–5

Không phải vì code chậm, mà vì đây là loại lỗi chỉ lộ ra khi ghép mọi thứ lại:

| Hạng mục | Vì sao khó nén |
|---|---|
| **Search tiếng Việt** | `unaccent` + `tsvector` phải chỉnh theo dữ liệu thật mới ra kết quả đúng. Không có data thì chưa biết sai ở đâu |
| **Pipeline `ffmpeg`** | Mỗi định dạng webcam xuất ra một kiểu. `webm` từ Chrome khác `mov` từ Safari |
| **`MediaRecorder` đa trình duyệt** | Codec và sự kiện khác nhau giữa Chrome / Firefox / Safari |
| **CORS + presigned URL** | Luôn sai vài vòng ở lần dựng đầu |
| **Tích hợp 3 service** | Lỗi phát sinh khi ghép, không thấy được lúc làm riêng từng service |

---

## 4.2. Nạp dữ liệu — luồng riêng, chạy sau khi có chức năng

Dữ liệu **không chặn phát triển**: dựng schema trước, nhập ~20 từ giả để code và test, nạp thật sau.

### Ba đường nạp, phục vụ ba mục đích khác nhau

| Đường | Dùng khi | Quy mô |
|---|---|---|
| **A. Nhập Excel** | Nạp lần đầu toàn bộ từ vựng | ~2.900 dòng |
| **B. Nạp video hàng loạt theo URL** | Kéo video về sau khi có phép | ~4.000 video |
| **C. Thêm từ mới thủ công** | Bổ sung từ, sửa nội dung, thay video | Vài chục |

### A. Nhập Excel — hạt giống từ dữ liệu photienanh

`label.csv` của [photienanh](https://github.com/photienanh/Vietnamese-Sign-Language-Recognition)
có **3.694 dòng → 2.877 nhãn**, cấu trúc `ID, VIDEO, LABEL`:

```
1,D0001N.mp4,địa chỉ
1,D0001T.mp4,địa chỉ
3,D0001B.mp4,địa chỉ
4,D0002.mp4,tỉnh
```

Chuyển sang Excel nhập vào SignAI, mỗi dòng là **một video**, không phải một từ:

```
word_vi  | video_id  | region | gloss  | unit_type | word_type | level    | topics   | domain
địa chỉ  | D0001N    | SOUTH  | DIA_CHI| WORD      | DANH_TU   | BEGINNER | co-ban   |
địa chỉ  | D0001T    | CENTRAL| DIA_CHI| WORD      | DANH_TU   | BEGINNER | co-ban   |
địa chỉ  | D0001B    | NORTH  | DIA_CHI| WORD      | DANH_TU   | BEGINNER | co-ban   |
tỉnh     | D0002     | COMMON | TINH   | WORD      | DANH_TU   | BEGINNER | dia-danh |
```

Hậu tố `B`/`T`/`N` → `region` suy tự động bằng script, không nhập tay. Các cột `unit_type`,
`word_type`, `level`, `topics` **để trống cũng nhập được** — bổ sung dần trong admin.

Trình nhập gộp nhiều dòng cùng `gloss` thành **một `signs`** kèm **nhiều `sign_videos`**.

> **Dữ liệu photienanh chỉ có 3 cột.** Nó cho bạn *từ* và *mã video*, không cho chủ đề, cấp độ hay từ loại.
> Đó là hạt giống tốt để khỏi gõ tay 2.877 dòng, nhưng phần phân loại vẫn phải làm — bằng script gán theo
> quy tắc, rồi soát tay nhóm từ thông dụng nhất.

### B. Nạp video hàng loạt — không tải tay 4.000 file

**Tải từng file qua giao diện admin là bất khả thi ở quy mô 4.000 video.** Cần một màn hình riêng:

```
Admin → Video ký hiệu → Nạp hàng loạt

Nguồn:  (•) Danh sách URL    ( ) Tải nhiều file lên
Mẫu URL: https://qipedc.moet.gov.vn/videos/{video_id}.mp4
Đối chiếu theo: video_id đã có trong bảng signs

[ Bắt đầu nạp ]   → chạy nền, hiện tiến độ  1.247 / 4.012
                     tự thử lại khi lỗi, bỏ qua file đã có
```

Server tự tải về, đẩy lên MinIO, sinh thumbnail, cập nhật `sign_videos.storage_key`.
Chạy nền qua hàng đợi Redis — đóng trình duyệt vẫn chạy tiếp.

Sau khi xong, chạy tiếp job **sinh exemplar** (MediaPipe hàng loạt) để AI checking dùng được.

### C. Thêm từ mới thủ công

Giữ nguyên như đã mô tả ở [04-admin-cms.md §3](04-admin-cms.md): form `BasicForm` + `BasicDrawer`,
tab Thông tin / Video / Từ liên quan / AI. Tải video bằng `Upload` của vben, chọn vùng miền và góc quay
cho từng video. Đây là đường dùng hằng ngày sau khi hệ thống đã chạy.

## 5. Rủi ro — xếp theo mức độ chặn dự án

| # | Rủi ro | Ảnh hưởng | Cách xử lý |
|---|---|---|---|
| 0 | **Phạm vi so với 5 tuần** — đủ hết chức năng + 4.000 từ + ba vùng miền + diễn đàn video, trong hơn 1 tháng | Cao nhất | Tự động hoá đã gỡ phần nội dung (§4.0), nên rủi ro còn lại nằm ở **số lượng tính năng**, không phải số lượng từ. Hai việc bắt buộc: chốt quy mô đội ngay, và **cắt theo đúng thứ tự §4.1 ngay khi trễ tuần đầu** — đừng đợi đến tuần 4 mới cắt. |
| 1 | **Chưa có văn bản cho phép dùng video của Bộ GD&ĐT** | Chặn toàn bộ nội dung | **Đã chốt nguồn: crawl có phép + tự quay bổ sung.** Rủi ro chuyển từ "không biết lấy đâu ra" thành "chưa có giấy phép trong tay". Gửi văn bản **ngày đầu tiên của Tuần 0** — thủ tục nằm ngoài tầm kiểm soát của đội. Trong lúc chờ: viết sẵn script crawl, và chuẩn bị phương án tự quay ~150 từ thông dụng nhất để có cái chạy demo nếu giấy phép về muộn. |
| 2 | ~~Không tồn tại model VSL pretrained~~ **Đã xử lý** | Thấp | Đã chốt **DTW + exemplar, mức A**. Không cần train, không cần GPU. Exemplar sinh tự động bằng MediaPipe từ chính video crawl được → cả 4.000 từ chấm được ngay. |
| 3 | Bản quyền dataset ASL/CSL (WLASL, MS-ASL) khi dùng thương mại | Trung bình | Chỉ dùng **trọng số pretrained self-supervised** làm khởi tạo; kiểm tra license từng repo trước khi đưa vào production |
| 4 | ~~Frontend template trộn v2/v5~~ **Đánh giá lại: không phải rủi ro** | Rất thấp | Kiểm tra 15.09 cho thấy repo v2.11.5 lành lặn, install và build chạy được. Thứ thừa chỉ là thư mục rỗng và `dist/` bỏ quên, không được tham chiếu. Xoá mất ~30 phút. Việc thật là **scaffold `apps/portal-view`**, không phải sửa chữa gì |
| 5 | Webcam không chạy trên máy yếu / trình duyệt cũ | Thấp | Fallback upload video; feature-detect WebGPU → WASM → server-side |
| 6 | Người khiếm thính không dùng được sản phẩm vì a11y kém | Cao, và âm thầm | Đưa checklist a11y vào Definition of Done từng sprint; test với người dùng thật từ Phase 2 |
| 7 | Phạm vi "Cộng đồng" chưa xác định | Thấp | Để ngoài MVP đến khi có spec |

---

## 6. Quyết định đã chốt — 15.09.2026

| # | Câu hỏi | Quyết định |
|---|---|---|
| 1 | Cộng đồng gồm gì? | ✅ Diễn đàn: bài viết + bình luận, **bình luận hỗ trợ video** (quay webcam hoặc tải file) |
| 2 | Nguồn video ký hiệu | ✅ **Crawl có phép từ Bộ GD&ĐT** + tự quay bổ sung khi thiếu |
| 3 | Số từ cho MVP | ✅ **Toàn bộ ~4.000 từ** đều có bài học — khả thi nhờ tự động hoá, xem §4.0 |
| 4 | AI checking có tính điểm? | ✅ **Mức A** — chỉ luyện tập, không tính điểm. Dùng **DTW** trước, M1 để sau |
| 5 | Thời hạn | ✅ **Hơn 1 tháng** — lộ trình 5 tuần ở §4 |
| 6 | Vùng miền | ✅ **Dạy cả ba, đánh dấu khác biệt** — trình phát 3 tab B/T/N, mặc định mở vùng người học chọn |

### Còn một điểm chưa rõ

**Quy mô đội.** Lộ trình 5 tuần ở §4 giả định **4–5 người** chia ba nhánh (BE / FE / Data-AI) làm song song.
Nếu đội dưới 3 người, lộ trình này không giữ được và cần cắt phạm vi theo thứ tự ở §4.1 ngay từ đầu
chứ không phải đợi đến lúc trễ.

---

## Tài liệu liên quan

- [01-ai-model-research.md](01-ai-model-research.md) — nghiên cứu model & repo AI
- [02-data-model.md](02-data-model.md) — thiết kế CSDL (gồm phân loại từ vựng, quiz trộn, streak, diễn đàn)
- [03-api-contract.md](03-api-contract.md) — hợp đồng API giữa 3 service
- [04-admin-cms.md](04-admin-cms.md) — trang quản trị, gắn với component vben-admin có sẵn
- [05-design-system.md](05-design-system.md) — bảng màu giáo dục & hoà bình, chữ, chỗ chờ icon
