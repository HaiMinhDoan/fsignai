# SignAI — Trang quản trị (Admin CMS)

Xây trên **vue-vben-admin v2.11.5** đã có sẵn trong [frontend/](../frontend/).
Nguyên tắc: **dùng tối đa component có sẵn, chỉ tự viết những gì vben không có.**

---

## 1. Kho component vben-admin sẵn dùng

Đã kiểm tra trong [frontend/src/components/](../frontend/src/components/) — đây là thứ không phải viết lại:

| Component vben | Dùng cho màn hình nào |
|---|---|
| `BasicTable` (`useTable`) | Mọi bảng CRUD: từ vựng, câu hỏi, bài học, người dùng, kiểm duyệt |
| `VxeTable` | Bảng từ vựng lớn (2.000+ dòng) — ảo hoá, sửa tại chỗ |
| `BasicForm` (`useForm`) | Mọi form. Khai báo schema, không viết tay từng input |
| `Upload` | Tải video ký hiệu, ảnh bìa, avatar |
| `Excel` + `xlsx` + `vxe-table-plugin-export-xlsx` | ⭐ **Nhập/xuất từ vựng hàng loạt** — xem §3 |
| `Tinymce` | Soạn bài blog, mô tả khoá học |
| `Markdown` | Soạn nội dung dạng markdown |
| `Tree` | Cây chủ đề cha–con |
| `Cropper` | Cắt ảnh avatar, ảnh bìa |
| `BasicModal` / `BasicDrawer` | Hộp thoại thêm/sửa |
| `Description` | Trang chi tiết dạng nhãn–giá trị |
| `Preview` | Xem trước ảnh |
| `CountDown` | Đồng hồ đếm ngược trong quiz và khi quay webcam |
| `CountTo` | Số liệu trên dashboard |
| `Icon` (`@iconify`, 200k+ icon) | Toàn bộ icon — dùng tạm cho tới khi có bộ icon riêng |
| `Authority` | Ẩn/hiện theo vai trò |
| `StrengthMeter` | Đo độ mạnh mật khẩu |
| `sortablejs` | ⭐ Kéo–thả sắp thứ tự bài học, thứ tự `lesson_items` |
| `echarts` | Biểu đồ thống kê |

**Chỉ ba thứ phải tự viết** (vben không có):

1. **Trình phát so sánh video** — hai video cạnh nhau, tua đồng bộ, phát chậm 0.25×–1×. Dùng để duyệt video ký hiệu.
2. **Ô quay webcam** — `MediaRecorder` + xem lại. Dùng cho cả admin (quay mẫu) lẫn người dùng (bình luận video).
3. **Lớp phủ khung xương** — vẽ landmark MediaPipe lên `<canvas>` chồng lên video. Dùng để kiểm tra chất lượng exemplar.

---

## 2. Cây menu quản trị

```
Bảng điều khiển
│
├── Nội dung
│   ├── Từ vựng                 ← màn hình quan trọng nhất
│   ├── Video ký hiệu           (duyệt, gắn vùng miền, đặt video chính)
│   ├── Chủ đề                  (cây phân cấp)
│   ├── Khoá học & Bài học      (kéo–thả sắp thứ tự)
│   └── Bài viết / Blog
│
├── Đánh giá
│   ├── Ngân hàng câu hỏi
│   ├── Đề cố định
│   └── Cấu hình đề trộn        (quiz_blueprints)
│
├── Cộng đồng
│   ├── Chuyên mục diễn đàn
│   ├── Bài đăng
│   ├── Hàng đợi kiểm duyệt     ← ưu tiên cao, có huy hiệu đếm
│   └── Báo cáo vi phạm
│
├── AI
│   ├── Exemplar theo từ        (từ nào đã chấm được / chưa)
│   ├── Ngưỡng chấm điểm        (ba tầng — xem §5.1)
│   └── Góp ý về chấm điểm      (lọc theo trọng số)
│
├── Người dùng
│   ├── Danh sách
│   ├── Tiến độ học
│   ├── Duyệt vai trò chuyên môn  ← có huy hiệu đếm, xem §5.2
│   └── Vai trò & phân quyền
│
└── Hệ thống
    ├── Thành tích & streak
    ├── Mẫu thông báo
    └── Nhật ký thao tác
```

---

## 3. Màn hình Từ vựng — màn hình quan trọng nhất

Đây là nơi quản trị viên sẽ ngồi hàng giờ. Làm tốt màn này thì cả dự án chạy được.

### Bảng danh sách — `VxeTable`

| Cột | Ghi chú |
|---|---|
| Thumbnail | Ảnh đầu video, hover thì phát preview |
| Từ tiếng Việt | Sửa nhanh tại chỗ |
| Gloss | `ME`, `CHA`, `GIA_DINH` |
| Đơn vị | `LETTER` / `NUMBER` / `WORD` / `PHRASE` / `SENTENCE` |
| Từ loại | Danh từ / Động từ / Tính từ… |
| Chủ đề | Nhiều thẻ |
| Cấp độ | |
| Vùng miền có video | Ba chấm tròn **B · T · N**, tô đậm vùng nào đã có video |
| Exemplar | ✅ đã chấm được / ⚠️ chưa đủ mẫu |
| Trạng thái | Nháp / Đã xuất bản |

**Bộ lọc** (`BasicForm` dạng thu gọn phía trên bảng): tìm theo chữ (có dấu và không dấu),
chủ đề, cấp độ, đơn vị, từ loại, lĩnh vực, vùng miền còn thiếu video, tình trạng exemplar.

Bộ lọc **"vùng miền còn thiếu video"** và **"chưa có exemplar"** là hai bộ lọc dùng nhiều nhất —
chúng trả lời câu "còn phải làm gì nữa", nên đặt thành nút lọc nhanh ngay trên bảng.

### Nhập hàng loạt từ Excel — tiết kiệm hàng tuần công

Với ~4.000 từ, nhập tay từng mục là bất khả thi. vben đã có sẵn `Excel` + `xlsx`:

```
Mẫu file .xlsx:
gloss | word_vi | word_en | unit_type | word_type | domain | level | topics        | description_vi
ME    | mẹ      | mother  | WORD      | DANH_TU   |        | BEGINNER | gia-dinh,co-ban | Bàn tay phải...
```

Luồng nhập:
1. Tải file mẫu
2. Kéo file lên → **xem trước dạng bảng, tô đỏ dòng lỗi**
3. Chọn cách xử lý trùng `gloss`: bỏ qua / ghi đè / tạo bản nháp
4. Nhập → báo cáo: *thêm mới N, cập nhật M, lỗi K dòng* kèm file lỗi tải về được

**Luôn cho xem trước trước khi ghi.** Nhập nhầm 4.000 dòng rồi mới phát hiện là thảm hoạ khó gỡ.

### Form thêm/sửa — `BasicForm` + `BasicDrawer`

Chia tab:

- **Thông tin** — từ tiếng Việt, gloss, tiếng Anh, đơn vị, từ loại, lĩnh vực, cấp độ, chủ đề, mô tả cách làm ký hiệu
- **Video** — `Upload` nhiều file, mỗi video gắn **vùng miền** (B/T/N/Chung) + **góc quay** + đánh dấu video chính
- **Từ liên quan** — chọn từ, gắn loại quan hệ: liên quan / đồng nghĩa / trái nghĩa / **dễ nhầm**
- **AI** — danh sách exemplar, nút "Tạo lại exemplar", điểm chất lượng từng mẫu

Trường **"dễ nhầm"** đáng bỏ công nhập: nó vừa cảnh báo người học, vừa là nguồn sinh đáp án nhiễu
chất lượng cao cho quiz. Đáp án nhiễu ngẫu nhiên làm quiz quá dễ và không đo được gì.

---

## 4. Ngân hàng câu hỏi & đề trộn

### Ngân hàng câu hỏi
`BasicTable` + form. Lọc theo chủ đề, dạng câu hỏi, cấp độ, độ khó. Xem trước đúng như người học thấy.

### Cấu hình đề trộn — `quiz_blueprints`

Một form khai báo luật, không phải soạn từng câu:

```
Tên đề          [ Kiểm tra chủ đề Gia đình – Cơ bản ]
Chủ đề          [ Gia đình ] [ Giao tiếp hằng ngày ]
Cấp độ          [x] Beginner  [x] Basic  [ ] Intermediate
Đơn vị          [x] WORD  [ ] PHRASE  [ ] SENTENCE
Số câu          [ 10 ]     Số đáp án [ 4 ]
Tỉ lệ dạng câu  Video→Từ [5]  Từ→Video [3]  Ghép cặp [2]
Đáp án nhiễu    (•) Ưu tiên từ dễ nhầm  ( ) Cùng chủ đề  ( ) Trộn
Điểm đạt        [ 70 ]     Thời gian [ 10 ] phút
```

Có nút **"Xem thử đề"** sinh ngay một đề mẫu để kiểm tra luật có ra đề hợp lý không.

**Cảnh báo tự động khi kho từ không đủ.** Nếu luật lọc ra ít hơn `số câu × 4`, hiển thị cảnh báo đỏ:
*"Chỉ có 18 từ khớp điều kiện, không đủ sinh 10 câu 4 đáp án cho vùng miền Nam."* Phát hiện lúc cấu hình
tốt hơn nhiều so với để người học gặp lỗi giữa bài thi.

---

## 5. Hàng đợi kiểm duyệt

Màn hình dùng hằng ngày khi diễn đàn chạy. Bố cục hai cột:

- **Trái** — danh sách chờ duyệt, sắp theo thời gian, có huy hiệu đếm ở menu
- **Phải** — xem nội dung: phát video, đọc phụ đề, xem lịch sử người đăng

Thao tác: **Duyệt** · **Từ chối** (kèm lý do, gửi thông báo cho người đăng) · **Ẩn** · **Cấm người dùng**.

Phím tắt `A` duyệt / `R` từ chối / `→` mục kế tiếp. Người duyệt xử lý hàng trăm mục mỗi ngày,
phím tắt là khác biệt giữa vài phút và cả buổi.

---

## 5.1. Màn hình Ngưỡng chấm điểm

Bảng từ vựng kèm cột ngưỡng, `source` và `sample_count`. Chọn một từ → panel bên phải hiện:

- **Biểu đồ phân bố** khoảng cách DTW của các lần thử đã thu, tô màu theo góp ý 👍/👎
- **Thanh trượt ngưỡng**, kéo tới đâu hiện ngay *"sẽ đổi kết quả của 23/180 lần thử"*
- Nguồn hiện tại: suy từ exemplar / mặc định nhóm / hiệu chỉnh từ góp ý / đặt tay
- Nút **Về mặc định nhóm**

Lọc nhanh cần có: **"từ có nhiều góp ý 👎 nhất"** — đó là danh sách những từ đang chấm sai nhiều nhất,
và cũng chính là đặc tả cho M1 sau này.

## 5.2. Màn hình Duyệt vai trò chuyên môn

Người dùng tự khai là giáo viên, người điếc bản ngữ hoặc phiên dịch viên sẽ vào hàng đợi này.
**Chưa duyệt thì góp ý của họ có trọng số 0** — vẫn lưu, và được tính ngược lại khi duyệt xong.

| Cột | |
|---|---|
| Họ tên · email · ngày đăng ký | |
| Vai trò tự khai | Giáo viên / Người điếc bản ngữ / Phiên dịch viên |
| Minh chứng | Nơi công tác, số chứng chỉ — do người dùng tự nhập |
| Hoạt động | Số bài học đã hoàn thành, số lần dùng AI checking |

Thao tác: **Duyệt** · **Từ chối** (kèm lý do) · **Yêu cầu bổ sung minh chứng**.

Cột "Hoạt động" đáng để ý khi duyệt: một tài khoản khai là giáo viên VSL nhưng chưa từng dùng sản phẩm
là tín hiệu đáng hỏi lại. Ngược lại, người điếc bản ngữ dùng đều đặn hằng ngày thì gần như chắc chắn khai thật.

**Duyệt theo hướng rộng rãi, nhưng gỡ được.** Đây không phải cấp chứng chỉ hành nghề — chỉ là quyết định
xem góp ý của ai được tính vào việc chỉnh ngưỡng. Sai sót có thể sửa: gỡ vai trò và tính lại. Đặt rào cản
quá cao sẽ không ai buồn nộp minh chứng, và Cách 3 chết vì thiếu người.

## 6. Phân quyền

| Vai trò | Quyền |
|---|---|
| `ADMIN` | Toàn quyền, kể cả phân quyền và cấu hình hệ thống |
| `CONTENT_EDITOR` | Từ vựng, video, chủ đề, bài học, câu hỏi. **Không** đụng người dùng |
| `MODERATOR` | Chỉ diễn đàn: duyệt, ẩn, xử lý báo cáo |
| `VSL_REVIEWER` | Chỉ **duyệt tính đúng đắn của ký hiệu** và **duyệt vai trò chuyên môn của người dùng** — dành cho người điếc bản ngữ hoặc giáo viên VSL |

Lưu ý phân biệt hai khái niệm dễ lẫn: `VSL_REVIEWER` là **vai trò quản trị** (có quyền vào CMS), còn
`users.vsl_role` là **khai báo chuyên môn của người dùng thường** (chỉ ảnh hưởng trọng số góp ý, không cho
quyền admin nào). Một giáo viên đăng ký học bình thường sẽ có `vsl_role = TEACHER` nhưng **không** có
`VSL_REVIEWER`.

Vai trò `VSL_REVIEWER` tách riêng là có chủ đích: **người thẩm định ký hiệu đúng hay sai phải là người
thành thạo VSL, và họ không cần — cũng không nên — có quyền sửa cấu trúc nội dung.** Dùng component
`Authority` của vben để gắn quyền vào từng nút.

---

## 7. Việc phải làm trước khi bắt đầu

Nhắc lại từ [00-project-plan.md](00-project-plan.md): thư mục `frontend/` đang trộn vben v2 và v5.
**Phải dọn ở Sprint 0**, trước khi viết màn hình đầu tiên. Bảng component ở §1 dựa trên
**v2.11.5** (`src/components/`); nếu chọn đi theo monorepo v5, đường dẫn và API component sẽ khác.
