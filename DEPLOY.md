# Triển khai SignAI bằng Docker

## Chạy

```bash
cp .env.example .env     # rồi sửa mật khẩu, địa chỉ MinIO, App Password email
docker compose build --no-cache
docker compose up -d
```

Cần sẵn mạng dùng chung với nginx-proxy-manager (chỉ tạo một lần):

```bash
docker network create slm_v2_app-network
```

## Bốn container

| Container | Cổng trong mạng | Ra ngoài Internet? | Việc |
|---|---|---|---|
| `fsign-portal` | `fsign-portal:5174` | ✅ trỏ tên miền cho bé học | Giao diện học, nginx tĩnh |
| `fsign-cms` | `fsign-cms:5173` | ✅ trỏ tên miền quản trị | Trang quản trị vben |
| `fsign-backend` | `fsign-backend:8080` | ⚠️ tuỳ chọn | Spring Boot, toàn bộ nghiệp vụ |
| `fsign-ai` | `fsign-ai:8001` | ❌ **không**, chỉ nội bộ | Chấm điểm ký hiệu (Python) |

### Vì sao `fsign-ai` không cần ra ngoài

Đúng như bạn nghĩ: **chỉ backend gọi nó**. Trình duyệt của bé chạy MediaPipe ngay trên máy mình rồi
gửi toạ độ landmark về Spring Boot; Spring mới gọi sang service Python, tra ngưỡng và quyết định
đạt hay chưa. Nếu để trình duyệt gọi thẳng service Python thì ai cũng tự đặt điểm cho mình được.

Vì vậy `fsign-ai` nằm ở mạng riêng `fsign-internal`, **không nối vào `slm_v2_app-network`**.
Cổng 8001 chỉ mở ra `127.0.0.1` của máy chủ để bạn gỡ lỗi (`curl localhost:8001/health`); bỏ dòng
`ports` trong `docker-compose.yml` là đóng hẳn.

### Vì sao backend có thể không cần tên miền

nginx **bên trong hai container frontend** đã chuyển tiếp sẵn:

- `fsign-cms`: `/basic-api/*` → `fsign-backend:8080/*` (bỏ tiền tố, giống hệt lúc chạy dev)
- `fsign-portal`: `/api/*` → `fsign-backend:8080/api/*`

Nên trình duyệt chỉ gọi về chính tên miền của nó: **không CORS, không phải nhúng tên miền API vào
bundle JavaScript, đổi tên miền không phải build lại**. Chỉ trỏ tên miền riêng cho backend khi bạn
muốn mở Swagger (`/swagger-ui.html`) hoặc gọi API từ ứng dụng khác.

### ⚠️ Tên container KHÔNG được có dấu gạch dưới

Đã đo thật: gọi backend với `Host: fsign_backend:8080` trả về **400 Bad Request**, còn cùng container
với `Host: api.signai.vn` trả về `{"status":"UP"}`. Tomcat từ chối Host header chứa `_` vì dấu này
không hợp lệ trong tên miền theo RFC 1123. Vì vậy mọi container ở đây đặt tên bằng **gạch ngang**
(`fsign-backend`), đừng đổi lại thành `fsign_backend`.

## Trỏ tên miền trong nginx-proxy-manager

Thêm Proxy Host, phần Forward:

| Tên miền | Forward Hostname | Forward Port |
|---|---|---|
| `hoc.signai.vn` (portal) | `fsign-portal` | `5174` |
| `admin.signai.vn` (CMS) | `fsign-cms` | `5173` |
| `api.signai.vn` (tuỳ chọn) | `fsign-backend` | `8080` |

Bật **Websockets Support** cho CMS (vben dùng WS cho vài màn hình), và nhớ tăng
`client_max_body_size` trong Advanced của NPM nếu nạp video lớn qua CMS:

```nginx
client_max_body_size 600m;
proxy_read_timeout 600s;
```

## Hai địa chỉ MinIO, đừng gộp làm một

Backend trả về **đường dẫn video tuyệt đối** cho trình duyệt, nên cần phân biệt:

| Biến | Ai dùng | Giá trị hiện tại | Vì sao |
|---|---|---|---|
| `MINIO_ENDPOINT` | Server (tải tệp lên/xuống) | `http://171.244.142.43:9000` | Đi thẳng, nhanh, không dính giới hạn dung lượng của proxy khi nạp video 500MB |
| `MINIO_DOMAIN` | Trình duyệt (xem video) | `https://minio.slmglobal.vn` | Trang HTTPS mà video HTTP thì trình duyệt **chặn sạch** (mixed content) |

Để trống `MINIO_DOMAIN` thì backend tự quay về dùng `MINIO_ENDPOINT`.

Đổi tên miền chỉ cần sửa `.env` rồi `docker compose up -d fsign-backend`, **không phải build lại**:
đường dẫn không lưu sẵn trong CSDL mà được ghép lại mỗi lần trả API.

Đã kiểm chứng `https://minio.slmglobal.vn` ngày 20.09.2026: tải đúng video
(`/fsignai/signs/W00424/W00424.mp4`, 578.332 bytes), chứng chỉ hợp lệ, có `Accept-Ranges: bytes`
nên tua video được.

## Những thứ KHÔNG nằm trong compose

PostgreSQL, MinIO, Redis, Kafka đang chạy sẵn ở `171.244.142.43` nên compose chỉ chứa 4 service của
SignAI. Toàn bộ địa chỉ và mật khẩu khai trong `.env`.

## Thao tác thường dùng

```bash
docker compose logs -f fsign-backend         # xem log
docker compose up -d --build fsign-portal    # build lại một service
docker compose ps                            # trạng thái + healthcheck
curl localhost:8001/health                   # kiểm tra service AI
docker compose down                          # dừng (giữ nguyên dữ liệu vì DB nằm ngoài)
```

## Ghi nhớ khi build

- **Build mất 10–20 phút lần đầu**: Maven tải phụ thuộc, pnpm cài cả workspace, và hai bước tải
  model MediaPipe (13MB cho service Python, 20MB cho portal). Máy build cần vào được Internet.
- **Bản build CMS dùng `--mode deploy`** (`frontend/.env.deploy`): tắt dữ liệu giả (`VITE_USE_MOCK`),
  giữ `VITE_GLOB_API_URL=/basic-api` tương đối.
- **`AI_SERVICE_SECRET`**: đặt một chuỗi ngẫu nhiên (`openssl rand -hex 32`) rồi điền vào `.env`.
  Để trống nghĩa là bất cứ container nào trong mạng nội bộ cũng gọi được service AI.
- **Sinh mẫu chấm điểm**: dữ liệu mẫu nằm trong CSDL + MinIO nên **không phải chạy lại sau khi
  deploy**. Chỉ chạy lại khi thêm video mới (CMS → Từ vựng → *Sinh mẫu AI*) hoặc khi đổi
  `AI_FEATURE_VERSION` (lúc đó mọi mẫu cũ bị coi là hết hạn).
