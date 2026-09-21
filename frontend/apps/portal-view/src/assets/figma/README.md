# Ảnh lấy từ Figma

Nguồn: file Figma `eoUFKOLUIBRg4R1NUj7aO7` — "web học sign language".

Tất cả là **bản dựng node (node render) ở 2×** kích thước khung thiết kế, xuất qua
`GET /v1/images/{key}?ids=...&format=png&scale=2`. Dùng cách này chứ **không** lấy qua
`/v1/files/{key}/images`: endpoint đó trả ảnh gốc đã bị co về 512px và **bỏ mất phép cắt**
(`imageTransform`) mà thiết kế đang áp — dán vào là lệch khung ngay.

| File | Khung thiết kế | Dùng ở |
|---|---|---|
| `home-camera-teaser.png` | 462×260 | Trang chủ — thẻ mời mở camera |
| `video-card-1..3.png` | 389×219 | Trang chủ — 3 thẻ video cử chỉ |
| `practice-teacher-pane.png` | 560×315 | Phòng luyện — khung mẫu bên trái |
| `practice-mirror-pane.png` | 560×315 | Phòng luyện — khung gương bên phải |
| `step-1..3.png` | 357×128 | Phòng luyện — thẻ hướng dẫn từng bước |
| `game-target-bird/cat/dog.png` | 48×48 | Góc trò chơi — mục tiêu |
| `game-gesture-looper.png` | 391×176 | Góc trò chơi — khung xem lặp |
| `leader-avatar-1..3.png` | 44×44 | Góc trò chơi — bảng xếp hạng |
| `guardian-avatar.png` | 48×48 | Bảng đồng hành — ảnh đại diện |
| `clip-1..4.png` | 293×224 | Bảng đồng hành — 4 clip nổi bật |

## Ba chỗ CỐ Ý không dùng ảnh Figma

**Linh vật gấu Mochi đã bỏ** (`mochi-hero/widget/dressing.png`, xoá 2026-09-20). Người dùng
chốt linh vật mới là **bàn tay làm ký hiệu “I love you”** — hợp mọi lứa tuổi hơn con gấu và
nói đúng điều sản phẩm dạy. Ảnh mới nằm ở `src/assets/mascot/`, xem README ở đó.


**`logo-signkids.png` đã bỏ.** Người dùng chốt giữ thương hiệu **SignAI**, mà logo trong
Figma có sẵn chữ "SignKids" nung vào ảnh. Dùng logo tự dựng (icon bàn tay + chữ SignAI).

**Ảnh minh hoạ nội dung là ảnh MẪU.** `video-card-*`, `step-*`, `clip-*` trong Figma chỉ là
ảnh chèn cho đẹp bản thiết kế. Sản phẩm thật phải lấy **video và ảnh ký hiệu từ MinIO**;
mấy ảnh này chỉ dùng làm nền chờ khi chưa có dữ liệu.
