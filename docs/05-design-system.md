# SignAI — Hệ thống thiết kế

Hướng màu: **giáo dục và hoà bình**.
Bộ icon và ảnh nền sẽ được cung cấp sau — tài liệu này định sẵn chỗ để lắp vào mà không phải sửa code.

---

## 1. Bảng màu

Xanh dương là màu của cả hai ý niệm: **tri thức, tin cậy** (giáo dục) và **bình yên, đối thoại** (hoà bình).
Xanh lá đi kèm mang nghĩa **hy vọng và sự lớn lên** — cũng là nhánh ô liu trong biểu tượng hoà bình.

### Màu thương hiệu

| Vai trò | Tên | Hex | Dùng ở đâu |
|---|---|---|---|
| **Primary** | Xanh tri thức | `#1B6CA8` | Nút chính, liên kết, tiêu đề mục, thanh điều hướng |
| Primary đậm | | `#155585` | Trạng thái nhấn, chữ trên nền sáng cần tương phản cao |
| Primary nhạt | | `#E8F1F8` | Nền vùng được chọn, thẻ nhẹ |
| **Secondary** | Xanh hoà bình | `#5B92E5` | Nhấn phụ, biểu đồ, huy hiệu |
| **Success** | Xanh hy vọng | `#2E9E6B` | Hoàn thành, đúng, tiến bộ |
| **Streak** | Cam ấm | `#E8913A` | Chuỗi ngày học, thành tích — **chỉ dùng ở đây** |
| **Warning** | Vàng đất | `#C98A1E` | Cảnh báo, chờ duyệt |
| **Danger** | Đỏ gạch | `#C4503F` | Lỗi, xoá, vi phạm |

Đỏ cố tình chọn tông **gạch trầm**, không phải đỏ tươi. Sản phẩm này báo "chưa khớp với mẫu" rất nhiều lần
với người đang học — đỏ chói biến mỗi lần thử thành một lời trách.

### Màu trung tính

Lệch nhẹ về xanh để ăn với màu thương hiệu, không phải xám thuần.

| Token | Sáng | Tối |
|---|---|---|
| `--bg` | `#F5F8FA` | `#0E1A21` |
| `--surface` | `#FFFFFF` | `#16242D` |
| `--surface-2` | `#EAF0F4` | `#1E2F3A` |
| `--text` | `#12232C` | `#E6EEF3` |
| `--text-muted` | `#5A7683` | `#94AEBA` |
| `--border` | `#D3E0E7` | `#2A3D48` |

**Chế độ tối không phải tính năng phụ ở sản phẩm này.** Người học nhìn video ký hiệu liên tục; nền tối
giảm chói và làm bàn tay nổi hơn hẳn. vben-admin đã hỗ trợ sẵn — giữ và chăm cho tử tế.

---

## 2. Bốn quy tắc riêng của sản phẩm này

**1. Khung video luôn trung tính.** Không viền màu, không nền màu, không phủ gradient quanh video ký hiệu.
Mắt người đọc ký hiệu cần đọc **hình bàn tay và sắc mặt**; bất kỳ mảng màu nào cạnh khung đều làm nhiễu.
Nền khung video dùng `--surface-2` hoặc xám trung tính, hết.

**2. Không bao giờ dùng màu làm kênh thông tin duy nhất.** Đúng/sai, đã học/chưa học, vùng miền — tất cả
phải kèm **chữ hoặc icon**. Đây vừa là yêu cầu cho người mù màu, vừa là nguyên tắc nền của sản phẩm:
người dùng chính giao tiếp bằng thị giác, nên tín hiệu thị giác phải dư thừa chứ không tối giản.

**3. Tương phản tối thiểu WCAG AA** — 4.5:1 cho chữ thường, 3:1 cho chữ lớn và thành phần giao diện.
Cặp `#1B6CA8` trên `#FFFFFF` đạt ~5.9:1. Kiểm tra lại mọi cặp màu mới trước khi dùng.

**4. Cam chỉ dành cho streak.** Nếu cam xuất hiện ở nút, ở nhãn, ở biểu đồ, thì lúc chuỗi 7 ngày hiện lên
nó sẽ không còn nghĩa gì. Một màu chỉ mang một thông điệp mới có sức nặng.

---

## 3. Token dùng trong code

```css
:root {
  --si-primary:        #1B6CA8;
  --si-primary-dark:   #155585;
  --si-primary-light:  #E8F1F8;
  --si-secondary:      #5B92E5;
  --si-success:        #2E9E6B;
  --si-streak:         #E8913A;
  --si-warning:        #C98A1E;
  --si-danger:         #C4503F;

  --si-bg:             #F5F8FA;
  --si-surface:        #FFFFFF;
  --si-surface-2:      #EAF0F4;
  --si-text:           #12232C;
  --si-text-muted:     #5A7683;
  --si-border:         #D3E0E7;

  --si-radius:         8px;
  --si-radius-lg:      14px;
}
```

**Ant Design Vue 4** (vben dùng bản này) nhận token trực tiếp, không cần ghi đè CSS:

```ts
// app config
theme: {
  token: {
    colorPrimary: '#1B6CA8',
    colorSuccess: '#2E9E6B',
    colorWarning: '#C98A1E',
    colorError:   '#C4503F',
    borderRadius: 8,
  },
}
```

**Tailwind** (portal):

```js
colors: {
  primary: { DEFAULT: '#1B6CA8', dark: '#155585', light: '#E8F1F8' },
  peace:   '#5B92E5',
  hope:    '#2E9E6B',
  streak:  '#E8913A',
}
```

Khai báo màu **một chỗ duy nhất** là bộ token này. Không hardcode hex trong component — đổi màu thương hiệu
sau này sẽ chỉ là sửa một file.

---

## 4. Chữ

| Vai trò | Font | Lý do |
|---|---|---|
| Tiêu đề | **Be Vietnam Pro** | Thiết kế riêng cho tiếng Việt, dấu thanh cân và rõ |
| Nội dung | **Be Vietnam Pro** hoặc **Inter** | Cùng họ để đơn giản; Inter cũng hỗ trợ đủ tiếng Việt |
| Số liệu, mã | **JetBrains Mono** | Gloss, mã từ, số liệu trong bảng |

Cỡ chữ thân bài tối thiểu **16px**, dòng cao **1.6**. Giao diện phải còn dùng được khi phóng to **200%** —
đây là yêu cầu WCAG, và người dùng lớn tuổi (ông bà học ký hiệu để nói chuyện với cháu) sẽ cần đến.

Dấu tiếng Việt bị cắt là lỗi hay gặp khi `line-height` quá chặt. Không đặt dưới 1.4 ở bất kỳ đâu có chữ Việt.

---

## 5. Chỗ chờ icon

Bộ icon sẽ được cung cấp sau. Để lắp vào không phải sửa code, quy ước trước:

```
frontend/apps/portal-view/src/assets/icons/     # icon riêng, dạng .svg
frontend/apps/portal-view/src/assets/bg/        # ảnh nền
```

- Icon vẽ trên khung **24×24**, nét **1.5px**, dùng `currentColor` để ăn theo màu chữ
- Cỡ dùng: `16` (trong dòng chữ) · `20` (nút) · `24` (menu) · `32` (thẻ chủ đề) · `48` (trạng thái rỗng)
- **Tạm thời dùng `@iconify`** (vben đã cài, hơn 200k icon). Bọc qua một component `<SiIcon name="..."/>`
  duy nhất để sau đổi sang bộ icon riêng chỉ cần sửa một file
- Ảnh nền phải có **phiên bản sáng và tối**, và luôn đủ mờ để chữ đè lên vẫn đạt tương phản 4.5:1

Mỗi chủ đề từ vựng cần một icon (`topics.icon_url`) — khi nhận bộ icon, đặt tên theo `slug` của chủ đề
để gán tự động thay vì gán tay từng mục.

---

## 6. Trạng thái giao diện

| Trạng thái | Thể hiện |
|---|---|
| Đang tải video | Khung giữ nguyên tỉ lệ + hiệu ứng skeleton. **Không** để layout nhảy |
| Chưa có nội dung | Icon 48px + một câu giải thích + một nút hành động |
| Chấm đúng | Viền xanh `--si-success` + icon ✓ + chữ "Khớp với mẫu" |
| Chấm chưa đúng | Viền vàng `--si-warning` + chữ **"Chưa khớp với mẫu"** — không phải "Sai" |
| Đang xử lý video | Nhãn "Đang xử lý" trên thumbnail, bình luận vẫn hiện |
| Mất streak | Thông báo trung tính, **không** dùng đỏ. Mất chuỗi đã đủ tiếc rồi |

Hai dòng cuối cùng của bảng này là chỗ dễ làm sai nhất, và cũng là chỗ quyết định người học có quay lại hôm sau hay không.

---

## 7. Lớp giao diện trẻ em (portal-view)

Sáu mục ở trên viết cho **cả nhà**: bé học, bố mẹ kèm, ông bà tra cứu, quản trị viên soạn bài.
Mục này là **lớp phủ riêng cho web học tập** (`frontend/apps/portal-view`) — nơi người ngồi trước
màn hình là **trẻ khiếm thính**. CMS quản trị **không** dùng lớp này.

### 7.1 Hai hướng xung khắc ở đâu, và giải thế nào

| Điểm va chạm | Hướng trẻ em muốn | Quy tắc §2 đòi | Cách hoà giải |
|---|---|---|---|
| Màu quanh video | Rực rỡ, vui mắt | Khung video trung tính tuyệt đối | Pastel phủ **nền trang và thẻ**, **dừng ở mép khung video**. Bên trong khung: `--si-surface-2`, hết. |
| Cam đào trong bảng màu | Cam đào là màu chủ đạo ấm | Cam chỉ dành cho streak | Tách hai sắc: **cam đào pastel** `--si-kid-peach` chỉ làm **nền**, không bao giờ làm chữ/nút/huy hiệu. Cam bão hoà `--si-streak` giữ nguyên độc quyền cho chuỗi ngày học. |
| Pháo hoa khi làm đúng | Ăn mừng thật to | Không dùng màu làm kênh duy nhất | Pháo hoa **cộng thêm** vào dấu ✓ và chữ "Khớp với mẫu", không thay thế. |
| Pastel nhạt | Càng dịu càng dễ thương | Tương phản ≥4.5:1 | Pastel chỉ làm **nền**. Chữ trên pastel luôn dùng `--si-text` (đạt >7:1 trên mọi pastel dưới đây). |

Nguyên tắc rút gọn một câu: **màu vui ở xung quanh, màu trung tính ở nơi bé phải nhìn kỹ.**

### 7.2 Bảng màu trẻ em

Toàn bộ là **màu nền**. Không màu nào trong nhóm này được dùng làm màu chữ.

| Token | Hex | Nghĩa gán cho |
|---|---|---|
| `--si-kid-sun` | `#FFD97D` | Vàng nắng — thử thách hằng ngày, phần thưởng |
| `--si-kid-peach` | `#FFB99A` | Cam đào — chủ đề cảm xúc, nhấn ấm |
| `--si-kid-mint` | `#9FE2C8` | Xanh mint — đã hoàn thành, an toàn |
| `--si-kid-sky` | `#A8D8F0` | Xanh da trời — đang mở, sẵn sàng học |
| `--si-kid-lilac` | `#C9BCF0` | Tím nhạt — khoá, chưa mở |
| `--si-kid-cream` | `#FFF8EC` | Kem — nền trang |

Mỗi chặng trên bản đồ hành trình nhận **một màu + một icon + một nhãn chữ**. Ba kênh, đúng §2.2:
bé mù màu vẫn phân biệt được, bé chưa đọc thạo vẫn nhận ra hình.

### 7.3 Hình khối và chữ

| | Người lớn (§3) | Trẻ em |
|---|---|---|
| Bo góc | `8px` / `14px` | `--si-kid-radius: 20px` / `--si-kid-radius-lg: 28px` |
| Vùng bấm | 36px | **tối thiểu 48px** — tay bé chưa chính xác |
| Cỡ chữ thân | 16px | **18px** |
| Tiêu đề thẻ | 17px | **20px, 700** |
| Đổ bóng | phẳng | `--si-kid-shadow` — bóng mềm, thấp, cho cảm giác vật thể bấm được |

### 7.4 Chuyển động

Bé khiếm thính không nhận được phản hồi bằng âm thanh, nên **chuyển động thay tiếng động**:
mọi hành động thành công phải có một chuyển động xác nhận.

- Hover thẻ: nhấc `-4px`, 160ms
- Bấm đúng: pháo hoa 900ms + dấu ✓ + chữ
- Mở chặng mới trên bản đồ: ổ khoá bật, 500ms
- **Bắt buộc tôn trọng `prefers-reduced-motion`** — có bé nhạy cảm tiền đình; khi bật, mọi
  hiệu ứng rút về đổi màu tức thì, riêng dấu ✓ và chữ thì giữ nguyên.

### 7.5 Linh vật

Nhân vật dẫn chuyện tên **Bé Vẫy** — một bàn tay đang vẫy. Chọn bàn tay vì đó chính là
"giọng nói" của ngôn ngữ ký hiệu; bé nhìn thấy bàn tay là hiểu ngay đây là nơi nói bằng tay.

Hiện vẽ bằng **SVG nội tuyến** (`src/components/MascotWave.vue`) để không phụ thuộc file ảnh.
Khi có bộ minh hoạ chính thức, thay ruột component đó — chỗ gọi không phải sửa.

Bé Vẫy nói câu ngắn, **tối đa 12 chữ**, luôn kèm hành động cụ thể. Không nói chuyện phiếm.

---

## 8. Figma là nguồn thiết kế chính thức (từ 2026-09-19)

Mục 7 ở trên là bản pastel tôi tự dựng khi chưa có thiết kế. Nay đã có file Figma
**"web học sign language"** (`eoUFKOLUIBRg4R1NUj7aO7`) — **Figma thắng** ở mọi điểm về
màu, chữ và bố cục. Mục 7 giữ lại vì phần lý lẽ (vì sao bo góc to, vì sao vùng bấm 48px,
vì sao chuyển động thay tiếng động) vẫn đúng.

Token thật nằm ở `frontend/apps/portal-view/src/styles/tokens.css`, lớp `--sk-*`.

| | Giá trị |
|---|---|
| Chữ | `Quicksand` (tiêu đề, UI) · `Nunito Sans` (nội dung) |
| Nền | `#F9F9FF` trang · `#FFFFFF` thẻ · `#F0F3FF` · `#E7EEFF` · `#DEE8FF` · `#D8E3FB` · `#CCE5FF` |
| Nhấn | `#F59E0B` hành động chính · `#FFDDB8` · `#85F8C4` · `#5BB8FE` · `#50C594` |
| Chữ màu | `#111C2D` · `#534434` · `#855300` · `#006398` · `#006C4A` |
| Bo góc | `9999px` pill · `32px` thẻ · `48px` khối |
| Lưới | khung 1280, container 1232 (lề 24) |

### Hai điều chỉnh có chủ đích so với Figma

**1. Cam không còn là màu riêng của streak.** Figma dùng `#F59E0B` cho mọi nút chính, nên
quy tắc §2.4 không còn áp dụng được. Thay bằng: streak nhận ra qua **icon 🔥 + số + chữ
"ngày"**. Dựa vào chữ và hình thì chắc hơn dựa vào màu — đúng tinh thần §2.2.

**2. Khung video vẫn trung tính.** Figma phủ pastel và gradient khắp trang; tôi cho pastel
**dừng lại ở mép khung video**. Bên trong dùng `--sk-stage` xám trung tính. §2.1 là quy tắc
chức năng cho người đọc ký hiệu, không phải lựa chọn thẩm mỹ để đánh đổi.

**3. Bản tối là phần thêm.** Figma chỉ vẽ bản sáng. `tokens.css` vẫn có đủ bộ tối vì người
học nhìn video ký hiệu liên tục và nền tối làm bàn tay nổi hơn (§1).
