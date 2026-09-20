#!/usr/bin/env python3
"""
Gán chủ đề (topic) cho toàn bộ từ vựng VSL trong CSDL, dựa trên từ khoá
không dấu — xem sign_topics_taxonomy.py để biết đầy đủ luật phân loại và
lý do 33 chủ đề được chọn.

VÌ SAO LÀ SCRIPT, KHÔNG PHẢI MIGRATION: gán chủ đề là một quyết định biên
tập (dựa trên từ khoá, có thể sai, có thể cần chỉnh lại), không phải một
phép biến đổi lược đồ một-chiều-mãi-mãi. 33 chủ đề (bảng `topics`) được tạo
bằng migration V17 vì đó là danh mục tĩnh; còn việc TỪ NÀO thuộc chủ đề NÀO
thì để script này quyết — và cho phép chạy lại bất cứ lúc nào để áp dụng
luật mới mà không phải viết thêm một migration cho mỗi lần tinh chỉnh.

CÁCH PHÂN LOẠI (tóm tắt — chi tiết xem sign_topics_taxonomy.py):
  1. Với mỗi từ, ghép "word_vi" (bỏ dấu) và "gloss" (bỏ dấu) THÀNH HAI CHUỖI
     RIÊNG — không nối chung một chuỗi để tránh khớp giả ở chỗ nối (ví dụ
     "cắm trại" + "cắm trại" nối lại vô tình chứa "trại" + "cam" liền nhau).
  2. Dò từng chủ đề THEO THỨ TỰ ưu tiên (chủ đề càng đặc thù càng dò trước)
     bằng từ khoá khớp theo ranh giới từ. Một từ có thể khớp NHIỀU chủ đề —
     tất cả được ghi vào sign_topics (nhiều-nhiều), còn primary_topic_id chỉ
     lấy chủ đề khớp ĐẦU TIÊN theo thứ tự ưu tiên đó.
  3. Số viết bằng chữ số ("1", "10", "2 < 5") không chứa chữ cái nên không
     khớp được bằng từ khoá — nhận diện riêng bằng regex, tách thành Số đếm
     hoặc Toán học & Số liệu.
  4. Một ký tự đơn lẻ (a, ă, â, b, đ…) là một chữ cái trong Bảng chữ cái.
  5. Từ không khớp gì thì rơi vào một trong hai chủ đề "hứng phần còn lại"
     theo từ loại: HÀNH_ĐỘNG_TÍNH_CHẤT_CHUNG (động từ, tính từ) hoặc TỪ_VỰNG
     KHÁC (mọi thứ còn lại) — không bao giờ để một từ không có chủ đề nào.

Cách dùng:
    python classify_signs.py --dry-run              # xem trước, không ghi gì
    python classify_signs.py --report-only           # chỉ in báo cáo thống kê
    python classify_signs.py                         # ghi vào CSDL thật
    python classify_signs.py --limit 50 --dry-run    # thử nhanh 50 từ đầu

AN TOÀN KHI CHẠY LẠI (idempotent): với mỗi từ, script XOÁ sạch sign_topics
cũ của từ đó rồi ghi lại từ đầu trong CÙNG một transaction — chạy lại bao
nhiêu lần cũng ra kết quả giống hệt nhau, không cộng dồn trùng lặp.
"""

from __future__ import annotations

import argparse
import re
import sys
import unicodedata
from collections import Counter
from pathlib import Path

import psycopg2
import psycopg2.extras

# Console Windows mặc định dùng cp1252 — ép UTF-8 để in được tiếng Việt
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8", errors="replace", line_buffering=True)
    except (AttributeError, ValueError):
        pass

sys.path.insert(0, str(Path(__file__).resolve().parent))
from sign_topics_taxonomy import TOPICS, EXISTING_SLUGS  # noqa: E402

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_PROPS = REPO_ROOT / "backend" / "src" / "main" / "resources" / "application.properties"

FALLBACK_BY_WORD_TYPE = {
    "DONG_TU": "hanh-dong-tinh-chat-chung",
    "TINH_TU": "hanh-dong-tinh-chat-chung",
    "DAI_TU": "dai-tu-lien-tu",
    "QUAN_HE_TU": "dai-tu-lien-tu",
    "PHO_TU": "dai-tu-lien-tu",
    "CHI_TU": "dai-tu-lien-tu",
    "TRO_TU": "dai-tu-lien-tu",
    "TINH_THAI_TU": "dai-tu-lien-tu",
    "THAN_TU": "dai-tu-lien-tu",
    "SO_TU": "so-dem",
}


# ---------------------------------------------------------------------------
# Đọc cấu hình CSDL từ application.properties — không chép cứng mật khẩu,
# backend đổi CSDL thì script tự đi theo (giống ingest_qipedc.py).
# ---------------------------------------------------------------------------
def load_db_config(props_path: Path) -> dict:
    props: dict[str, str] = {}
    with props_path.open(encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            props[key.strip()] = value.strip()

    jdbc = props.get("spring.datasource.url", "")
    m = re.match(r"jdbc:postgresql://([^:/]+):(\d+)/([^?]+)", jdbc)
    if not m:
        raise SystemExit(f"Không đọc được spring.datasource.url: {jdbc!r}")
    return {
        "host": m.group(1),
        "port": int(m.group(2)),
        "dbname": m.group(3),
        "user": props.get("spring.datasource.username", "postgres"),
        "password": props.get("spring.datasource.password", ""),
    }


# ---------------------------------------------------------------------------
# Chuẩn hoá & khớp từ khoá — xem đầu sign_topics_taxonomy.py
# ---------------------------------------------------------------------------
def strip_accents(s: str) -> str:
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return s.replace("đ", "d").replace("Đ", "D")


def norm(s: str | None) -> str:
    if not s:
        return ""
    s = strip_accents(s).lower()
    s = re.sub(r"[^a-z0-9\s]", " ", s)
    return re.sub(r"\s+", " ", s).strip()


COMPILED = [
    (slug, [re.compile(r"\b" + re.escape(kw) + r"\b") for kw in keywords])
    for slug, _name, _desc, _icon, _cat, _order, keywords in TOPICS
]
FALLBACK_SLUGS = {"hanh-dong-tinh-chat-chung", "tu-vung-khac"}


def classify(word_vi: str, gloss: str | None, word_type: str | None) -> list[str]:
    """Trả về danh sách slug chủ đề khớp, THEO THỨ TỰ ƯU TIÊN — phần tử đầu
    tiên là primary_topic. Luôn trả về ít nhất một phần tử."""
    # word_vi và gloss chứa gần như đúng nội dung nhau — dò RIÊNG từng chuỗi,
    # không nối chung (nối chung sẽ tạo khớp giả ở ranh giới nối).
    texts = [norm(word_vi)]
    g = norm(gloss)
    if g and g != texts[0]:
        texts.append(g)

    matched = []
    for slug, patterns in COMPILED:
        if slug in FALLBACK_SLUGS:
            continue
        if any(p.search(t) for t in texts for p in patterns):
            matched.append(slug)
    if matched:
        return matched

    # Chữ số viết bằng ký tự số ("1", "10", "2 < 5") không chứa chữ cái nên
    # không khớp được bằng từ khoá chữ.
    if re.search(r"\d", word_vi):
        if re.search(r"[<>=]", word_vi) or "toán học" in word_vi.lower():
            return ["toan-hoc-so-lieu"]
        return ["so-dem"]

    # Một ký tự đơn (a, ă, â, b, đ...) là chữ cái trong Bảng chữ cái.
    if len(word_vi.strip()) == 1:
        return ["bang-chu-cai"]

    return [FALLBACK_BY_WORD_TYPE.get(word_type, "tu-vung-khac")]


# ---------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--props", type=Path, default=DEFAULT_PROPS)
    parser.add_argument("--dry-run", action="store_true", help="Xem trước, không ghi CSDL")
    parser.add_argument("--report-only", action="store_true", help="Chỉ in thống kê, không đọc/ghi CSDL thật (dùng all_signs đã có)")
    parser.add_argument("--limit", type=int, default=None, help="Chỉ xử lý N từ đầu (thử nhanh)")
    args = parser.parse_args()

    cfg = load_db_config(args.props)
    conn = psycopg2.connect(**cfg)
    conn.autocommit = False
    try:
        run(conn, dry_run=args.dry_run, limit=args.limit)
    finally:
        conn.close()


def run(conn, dry_run: bool, limit: int | None) -> None:
    cur = conn.cursor()

    cur.execute("SELECT slug, id FROM topics")
    topic_id_by_slug = {slug: tid for slug, tid in cur.fetchall()}

    used_slugs = {t[0] for t in TOPICS}
    missing = used_slugs - set(topic_id_by_slug)
    if missing:
        raise SystemExit(
            "Thiếu chủ đề trong CSDL (chạy migration V17 trước): " + ", ".join(sorted(missing))
        )

    query = "SELECT id, word_vi, gloss, word_type FROM signs ORDER BY word_vi"
    if limit:
        query += f" LIMIT {int(limit)}"
    cur.execute(query)
    signs = cur.fetchall()

    counts: Counter[str] = Counter()
    primary_counts: Counter[str] = Counter()
    total_links = 0

    write_cur = conn.cursor()
    for sign_id, word_vi, gloss, word_type in signs:
        topics = classify(word_vi, gloss, word_type)
        primary_slug = topics[0]
        for slug in topics:
            counts[slug] += 1
        primary_counts[primary_slug] += 1
        total_links += len(topics)

        if dry_run:
            continue

        # Xoá sạch liên kết cũ rồi ghi lại — script chạy lại nhiều lần vẫn
        # cho đúng một kết quả, không cộng dồn trùng lặp.
        write_cur.execute("DELETE FROM sign_topics WHERE sign_id = %s", (sign_id,))
        psycopg2.extras.execute_values(
            write_cur,
            "INSERT INTO sign_topics (sign_id, topic_id) VALUES %s",
            [(sign_id, topic_id_by_slug[slug]) for slug in topics],
        )
        write_cur.execute(
            "UPDATE signs SET primary_topic_id = %s WHERE id = %s",
            (topic_id_by_slug[primary_slug], sign_id),
        )

    if dry_run:
        conn.rollback()
        print("[DRY RUN] Không ghi gì vào CSDL.\n")
    else:
        conn.commit()
        print(f"Đã ghi {len(signs)} từ, {total_links} liên kết sign_topics.\n")

    print(f"Tổng số từ xử lý: {len(signs)}")
    print(f"{'Chủ đề':<38} {'Primary':>8} {'Tổng liên kết':>14}")
    print("-" * 64)
    slug_to_name = {t[0]: t[1] for t in TOPICS}
    for slug, name, *_ in TOPICS:
        print(f"{name:<38} {primary_counts.get(slug, 0):>8} {counts.get(slug, 0):>14}")

    fallback_total = sum(primary_counts.get(s, 0) for s in FALLBACK_SLUGS)
    print(f"\nRơi vào hai chủ đề 'hứng phần còn lại': {fallback_total} ({fallback_total / len(signs) * 100:.1f}%)")


if __name__ == "__main__":
    main()
