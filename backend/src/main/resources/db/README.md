# CSDL SignAI — Flyway

PostgreSQL 16 · Flyway sở hữu toàn bộ schema · `spring.jpa.hibernate.ddl-auto=none`

## Chạy migration

```bash
# Xem hiện trạng (CHỈ ĐỌC, an toàn)
./mvnw flyway:info

# Áp dụng migration còn thiếu
./mvnw flyway:migrate

# Đối chiếu checksum file với những gì đã chạy
./mvnw flyway:validate

# Trỏ sang DB khác mà không sửa pom
./mvnw flyway:migrate -Ddb.url=jdbc:postgresql://localhost:5432/fsign_dev -Ddb.password=***
```

Ứng dụng Spring Boot **cũng tự chạy migration khi khởi động** (`spring.flyway.enabled=true`),
nên ở môi trường dev thường chỉ cần chạy app là đủ. Plugin Maven dùng khi muốn dựng DB mà
không cần Kafka / Redis / MinIO cùng lên.

> Database `fsign` phải **tồn tại trước**. Flyway tạo được schema, không tạo được database:
> ```sql
> CREATE DATABASE fsign ENCODING 'UTF8';
> ```

## Các file migration

| File | Nội dung |
|---|---|
| `V1__extensions_and_helpers.sql` | `pgcrypto`, `unaccent`, `pg_trgm`; hàm `f_unaccent`, trigger `updated_at`; bảng `file_attachments` |
| `V2__users_and_auth.sql` | `users`, vai trò & quyền, `user_settings`, onboarding, token, audit log |
| `V3__dictionary.sql` | `topics`, `signs`, `sign_videos`, `sign_relations`, `saved_signs`, nhập liệu hàng loạt |
| `V4__courses_and_lessons.sql` | `courses`, `lessons`, `lesson_items`, lộ trình học |
| `V5__practice_and_quiz.sql` | Flashcard SRS, quiz cố định, `quiz_blueprints` (đề trộn), bài làm |
| `V6__progress_and_streak.sql` | Tiến độ, `daily_activity`, streak + băng cứu chuỗi, thành tích |
| `V7__ai_checking.sql` | Exemplar, ngưỡng DTW ba tầng, kết quả chấm, góp ý có trọng số |
| `V8__forum.sql` | Diễn đàn, `media_assets` (video người dùng), kiểm duyệt, báo cáo |
| `V9__notifications_and_content.sql` | Thông báo, Web Push, blog, cấu hình hệ thống, job |
| `V10__seed_reference_data.sql` | Vai trò, quyền, 12 chủ đề, thành tích, mẫu thông báo, ngưỡng mặc định |

## Quy ước

**Khoá chính UUID.** Mọi bảng dùng `UUID PRIMARY KEY DEFAULT gen_random_uuid()`, khớp entity
`FileAttachment` sẵn có. Hibernate sinh UUID ở tầng Java (`GenerationType.AUTO`); giá trị mặc định
ở DB là lớp đỡ cho INSERT trực tiếp từ script.

**Thời gian dùng `TIMESTAMPTZ`** → ánh xạ sang `OffsetDateTime` của Java.

**Không dùng kiểu ENUM của PostgreSQL.** Dự án biểu diễn enum bằng String (xem
`constant/enums/RoleType.java`), nên dùng `VARCHAR + CHECK`. Thêm giá trị mới chỉ cần
`ALTER TABLE ... DROP CONSTRAINT ... ADD CONSTRAINT`, không phải `ALTER TYPE` vốn không rollback được.

**File luôn nằm ở `file_attachments`.** Không bảng nghiệp vụ nào tự lưu `object_key`; chúng chỉ giữ
metadata riêng và trỏ tới `file_attachments.id`. Video ký hiệu → `sign_videos`, video người dùng →
`media_assets`; cả hai đều trỏ sang `file_attachments`.

**`updated_at` có trigger.** Hàm `attach_updated_at('ten_bang')` gắn trigger; gọi ở cuối mỗi migration
cho các bảng mới. Trigger bảo đảm cả thao tác SQL trực tiếp cũng đúng, không chỉ khi đi qua Hibernate.

**Tìm kiếm tiếng Việt.** `signs.word_vi_unaccent` và `signs.search_vector` là **generated column** nên
không bao giờ lệch với dữ liệu. Gõ `me` ra `mẹ`, gõ `dia chi` ra `địa chỉ`.

```sql
SELECT word_vi FROM signs WHERE word_vi_unaccent LIKE '%' || f_unaccent(lower(:q)) || '%';
SELECT word_vi FROM signs WHERE search_vector @@ plainto_tsquery('simple', f_unaccent(:q));
```

## Thêm migration mới

1. Đặt tên `V11__mo_ta_ngan.sql`, số tăng dần, **không sửa file đã chạy** (checksum sẽ lệch)
2. Bảng mới → thêm `created_at` / `updated_at` và gọi `SELECT attach_updated_at('ten_bang');`
3. Chạy `./mvnw flyway:migrate` rồi `./mvnw flyway:info` để xác nhận

Nếu lỡ sửa file đã chạy trên môi trường dev:
`./mvnw flyway:repair` để cập nhật lại checksum.

## Hiện trạng đã kiểm chứng (18.09.2026)

```
60 bảng · 167 index · 104 foreign key · 52 trigger updated_at
Extension: pgcrypto, pg_trgm, unaccent
Dữ liệu mẫu: 6 vai trò · 13 quyền · 12 chủ đề · 4 chuyên mục diễn đàn
              8 thành tích · 9 mẫu thông báo · 10 ngưỡng nhóm · 12 cấu hình
```

## Lưu ý bảo mật

Mật khẩu DB đang nằm trong `pom.xml` (`db.password`) và `application.properties`.
Trước khi đẩy lên repo công khai, chuyển sang biến môi trường:

```properties
spring.datasource.password=${DB_PASSWORD}
```
```bash
./mvnw flyway:migrate -Ddb.password=$DB_PASSWORD
```
