-- Gói dịch vụ Free / Premium và thanh toán chuyển khoản qua SePay (2026-10-04).
--
-- Luồng: người học bấm "Nâng cấp" → tạo payment_orders (mã thanh toán ngẫu nhiên, khó đoán)
-- → quét QR chuyển khoản đúng số tiền + nội dung chứa mã → SePay gọi webhook → lưu
-- sepay_transactions (CHỐNG TRÙNG bằng id giao dịch của SePay) → khớp mã với đơn → gia hạn
-- users.premium_until.
--
-- Không lưu số tài khoản / khoá bí mật ở CSDL: cấu hình qua biến môi trường (SEPAY_*).

-- Premium tính đến thời điểm nào. NULL hoặc đã qua = gói Free.
ALTER TABLE users ADD COLUMN premium_until timestamptz;

CREATE TABLE payment_orders (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan_code            varchar(20) NOT NULL CHECK (plan_code IN ('PREMIUM')),
    amount               bigint      NOT NULL CHECK (amount > 0),
    -- Nội dung chuyển khoản. SePay nhận diện theo tiền tố cấu hình ở
    -- "Công ty → Cấu hình chung → Cấu trúc mã thanh toán" (mặc định SIGNAI + 8 ký tự).
    payment_code         varchar(40) NOT NULL UNIQUE,
    status               varchar(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'PAID', 'EXPIRED', 'CANCELLED')),
    duration_days        integer     NOT NULL CHECK (duration_days > 0),
    expires_at           timestamptz NOT NULL,
    paid_at              timestamptz,
    paid_amount          bigint,
    sepay_transaction_id bigint,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_payment_orders_user ON payment_orders (user_id, created_at DESC);

-- Mọi giao dịch SePay báo về, kể cả giao dịch không khớp đơn nào (để đối soát tay).
CREATE TABLE sepay_transactions (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- id giao dịch phía SePay: UNIQUE để webhook gửi lại (tối đa 7 lần trong 5 giờ) không cộng tiền hai lần
    sepay_id         bigint      NOT NULL UNIQUE,
    gateway          varchar(100),
    transaction_date varchar(30),
    account_number   varchar(50),
    sub_account      varchar(50),
    code             varchar(100),
    content          text,
    transfer_type    varchar(10),
    description      text,
    transfer_amount  bigint,
    accumulated      bigint,
    reference_code   varchar(100),
    raw_payload      jsonb       NOT NULL,
    matched_order_id uuid REFERENCES payment_orders (id) ON DELETE SET NULL,
    -- Vì sao khớp / không khớp, để người đối soát đọc được ngay
    note             text,
    created_at       timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_sepay_transactions_order ON sepay_transactions (matched_order_id);
