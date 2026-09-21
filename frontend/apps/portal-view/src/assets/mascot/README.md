# Linh vật Mochi — bàn tay ký hiệu

Nguồn: ảnh người dùng cung cấp (2026-09-20), thay cho con gấu Mochi cũ trong Figma.
Là **bàn tay làm ký hiệu “I love you”** kèm khuôn mặt cười — hợp mọi lứa tuổi và nói
đúng thứ sản phẩm dạy, thay vì một con thú chỉ hợp trẻ nhỏ.

| File | Kích thước | Gồm gì | Dùng ở |
|---|---|---|---|
| `mochi-hand.png` | 610×768 | bàn tay + vòng gạch đứt + tia lấp lánh | Trang chủ (khung linh vật), rương phụ kiện |
| `mochi-hand-solo.png` | 489×653 | chỉ bàn tay | `MascotWave.vue`, widget Góc Trò Chơi, favicon |

## Đã tách nền

Ảnh gốc có một **đĩa xám `#F8FAFC`** phủ kín phía sau. Đã bỏ đĩa đó đi: nền trong suốt,
ruột bàn tay tô trắng đục, nét vẽ giữ nguyên `#0F172A`. Nhờ vậy đặt được lên mọi nền màu —
các màn hình hiện đặt lên một quầng pastel (đào → xanh) và cho bàn tay vẫy nhẹ.

Cách làm, nếu cần dựng lại từ ảnh gốc:

1. Coi pixel trong suốt của ảnh gốc là **nền**, không phải nét vẽ — nếu không, bốn góc
   ảnh (RGB `0,0,0`) bị nhận nhầm là nét và chặn luôn bước lan từ viền vào.
2. Ruột bàn tay = vùng không lan tới được từ viền ảnh. Nét vẽ có chỗ mỏng 1px nên phải
   nở nét thêm 1px mới kín, rồi trả lại 1px vừa nở.
3. Pixel quanh nét sẫm hơn nền thì cho alpha từng phần để hết răng cưa; ngưỡng độ sáng
   `215` để **vòng gạch đứt** (`#E2E8F0`, độ sáng ~231) bị loại hẳn khỏi bản `solo`.

`object-fit` phải là **`contain`**, không phải `cover`: ảnh đã tách nền nên `cover` cắt
cụt ngón tay ngay.
