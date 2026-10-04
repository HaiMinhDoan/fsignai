#!/usr/bin/env python3
"""
Sinh SQL tạo tài khoản SignAI từ danh sách sinh viên (user_data/*.json) — dự án của trường.

Chỉ lấy ĐÚNG những gì tài khoản cần, không chép thêm dữ liệu cá nhân vào CSDL của ứng dụng:
  • Họ tên, email trường            → full_name, email
  • Năm sinh                         → age_range (chỉ giữ khoảng tuổi, không giữ ngày sinh)
  • Tỉnh trong địa chỉ / nơi cấp     → region (Bắc / Trung / Nam — để chọn video ký hiệu theo vùng)
KHÔNG lấy: số CCCD, số điện thoại, địa chỉ chi tiết, ngày sinh, giới tính, thông tin bố mẹ.

Tài khoản tạo ra:
  • Mật khẩu ngẫu nhiên không ai biết (kể cả người chạy script) → không ai đăng nhập thay sinh viên
    được. Sinh viên tự nhận tài khoản bằng "Quên mật khẩu" với email trường.
  • Tắt thông báo email (notify_email = false): sinh viên chưa tự đăng ký, không gửi thư nhắc học.
  • Vai trò STUDENT, loại tài khoản ADULT (sinh viên đại học), chưa làm onboarding.
  • Chạy lại nhiều lần không tạo trùng: bỏ qua email đã tồn tại.

Cách dùng:
  python3 backend/scripts/import_roster_users.py --total 70 > /tmp/roster_users.sql
  psql "$DB_URL" -v ON_ERROR_STOP=1 -f /tmp/roster_users.sql

KHÔNG commit tệp .sql sinh ra: nó chứa họ tên và email thật.
"""
import argparse
import json
import os
import re
import sys
import unicodedata
from datetime import date

NORTH = {
    'hà nội', 'hải phòng', 'hải dương', 'hưng yên', 'bắc ninh', 'bắc giang', 'vĩnh phúc', 'phú thọ', 'thái nguyên',
    'thái bình', 'nam định', 'ninh bình', 'hà nam', 'hòa bình', 'hoà bình', 'quảng ninh', 'lạng sơn', 'cao bằng',
    'bắc kạn', 'hà giang', 'tuyên quang', 'yên bái', 'lào cai', 'lai châu', 'điện biên', 'sơn la',
}
CENTRAL = {
    'thanh hóa', 'thanh hoá', 'nghệ an', 'hà tĩnh', 'quảng bình', 'quảng trị', 'thừa thiên huế', 'thừa thiên - huế',
    'huế', 'đà nẵng', 'quảng nam', 'quảng ngãi', 'bình định', 'phú yên', 'khánh hòa', 'khánh hoà', 'ninh thuận',
    'bình thuận', 'kon tum', 'gia lai', 'đắk lắk', 'đắk nông', 'lâm đồng',
}
SOUTH = {
    'hồ chí minh', 'hcm', 'tp.hồ chí minh', 'tp. hồ chí minh', 'thành phố hồ chí minh', 'đồng nai', 'bình dương',
    'bình phước', 'tây ninh', 'bà rịa - vũng tàu', 'bà rịa vũng tàu', 'long an', 'tiền giang', 'bến tre',
    'đồng tháp', 'vĩnh long', 'trà vinh', 'an giang', 'kiên giang', 'cần thơ', 'hậu giang', 'sóc trăng',
    'bạc liêu', 'cà mau',
}

# Thứ tự lấy file và số người mỗi khối — rải đều các ngành
FILES = ['SE18', 'SS18', 'SA18', 'HE18', 'HS18', 'HA18']


def region_of(*places):
    for p in places:
        if not p:
            continue
        # Dữ liệu gõ tay lẫn hai kiểu dấu tiếng Việt (dựng sẵn / tổ hợp), khoảng trắng cứng, dấu chấm cuối
        p = unicodedata.normalize('NFC', p).replace('\xa0', ' ')
        # Lấy tỉnh ở cuối địa chỉ: "..., tỉnh Bình Phước" / "..., TP. Hồ Chí Minh"
        last = p.split(',')[-1].strip(' .;').lower()
        last = re.sub(r'^(tỉnh|thành phố|tp\.?)\s*', '', last).strip()
        for cand in (last, p.strip(' .;').lower()):
            if cand in NORTH or any(cand.endswith(x) for x in NORTH):
                return 'NORTH'
            if cand in CENTRAL or any(cand.endswith(x) for x in CENTRAL):
                return 'CENTRAL'
            if cand in SOUTH or any(cand.endswith(x) for x in SOUTH):
                return 'SOUTH'
    return 'COMMON'


def age_range_of(dob):
    try:
        y, m, d = (int(x) for x in dob[:10].split('-'))
    except (TypeError, ValueError, AttributeError):
        return None
    today = date.today()
    age = today.year - y - ((today.month, today.day) < (m, d))
    if age < 12:
        return 'UNDER_12'
    if age <= 17:
        return 'AGE_12_17'
    if age <= 24:
        return 'AGE_18_24'
    if age <= 34:
        return 'AGE_25_34'
    if age <= 49:
        return 'AGE_35_49'
    return 'AGE_50_PLUS'


def q(s):
    return 'NULL' if s is None else "'" + s.replace("'", "''") + "'"


def pick(data_dir, total):
    per = [total // len(FILES) + (1 if i < total % len(FILES) else 0) for i in range(len(FILES))]
    chosen, seen = [], set()
    for name, n in zip(FILES, per):
        with open(os.path.join(data_dir, f'{name}.json'), encoding='utf-8') as f:
            rows = json.load(f)
        rows.sort(key=lambda r: r.get('RollNumber') or '')
        got = 0
        for r in rows:
            email = (r.get('Email') or '').strip()
            full_name = (r.get('Fullname') or '').strip()
            # Chỉ sinh viên đang học (HD), có email trường hợp lệ
            if r.get('StatusCode') != 'HD' or not full_name:
                continue
            if not re.fullmatch(r'[^@\s]+@fpt\.edu\.vn', email, re.I) or email.lower() in seen:
                continue
            seen.add(email.lower())
            chosen.append({
                'full_name': re.sub(r'\s+', ' ', full_name)[:150],
                'email': email,
                'age_range': age_range_of(r.get('DateOfBirth')),
                'region': region_of(r.get('Address'), r.get('PlaceOfIssue')),
            })
            got += 1
            if got == n:
                break
    return chosen


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--data-dir', default=os.path.join(os.path.dirname(__file__), '..', '..', 'user_data'))
    ap.add_argument('--total', type=int, default=70)
    a = ap.parse_args()

    users = pick(a.data_dir, a.total)
    if len(users) < a.total:
        sys.exit(f'Chỉ tìm được {len(users)} sinh viên hợp lệ, cần {a.total}')

    values = ',\n'.join(
        f"    ({q(u['full_name'])}, {q(u['email'])}, {q(u['age_range'])}, {q(u['region'])})" for u in users)
    print(f"""-- Sinh bởi backend/scripts/import_roster_users.py — {len(users)} tài khoản sinh viên.
-- CHỨA HỌ TÊN VÀ EMAIL THẬT: không commit tệp này.
BEGIN;

CREATE TEMP TABLE roster (full_name varchar(150), email varchar(255), age_range varchar(20), region varchar(20))
    ON COMMIT DROP;
INSERT INTO roster (full_name, email, age_range, region) VALUES
{values};

-- Mật khẩu: bcrypt của một chuỗi ngẫu nhiên không lưu ở đâu cả → không ai đăng nhập được
-- cho tới khi sinh viên tự đặt mật khẩu qua "Quên mật khẩu".
WITH moi AS (
    INSERT INTO users (email, password_hash, full_name, age_range, account_kind, user_type, region, status)
    SELECT r.email, crypt(gen_random_uuid()::text || gen_random_uuid()::text, gen_salt('bf', 10)),
           r.full_name, r.age_range, 'ADULT', 'OTHER', r.region, 'ACTIVE'
      FROM roster r
     WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email_normalized = lower(r.email))
    RETURNING id
), vai_tro AS (
    INSERT INTO user_roles (user_id, role_id)
    SELECT moi.id, ro.id FROM moi CROSS JOIN roles ro WHERE ro.code = 'STUDENT'
    RETURNING user_id
)
INSERT INTO user_settings (user_id, notify_email)
SELECT moi.id, FALSE FROM moi;

SELECT count(*) AS so_tai_khoan_sinh_vien
  FROM users u JOIN roster r ON lower(r.email) = u.email_normalized;

COMMIT;
""")


if __name__ == '__main__':
    main()
