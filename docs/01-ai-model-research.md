# Nghiên cứu model & repo AI cho SignAI

> Mục tiêu: **kéo model có sẵn về dùng, không xây lại từ đầu.**
> Tiêu chí bạn đặt ra: uy tín, độ chính xác cao, nhiều từ.
> Khảo sát: 2026-09-14

---

## 1. Kết luận trước, lý do sau

Ba điều cần biết trước khi chọn bất cứ repo nào:

**1. Không tồn tại model nhận diện Ngôn ngữ Ký hiệu Việt Nam (VSL) pretrained nào đủ tốt để dùng thẳng.**
Toàn bộ model "uy tín, chính xác cao, nhiều từ" trên thế giới đều là **ASL** (Mỹ), **CSL** (Trung Quốc),
**DGS** (Đức), **ISL** (Ấn Độ). VSL có ngữ pháp, bảng chữ cái và từ vựng riêng — một model ASL 2000 từ
**không nhận được một từ VSL nào**. Đây là ràng buộc cứng, không có cách đi vòng.

**2. Ngay cả SOTA thế giới cũng chưa đủ chính xác cho nhận diện mở.**
Trên WLASL-2000 (chuẩn khó nhất cho ASL), SOTA hiện tại là **~68% top-1** ([SignBart](https://arxiv.org/abs/2506.21592), 2025),
trước đó là **58.31%** ([NLA-SLR](https://arxiv.org/abs/2303.12080), CVPR 2023). Nghĩa là **cứ 3 từ thì sai 1**.
Nếu đưa con số đó vào sản phẩm dạy học, người học sẽ bị chấm sai liên tục và mất niềm tin.

**3. Nhưng SignAI không cần giải bài toán đó.**

| | Nhận diện mở | Verification (cái SignAI cần) |
|---|---|---|
| Câu hỏi | "Video này là từ nào trong 400 từ?" | "Người dùng đang học từ MẸ — họ làm có giống mẫu không?" |
| Không gian đáp án | 400 lớp | 2 (đúng / chưa đúng) + điểm số |
| SOTA hiện tại | ~60–68% | >90% khả thi |
| Dữ liệu cần | Hàng chục nghìn clip có nhãn | **3–10 clip mẫu mỗi từ** |
| Thêm từ mới | Train lại toàn bộ model | Quay thêm 3 clip, xong |

Giao diện học đã luôn biết trước từ mục tiêu. Tận dụng điều đó biến bài toán khó nhất trong ngành
thành bài toán đo khoảng cách giữa hai chuỗi — và **hoàn toàn né được việc thiếu dữ liệu VSL**.

Thêm một hệ quả quan trọng: **thêm từ vựng mới không cần train lại**. Với 400+ từ sẽ mở rộng dần
theo thời gian, đây là khác biệt giữa "làm được" và "không làm nổi".

---

## 2. Kiến trúc AI đề xuất

```
Webcam
  │
  ▼
[ TẦNG 1 ] MediaPipe Holistic Landmarker      ← dùng nguyên, KHÔNG train
  │        Google · Apache-2.0 · chạy trong browser
  │        → 553 landmark/frame (33 pose + 478 face + 42 hands)
  ▼
[ TẦNG 2 ] Chuẩn hoá                           ← code thuần, KHÔNG train
  │        • neo gốc toạ độ về vai/mũi (bất biến vị trí trong khung hình)
  │        • chuẩn hoá theo khoảng cách hai vai (bất biến khoảng cách camera)
  │        • resample về 32 frame cố định (bất biến tốc độ ký hiệu)
  │        • loại 478 face landmark, chỉ giữ ~10 điểm quanh miệng
  ▼
[ TẦNG 3 ] Encoder → embedding                 ← đây là model duy nhất cần train
  │        Transformer nhỏ (~750K params), input landmark, output vector 256-d
  ▼
[ TẦNG 4 ] So khớp
           • DTW + cosine với exemplar của từ mục tiêu
           • → score 0–100 + feedback theo bộ phận (tay / vị trí / chuyển động)
```

Điểm mấu chốt: **Tầng 1 và 2 giải quyết 80% bài toán và không tốn một dòng training nào.**
MediaPipe đã xử lý phần khó nhất của thị giác máy tính (tìm bàn tay, ước lượng 21 khớp 3D mỗi tay,
ổn định qua các frame). Tầng 3 chỉ học cách so sánh hai chuỗi khớp — nhỏ hơn nhiều bậc so với việc học từ pixel.

### Lộ trình 3 mức, triển khai tăng dần

| Mức | Cách làm | Dữ liệu cần | Khi nào | Độ chính xác kỳ vọng |
|---|---|---|---|---|
| **M0** | DTW thẳng trên landmark đã chuẩn hoá, **không có model học** | 3 clip mẫu/từ | Sprint 7, ~1 tuần | 75–85% với từ khác biệt rõ |
| **M1** | Encoder nhỏ train bằng contrastive learning (triplet loss) trên chính dữ liệu VSL của bạn | 10 clip/từ × 50 từ | Sprint 8 | 88–93% |
| **M2** | Classifier 400 lớp, fine-tune từ trọng số pretrained (Uni-Sign / GISLR) trên VSL400 | VSL400 đầy đủ | Phase 6 | 60–75% top-1, dùng cho nhận diện mở |

**Bắt đầu từ M0.** Nó cho feedback thật cho người dùng ngay trong sprint đầu của AI, và tạo ra chính
dữ liệu cần cho M1. Nhảy thẳng lên M2 là cách phổ biến nhất để dự án loại này chết: tốn 3 tháng train
model và vẫn không có gì để người dùng bấm vào.

---

## 3. Bảng so sánh các model / repo

### 3.1 Trích xuất landmark — dùng nguyên, không bàn cãi

| Repo | Nguồn | License | Vì sao chọn |
|---|---|---|---|
| **[MediaPipe Holistic Landmarker](https://developers.google.com/edge/mediapipe/solutions/vision/holistic_landmarker)** | **Google** | Apache-2.0 | 553 landmark (33 pose + 478 face + 42 hands). Chạy realtime trên CPU **và trong browser** qua WASM/WebGPU. Có SDK Python cho server-side. Đây là nền của gần như mọi hệ thống SLR hiện đại. |

Lưu ý phiên bản: `mediapipe.solutions.holistic` (legacy) **đã bị deprecate từ 2023**. Dùng API mới
`mediapipe.tasks` — package JS là `@mediapipe/tasks-vision`, Python là `mediapipe.tasks.python.vision`.
Rất nhiều tutorial trên mạng vẫn dùng API cũ; đừng copy.

### 3.2 Model nhận diện — xếp theo mức phù hợp với SignAI

| # | Model | Nguồn / uy tín | Vocab | Độ chính xác | Weights | Đánh giá cho SignAI |
|---|---|---|---|---|---|---|
| 1 | **[Kaggle GISLR — Google × PopSign](https://www.kaggle.com/competitions/asl-signs)** solutions | **Google** tài trợ, ~100k video, 21 người ký hiệu điếc | 250 từ ASL | Top solutions rất mạnh | ✅ Public, nhiều repo | ⭐ **Template thực dụng nhất.** Input đúng là MediaPipe landmark — trùng khít pipeline của bạn. Kiến trúc landmark → Transformer → TFLite đã được chứng minh ở quy mô 100k video và chạy realtime trên điện thoại. **Lấy kiến trúc, đổi head sang từ vựng VSL.** Repo tham khảo: [abhinand5](https://github.com/abhinand5/isolated-sign-language-recognition), [JosephZahar](https://github.com/JosephZahar/Google-Isolated-Sign-Language-Recognition-Kaggle) |
| 2 | **[Uni-Sign](https://github.com/ZechengLi19/Uni-Sign)** | **ICLR 2025** | Đa tác vụ (ISLR + CSLR + SLT) | SOTA nhiều benchmark | ✅ [HuggingFace](https://huggingface.co/ZechengLi19/Uni-Sign) | Mạnh nhất về chất lượng. Pretrain trên **CSL-News — 1.985 giờ video**. Nhưng nặng (fusion pose + RGB), cần GPU. **Dùng làm backbone cho M2**, không dùng cho MVP. |
| 3 | **[SignBart](https://arxiv.org/abs/2506.21592)** | arXiv 2025 | WLASL-2000, ASL-Citizen, LSA-64 | **68.00% top-1 WLASL-2000** (vượt SOTA cũ +9.69%); 96.04% LSA-64 | ⚠️ Chưa thấy repo chính thức | Kiến trúc đáng học nhất cho bạn: **chỉ 749.888 tham số**, tách riêng toạ độ x và y rồi nối bằng cross-attention. Nhẹ đến mức chạy được realtime trên CPU. **Đọc paper và tự cài lại tầng 3** — với model nhỏ như vậy, cài lại rẻ hơn chờ weights. |
| 4 | **[NLA-SLR](https://arxiv.org/abs/2303.12080)** | **CVPR 2023** | WLASL-2000 | 58.31% top-1 | ✅ Public | Ổn định, code sạch, được trích dẫn nhiều. Là baseline tốt để đối chiếu. Nặng hơn SignBart. |
| 5 | **[OpenHands](https://github.com/AI4Bharat/OpenHands)** | **ACL 2022**, AI4Bharat + IIT-Madras | 4 model × **6 ngôn ngữ ký hiệu** | — | ✅ Có checkpoint | ⚠️ **Repo đã ngừng bảo trì** (README ghi rõ). Nhưng đóng góp về ý tưởng thì cực kỳ liên quan: chứng minh **self-supervised pretraining giúp rất nhiều ở ngôn ngữ ký hiệu ít dữ liệu**, và **transfer từ ISL sang ngôn ngữ ký hiệu khác có hiệu quả**. Đây chính xác là tình huống của VSL. **Đọc paper, đừng phụ thuộc vào code.** |
| 6 | **[SignBERT+](https://arxiv.org/abs/2305.04868)** | TPAMI | Đa benchmark | Mạnh | Một phần | Pretrain self-supervised nhận biết mô hình bàn tay. Tham khảo cho chiến lược pretrain. |

**[Awesome-Sign-Language](https://github.com/ZechengLi19/Awesome-Sign-Language)** — danh sách paper do chính
tác giả Uni-Sign duy trì. Theo dõi repo này để cập nhật SOTA.

### 3.3 Dữ liệu — phần quyết định thật sự

| Dataset | Ngôn ngữ | Quy mô | Tiếp cận | Giá trị với SignAI |
|---|---|---|---|---|
| **[QIPEDC — Từ điển NNKH của Bộ GD&ĐT](https://qipedc.moet.gov.vn/dictionary)** | 🇻🇳 **VSL** | **~4.000 từ** có video | Website công khai, đang hoạt động (kiểm tra 15.09.2026). **Điều khoản sử dụng chưa rõ — phải liên hệ xin phép** | ⭐⭐⭐ **Phát hiện quan trọng nhất.** Dự án của **Bộ Giáo dục và Đào tạo**, do World Bank (GPRBA) tài trợ. Từ vựng **do chính người điếc ở cả ba miền Bắc–Trung–Nam đóng góp và thẩm định**. Lớn gấp 10 lần VSL400 và là nguồn có thẩm quyền về chuẩn VSL. Xem §3.5. |
| **[VSL400](https://www.authorea.com/doi/full/10.22541/au.177075292.21810740/v1)** | 🇻🇳 **VSL** | **74.259 clip · 400 gloss · 28 người ký hiệu · 3 góc quay đồng bộ** (trước, trái, phải) | Metadata + code trên **Zenodo**, open license; **video qua controlled access** (phải xin) | ⭐⭐⭐ **Tài sản quan trọng nhất trong toàn bộ danh sách này.** Là bộ dữ liệu VSL lớn nhất công khai, có cả người điếc bản ngữ lẫn người nghe được đào tạo. 400 gloss khớp đúng với mục tiêu "nhiều từ". Có sẵn pipeline tiền xử lý (phát hiện ranh giới, phân đoạn thời gian, chuẩn hoá không gian). **Gửi yêu cầu cấp quyền ngay Sprint 0** — thủ tục này mất thời gian. |
| [VOYA_VSL](https://huggingface.co/datasets/Kateht/VOYA_VSL) | 🇻🇳 VSL | — | HuggingFace, 2025 | Bổ sung. Cần kiểm tra quy mô & license. |
| Dataset bảng chữ cái VSL | 🇻🇳 VSL | 23 chữ + 2 dấu | Nhiều paper VN | Dùng cho bài học đánh vần ngón tay. Có paper báo 91–95%. |
| [ASL Citizen](https://arxiv.org/abs/2304.05934) | 🇺🇸 ASL | Cộng đồng đóng góp | Microsoft, public | Chỉ để pretrain / benchmark |
| WLASL-2000 | 🇺🇸 ASL | 2000 từ | Public | Benchmark chuẩn. ⚠️ Kiểm tra license kỹ trước khi dùng thương mại. |
| CSL-News | 🇨🇳 CSL | **1.985 giờ** | [HuggingFace](https://huggingface.co/datasets/ZechengLi19/CSL-News) | Nguồn pretrain của Uni-Sign |

Về các paper VSL báo **99,5% accuracy** (LSTM + MediaPipe Holistic, 1000 video): con số đúng nhưng
**không so sánh được** — vocab rất nhỏ, ít người ký hiệu, thường chia train/test ngẫu nhiên thay vì
tách theo người ký hiệu. Dùng làm bằng chứng rằng "landmark + model nhỏ là đủ cho vocab nhỏ", chứ
đừng dùng làm mục tiêu cho 400 từ.

---

## 3.5. Repo VSL tiếng Việt — đã kiểm chứng trực tiếp

Bốn repo dưới đây do người dùng cung cấp. **Cả bốn đều tồn tại thật**, nhưng số liệu thực tế lệch khá nhiều so
với mô tả ban đầu, nên phần này ghi lại đúng những gì kiểm tra được qua GitHub API ngày 15.09.2026.

| Repo | Weights | Từ vựng | Cập nhật cuối | License | Kết luận |
|---|---|---|---|---|---|
| **[tawannt/SignLang](https://github.com/tawannt/SignLang)** (VSignChat) | ✅ `sign_sstcn_attention_model.pth` · 7.8 MB | Trích từ giáo trình NNKH chính thức của **Trần Thị Thiệp** (ĐH Sư phạm Hà Nội) | **03.2026** — mới nhất | ✅ **MIT** | ⭐ **Tham khảo tốt nhất.** Là repo duy nhất trong bốn cái có license → repo duy nhất dùng hợp pháp được. Kiến trúc SSTCN + attention trên skeleton, đúng hướng của ta. |
| **[photienanh/Vietnamese-Sign-Language-Recognition](https://github.com/photienanh/Vietnamese-Sign-Language-Recognition)** | ✅ `final_model.keras` · 59.6 MB | **2.764 nhãn · 3.695 video** | 06.2025 | ❌ **Không có** | Quy mô từ vựng lớn nhất. Nhưng giá trị thật không nằm ở model — nằm ở chỗ `download_data.py` tiết lộ nguồn dữ liệu là **qipedc.moet.gov.vn** (xem dưới). |
| **[paty0504/SIGNTEGRATE](https://github.com/paty0504/SIGNTEGRATE)** | ✅ 2 file `.h5` + **4 file `.tflite`** · 7.1 MB | ⚠️ **Chỉ 10 câu cố định**, không phải từ đơn — "xin chào rất vui được gặp bạn", "tôi là người điếc"… | **12.2021** — gần 5 năm | ❌ **Không có** | Có weights thật và bản TFLite chạy được, nhưng 10 câu cố định không dùng được cho từ điển. Giá trị: **bản cài đặt DD-Net tham khảo**. |
| **[trangphan10/Vietnamese-Sign-Language](https://github.com/trangphan10/Vietnamese-Sign-Language)** | ❌ **Không có file weights nào** — chỉ 7 notebook, repo 1.8 MB | ⚠️ **Không phải dữ liệu Việt Nam** | 06.2024 | ❌ **Không có** | Xem cảnh báo bên dưới. |

### Ba điều cần đính chính

**1. `trangphan10/Vietnamese-Sign-Language` không dùng dữ liệu VSL, và không đạt 99,5%.**
README của chính repo ghi rõ: dữ liệu là **250 video lấy từ [LSA64 — Ngôn ngữ Ký hiệu Argentina](https://facundoq.github.io/datasets/lsa64/)**,
dùng "với mục đích **mô phỏng** dữ liệu ngôn ngữ ký hiệu Việt Nam", vì *"Việt Nam chưa có bộ dữ liệu công khai,
đủ tốt để huấn luyện"*. Kết quả tự công bố trong README:

> Độ chính xác với phương pháp Mediapipe + LSTM: **37%**
> Độ chính xác với phương pháp ResNet50 + LSTM tốt nhất: **83%**

Con số **99,5% / Flask API / độ trễ <300ms** gắn với bài báo Springer là **một công trình khác**, không phải
repo này. Đừng kéo repo này về và kỳ vọng 99,5%.

**2. Ba trong bốn repo không có license.** Theo luật bản quyền mặc định, không có license nghĩa là
**tác giả giữ toàn bộ quyền** — không được dùng lại trong sản phẩm, kể cả sản phẩm phi lợi nhuận, trừ khi
xin phép bằng văn bản. Chỉ `tawannt/SignLang` (MIT) là dùng được ngay.

**3. Việc "ai cũng dùng MediaPipe → LSTM" không có nghĩa đó là kiến trúc đúng.**
Chính `trangphan10` đo được MediaPipe + LSTM chỉ **37%**, thua ResNet50 + LSTM (83%) trên cùng bộ dữ liệu.
LSTM phổ biến vì dễ viết, không phải vì mạnh. SOTA hiện tại là **transformer/attention trên skeleton**
(SignBart 68% trên 2000 từ, hoặc SSTCN + attention như `tawannt/SignLang` dùng), không phải LSTM.

### Phát hiện lớn nhất: từ điển NNKH của Bộ GD&ĐT

`download_data.py` của photienanh cho thấy dữ liệu được crawl từ:

```python
BASE_URL = "https://qipedc.moet.gov.vn"
video_url = f"{BASE_URL}/videos/{video_id}.mp4"
```

Kiểm tra trực tiếp ngày 15.09.2026: site trả `HTTP 200`, video mẫu `D0001N.mp4` trả `content-type: video/mp4`.
Đây là **[Dự án QIPEDC](https://qipedc.moet.gov.vn/introduce)** (Quality Improvement of Primary Education for
Deaf Children) — **chủ quản là Bộ Giáo dục và Đào tạo**, do **World Bank / GPRBA** tài trợ, triển khai ở 20
tỉnh thành. Trang từ điển ghi **"Video 4000 từ"**.

Vì sao nó quan trọng hơn mọi thứ khác trong tài liệu này:

- **~4.000 từ có video** — gấp 10 lần VSL400 (400 gloss)
- Từ vựng **do chính người điếc ở cả ba miền Bắc–Trung–Nam đóng góp và thẩm định** — đúng thứ SignAI cần dạy
- Là **nguồn có thẩm quyền** về chuẩn VSL; dạy theo chuẩn của Bộ GD&ĐT đáng tin hơn tự chọn từ vựng
- Video đã quay sẵn, chất lượng đồng nhất — không phải tự tổ chức quay từ đầu

> ⚠️ **Nhưng tuyệt đối không crawl rồi dùng luôn.** Trang chủ có mục *Term of use* nhưng nội dung điều khoản
> chưa xác định được qua khảo sát này. Việc `photienanh` crawl được **không** đồng nghĩa SignAI được phép dùng lại.
> **Phải liên hệ Ban quản lý dự án QIPEDC / Bộ GD&ĐT xin phép bằng văn bản** — thông tin liên hệ có trên
> [trang giới thiệu](https://qipedc.moet.gov.vn/introduce). Đây là dự án giáo dục công phục vụ trẻ điếc, nên
> một nền tảng dạy VSL miễn phí có lý do chính đáng để xin hợp tác — nhưng vẫn phải xin.

---

## 4. Vì sao không chọn những hướng khác

**Không train từ pixel (I3D, video transformer).** Cần GPU nhiều tuần, cần dataset lớn, và MediaPipe
đã giải xong phần đó. Landmark cũng nhẹ hơn video ~1000 lần khi truyền qua mạng.

**Không dùng model ASL/CSL để nhận diện VSL trực tiếp.** Từ vựng khác hoàn toàn. Chỉ dùng **trọng số**
làm điểm khởi tạo, không dùng **đầu ra**.

**Không dùng LLM đa phương thức (GPT-4V, Gemini Vision) để đọc ký hiệu.** Độ trễ cao, giá theo request,
chưa đạt độ chính xác cho ngôn ngữ ký hiệu ở mức từ, và buộc phải gửi video người dùng lên bên thứ ba —
điều này đặc biệt nhạy cảm với nhóm người dùng của SignAI.

**Không xây avatar 3D sinh ký hiệu (SiGML/JASigning) cho MVP.** Repo [raianrido/VSL](https://github.com/raianrido/VSL)
có 3873 trạng thái SiGML cho VSL, đáng lưu ý, nhưng avatar 3D có chất lượng thấp hơn video người thật
rõ rệt và người khiếm thính thường không thích. Video người thật tốt hơn cho việc dạy.

---

## 5. Việc cần làm — theo thứ tự

| # | Việc | Ai | Khi nào | Chặn cái gì |
|---|---|---|---|---|
| 1 | **Liên hệ Ban quản lý dự án QIPEDC / Bộ GD&ĐT** xin phép dùng lại ~4.000 video từ điển | PM | **Sprint 0 — việc số 1** | Chặn toàn bộ nội dung. Nếu xin được, gần như giải quyết xong bài toán nội dung |
| 2 | **Gửi yêu cầu cấp quyền VSL400** trên Zenodo (phương án song song) | PM | **Sprint 0 — ngay** | Chặn M1, M2. Thủ tục ngoài tầm kiểm soát → làm sớm nhất |
| 2b | Liên hệ trung tâm/CLB người khiếm thính để quay bộ từ MVP (phương án dự phòng nếu cả 1 và 2 không xong) | PM | Sprint 0 | Chặn toàn bộ nội dung |
| 3 | Dựng pipeline chuẩn hoá landmark (tầng 1–2) | ML | Sprint 5 | Chặn M0 |
| 4 | Thu exemplar: 3 clip/từ cho 50 từ đầu | Content | Sprint 6 | Chặn M0 |
| 5 | M0: DTW matcher + API `/verify` | ML | Sprint 7 | — |
| 6 | Đọc paper SignBart, cài lại encoder tầng 3 | ML | Sprint 8 | Chặn M1 |
| 7 | M1: train contrastive encoder trên dữ liệu đã thu | ML | Sprint 8 | — |
| 8 | Xin cấp quyền xong → M2 fine-tune trên VSL400 | ML | Phase 6 | — |

---

## 6. Ghi chú về license

Kiểm tra trước khi đưa bất cứ thứ gì vào production:

- **MediaPipe** — Apache-2.0, dùng thương mại thoải mái ✅ **Miễn phí, không API key, không quota, không cần
  tài khoản Google Cloud.** Suy luận chạy hoàn toàn on-device; theo
  [ToS của Google](https://ai.google.dev/edge/mediapipe/legal/tos), dữ liệu đầu vào (ảnh, video) **không được
  gửi về server Google**. Đây vừa là lý do chi phí bằng 0, vừa là cơ sở pháp lý cho cam kết riêng tư với người
  dùng khiếm thính. Đừng nhầm với Cloud Vision API / Gemini Vision — những cái đó tính tiền theo request.
- **Uni-Sign, NLA-SLR, OpenHands** — license theo repo, phần lớn cho nghiên cứu. Đọc kỹ `LICENSE` trước khi thương mại hoá.
- **Dataset** thường có license **riêng và chặt hơn** code. WLASL và MS-ASL đặc biệt cần chú ý vì video lấy từ nguồn web.
- **VSL400** — video ở chế độ controlled access, sẽ có thoả thuận riêng khi được cấp quyền.
- **Video do bạn quay** — cần **văn bản đồng ý** của người ký hiệu cho việc dùng thương mại và công khai. Làm ngay từ buổi quay đầu tiên.

---

## Nguồn

- [Uni-Sign: Toward Unified Sign Language Understanding at Scale (ICLR'25) — GitHub](https://github.com/ZechengLi19/Uni-Sign) · [HuggingFace](https://huggingface.co/ZechengLi19/Uni-Sign) · [paper](https://arxiv.org/pdf/2501.15187)
- [SignBart — New approach with the skeleton sequence for Isolated Sign Language Recognition](https://arxiv.org/abs/2506.21592)
- [NLA-SLR / WLASL SOTA discussion](https://arxiv.org/pdf/2506.21592)
- [OpenHands: Making Sign Language Recognition Accessible (ACL 2022)](https://github.com/AI4Bharat/OpenHands) · [paper](https://aclanthology.org/2022.acl-long.150.pdf)
- [SignBERT+: Hand-model-aware Self-supervised Pre-training](https://arxiv.org/pdf/2305.04868)
- [Google — Isolated Sign Language Recognition (Kaggle, PopSign)](https://www.kaggle.com/competitions/asl-signs) · [solution repo](https://github.com/abhinand5/isolated-sign-language-recognition) · [solution repo](https://github.com/JosephZahar/Google-Isolated-Sign-Language-Recognition-Kaggle)
- [VSL400: A Multi-view Dataset for Vietnamese Word-Level Sign Language Recognition](https://www.authorea.com/doi/full/10.22541/au.177075292.21810740/v1)
- [VOYA_VSL dataset](https://huggingface.co/datasets/Kateht/VOYA_VSL)
- [A Real-Time Vietnamese Sign Language Recognition System Using a Lightweight LSTM Model](https://link.springer.com/chapter/10.1007/978-3-032-19488-6_8)
- [A Comprehensive Review of Vietnamese Sign Language Recognition](https://jst.vn/index.php/etsd/article/download/1293/1315)
- [ASL Citizen: A Community-Sourced Dataset](https://arxiv.org/pdf/2304.05934)
- [MediaPipe Holistic Landmarker](https://developers.google.com/edge/mediapipe/solutions/vision/holistic_landmarker)
- [Awesome-Sign-Language (paper list)](https://github.com/ZechengLi19/Awesome-Sign-Language)
