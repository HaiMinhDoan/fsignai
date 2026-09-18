# SignAI — Thiết kế cơ sở dữ liệu

PostgreSQL 16. Migration bằng Flyway. Đặt tên bảng số nhiều, snake_case.
Mọi bảng có `id BIGSERIAL`, `created_at`, `updated_at`.

---

## 1. Tài khoản & onboarding

```sql
users
  id, email (unique), password_hash (null nếu chỉ dùng Google),
  google_id (unique, null), full_name, avatar_url, address,
  age_range,            -- enum: UNDER_12, 12_17, 18_24, 25_34, 35_49, 50_PLUS
  user_type,            -- enum: DEAF_HOH, FAMILY, FRIEND, CAREGIVER, OTHER
  status,               -- ACTIVE, PENDING_VERIFICATION, DISABLED
  email_verified_at, last_login_at,

  -- VAI TRÒ CHUYÊN MÔN VSL — hỏi lúc đăng ký, xem §1.1
  vsl_role,             -- LEARNER | TEACHER | DEAF_NATIVE | INTERPRETER
  vsl_role_status,      -- SELF_DECLARED | PENDING | VERIFIED | REJECTED
  vsl_role_evidence,    -- người dùng tự khai: trường/trung tâm, số chứng chỉ...
  vsl_role_verified_by (FK, null),
  vsl_role_verified_at

user_settings
  user_id (FK, unique), locale (default 'vi'),
  daily_goal_minutes,   -- 5 | 10 | 15 | 30
  notify_email, notify_push, notify_streak, notify_new_course,
  reminder_time,        -- TIME, giờ gửi nhắc học
  timezone              -- default 'Asia/Ho_Chi_Minh'

onboarding_responses
  user_id (FK, unique),
  learn_reason,         -- FAMILY, FRIENDS, PERSONAL, WORK, BASIC_COMM, OTHER
  current_level,        -- BEGINNER, BASIC, INTERMEDIATE, ADVANCED, UNSURE
  daily_minutes,
  interested_topics,    -- jsonb: ["family","food","school"]
  completed_at

refresh_tokens
  user_id (FK), token_hash, expires_at, revoked_at, user_agent, ip
```

### 1.1. Vai trò chuyên môn VSL — nguồn tin cậy để hiệu chỉnh ngưỡng

Hỏi ngay ở form đăng ký, **một trường duy nhất, không bắt buộc**, mặc định `LEARNER`. Đăng ký phải ngắn —
95% người dùng là người học, đừng bắt họ đọc thêm nửa màn hình.

```
Bạn là ai trong cộng đồng ngôn ngữ ký hiệu?
 (•) Người học                      → LEARNER      (mặc định)
 ( ) Người điếc sử dụng VSL hằng ngày → DEAF_NATIVE
 ( ) Giáo viên VSL / giáo viên dạy trẻ điếc → TEACHER
 ( ) Phiên dịch viên NNKH            → INTERPRETER
     └─ chọn 3 mục dưới → hiện ô "Nơi công tác / chứng chỉ" và ghi chú
        "Chúng tôi sẽ xác minh trước khi kích hoạt quyền góp ý chuyên môn."
```

**`DEAF_NATIVE` phải có trong danh sách, và có trọng số ngang giáo viên.** Người điếc dùng VSL hằng ngày là
người bản ngữ của ngôn ngữ đó — về tính đúng đắn của một ký hiệu, họ là nguồn đáng tin nhất, thường hơn cả
giáo viên nghe được. Một hệ thống chỉ tin "giáo viên" sẽ vô tình gạt đúng những người sở hữu ngôn ngữ này
ra khỏi việc thẩm định nó.

### Trọng số góp ý

| `vsl_role` | `vsl_role_status` | Trọng số | Dùng để |
|---|---|---|---|
| `TEACHER` / `DEAF_NATIVE` / `INTERPRETER` | `VERIFIED` | **5** | Hiệu chỉnh ngưỡng DTW |
| như trên | `PENDING` / `SELF_DECLARED` | 0 | Lưu lại, kích hoạt ngược khi được duyệt |
| `LEARNER` | bất kỳ | **1** | Không dùng chỉnh ngưỡng; chỉ dùng **phát hiện bất thường** (xem dưới) |

**Vẫn thu góp ý của người học dù không dùng để chỉnh ngưỡng.** Hai loại tín hiệu khác nhau:
*thẩm quyền* (một giáo viên nói sai → tin ngay) và *số đông* (200 người học cùng kêu một từ chấm sai →
chắc chắn có vấn đề, kể cả khi chưa giáo viên nào đụng tới). Bỏ nhóm thứ hai là bỏ hệ thống cảnh báo sớm.

**Bắt buộc xác minh, không tin lời tự khai.** Ai cũng tích được ô "giáo viên". Nếu ngưỡng chấm điểm của
4.000 từ được hiệu chỉnh từ lời tự khai, một tài khoản cẩu thả là đủ làm lệch. Chỉ `VERIFIED` mới có trọng số,
và việc duyệt do admin làm trong CMS — dùng lại đúng vai trò `VSL_REVIEWER` đã có ở
[04-admin-cms.md](04-admin-cms.md).

```sql
ai_check_feedback            -- "chấm như vậy có đúng không?" 👍/👎
  user_id (FK), ai_check_result_id (FK),
  verdict,                   -- AGREE | DISAGREE
  note,                      -- tuỳ chọn: "tôi làm đúng mà bị báo chưa khớp"
  weight_snapshot,           -- trọng số TẠI THỜI ĐIỂM góp ý
  role_snapshot,             -- vsl_role tại thời điểm đó
  created_at
  UNIQUE(user_id, ai_check_result_id)
```

**`weight_snapshot` phải chụp lại tại thời điểm góp ý.** Nếu tính trọng số động theo vai trò hiện tại,
thì một người được duyệt hoặc bị gỡ vai trò sẽ làm toàn bộ ngưỡng đã hiệu chỉnh trong quá khứ âm thầm
đổi theo — và không ai giải thích nổi vì sao điểm số hôm nay khác hôm qua.

`interested_topics` để `jsonb` vì danh sách chủ đề sẽ thay đổi và câu trả lời onboarding
là dữ liệu lịch sử — không nên ràng buộc FK vào bảng topic có thể bị xoá.

---

## 2. Từ điển VSL

Đây là trung tâm của toàn hệ thống. Bài học, flashcard, quiz đều trỏ về `signs`.

```sql
topics
  slug (unique), name_vi, description_vi, icon_url, cover_url,
  display_order, category      -- SIMPLE_SIGN, COMPLEX_SIGN, SITUATION
  is_published

signs                          -- một mục từ vựng VSL
  gloss (unique),              -- mã định danh: "ME", "CHA", "GIA_DINH"
  word_vi,                     -- "mẹ"
  word_vi_normalized,          -- "me"  (bỏ dấu, phục vụ search)
  word_en,                     -- "mother"
  description_vi,              -- mô tả cách làm ký hiệu bằng chữ (a11y)
  level,                       -- BEGINNER, BASIC, INTERMEDIATE, ADVANCED
  primary_topic_id (FK),

  -- PHÂN LOẠI (xem §2.1)
  unit_type,                   -- LETTER | NUMBER | WORD | PHRASE | SENTENCE
  word_type,                   -- từ loại tiếng Việt, xem bảng dưới
  word_subtype,                -- phân loại con, ví dụ danh từ riêng
  domain,                      -- lĩnh vực: MATH, GEOGRAPHY, COUNTRY, MEDICAL...

  search_vector,               -- tsvector, GIN index
  is_published

sign_topics                    -- một từ thuộc nhiều chủ đề
  sign_id (FK), topic_id (FK)  -- PK kép

sign_videos                    -- một từ có nhiều video (nhiều góc, nhiều người ký hiệu)
  sign_id (FK), storage_key, thumbnail_key,
  duration_ms, width, height,
  view_angle,                  -- FRONT, LEFT, RIGHT
  region,                      -- NORTH | CENTRAL | SOUTH | COMMON  ← xem ghi chú dưới
  signer_label,                -- ẩn danh: "signer_01"
  is_primary,                  -- video hiển thị mặc định
  caption_vi                   -- phụ đề

sign_relations                 -- "Related: Cha, Bố, Con, Gia đình"
  sign_id (FK), related_sign_id (FK),
  relation_type                -- RELATED, SYNONYM, ANTONYM, EASILY_CONFUSED
```

### 2.1. Phân loại từ vựng theo chuẩn tiếng Việt

Bốn trục phân loại độc lập. Một mục từ mang cả bốn, không phải chọn một.

**Trục 1 — `unit_type` (đơn vị ngôn ngữ).** Bắt buộc, vì từ điển VSL không chỉ chứa từ:

| Giá trị | Nghĩa | Ví dụ có thật trong dữ liệu Bộ GD&ĐT |
|---|---|---|
| `LETTER` | Chữ cái ngón tay | A, B, Ă, Â, Đ, Ơ, Ư + dấu thanh |
| `NUMBER` | Chữ số, số đếm | 0, 7, 40, 1000 |
| `WORD` | Từ đơn / từ ghép | mẹ, gia đình, tiếp tân |
| `PHRASE` | Cụm từ, thành ngữ | "ba chân bốn cẳng", "2 nhỏ hơn 5" |
| `SENTENCE` | Câu giao tiếp trọn vẹn | "xin chào, rất vui được gặp bạn" |

**Trục 2 — `word_type` (từ loại).** Theo ngữ pháp tiếng Việt phổ thông:

| Nhóm | `word_type` | Ghi chú |
|---|---|---|
| **Thực từ** | `DANH_TU` | `word_subtype`: `CHUNG` / `RIENG` / `DON_VI` |
| | `DONG_TU` | |
| | `TINH_TU` | |
| | `SO_TU` | |
| | `DAI_TU` | tôi, bạn, nó, ai, gì |
| **Hư từ** | `PHO_TU` | đã, đang, sẽ, rất, lắm |
| | `QUAN_HE_TU` | và, nhưng, của, với, vì |
| | `LUONG_TU` | những, các, mọi |
| | `CHI_TU` | này, kia, đó, ấy |
| | `TRO_TU` | chính, ngay, cả |
| | `TINH_THAI_TU` | à, ư, nhỉ, nhé, ạ |
| | `THAN_TU` | ôi, ái, than ôi |
| **Khác** | `KHONG_XAC_DINH` | dùng cho `LETTER`, `NUMBER`, `SENTENCE` |

**Trục 3 — `domain` (lĩnh vực chuyên môn).** Không phải từ nào cũng có:
`MATH`, `GEOGRAPHY` (An Giang, Bà Nà), `COUNTRY` (Albania, Mi-an-ma), `MEDICAL`, `LEGAL`, `SCHOOL`, `IT`, `NULL`.

**Trục 4 — chủ đề ngữ nghĩa.** Qua bảng `sign_topics` (nhiều-nhiều) — Gia đình, Thực phẩm, Trường học,
Công việc, Cảm xúc, Sức khoẻ, Du lịch, Giao tiếp hằng ngày…

Bốn trục này phục vụ bốn việc khác nhau, nên không gộp được:

| Trục | Dùng để |
|---|---|
| `unit_type` | Lọc bài học — bài đánh vần chỉ lấy `LETTER`, bài hội thoại lấy `SENTENCE` |
| `word_type` | Dạy ngữ pháp VSL ở trình độ cao; lọc bài tập theo từ loại |
| `domain` | Gom khoá chuyên ngành (VSL trong trường học, VSL y tế) |
| Chủ đề | Điều hướng chính của người dùng — cái họ thấy trên giao diện |

**Search tiếng Việt.** Người dùng gõ "me" phải ra "mẹ". Bật extension `unaccent`:

```sql
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE INDEX idx_signs_search ON signs USING GIN (search_vector);
-- search_vector = to_tsvector('simple', unaccent(word_vi) || ' ' || unaccent(coalesce(description_vi,'')))
-- cập nhật bằng trigger
```

**`region` không phải cột phụ — VSL khác nhau theo vùng miền.** Từ điển của Bộ GD&ĐT mã hoá điều này ngay
trong tên file: cùng một từ có ba video `W00665B` / `W00665T` / `W00665N` = **Bắc / Trung / Nam**. Trong bản
khảo sát 2.877 từ, có **310 từ mang đủ ba biến thể vùng miền**.

Hệ quả sản phẩm: nếu người học ở Hà Nội được dạy ký hiệu miền Nam, họ sẽ không giao tiếp được với người điếc
quanh mình. Vì vậy `users` cần biết vùng của người học (suy từ `address` hoặc hỏi thẳng lúc onboarding), và
từ điển phải chọn video theo vùng đó, với `COMMON` làm mặc định khi từ không có biến thể. Thiết kế thiếu cột
này thì sau phải migrate toàn bộ bảng video.

`EASILY_CONFUSED` phục vụ hai việc: cảnh báo người học, và **sinh đáp án nhiễu chất lượng cao cho quiz**.
Quiz với 3 đáp án sai ngẫu nhiên thì quá dễ; lấy từ dễ nhầm mới đo được năng lực thật.

---

## 3. Khoá học & bài học

```sql
courses
  slug (unique), title_vi, description_vi, cover_url,
  level, display_order, is_published

lessons
  course_id (FK), title_vi, description_vi,
  display_order, estimated_minutes, is_published

lesson_items                   -- nội dung trong bài
  lesson_id (FK), sign_id (FK, null),
  item_type,                   -- SIGN, VIDEO, TEXT, PRACTICE, QUIZ
  content_json,                -- jsonb cho TEXT/VIDEO tự do
  display_order

learning_paths                 -- sinh từ onboarding
  user_id (FK), name, generated_from,   -- ONBOARDING | MANUAL
  is_active

learning_path_items
  learning_path_id (FK), course_id (FK), display_order
```

---

## 4. Luyện tập

```sql
flashcard_reviews              -- spaced repetition (SM-2 / FSRS)
  user_id (FK), sign_id (FK),
  ease_factor,                 -- default 2.5
  interval_days, repetitions,
  due_at,                      -- lần ôn tiếp theo
  last_result,                 -- KNOWN | NEEDS_PRACTICE
  last_reviewed_at
  UNIQUE(user_id, sign_id)

quizzes                        -- đề CỐ ĐỊNH, do admin soạn tay
  lesson_id (FK, null), topic_id (FK, null),
  title_vi, quiz_type,         -- LESSON_QUIZ | TOPIC_TEST | PRACTICE
  pass_score,                  -- default 70
  time_limit_seconds (null)

quiz_questions                 -- câu hỏi của đề cố định
  quiz_id (FK),
  question_type,               -- VIDEO_TO_WORD | WORD_TO_VIDEO | MULTIPLE_CHOICE | MATCHING | AI_PERFORM
  sign_id (FK),                -- đáp án đúng
  prompt_vi,
  options_json,                -- jsonb: [{signId, label, videoKey}, ...]
  correct_option_index,
  points, display_order

quiz_blueprints                -- ĐỀ SINH TỰ ĐỘNG, trộn theo lựa chọn người dùng
  code (unique), title_vi,
  topic_ids,                   -- jsonb: chủ đề được rút câu hỏi
  levels,                      -- jsonb: ["BEGINNER","BASIC"]
  unit_types,                  -- jsonb: giới hạn LETTER / WORD / SENTENCE...
  word_types,                  -- jsonb, null = không giới hạn từ loại
  question_type_mix,           -- jsonb: {"VIDEO_TO_WORD":5,"WORD_TO_VIDEO":3,"MATCHING":2}
  question_count, option_count,-- mặc định 10 câu, 4 đáp án
  pass_score, time_limit_seconds,
  distractor_strategy,         -- SAME_TOPIC | EASILY_CONFUSED | MIXED
  is_active

quiz_attempts
  user_id (FK),
  quiz_id (FK, null),          -- đề cố định
  blueprint_id (FK, null),     -- hoặc đề sinh tự động
  region,                      -- vùng miền người dùng chọn cho lần thi này
  generated_questions,         -- jsonb: ẢNH CHỤP đề đã sinh — xem ghi chú
  seed,                        -- số ngẫu nhiên, để tái tạo đúng đề
  score, max_score, passed,
  started_at, submitted_at, duration_seconds

quiz_answers
  attempt_id (FK),
  question_id (FK, null),      -- đề cố định
  question_index (null),       -- đề sinh: vị trí trong generated_questions
  selected_option_index, is_correct,
  ai_check_result_id (FK, null),   -- với câu AI_PERFORM
  answered_at
```

`question_type = AI_PERFORM` là cầu nối tới FastAPI: câu hỏi yêu cầu người dùng **tự làm ký hiệu**
trước webcam thay vì chọn đáp án.

### 4.1. Sinh đề trộn theo chủ đề và vùng miền

Người dùng chọn **chủ đề** + **vùng miền** → hệ thống rút ngẫu nhiên từ kho `signs` và dựng đề tại chỗ.
Thuật toán:

```
1. Lọc kho từ:  sign_topics ∈ topic_ids
                AND level ∈ levels
                AND unit_type ∈ unit_types
                AND is_published
                AND tồn tại sign_videos với region = <vùng đã chọn> HOẶC region = COMMON
2. Trộn, lấy `question_count` từ làm đáp án đúng
3. Với mỗi câu, sinh đáp án nhiễu theo distractor_strategy:
     EASILY_CONFUSED → lấy từ sign_relations (chất lượng cao nhất)
     SAME_TOPIC      → lấy từ cùng chủ đề
     bù bằng từ ngẫu nhiên cùng level nếu chưa đủ
4. Trộn thứ tự đáp án trong từng câu
5. Gán question_type theo question_type_mix
6. Lưu ẢNH CHỤP vào quiz_attempts.generated_questions
```

**Vì sao phải lưu ảnh chụp đề (`generated_questions`), không chỉ lưu `seed`.** Nếu chỉ lưu seed rồi
sinh lại khi cần xem lại bài, chỉ cần một từ bị sửa hoặc gỡ xuất bản là đề tái tạo ra khác với đề người
dùng đã làm. Điểm số sẽ không khớp với câu hỏi, và không ai giải thích được. Ảnh chụp giữ cho lịch sử
bất biến. `seed` vẫn lưu để gỡ lỗi.

**Vùng miền phải lọc ở bước 1, không phải lọc lúc hiển thị.** Nếu chọn từ trước rồi mới tìm video theo
vùng, sẽ có câu không có video đúng vùng và phải thay thế giữa chừng — đề bị lệch khỏi cấu hình.
Đưa điều kiện vùng vào truy vấn lọc ngay từ đầu.

**Chống trùng lặp giữa các lần thi.** Ưu tiên rút những từ người dùng ít gặp gần đây: sắp xếp kho theo
số lần xuất hiện trong `quiz_attempts` 30 ngày qua tăng dần, rồi mới trộn trong nhóm ít gặp nhất.
Không thì người dùng thi 5 lần sẽ gặp lại cùng một nhóm từ.

---

## 5. Tiến độ & giữ chân

```sql
user_lesson_progress
  user_id (FK), lesson_id (FK),
  status,                      -- NOT_STARTED | IN_PROGRESS | COMPLETED
  progress_percent, last_item_id,
  time_spent_seconds, started_at, completed_at
  UNIQUE(user_id, lesson_id)

user_course_progress
  user_id (FK), course_id (FK),
  lessons_completed, lessons_total, progress_percent,
  status, completed_at
  UNIQUE(user_id, course_id)

daily_activity               -- một dòng mỗi user mỗi ngày
  user_id (FK), activity_date (DATE),
  minutes_studied, lessons_completed, signs_reviewed,
  quizzes_taken, goal_met (bool)
  UNIQUE(user_id, activity_date)

user_streaks
  user_id (FK, unique),
  current_streak, longest_streak,
  last_activity_date,
  freeze_available,            -- số "băng" còn lại, mặc định 2
  freeze_used_total,
  streak_start_date

streak_freezes               -- nhật ký dùng băng, để giải thích được khi user thắc mắc
  user_id (FK), used_for_date (DATE),
  granted_reason,            -- MONTHLY_GRANT | ACHIEVEMENT | ADMIN_GIFT
  created_at
  UNIQUE(user_id, used_for_date)

saved_signs                  -- "Từ vựng đã lưu"
  user_id (FK), sign_id (FK), note, saved_at
  UNIQUE(user_id, sign_id)

achievements
  code (unique), name_vi, description_vi, icon_url,
  criteria_json              -- {"type":"streak","value":7}

user_achievements
  user_id (FK), achievement_id (FK), earned_at
  UNIQUE(user_id, achievement_id)
```

### 5.1. Streak — quy tắc đầy đủ

| Câu hỏi | Quy tắc |
|---|---|
| Thế nào là "đã học hôm nay"? | Đạt `user_settings.daily_goal_minutes`, **hoặc** hoàn thành 1 bài học, **hoặc** ôn xong lượt flashcard đến hạn. Ba đường, chỉ cần một |
| Mốc "hôm nay" tính theo gì? | `user_settings.timezone`, mặc định `Asia/Ho_Chi_Minh` — **không** theo giờ server |
| Quên một ngày thì sao? | Tự động tiêu 1 **băng** (`freeze`) nếu còn. Hết băng thì streak về 0 |
| Băng lấy đâu ra? | Mỗi tháng cấp 2, tối đa tích 5. Thêm băng qua thành tích |
| Nghỉ nhiều ngày liền? | Băng chỉ cứu **một ngày liên tiếp**. Nghỉ 2 ngày là mất streak |
| Có mua được băng không? | Không. Sản phẩm giáo dục cho nhóm yếu thế, không gắn cơ chế trả tiền để giữ chuỗi |

Job chạy 01:00 mỗi ngày theo từng múi giờ, `@Scheduled` + ShedLock: duyệt user có
`last_activity_date = hôm kia`, tiêu băng hoặc reset streak, rồi đẩy thông báo.

**Cảnh báo trước khi mất mới là thứ giữ chân người dùng**, không phải thông báo sau khi đã mất.
Gửi nhắc lúc 20:00 giờ địa phương nếu hôm đó chưa đạt mục tiêu và `current_streak >= 3` — dưới 3 ngày
thì chuỗi chưa đủ để người ta thấy tiếc, nhắc chỉ gây phiền.

**Streak tính từ `daily_activity`, không tự tăng một biến đếm.** Biến đếm sẽ lệch khi có lỗi,
khi user đổi múi giờ, hoặc khi cần tính bù. `daily_activity` là dữ liệu gốc, streak là kết quả
suy ra — sai thì tính lại được.

Múi giờ: mốc "hôm nay" theo `user_settings.timezone`, không theo giờ server. Người dùng ở VN
học lúc 23h mà server chạy UTC sẽ bị tính sang ngày hôm sau và mất streak.

---

## 6. AI Checking

```sql
sign_exemplars               -- mẫu chuẩn để so khớp (do FastAPI tạo, Spring Boot lưu)
  sign_id (FK), sign_video_id (FK),
  embedding,                 -- bytea hoặc pgvector(256)
  landmark_key,              -- đường dẫn file landmark trên MinIO
  model_version,
  quality_score,             -- lọc bỏ mẫu kém
  is_active

ai_check_results
  user_id (FK), sign_id (FK),
  score,                     -- 0-100
  passed,
  feedback_json,             -- jsonb: {"handshape":85,"location":62,"movement":78,"hints":["..."]}
  model_version,
  landmark_key,              -- null nếu người dùng không cho lưu
  consent_to_store,          -- bool — mặc định false
  checked_at
```

```sql
verify_thresholds            -- ngưỡng "đạt" cho mỗi từ
  sign_id (FK, unique),
  threshold,                 -- khoảng cách DTW tối đa còn tính là khớp
  source,                    -- EXEMPLAR_DERIVED | GROUP_DEFAULT | FEEDBACK_TUNED | MANUAL
  sample_count,              -- số góp ý có trọng số đã dùng để chỉnh
  updated_by (FK, null), updated_at

verify_threshold_groups      -- ngưỡng mặc định theo nhóm, dùng khi từ chưa có ngưỡng riêng
  unit_type, hand_count,     -- (LETTER|NUMBER|WORD|PHRASE|SENTENCE) × (1|2)
  threshold
  UNIQUE(unit_type, hand_count)
```

### Ba tầng ngưỡng, xếp theo thứ tự ưu tiên khi tra

| Tầng | `source` | Có từ khi nào | Phủ bao nhiêu từ |
|---|---|---|---|
| 1 | `FEEDBACK_TUNED` | Sau MVP, khi đủ góp ý trọng số | Các từ dùng nhiều nhất |
| 2 | `EXEMPLAR_DERIVED` | **Tuần 4**, job batch | ~500 từ có ≥2 exemplar cùng vùng |
| 3 | `GROUP_DEFAULT` | **Tuần 4** | Toàn bộ phần còn lại |

**Tầng 3 phải có sẵn từ ngày đầu.** Cách hiệu chỉnh từ góp ý giáo viên chỉ hoạt động khi đã có người dùng
và đã có giáo viên được duyệt — ngày mở bán thì cả hai đều bằng không. Nó là **lớp tinh chỉnh**, không phải
lớp khởi tạo. Không có tầng 2 và 3 thì ngày đầu tiên không chấm được từ nào.

Ngưỡng chỉ được cập nhật khi **tổng trọng số góp ý ≥ 10** cho từ đó (ví dụ 2 giáo viên đã xác minh), và
mỗi lần dịch chuyển tối đa 15% để một góp ý lệch không kéo ngưỡng đi quá xa. Mọi thay đổi ghi vào
`updated_by` + `updated_at` để truy vết được.

**`consent_to_store` mặc định `false`.** Dữ liệu chuyển động của người khiếm thính là dữ liệu sinh trắc.
Chỉ lưu khi người dùng chủ động đồng ý (và đây là cách hợp lệ duy nhất để có thêm dữ liệu train).
Nếu dùng `pgvector`, cài extension `vector` và index HNSW trên `sign_exemplars.embedding`.

---

## 7. Thông báo & nội dung

```sql
notifications
  user_id (FK), type,        -- LESSON_REMINDER | STREAK_WARNING | NEW_COURSE | ACHIEVEMENT
  title_vi, body_vi, action_url,
  read_at, sent_at,
  channel                    -- IN_APP | EMAIL | PUSH

push_subscriptions           -- Web Push
  user_id (FK), endpoint, p256dh, auth, user_agent

blog_posts                   -- "blog info vsl", "our mission" ở trang chủ
  slug (unique), title_vi, excerpt_vi, content_md,
  cover_url, author_id (FK), published_at, is_published
```

---

## 7.1. Diễn đàn — bài viết & bình luận bằng video

> **Bình luận video không phải tính năng phụ. Với nhiều người điếc, tiếng Việt viết là ngôn ngữ thứ hai.**
> Bắt họ gõ chữ để tham gia cộng đồng chính là dựng lại đúng rào cản mà sản phẩm này tồn tại để gỡ.
> Vì vậy **video là dạng bình luận ngang hàng với chữ, không phải tuỳ chọn nâng cao** — và ô soạn bình luận
> phải cho quay video ngay ở trạng thái mặc định, không giấu sau một nút "nâng cao".

```sql
forum_categories
  slug (unique), name_vi, description_vi, icon_url,
  display_order, is_locked, is_published

forum_posts                  -- bài viết
  category_id (FK), author_id (FK),
  title_vi, body_md,         -- thân bài dạng markdown, có thể rỗng nếu bài chỉ có video
  sign_id (FK, null),        -- bài gắn với một từ vựng cụ thể
  view_count, comment_count, reaction_count,
  last_activity_at,          -- để sắp xếp "hoạt động gần nhất"
  is_pinned, is_locked,
  status,                    -- DRAFT | PENDING_REVIEW | PUBLISHED | HIDDEN | REMOVED
  published_at

forum_comments
  post_id (FK), author_id (FK),
  parent_id (FK, null),      -- trả lời lồng nhau, giới hạn 2 cấp
  body_text,                 -- có thể rỗng nếu bình luận chỉ có video
  status,                    -- PENDING_REVIEW | PUBLISHED | HIDDEN | REMOVED
  reaction_count, created_at, edited_at

media_assets                 -- video/ảnh do NGƯỜI DÙNG tải lên (tách khỏi sign_videos)
  owner_id (FK),
  kind,                      -- VIDEO | IMAGE
  source,                    -- WEBCAM_RECORDED | FILE_UPLOAD
  storage_key, thumbnail_key,
  duration_ms, width, height, size_bytes, mime_type,
  transcode_status,          -- PENDING | PROCESSING | READY | FAILED
  moderation_status,         -- PENDING | APPROVED | REJECTED
  caption_vi,                -- phụ đề do người đăng tự nhập
  created_at

forum_post_media
  post_id (FK), media_id (FK), display_order
forum_comment_media
  comment_id (FK), media_id (FK), display_order

forum_reactions
  user_id (FK),
  target_type,               -- POST | COMMENT
  target_id, reaction,       -- LIKE | HELPFUL | THANKS
  UNIQUE(user_id, target_type, target_id)

forum_reports                -- người dùng báo cáo nội dung
  reporter_id (FK), target_type, target_id,
  reason,                    -- SPAM | ABUSE | WRONG_SIGN | OFF_TOPIC | OTHER
  note, status,              -- OPEN | REVIEWING | RESOLVED | DISMISSED
  handled_by (FK, null), handled_at

forum_subscriptions          -- theo dõi bài để nhận thông báo
  user_id (FK), post_id (FK)
  UNIQUE(user_id, post_id)
```

### Giới hạn và xử lý video

| Hạng mục | Quy định | Lý do |
|---|---|---|
| Thời lượng | Tối đa **60 giây** | Đủ cho một câu ký hiệu; giữ chi phí lưu trữ có thể dự đoán |
| Kích thước tải lên | Tối đa **100 MB** | |
| Định dạng nhận | `mp4`, `webm`, `mov` | `webm` là thứ `MediaRecorder` xuất ra từ webcam |
| Sau khi nhận | Transcode về **H.264 mp4 720p** + sinh thumbnail | Đồng nhất để phát được trên mọi trình duyệt |
| Công cụ | `ffmpeg` chạy trong worker riêng, hàng đợi qua Redis | Không chặn request |
| Trạng thái | Bình luận hiện ngay với nhãn "đang xử lý", đổi sang phát được khi `transcode_status = READY` | Người dùng không phải chờ |

### Kiểm duyệt

Nội dung video do người dùng tạo, trong một sản phẩm phục vụ cả **học sinh tiểu học điếc**
(đối tượng của dự án QIPEDC) — kiểm duyệt là bắt buộc, không phải tuỳ chọn.

- Mặc định `moderation_status = PENDING`; admin duyệt trong CMS trước khi hiện công khai
- Khi lượng bài tăng, chuyển sang **hậu kiểm** cho người dùng đã có uy tín, **tiền kiểm** cho tài khoản mới
- Mọi nội dung đều báo cáo được; báo cáo vào hàng đợi `forum_reports`
- Xoá là **xoá mềm** (`status = REMOVED`), giữ bản ghi để xử lý khiếu nại

### Phụ đề là bắt buộc, không phải khuyến khích

Một bình luận video không có mô tả chữ thì **người khiếm thị, người chưa biết ký hiệu đó, và công cụ
tìm kiếm đều không đọc được**. Ô `caption_vi` nên bắt buộc nhập tối thiểu một dòng khi đăng video —
đây cũng chính là điều biến diễn đàn thành nguồn dữ liệu tra cứu được theo thời gian.

## 8. Index cần có ngay

```sql
CREATE INDEX idx_signs_level_topic    ON signs(level, primary_topic_id) WHERE is_published;
CREATE INDEX idx_signs_search         ON signs USING GIN (search_vector);
CREATE INDEX idx_flashcard_due        ON flashcard_reviews(user_id, due_at);
CREATE INDEX idx_daily_activity_user  ON daily_activity(user_id, activity_date DESC);
CREATE INDEX idx_lesson_progress_user ON user_lesson_progress(user_id, status);
CREATE INDEX idx_notifications_unread ON notifications(user_id, read_at) WHERE read_at IS NULL;
CREATE INDEX idx_quiz_attempts_user   ON quiz_attempts(user_id, submitted_at DESC);
CREATE INDEX idx_signs_unit_type      ON signs(unit_type, level) WHERE is_published;
CREATE INDEX idx_sign_videos_region   ON sign_videos(sign_id, region);
CREATE INDEX idx_forum_posts_feed     ON forum_posts(category_id, last_activity_at DESC) WHERE status = 'PUBLISHED';
CREATE INDEX idx_forum_comments_post  ON forum_comments(post_id, created_at);
CREATE INDEX idx_media_moderation     ON media_assets(moderation_status, created_at) WHERE moderation_status = 'PENDING';
CREATE INDEX idx_streaks_at_risk      ON user_streaks(last_activity_date) WHERE current_streak > 0;
```

`idx_flashcard_due` là index quan trọng nhất về hiệu năng — truy vấn "thẻ nào đến hạn ôn hôm nay"
chạy mỗi lần người dùng mở mục luyện tập.
