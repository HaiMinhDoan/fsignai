#!/usr/bin/env python3
"""
Nạp từ điển ngôn ngữ ký hiệu của Bộ GD&ĐT (qipedc.moet.gov.vn) vào SignAI.

Chạy một lần, nhưng được thiết kế để CHẠY LẠI ĐƯỢC nhiều lần mà không hỏng dữ
liệu. Tải 4.362 video qua mạng chắc chắn sẽ có lần đứt giữa chừng — máy ngủ,
rớt mạng, server nguồn chậm. Nếu phải xoá sạch làm lại từ đầu mỗi lần thì việc
nạp sẽ không bao giờ xong.

Cách chống trùng: mỗi video có source_url duy nhất, và CSDL đã có sẵn ràng buộc
  CREATE UNIQUE INDEX uq_sign_videos_source_url ON sign_videos (source_url)
nên dù script có sai sót thì CSDL vẫn là chốt chặn cuối cùng.

Hai giai đoạn tách rời:

  Giai đoạn 1 — DANH MỤC (nhanh, vài giây)
      Tạo signs + sign_videos với ingest_status = 'PENDING'.
      Xong bước này là đã quản lý được toàn bộ từ vựng trên giao diện admin,
      không cần chờ tải xong video.

  Giai đoạn 2 — VIDEO (chậm, nhiều giờ)
      Tải từng video, đẩy lên MinIO, ghi file_attachments, cập nhật sign_videos.
      Chạy lại chỉ xử lý những video chưa xong.

Tách hai giai đoạn vì chúng hỏng theo cách khác nhau: danh mục hỏng là do dữ
liệu, video hỏng là do mạng. Gộp chung thì một video lỗi sẽ chặn cả lô.

Cách dùng:
    python ingest_qipedc.py --dry-run              # xem trước, không ghi gì
    python ingest_qipedc.py --limit 20             # thử 20 từ đầu
    python ingest_qipedc.py --catalog-only         # chỉ nạp danh mục
    python ingest_qipedc.py                        # nạp đầy đủ
    python ingest_qipedc.py --videos-only          # chạy tiếp phần video còn dở
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import threading
import time
import unicodedata
from collections import Counter, defaultdict
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

import psycopg2
import psycopg2.extras
import requests
from minio import Minio
from minio.error import S3Error

# ---------------------------------------------------------------------------
# Đường dẫn mặc định
# ---------------------------------------------------------------------------

# Console Windows mặc định dùng cp1252, không in được tiếng Việt và sẽ ném
# UnicodeEncodeError giữa chừng — làm hỏng cả lần chạy chỉ vì một dòng log.
# Ép UTF-8 ngay từ đầu.
for _stream in (sys.stdout, sys.stderr):
    try:
        # line_buffering: chạy đầy đủ mất hàng giờ và người dùng thường
        # chuyển hướng ra file log. Không bật thì Python gom đệm 8KB và file
        # log trống trơn suốt cả tiếng — không biết script sống hay chết.
        _stream.reconfigure(encoding="utf-8", errors="replace", line_buffering=True)
    except (AttributeError, ValueError):
        pass


REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_DATA = REPO_ROOT / "data.json"
DEFAULT_PROPS = REPO_ROOT / "backend" / "src" / "main" / "resources" / "application.properties"

VIDEO_URL_TEMPLATE = "https://qipedc.moet.gov.vn/videos/{video_id}.mp4"

# Ảnh đại diện của video, cùng mã: .../videos/D0001B.mp4 <-> .../thumbs/D0001B.png
# Không có ảnh thì bảng từ vựng 3.322 dòng trong CMS và lưới tra cứu ngoài
# trang học đều chỉ là chữ — người dùng phải mở từng từ mới biết đó là ký hiệu gì.
THUMB_URL_TEMPLATE = "https://qipedc.moet.gov.vn/thumbs/{video_id}.png"

# Nguồn dữ liệu: đánh dấu để sau này truy vết bản quyền và ghi công Bộ GD&ĐT
SOURCE = "MOET_QIPEDC"

ENTITY_TYPE_SIGN_VIDEO = "sign_videos"  # khớp constant/enums/EntityType.java


# ---------------------------------------------------------------------------
# Đọc cấu hình từ application.properties
#
# Cố tình KHÔNG chép cứng mật khẩu vào đây. Backend đổi CSDL hay đổi MinIO thì
# script tự đi theo, không lệch.
# ---------------------------------------------------------------------------


@dataclass
class Config:
    db_host: str
    db_port: int
    db_name: str
    db_user: str
    db_password: str
    minio_endpoint: str  # dạng "host:port", không kèm scheme
    minio_secure: bool
    minio_access_key: str
    minio_secret_key: str
    minio_bucket: str


def load_properties(path: Path) -> dict[str, str]:
    props: dict[str, str] = {}
    with path.open(encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            props[key.strip()] = value.strip()
    return props


def build_config(props_path: Path) -> Config:
    p = load_properties(props_path)

    jdbc = p.get("spring.datasource.url", "")
    m = re.match(r"jdbc:postgresql://([^:/]+):(\d+)/([^?]+)", jdbc)
    if not m:
        raise SystemExit(f"Không đọc được spring.datasource.url: {jdbc!r}")
    host, port, dbname = m.group(1), int(m.group(2)), m.group(3)

    endpoint = p.get("minio.endpoint", "")
    secure = endpoint.startswith("https://")
    endpoint_hostport = re.sub(r"^https?://", "", endpoint).rstrip("/")

    return Config(
        db_host=host,
        db_port=port,
        db_name=dbname,
        db_user=p.get("spring.datasource.username", ""),
        db_password=p.get("spring.datasource.password", ""),
        minio_endpoint=endpoint_hostport,
        minio_secure=secure,
        minio_access_key=p.get("minio.accessKey", ""),
        minio_secret_key=p.get("minio.secretKey", ""),
        minio_bucket=p.get("minio.bucketName", ""),
    )


# ---------------------------------------------------------------------------
# Xử lý chuỗi tiếng Việt
#
# Phải cho ra KẾT QUẢ GIỐNG HỆT VietnameseTextUtil.java, nếu không thì gloss do
# script sinh sẽ khác gloss do giao diện admin sinh cho cùng một từ, và ràng
# buộc UNIQUE trên signs.gloss sẽ nổ khi admin thêm tay một từ đã có.
# ---------------------------------------------------------------------------

_NON_GLOSS_CHARS = re.compile(r"[^a-z0-9\s_]")
_WHITESPACE = re.compile(r"\s+")
_REPEATED_UNDERSCORE = re.compile(r"_{2,}")
_EDGE_UNDERSCORE = re.compile(r"^_|_$")


def unaccent(text: Optional[str]) -> str:
    """Bỏ dấu tiếng Việt. 'Địa Chỉ' -> 'dia chi'."""
    if not text or not text.strip():
        return ""
    # Chữ đ/Đ không phải nguyên âm ghép dấu nên NFD không tách được, phải
    # thay tay — đúng như bản Java đang làm.
    normalized = unicodedata.normalize("NFD", text)
    stripped = "".join(c for c in normalized if not unicodedata.combining(c))
    return stripped.replace("đ", "d").replace("Đ", "D").lower().strip()


def to_gloss(text: Optional[str]) -> str:
    """'địa chỉ' -> 'DIA_CHI'."""
    base = unaccent(text)
    if not base:
        return ""
    gloss = _NON_GLOSS_CHARS.sub("", base)
    gloss = _WHITESPACE.sub("_", gloss)
    gloss = _REPEATED_UNDERSCORE.sub("_", gloss)
    gloss = _EDGE_UNDERSCORE.sub("", gloss)
    return gloss.upper()


# ---------------------------------------------------------------------------
# Tách mã video thành (mã từ, vùng miền)
#
# 'W00665B' -> ('W00665', 'NORTH')   B = Bắc
# 'W00665T' -> ('W00665', 'CENTRAL') T = Trung
# 'W00665N' -> ('W00665', 'SOUTH')   N = Nam
# 'D0014'   -> ('D0014',  'COMMON')  không hậu tố = dùng chung cả nước
#
# Giống hệt SignServiceImpl.stripRegionSuffix() ở backend.
# ---------------------------------------------------------------------------

_REGION_BY_SUFFIX = {"B": "NORTH", "T": "CENTRAL", "N": "SOUTH"}


def split_video_id(video_id: str) -> tuple[str, str]:
    if len(video_id) >= 2 and video_id[-1].upper() in _REGION_BY_SUFFIX:
        return video_id[:-1], _REGION_BY_SUFFIX[video_id[-1].upper()]
    return video_id, "COMMON"


# ---------------------------------------------------------------------------
# Từ loại
#
# Chỉ ánh xạ những trường hợp KHÔNG THỂ NHẦM. Cái nào mơ hồ thì để
# KHONG_XAC_DINH cho người dùng tự phân loại trên giao diện admin — đúng yêu
# cầu "có thể để trống phân loại nếu chưa biết phân loại kiểu gì".
#
# Đoán bừa còn tệ hơn để trống: ô trống thì nhìn là biết cần sửa, còn đoán sai
# thì trông như đã xong và sẽ không ai kiểm lại.
# ---------------------------------------------------------------------------

WORD_TYPE_MAP = {
    "danh tu": "DANH_TU",
    "dong tu": "DONG_TU",
    "tinh tu": "TINH_TU",
    "so tu": "SO_TU",
    "dai tu": "DAI_TU",
    "cam tu": "THAN_TU",  # cảm từ là tên gọi khác của thán từ
    # Ngữ pháp phổ thông xếp giới từ, liên từ, kết từ chung vào quan hệ từ
    "gioi tu": "QUAN_HE_TU",
    "lien tu": "QUAN_HE_TU",
    "ket tu": "QUAN_HE_TU",
    # Cụm/ngữ mang từ loại của thành tố chính
    "cum danh tu": "DANH_TU",
    "ngu danh tu": "DANH_TU",
    "ngu dong tu": "DONG_TU",
    "ngu tinh tu": "TINH_TU",
}

# Đơn vị ngôn ngữ: suy từ chính chữ "cụm"/"ngữ"/"câu" trong nhãn từ loại gốc
UNIT_TYPE_MAP = {
    "cau": "SENTENCE",
    "cau hoi": "SENTENCE",
    "cum tu": "PHRASE",
    "cum danh tu": "PHRASE",
    "ngu danh tu": "PHRASE",
    "ngu dong tu": "PHRASE",
    "ngu tinh tu": "PHRASE",
    "thanh ngu": "PHRASE",
    "khau ngu": "PHRASE",
}


def classify(tl_raw: Optional[str]) -> tuple[str, str]:
    """Trả về (word_type, unit_type)."""
    key = unaccent(tl_raw)
    key = _WHITESPACE.sub(" ", key).strip()
    return WORD_TYPE_MAP.get(key, "KHONG_XAC_DINH"), UNIT_TYPE_MAP.get(key, "WORD")


# ---------------------------------------------------------------------------
# Gom dữ liệu JSON thành cây: một từ (sign) có nhiều video theo vùng miền
# ---------------------------------------------------------------------------


@dataclass
class VideoRow:
    video_id: str
    region: str


@dataclass
class SignRow:
    source_ref: str  # mã từ sau khi bỏ hậu tố vùng miền, ví dụ 'W00665'
    word_vi: str
    description_vi: Optional[str]
    word_type: str
    unit_type: str
    gloss: str = ""
    videos: list[VideoRow] = field(default_factory=list)


def group_rows(raw_rows: list[dict]) -> list[SignRow]:
    grouped: dict[str, SignRow] = {}
    order: list[str] = []

    for raw in raw_rows:
        video_id = (raw.get("_id") or "").strip()
        word = (raw.get("word") or "").strip()
        if not video_id or not word:
            continue

        source_ref, region = split_video_id(video_id)

        if source_ref not in grouped:
            description = (raw.get("description") or "").strip() or None
            word_type, unit_type = classify(raw.get("tl"))
            grouped[source_ref] = SignRow(
                source_ref=source_ref,
                word_vi=word,
                description_vi=description,
                word_type=word_type,
                unit_type=unit_type,
            )
            order.append(source_ref)

        grouped[source_ref].videos.append(VideoRow(video_id=video_id, region=region))

    return [grouped[ref] for ref in order]


def assign_glosses(signs: list[SignRow], taken: set[str]) -> int:
    """
    gloss là NOT NULL UNIQUE. Bảy từ trong data.json xuất hiện dưới hai mã khác
    nhau (ví dụ 'giám sát' có cả D0255 lẫn W01426) — đó là hai ký hiệu khác
    nhau cho cùng một chữ, không phải dữ liệu trùng, nên phải giữ cả hai.

    Cái thứ hai được gắn thêm mã nguồn: GIAM_SAT, rồi GIAM_SAT_W01426.
    """
    collisions = 0
    for sign in signs:
        base = to_gloss(sign.word_vi) or to_gloss(sign.source_ref) or sign.source_ref.upper()
        base = base[:150]
        gloss = base
        if gloss in taken:
            collisions += 1
            suffix = "_" + re.sub(r"[^A-Z0-9]", "", sign.source_ref.upper())
            gloss = base[: 150 - len(suffix)] + suffix
            n = 2
            while gloss in taken:
                tail = f"{suffix}_{n}"
                gloss = base[: 150 - len(tail)] + tail
                n += 1
        taken.add(gloss)
        sign.gloss = gloss
    return collisions


# ---------------------------------------------------------------------------
# Giai đoạn 1 — danh mục
# ---------------------------------------------------------------------------


def sync_catalog(conn, signs: list[SignRow], update_existing: bool, dry_run: bool) -> dict:
    stats = Counter()

    with conn.cursor() as cur:
        cur.execute("SELECT source_ref, id FROM signs WHERE source = %s AND source_ref IS NOT NULL", (SOURCE,))
        existing_signs = {ref: sid for ref, sid in cur.fetchall()}

        cur.execute("SELECT gloss FROM signs")
        taken_glosses = {g for (g,) in cur.fetchall()}

        cur.execute("SELECT source_url, id FROM sign_videos WHERE source_url IS NOT NULL")
        existing_videos = {url: vid for url, vid in cur.fetchall()}

    # Gloss đã dùng bởi các từ sẽ được cập nhật (không phải tạo mới) thì không
    # tính là xung đột với chính nó
    new_signs = [s for s in signs if s.source_ref not in existing_signs]
    collisions = assign_glosses(new_signs, set(taken_glosses))
    stats["gloss_collisions"] = collisions

    for sign in signs:
        sign_id = existing_signs.get(sign.source_ref)

        if sign_id is None:
            stats["signs_created"] += 1
            if not dry_run:
                with conn.cursor() as cur:
                    cur.execute(
                        """
                        INSERT INTO signs (gloss, word_vi, description_vi, word_type, unit_type,
                                           source, source_ref, is_published, review_status)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, FALSE, 'UNREVIEWED')
                        RETURNING id
                        """,
                        (sign.gloss, sign.word_vi, sign.description_vi,
                         sign.word_type, sign.unit_type, SOURCE, sign.source_ref),
                    )
                    sign_id = cur.fetchone()[0]
                    existing_signs[sign.source_ref] = sign_id
        elif update_existing:
            stats["signs_updated"] += 1
            if not dry_run:
                with conn.cursor() as cur:
                    cur.execute(
                        """
                        UPDATE signs
                           SET word_vi = %s, description_vi = %s,
                               word_type = %s, unit_type = %s, updated_at = now()
                         WHERE id = %s
                        """,
                        (sign.word_vi, sign.description_vi,
                         sign.word_type, sign.unit_type, sign_id),
                    )
        else:
            # Mặc định KHÔNG ghi đè. Sau khi nạp, người quản trị sẽ tự phân loại
            # từ vựng trên giao diện; chạy lại script mà ghi đè thì công sức đó
            # mất sạch. Muốn ghi đè phải nói rõ bằng --update-existing.
            stats["signs_skipped"] += 1

        # Khi chạy thử thì sign_id còn None vì chưa ghi gì, nhưng vẫn phải đếm
        # video — nếu không, bản chạy thử báo "0 video" và mất hết tác dụng
        # xem trước.
        for video in sign.videos:
            source_url = VIDEO_URL_TEMPLATE.format(video_id=video.video_id)
            if source_url in existing_videos:
                stats["videos_existing"] += 1
                continue

            stats["videos_created"] += 1
            if not dry_run:
                with conn.cursor() as cur:
                    # is_primary = TRUE cho mọi video: mỗi từ chỉ có tối đa một
                    # video cho mỗi vùng miền (đã kiểm tra trên toàn bộ data.json),
                    # nên không đụng uq_sign_videos_primary_per_region.
                    cur.execute(
                        """
                        INSERT INTO sign_videos (sign_id, region, view_angle, is_primary,
                                                 ingest_status, source_url)
                        VALUES (%s, %s, 'FRONT', TRUE, 'PENDING', %s)
                        ON CONFLICT (source_url) WHERE source_url IS NOT NULL DO NOTHING
                        RETURNING id
                        """,
                        (sign_id, video.region, source_url),
                    )
                    row = cur.fetchone()
                    if row:
                        existing_videos[source_url] = row[0]

        if not dry_run:
            conn.commit()

    if not dry_run:
        conn.commit()
    return stats


# ---------------------------------------------------------------------------
# Giai đoạn 2 — tải video và đẩy lên MinIO
# ---------------------------------------------------------------------------

_thread_local = threading.local()


def get_thread_conn(cfg: Config):
    """psycopg2 không an toàn khi dùng chung một connection giữa nhiều luồng."""
    conn = getattr(_thread_local, "conn", None)
    if conn is None or conn.closed:
        conn = psycopg2.connect(
            host=cfg.db_host, port=cfg.db_port, dbname=cfg.db_name,
            user=cfg.db_user, password=cfg.db_password,
        )
        _thread_local.conn = conn
    return conn


def get_thread_minio(cfg: Config) -> Minio:
    client = getattr(_thread_local, "minio", None)
    if client is None:
        client = Minio(
            cfg.minio_endpoint,
            access_key=cfg.minio_access_key,
            secret_key=cfg.minio_secret_key,
            secure=cfg.minio_secure,
        )
        _thread_local.minio = client
    return client


def probe_video(path: Path) -> dict:
    """
    Lấy thời lượng và kích thước khung hình bằng ffprobe.

    Thời lượng không phải để hiển thị cho đẹp: phần chấm điểm DTW sau này cần
    biết video mẫu dài bao nhiêu để chuẩn hoá chuỗi khung hình. Lấy ngay lúc
    nạp thì khỏi phải tải lại 4.362 video lần nữa.

    Thiếu ffprobe cũng không sao — bỏ qua, không làm hỏng việc nạp.
    """
    try:
        out = subprocess.run(
            ["ffprobe", "-v", "error", "-select_streams", "v:0",
             "-show_entries", "stream=width,height:format=duration",
             "-of", "json", str(path)],
            capture_output=True, text=True, timeout=30, check=True,
        )
        data = json.loads(out.stdout)
        stream = (data.get("streams") or [{}])[0]
        duration = data.get("format", {}).get("duration")
        return {
            "width": stream.get("width"),
            "height": stream.get("height"),
            "duration_ms": int(float(duration) * 1000) if duration else None,
        }
    except Exception:
        return {"width": None, "height": None, "duration_ms": None}


def fetch_one(cfg: Config, task: dict, session_headers: dict, attempts: int = 3) -> tuple[str, str]:
    """
    Tải một video, đẩy lên MinIO, ghi file_attachments, cập nhật sign_videos.
    Trả về (trạng thái, thông điệp).
    """
    video_id = task["video_id"]
    sign_video_id = task["sign_video_id"]
    source_url = task["source_url"]
    object_key = f"signs/{task['source_ref']}/{video_id}.mp4"

    conn = get_thread_conn(cfg)
    client = get_thread_minio(cfg)

    tmp_path: Optional[Path] = None
    try:
        # Đã có sẵn trên MinIO (lần chạy trước tải xong nhưng chưa kịp ghi CSDL)
        size_bytes = None
        already_there = False
        try:
            stat = client.stat_object(cfg.minio_bucket, object_key)
            size_bytes = stat.size
            already_there = size_bytes and size_bytes > 0
        except S3Error:
            already_there = False

        meta = {"width": None, "height": None, "duration_ms": None}

        if not already_there:
            last_error: Optional[Exception] = None
            for attempt in range(1, attempts + 1):
                try:
                    with requests.get(source_url, headers=session_headers,
                                      stream=True, timeout=(10, 120)) as resp:
                        resp.raise_for_status()
                        ctype = resp.headers.get("Content-Type", "")
                        if "html" in ctype.lower():
                            raise RuntimeError(f"Máy chủ trả HTML thay vì video ({ctype})")

                        fd, tmp_name = tempfile.mkstemp(suffix=".mp4")
                        os.close(fd)
                        tmp_path = Path(tmp_name)
                        with tmp_path.open("wb") as fh:
                            for chunk in resp.iter_content(chunk_size=256 * 1024):
                                if chunk:
                                    fh.write(chunk)
                    size_bytes = tmp_path.stat().st_size
                    if size_bytes == 0:
                        raise RuntimeError("Tải về 0 byte")
                    last_error = None
                    break
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    if tmp_path and tmp_path.exists():
                        tmp_path.unlink(missing_ok=True)
                        tmp_path = None
                    if attempt < attempts:
                        time.sleep(2 ** attempt)
            if last_error is not None:
                raise last_error

            meta = probe_video(tmp_path)
            client.fput_object(cfg.minio_bucket, object_key, str(tmp_path),
                               content_type="video/mp4")

        with conn.cursor() as cur:
            # Dùng lại bản ghi file cũ nếu có, tránh đẻ ra file_attachments mồ côi
            cur.execute(
                """
                SELECT id FROM file_attachments
                 WHERE bucket = %s AND object_key = %s
                 LIMIT 1
                """,
                (cfg.minio_bucket, object_key),
            )
            row = cur.fetchone()
            if row:
                file_id = row[0]
                cur.execute(
                    """
                    UPDATE file_attachments
                       SET size_bytes = %s, status = 'active',
                           entity_type = %s, entity_id = %s, updated_at = now()
                     WHERE id = %s
                    """,
                    (size_bytes, ENTITY_TYPE_SIGN_VIDEO, sign_video_id, file_id),
                )
            else:
                cur.execute(
                    """
                    INSERT INTO file_attachments
                        (bucket, object_key, original_name, mime_type, extension,
                         size_bytes, entity_type, entity_id, status)
                    VALUES (%s, %s, %s, 'video/mp4', 'mp4', %s, %s, %s, 'active')
                    RETURNING id
                    """,
                    (cfg.minio_bucket, object_key, f"{video_id}.mp4",
                     size_bytes, ENTITY_TYPE_SIGN_VIDEO, sign_video_id),
                )
                file_id = cur.fetchone()[0]

            cur.execute(
                """
                UPDATE sign_videos
                   SET file_id = %s,
                       duration_ms = COALESCE(%s, duration_ms),
                       width = COALESCE(%s, width),
                       height = COALESCE(%s, height),
                       ingest_status = 'READY',
                       ingest_error = NULL,
                       updated_at = now()
                 WHERE id = %s
                """,
                (file_id, meta["duration_ms"], meta["width"], meta["height"], sign_video_id),
            )
        conn.commit()
        return ("READY" if not already_there else "ALREADY", video_id)

    except Exception as exc:  # noqa: BLE001
        conn.rollback()
        message = f"{type(exc).__name__}: {exc}"[:500]
        try:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    UPDATE sign_videos
                       SET ingest_status = 'FAILED', ingest_error = %s, updated_at = now()
                     WHERE id = %s
                    """,
                    (message, sign_video_id),
                )
            conn.commit()
        except Exception:  # noqa: BLE001
            conn.rollback()
        return ("FAILED", f"{video_id}: {message}")

    finally:
        if tmp_path and tmp_path.exists():
            tmp_path.unlink(missing_ok=True)


def fetch_videos(cfg: Config, conn, workers: int, limit: Optional[int],
                 only_refs: Optional[set[str]] = None) -> dict:
    with conn.cursor() as cur:
        # DOWNLOADING cũng lấy lại: đó là dấu vết của lần chạy bị giết giữa chừng
        cur.execute(
            """
            SELECT sv.id, sv.source_url, s.source_ref
              FROM sign_videos sv
              JOIN signs s ON s.id = sv.sign_id
             WHERE sv.source_url LIKE 'https://qipedc.moet.gov.vn/%%'
               AND (sv.ingest_status IN ('PENDING', 'FAILED', 'DOWNLOADING')
                    OR sv.file_id IS NULL)
             ORDER BY s.source_ref, sv.region
            """
        )
        rows = cur.fetchall()

    # Khi chạy đủ hai giai đoạn, chỉ tải video của đúng những từ vừa xử lý.
    # Nếu không, --limit 20 sẽ tạo 20 từ rồi lại đi tải hết mọi video PENDING
    # còn tồn từ lần chạy trước — không phải điều người dùng muốn khi thử.
    if only_refs is not None:
        rows = [r for r in rows if r[2] in only_refs]

    tasks = []
    for sign_video_id, source_url, source_ref in rows:
        video_id = source_url.rsplit("/", 1)[-1].removesuffix(".mp4")
        tasks.append({
            "sign_video_id": sign_video_id,
            "source_url": source_url,
            "source_ref": source_ref,
            "video_id": video_id,
        })

    if limit:
        tasks = tasks[:limit]

    if not tasks:
        print("Không còn video nào cần tải.")
        return Counter()

    print(f"Cần tải {len(tasks)} video, chạy {workers} luồng song song.")

    # Máy chủ của Bộ GD&ĐT là tài nguyên công. Giữ số luồng ở mức vừa phải và
    # khai báo User-Agent rõ ràng để bên họ biết lưu lượng này từ đâu tới.
    headers = {
        "User-Agent": "SignAI-ingest/1.0 (+https://qipedc.moet.gov.vn data sync)",
        "Referer": "https://qipedc.moet.gov.vn/",
    }

    stats = Counter()
    failures: list[str] = []
    started = time.time()
    done = 0

    with ThreadPoolExecutor(max_workers=workers) as pool:
        futures = {pool.submit(fetch_one, cfg, t, headers): t for t in tasks}
        for future in as_completed(futures):
            status, message = future.result()
            stats[status] += 1
            done += 1
            if status == "FAILED":
                failures.append(message)
            if done % 25 == 0 or done == len(tasks):
                elapsed = time.time() - started
                rate = done / elapsed if elapsed else 0
                remaining = (len(tasks) - done) / rate if rate else 0
                print(f"  {done}/{len(tasks)}  ok={stats['READY'] + stats['ALREADY']}  "
                      f"loi={stats['FAILED']}  ~{remaining / 60:.1f} phut con lai")

    if failures:
        print(f"\n{len(failures)} video lỗi (đã ghi vào sign_videos.ingest_error):")
        for line in failures[:20]:
            print("   -", line)
        if len(failures) > 20:
            print(f"   ... và {len(failures) - 20} lỗi khác")
        print("Chạy lại script để thử lại chỉ những video này.")

    return stats


# ---------------------------------------------------------------------------
# Giai đoạn 3: ảnh đại diện (thumbnail)
#
# Tách hẳn khỏi giai đoạn video vì hai lý do:
#
# 1. Video đã nạp xong từ trước (4.362 file), chạy lại giai đoạn 2 chỉ để lấy
#    thêm ảnh thì mỗi dòng đều phải đi hỏi MinIO "file này có chưa" rồi bỏ qua.
#    Quét riêng theo thumbnail_file_id IS NULL vừa nhanh vừa nối lại được sau
#    khi bị ngắt giữa chừng.
#
# 2. Thiếu ảnh KHÁC hẳn thiếu video. Video hỏng thì từ đó không dạy được; ảnh
#    hỏng chỉ làm ô xem trước trống. Nên lỗi ở đây KHÔNG được đụng vào
#    ingest_status của sign_videos — đánh dấu FAILED sẽ khiến lần chạy sau tải
#    lại cả video vốn đang hoàn toàn bình thường.
# ---------------------------------------------------------------------------


def thumb_url_from_video_url(source_url: str) -> str:
    """
    Suy URL ảnh từ chính source_url đã lưu, KHÔNG dựng lại từ source_ref +
    vùng miền. Lý do: source_url là thứ đã tải video về thành công, nên nó là
    bản ghi đúng nhất về mã video thật — dựng lại từ quy tắc đặt tên sẽ sai ở
    đúng những dòng có mã bất thường.
    """
    return source_url.replace("/videos/", "/thumbs/").rsplit(".", 1)[0] + ".png"


def fetch_one_thumb(cfg: Config, task: dict, session_headers: dict,
                    attempts: int = 3) -> tuple[str, str]:
    """Tải một ảnh đại diện, đẩy lên MinIO, ghi file_attachments, gắn vào sign_videos."""
    video_id = task["video_id"]
    sign_video_id = task["sign_video_id"]
    thumb_url = task["thumb_url"]
    object_key = f"signs/{task['source_ref']}/{video_id}.png"

    conn = get_thread_conn(cfg)
    client = get_thread_minio(cfg)

    tmp_path: Optional[Path] = None
    try:
        size_bytes = None
        already_there = False
        try:
            stat = client.stat_object(cfg.minio_bucket, object_key)
            size_bytes = stat.size
            already_there = bool(size_bytes and size_bytes > 0)
        except S3Error:
            already_there = False

        if not already_there:
            last_error: Optional[Exception] = None
            for attempt in range(1, attempts + 1):
                try:
                    with requests.get(thumb_url, headers=session_headers,
                                      stream=True, timeout=(10, 60)) as resp:
                        resp.raise_for_status()
                        ctype = resp.headers.get("Content-Type", "")
                        # Máy chủ trả trang HTML "không tìm thấy" với mã 200 —
                        # nhận nhầm sẽ ghi một file .png chứa HTML vào MinIO.
                        if "image" not in ctype.lower():
                            raise RuntimeError(f"Máy chủ trả {ctype or 'kiểu không rõ'} thay vì ảnh")

                        fd, tmp_name = tempfile.mkstemp(suffix=".png")
                        os.close(fd)
                        tmp_path = Path(tmp_name)
                        with tmp_path.open("wb") as fh:
                            for chunk in resp.iter_content(chunk_size=64 * 1024):
                                if chunk:
                                    fh.write(chunk)
                    size_bytes = tmp_path.stat().st_size
                    if size_bytes == 0:
                        raise RuntimeError("Tải về 0 byte")
                    last_error = None
                    break
                except Exception as exc:  # noqa: BLE001
                    last_error = exc
                    if tmp_path and tmp_path.exists():
                        tmp_path.unlink(missing_ok=True)
                        tmp_path = None
                    if attempt < attempts:
                        time.sleep(2 ** attempt)
            if last_error is not None:
                raise last_error

            client.fput_object(cfg.minio_bucket, object_key, str(tmp_path),
                               content_type="image/png")

        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT id FROM file_attachments
                 WHERE bucket = %s AND object_key = %s
                 LIMIT 1
                """,
                (cfg.minio_bucket, object_key),
            )
            row = cur.fetchone()
            if row:
                file_id = row[0]
                cur.execute(
                    """
                    UPDATE file_attachments
                       SET size_bytes = %s, status = 'active',
                           entity_type = %s, entity_id = %s, updated_at = now()
                     WHERE id = %s
                    """,
                    (size_bytes, ENTITY_TYPE_SIGN_VIDEO, sign_video_id, file_id),
                )
            else:
                cur.execute(
                    """
                    INSERT INTO file_attachments
                        (bucket, object_key, original_name, mime_type, extension,
                         size_bytes, entity_type, entity_id, status)
                    VALUES (%s, %s, %s, 'image/png', 'png', %s, %s, %s, 'active')
                    RETURNING id
                    """,
                    (cfg.minio_bucket, object_key, f"{video_id}.png",
                     size_bytes, ENTITY_TYPE_SIGN_VIDEO, sign_video_id),
                )
                file_id = cur.fetchone()[0]

            cur.execute(
                """
                UPDATE sign_videos
                   SET thumbnail_file_id = %s, updated_at = now()
                 WHERE id = %s
                """,
                (file_id, sign_video_id),
            )
        conn.commit()
        return ("READY" if not already_there else "ALREADY", video_id)

    except Exception as exc:  # noqa: BLE001
        conn.rollback()
        # Cố ý KHÔNG ghi gì vào sign_videos: xem ghi chú đầu mục này.
        message = f"{type(exc).__name__}: {exc}"[:300]
        return ("FAILED", f"{video_id}: {message}")

    finally:
        if tmp_path and tmp_path.exists():
            tmp_path.unlink(missing_ok=True)


def fetch_thumbs(cfg: Config, conn, workers: int, limit: Optional[int],
                 only_refs: Optional[set[str]] = None) -> Counter:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT sv.id, sv.source_url, s.source_ref
              FROM sign_videos sv
              JOIN signs s ON s.id = sv.sign_id
             WHERE sv.source_url LIKE 'https://qipedc.moet.gov.vn/%%'
               AND sv.thumbnail_file_id IS NULL
             ORDER BY s.source_ref, sv.region
            """
        )
        rows = cur.fetchall()

    if only_refs is not None:
        rows = [r for r in rows if r[2] in only_refs]

    tasks = []
    for sign_video_id, source_url, source_ref in rows:
        video_id = source_url.rsplit("/", 1)[-1].removesuffix(".mp4")
        tasks.append({
            "sign_video_id": sign_video_id,
            "source_ref": source_ref,
            "video_id": video_id,
            "thumb_url": thumb_url_from_video_url(source_url),
        })

    if limit:
        tasks = tasks[:limit]

    if not tasks:
        print("Không còn ảnh đại diện nào cần tải.")
        return Counter()

    print(f"Cần tải {len(tasks)} ảnh đại diện, chạy {workers} luồng song song.")

    headers = {
        "User-Agent": "SignAI-ingest/1.0 (+https://qipedc.moet.gov.vn data sync)",
        "Referer": "https://qipedc.moet.gov.vn/",
    }

    stats: Counter = Counter()
    failures: list[str] = []
    started = time.time()
    done = 0

    with ThreadPoolExecutor(max_workers=workers) as pool:
        futures = {pool.submit(fetch_one_thumb, cfg, t, headers): t for t in tasks}
        for future in as_completed(futures):
            status, message = future.result()
            stats[status] += 1
            done += 1
            if status == "FAILED":
                failures.append(message)
            if done % 100 == 0 or done == len(tasks):
                elapsed = time.time() - started
                rate = done / elapsed if elapsed else 0
                remaining = (len(tasks) - done) / rate if rate else 0
                print(f"  {done}/{len(tasks)}  ok={stats['READY'] + stats['ALREADY']}  "
                      f"loi={stats['FAILED']}  ~{remaining / 60:.1f} phut con lai")

    if failures:
        print(f"\n{len(failures)} ảnh lỗi (video vẫn dùng bình thường):")
        for line in failures[:20]:
            print("   -", line)
        if len(failures) > 20:
            print(f"   ... và {len(failures) - 20} lỗi khác")
        print("Chạy lại với --thumbs-only để thử lại chỉ những ảnh này.")

    return stats


# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Nạp từ điển VSL của Bộ GD&ĐT vào SignAI (chạy lại được nhiều lần)."
    )
    parser.add_argument("--data", type=Path, default=DEFAULT_DATA, help="Đường dẫn data.json")
    parser.add_argument("--properties", type=Path, default=DEFAULT_PROPS,
                        help="application.properties để lấy cấu hình CSDL và MinIO")
    parser.add_argument("--limit", type=int, help="Chỉ xử lý N từ đầu tiên (để thử)")
    parser.add_argument("--workers", type=int, default=6, help="Số luồng tải song song (mặc định 6)")
    parser.add_argument("--dry-run", action="store_true", help="Chỉ in kết quả dự kiến, không ghi gì")
    parser.add_argument("--catalog-only", action="store_true", help="Chỉ nạp danh mục, không tải video")
    parser.add_argument("--videos-only", action="store_true", help="Bỏ qua danh mục, chỉ tải video còn thiếu")
    parser.add_argument("--thumbs-only", action="store_true",
                        help="Chỉ tải ảnh đại diện cho video đã có (bỏ qua danh mục và video)")
    parser.add_argument("--skip-thumbs", action="store_true",
                        help="Không tải ảnh đại diện ở lần chạy này")
    parser.add_argument("--update-existing", action="store_true",
                        help="Ghi đè từ đã có trong CSDL (mặc định KHÔNG, để giữ phần admin đã sửa tay)")
    args = parser.parse_args()

    cfg = build_config(args.properties)
    print(f"CSDL   : {cfg.db_user}@{cfg.db_host}:{cfg.db_port}/{cfg.db_name}")
    print(f"MinIO  : {cfg.minio_endpoint} bucket={cfg.minio_bucket}")
    print(f"Nguồn  : {VIDEO_URL_TEMPLATE.format(video_id='<mã từ>')}")
    print(f"         {THUMB_URL_TEMPLATE.format(video_id='<mã từ>')}")
    if args.dry_run:
        print(">>> CHẠY THỬ — không ghi gì vào CSDL hay MinIO <<<")
    print()

    conn = psycopg2.connect(
        host=cfg.db_host, port=cfg.db_port, dbname=cfg.db_name,
        user=cfg.db_user, password=cfg.db_password,
    )

    only_refs: Optional[set[str]] = None

    try:
        # --thumbs-only: video đã nạp xong từ trước, chỉ đi lấy phần ảnh còn thiếu
        if args.thumbs_only:
            if args.dry_run:
                with conn.cursor() as cur:
                    cur.execute(
                        """
                        SELECT count(*) FROM sign_videos
                         WHERE source_url LIKE 'https://qipedc.moet.gov.vn/%%'
                           AND thumbnail_file_id IS NULL
                        """
                    )
                    print(f"Chạy thử: {cur.fetchone()[0]} video chưa có ảnh đại diện.")
                return 0

            client = Minio(cfg.minio_endpoint, access_key=cfg.minio_access_key,
                           secret_key=cfg.minio_secret_key, secure=cfg.minio_secure)
            if not client.bucket_exists(cfg.minio_bucket):
                print(f"Tạo bucket {cfg.minio_bucket}")
                client.make_bucket(cfg.minio_bucket)

            stats = fetch_thumbs(cfg, conn, args.workers, args.limit, None)
            print()
            print(f"  Ảnh tải mới   : {stats['READY']}")
            print(f"  Ảnh đã có sẵn : {stats['ALREADY']}")
            print(f"  Ảnh lỗi       : {stats['FAILED']}")
            return 0 if stats["FAILED"] == 0 else 1

        if not args.videos_only:
            raw = json.loads(args.data.read_text(encoding="utf-8"))
            raw_rows = raw["data"] if isinstance(raw, dict) else raw
            signs = group_rows(raw_rows)
            if args.limit:
                signs = signs[: args.limit]

            # Giai đoạn 2 chỉ tải video của đúng những từ này
            only_refs = {s.source_ref for s in signs}

            total_videos = sum(len(s.videos) for s in signs)
            unknown = sum(1 for s in signs if s.word_type == "KHONG_XAC_DINH")
            print(f"data.json: {len(raw_rows)} dòng -> {len(signs)} từ, {total_videos} video")
            print(f"Từ loại chưa xác định (để admin phân loại sau): {unknown}")

            stats = sync_catalog(conn, signs, args.update_existing, args.dry_run)
            print(f"  Từ mới        : {stats['signs_created']}")
            print(f"  Từ cập nhật   : {stats['signs_updated']}")
            print(f"  Từ bỏ qua     : {stats['signs_skipped']} (đã có, không ghi đè)")
            print(f"  Video mới     : {stats['videos_created']}")
            print(f"  Video đã có   : {stats['videos_existing']}")
            if stats["gloss_collisions"]:
                print(f"  Gloss trùng   : {stats['gloss_collisions']} (đã gắn thêm mã nguồn để phân biệt)")
            print()

        if args.dry_run:
            print("Chạy thử xong. Bỏ --dry-run để ghi thật.")
            return 0

        if args.catalog_only:
            print("Đã nạp xong danh mục. Chạy lại với --videos-only để tải video.")
            return 0

        client = Minio(cfg.minio_endpoint, access_key=cfg.minio_access_key,
                       secret_key=cfg.minio_secret_key, secure=cfg.minio_secure)
        if not client.bucket_exists(cfg.minio_bucket):
            print(f"Tạo bucket {cfg.minio_bucket}")
            client.make_bucket(cfg.minio_bucket)

        stats = fetch_videos(cfg, conn, args.workers, args.limit, only_refs)
        print()
        print(f"  Tải mới       : {stats['READY']}")
        print(f"  Đã có sẵn     : {stats['ALREADY']}")
        print(f"  Lỗi           : {stats['FAILED']}")

        if not args.skip_thumbs:
            print()
            thumb_stats = fetch_thumbs(cfg, conn, args.workers, args.limit, only_refs)
            print()
            print(f"  Ảnh tải mới   : {thumb_stats['READY']}")
            print(f"  Ảnh đã có sẵn : {thumb_stats['ALREADY']}")
            print(f"  Ảnh lỗi       : {thumb_stats['FAILED']}")

        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT ingest_status, count(*)
                  FROM sign_videos
                 WHERE source_url LIKE 'https://qipedc.moet.gov.vn/%%'
                 GROUP BY ingest_status ORDER BY 1
                """
            )
            print("\nTrạng thái toàn bộ video trong CSDL:")
            for status, count in cur.fetchall():
                print(f"  {status:12s} {count}")

            cur.execute(
                """
                SELECT count(*), count(thumbnail_file_id)
                  FROM sign_videos
                 WHERE source_url LIKE 'https://qipedc.moet.gov.vn/%%'
                """
            )
            total, with_thumb = cur.fetchone()
            print(f"\nẢnh đại diện: {with_thumb}/{total} video đã có")

        return 0 if stats["FAILED"] == 0 else 1

    finally:
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
