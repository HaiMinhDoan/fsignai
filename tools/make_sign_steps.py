#!/usr/bin/env python3
"""
Dựng "hướng dẫn từng bước" cho từ vựng VSL từ chính video mẫu.

Bảng sign_steps được làm từ lâu (API + màn soạn trong CMS) nhưng trống trơn:
3.322 từ, 0 bước. Soạn tay từng bước cho ngần ấy từ là việc của nhiều tháng,
nên script này dựng sẵn bản nháp ba bước cho mỗi từ để biên tập viên sửa lại,
thay vì bắt họ bắt đầu từ trang giấy trắng.

Mỗi bước gồm:
  - ẢNH: một khung hình cắt thẳng từ video mẫu, ở đầu / giữa / cuối đoạn ký hiệu
  - CHỮ: mô tả đo được từ landmark (tay nào, ngang tầm nào, di chuyển ra sao)

Chữ là BẮT BUỘC, ảnh thì không: người học không đọc được ảnh vẫn phải hiểu bước
đó làm gì (docs/05-design-system.md §2.2). Vì thế script không bao giờ ghi bước
chỉ có ảnh.

VÌ SAO PHẢI CHẠY LẠI MEDIAPIPE, KHÔNG CẮT THEO TỈ LỆ CỐ ĐỊNH
    Đo trên 20 video ngẫu nhiên: đoạn ký hiệu thật bắt đầu ở 10–40% và kết thúc
    ở 64–92% độ dài video, mỗi video một khác. Cắt ở 20/50/80% thì chỉ 4/20 video
    có cả ba mốc rơi đúng vào đoạn ký hiệu — số còn lại sẽ cho ảnh lúc người mẫu
    còn đang buông tay. Chạy nhận diện tốn thời gian nhưng cho đúng đoạn.

Chạy lại được nhiều lần: từ nào đã có bước thì bỏ qua (trừ khi --force).

Cách dùng:
    python tools/make_sign_steps.py --limit 5 --dry-run   # xem trước, không ghi gì
    python tools/make_sign_steps.py --limit 20            # dựng thử 20 từ
    python tools/make_sign_steps.py --workers 4           # dựng cả kho
    python tools/make_sign_steps.py --word "xin chào"     # dựng lại đúng một từ
    python tools/make_sign_steps.py --force --word "ăn"   # ghi đè bước đã có
"""

from __future__ import annotations

import argparse
import io
import os
import re
import sys
import tempfile
import threading
import time
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
AI_SERVICE = REPO_ROOT / "ai-service"
sys.path.insert(0, str(AI_SERVICE))
os.environ.setdefault("AI_POSE_MODEL", str(AI_SERVICE / "models" / "pose_landmarker_lite.task"))
os.environ.setdefault("AI_HAND_MODEL", str(AI_SERVICE / "models" / "hand_landmarker.task"))

# Console Windows mặc định là cp1252, in tiếng Việt vào đó là vỡ ngay
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

import cv2                      # noqa: E402
import numpy as np              # noqa: E402
import psycopg2                 # noqa: E402
import psycopg2.extras          # noqa: E402
from minio import Minio         # noqa: E402

from app import features as F                    # noqa: E402
from app.landmarks import Extractor              # noqa: E402


# Đọc cấu hình giống ingest_qipedc.py nhưng chép lại tại chỗ, KHÔNG import script đó:
# nó kéo theo requests/BeautifulSoup mà công cụ này không dùng tới, cài thiếu một gói
# là chết ngay ở dòng import.
@dataclass
class Config:
    db_host: str
    db_port: int
    db_name: str
    db_user: str
    db_password: str
    minio_endpoint: str      # dạng "host:port", không kèm scheme
    minio_secure: bool
    minio_access_key: str
    minio_secret_key: str
    minio_bucket: str


def build_config(props_path: Path) -> Config:
    props: dict[str, str] = {}
    for dong in props_path.read_text(encoding="utf-8").splitlines():
        dong = dong.strip()
        if dong and not dong.startswith("#") and "=" in dong:
            k, _, v = dong.partition("=")
            props[k.strip()] = v.strip()

    m = re.match(r"jdbc:postgresql://([^:/]+):(\d+)/([^?]+)", props.get("spring.datasource.url", ""))
    if not m:
        raise SystemExit("Không đọc được spring.datasource.url trong " + str(props_path))
    diem = props.get("minio.endpoint", "")
    return Config(
        db_host=m.group(1), db_port=int(m.group(2)), db_name=m.group(3),
        db_user=props.get("spring.datasource.username", ""),
        db_password=props.get("spring.datasource.password", ""),
        minio_endpoint=re.sub(r"^https?://", "", diem).rstrip("/"),
        minio_secure=diem.startswith("https://"),
        minio_access_key=props.get("minio.accessKey", ""),
        minio_secret_key=props.get("minio.secretKey", ""),
        minio_bucket=props.get("minio.bucketName", ""),
    )


# Ba mốc trong đoạn ký hiệu. Không lấy sát hai đầu: khung đầu/cuối là lúc tay
# vừa nhấc lên hoặc đã bắt đầu hạ xuống, chụp vào đó ra ảnh nửa vời.
MOCS = (0.12, 0.50, 0.88)
TIEU_DE = ("Chuẩn bị", "Thực hiện", "Kết thúc")

# Ngưỡng tính theo ĐỘ RỘNG VAI, cùng đơn vị với ai-service/app/features.py
DICH_CHUYEN_TOI_THIEU = 0.18   # nhỏ hơn thế coi như đứng yên
LECH_BEN_TOI_THIEU = 0.30      # vắt chéo qua nửa bên kia bao nhiêu thì mới đáng nói
DANG_XA = 0.95                 # dang ra xa khỏi thân bao nhiêu thì mới đáng nói

# Chỉ số landmark trong pose 33 điểm của MediaPipe — chỉ dùng để xén ảnh
VAI_T, VAI_P = 11, 12

in_khoa = threading.Lock()


def in_ra(*a):
    with in_khoa:
        print(*a, flush=True)


# ---------------------------------------------------------------------------
# Đọc hình thể
#
# KHÔNG tự tính lại từ pose thô: features.build_clip() đã làm sạch hộ (lấy cổ tay
# từ Hand Landmarker khi pose không thấy, nội suy khung khuyết, làm mượt rung).
# Bản đầu tôi tự đọc pose và thấy "không có tay nào giơ" ở 1/3 số khung đầu —
# đúng những khung mà pose để visibility thấp nhưng Hand Landmarker vẫn thấy rõ.
# ---------------------------------------------------------------------------

# Mốc cơ thể đo trên 25 video từ điển, đơn vị "độ rộng vai", gốc ở giữa hai vai,
# trục y hướng xuống:  mũi -0,58 · miệng -0,48 · vai 0,00 · khuỷu 0,78 · hông 1,51
TAM_CAO = (
    (-0.75, "trên đỉnh đầu"),
    (-0.40, "ngang mặt"),
    (-0.10, "ngang cằm"),
    (0.25, "ngang vai"),
    (0.75, "trước ngực"),
    (1.15, "ngang bụng"),
)


@dataclass
class KhungDo:
    """Số đo tại MỘT mốc thời gian, đọc từ vector đặc trưng đã chuẩn hoá."""
    gio: tuple[bool, bool]              # (tay trái, tay phải) có tham gia ký hiệu không
    vt: dict[int, tuple[float, float]]  # vị trí cổ tay từng tay

    @property
    def so_tay(self) -> int:
        return sum(self.gio)


def doc_dac_trung(hang: np.ndarray) -> KhungDo:
    pres = hang[F.PRES]
    return KhungDo(
        gio=(bool(pres[0] >= 0.5), bool(pres[1] >= 0.5)),
        vt={0: tuple(hang[F.LOC_L]), 1: tuple(hang[F.LOC_R])},
    )


def tam_cao(y: float) -> str:
    for nguong, ten in TAM_CAO:
        if y < nguong:
            return ten
    return "ngang hông"


def ben(x: float, tay: int) -> str:
    """Lệch sang bên nào — nói theo phía của NGƯỜI LÀM ký hiệu, không phải phía người xem.

    Chỉ nói khi thật sự đáng nói. Tay phải nằm ở nửa phải người là chuyện đương
    nhiên, thêm "chếch sang phải" vào mọi bước chỉ làm câu dài ra mà không thêm
    thông tin gì. Đáng nói là khi tay vắt chéo sang nửa bên kia, hoặc dang rất xa.
    """
    # x dương nằm bên phải khung hình, tức bên TRÁI của người trong video
    ben_minh = 1 if tay == 0 else -1          # phía "tự nhiên" của chính tay đó
    vat_cheo = x * ben_minh < 0
    if not vat_cheo and abs(x) < DANG_XA:
        return ""
    if vat_cheo and abs(x) < LECH_BEN_TOI_THIEU:
        return ""
    phia = "trái" if x > 0 else "phải"
    return f" vắt chéo sang bên {phia}" if vat_cheo else f" dang xa sang bên {phia}"


def ten_tay(k: KhungDo) -> str:
    if k.so_tay == 2:
        return "hai tay"
    if k.gio[0]:
        return "tay trái"
    if k.gio[1]:
        return "tay phải"
    return "tay"


def _tay_chinh(k: KhungDo) -> int | None:
    """Tay cao nhất trong số tay đang tham gia — tay dẫn dắt động tác."""
    ung = [h for h in (0, 1) if k.gio[h]]
    if not ung:
        return None
    return min(ung, key=lambda h: k.vt[h][1])


def cho_dung(k: KhungDo) -> str:
    h = _tay_chinh(k)
    if h is None:
        return ""
    x, y = k.vt[h]
    return tam_cao(y) + ben(x, h)


def huong_di(tu: KhungDo, den: KhungDo) -> str:
    """Hướng dịch chuyển của tay dẫn; rỗng nếu gần như đứng yên."""
    h = _tay_chinh(den) or _tay_chinh(tu)
    if h is None:
        return ""
    dx = den.vt[h][0] - tu.vt[h][0]
    dy = den.vt[h][1] - tu.vt[h][1]
    if (dx * dx + dy * dy) ** 0.5 < DICH_CHUYEN_TOI_THIEU:
        return ""
    if abs(dy) >= abs(dx):
        return "đưa tay lên cao hơn" if dy < 0 else "đưa tay xuống thấp hơn"
    return "đưa tay sang trái" if dx > 0 else "đưa tay sang phải"


def cau_buoc(i: int, k: KhungDo, truoc: KhungDo | None, giay_giu: float) -> str:
    """Một câu tả bước, ghép hoàn toàn từ số đo — không có chỗ nào đoán."""
    cho = cho_dung(k)
    if not cho:
        return "Giữ nguyên thế tay như trong video mẫu."

    if i == 0:
        return f"Giơ {ten_tay(k)} lên {cho}."

    them_tay = truoc is not None and k.so_tay > truoc.so_tay
    di = huong_di(truoc, k) if truoc else ""

    if i == 1:
        cau = f"{di[0].upper()}{di[1:]} tới {cho}" if di else (
            f"Giữ {ten_tay(k)} ở {cho} — động tác chính nằm ở hình bàn tay, xem kỹ video mẫu")
        if them_tay:
            cau += ", lúc này tay còn lại cùng tham gia"
        return cau + "."

    cau = f"Kết thúc với {ten_tay(k)} ở {cho}"
    if di:
        cau = f"{di[0].upper()}{di[1:]}, kết thúc ở {cho}"
    if giay_giu >= 0.3:
        cau += f", giữ khoảng {giay_giu:.1f} giây"
    return cau + " rồi hạ tay xuống."


def diem_co_the(k: KhungDo) -> str:
    return {2: "BOTH_HANDS", 1: "LEFT_HAND" if k.gio[0] else "RIGHT_HAND"}.get(k.so_tay, "")


# ---------------------------------------------------------------------------
# Cắt ảnh
# ---------------------------------------------------------------------------

def cat_anh(path: str, giay: float, pose, aspect: float) -> bytes | None:
    """Lấy khung hình tại giây `giay`, xén quanh nửa người trên rồi nén JPEG."""
    cap = cv2.VideoCapture(path)
    try:
        cap.set(cv2.CAP_PROP_POS_MSEC, giay * 1000)
        ok, bgr = cap.read()
        if not ok or bgr is None:
            return None
    finally:
        cap.release()

    h, w = bgr.shape[:2]
    if pose is not None:
        p = np.asarray([lm[:4] for lm in pose], dtype=np.float64)
        vt_t, vt_p = p[VAI_T][:2] * [w, h], p[VAI_P][:2] * [w, h]
        tam_x = (vt_t[0] + vt_p[0]) / 2
        rong_vai = max(abs(vt_t[0] - vt_p[0]), w * 0.12)
        vai_y = (vt_t[1] + vt_p[1]) / 2
        # Khung ôm từ trên đầu tới quá thắt lưng, rộng gấp ~3,4 lần vai
        x0 = int(max(0, tam_x - rong_vai * 1.7))
        x1 = int(min(w, tam_x + rong_vai * 1.7))
        y0 = int(max(0, vai_y - rong_vai * 1.25))
        y1 = int(min(h, vai_y + rong_vai * 1.75))
        if x1 - x0 > 40 and y1 - y0 > 40:
            bgr = bgr[y0:y1, x0:x1]

    cao, rong = bgr.shape[:2]
    if rong > 480:
        bgr = cv2.resize(bgr, (480, int(cao * 480 / rong)), interpolation=cv2.INTER_AREA)
    ok, buf = cv2.imencode(".jpg", bgr, [int(cv2.IMWRITE_JPEG_QUALITY), 86])
    return buf.tobytes() if ok else None


# ---------------------------------------------------------------------------
# Một từ
# ---------------------------------------------------------------------------

@dataclass
class KetQua:
    gloss: str
    trang_thai: str      # xong | bo_qua | loi
    ghi_chu: str = ""


def dung_mot_tu(cfg: Config, ex: Extractor, sign_id: str, gloss: str, word: str,
                object_key: str, dry_run: bool) -> KetQua:
    mc = Minio(cfg.minio_endpoint, access_key=cfg.minio_access_key,
               secret_key=cfg.minio_secret_key, secure=cfg.minio_secure)
    data = mc.get_object(cfg.minio_bucket, object_key).read()

    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as fh:
        fh.write(data)
        tam = fh.name
    try:
        clip = ex.extract_file(tam)
        try:
            c = F.build_clip(clip.frames, clip.aspect)
        except F.FeatureError as e:
            # Ký hiệu chỉ dùng nét mặt / gật đầu thì không có "tay ở tầm nào" để tả
            return KetQua(gloss, "bo_qua", f"không đo được tay ({e.code})")

        dau, cuoi = c.meta["active_first"], c.meta["active_last"]
        # Đặc trưng đã được nội suy đều về N_STEPS mốc trên đúng đoạn ký hiệu,
        # nên mốc thứ t tương ứng bước thứ t*(N_STEPS-1) và khung hình thứ dau+t*(cuoi-dau)
        chi_so = [min(cuoi, max(dau, int(round(dau + t * (cuoi - dau))))) for t in MOCS]
        khung = [doc_dac_trung(c.features[int(round(t * (F.N_STEPS - 1)))]) for t in MOCS]
        if all(k.so_tay == 0 for k in khung):
            return KetQua(gloss, "bo_qua", "không thấy tay nào tham gia ở ba mốc")

        giay_giu = max(0.0, (cuoi - chi_so[2]) / clip.fps)
        buoc = []
        for i, (idx, k) in enumerate(zip(chi_so, khung)):
            buoc.append({
                "thu_tu": i + 1,
                "tieu_de": TIEU_DE[i],
                "mo_ta": cau_buoc(i, k, khung[i - 1] if i else None, giay_giu),
                "diem": diem_co_the(k),
                "giay": round(idx / clip.fps, 2),
                "anh": None if dry_run else cat_anh(tam, idx / clip.fps, clip.frames[idx].pose, clip.aspect),
            })

        if dry_run:
            in_ra(f"\n  {word}  [{dau}-{cuoi}/{c.meta['input_frames']} khung, {clip.duration_s:.1f}s]")
            for b in buoc:
                in_ra(f"    {b['thu_tu']}. {b['tieu_de']} ({b['giay']}s, {b['diem']}): {b['mo_ta']}")
            return KetQua(gloss, "xong", "dry-run")

        ghi_csdl(cfg, mc, sign_id, gloss, buoc)
        return KetQua(gloss, "xong")
    finally:
        Path(tam).unlink(missing_ok=True)


def ghi_csdl(cfg: Config, mc: Minio, sign_id: str, gloss: str, buoc: list[dict]) -> None:
    """Ghi 3 bước + 3 ảnh. Mỗi từ một giao dịch: đứt giữa chừng không để lại bước nửa vời."""
    con = psycopg2.connect(host=cfg.db_host, port=cfg.db_port, dbname=cfg.db_name,
                           user=cfg.db_user, password=cfg.db_password)
    try:
        with con, con.cursor() as cur:
            # Dọn ảnh của bước cũ TRƯỚC khi xoá bước: khoá ngoại image_file_id là
            # SET NULL, xoá bước không kéo theo ảnh — chạy lại --force vài lần là
            # bỏ lại một đống ảnh mồ côi trên MinIO.
            cur.execute("""select fa.id, fa.object_key from sign_steps st
                           join file_attachments fa on fa.id = st.image_file_id
                           where st.sign_id = %s""", (sign_id,))
            anh_cu = cur.fetchall()
            cur.execute("delete from sign_steps where sign_id = %s", (sign_id,))
            for fid, key in anh_cu:
                try:
                    mc.remove_object(cfg.minio_bucket, key)
                except Exception:      # noqa: BLE001 — mất tệp thì thôi, dòng vẫn phải xoá
                    pass
                cur.execute("delete from file_attachments where id = %s", (fid,))
            for b in buoc:
                cur.execute(
                    """insert into sign_steps (id, sign_id, step_order, title_vi, description_vi,
                                               body_focus, created_at, updated_at)
                       values (gen_random_uuid(), %s, %s, %s, %s, nullif(%s, ''), now(), now())
                       returning id""",
                    (sign_id, b["thu_tu"], b["tieu_de"], b["mo_ta"], b["diem"]))
                step_id = cur.fetchone()[0]
                if not b["anh"]:
                    continue

                key = f"signs/{gloss}/steps/{uuid.uuid4()}_b{b['thu_tu']}.jpg"
                mc.put_object(cfg.minio_bucket, key, io.BytesIO(b["anh"]), len(b["anh"]),
                              content_type="image/jpeg")
                cur.execute(
                    """insert into file_attachments (id, bucket, object_key, original_name, mime_type,
                                                     extension, size_bytes, entity_type, entity_id,
                                                     created_at, updated_at)
                       values (gen_random_uuid(), %s, %s, %s, 'image/jpeg', 'jpg', %s,
                               'sign_steps', %s, now(), now())
                       returning id""",
                    (cfg.minio_bucket, key, f"{gloss}_b{b['thu_tu']}.jpg", len(b["anh"]), step_id))
                cur.execute("update sign_steps set image_file_id = %s where id = %s",
                            (cur.fetchone()[0], step_id))
    finally:
        con.close()


# ---------------------------------------------------------------------------

def lay_danh_sach(cfg: Config, limit: int | None, word: str | None, force: bool):
    con = psycopg2.connect(host=cfg.db_host, port=cfg.db_port, dbname=cfg.db_name,
                           user=cfg.db_user, password=cfg.db_password)
    try:
        with con.cursor() as cur:
            # distinct on: 545 từ có NHIỀU video chính (mỗi vùng miền một video, chỉ số
            # unique là theo từng vùng). Không lọc thì cùng một từ chạy ba lần song song
            # và đâm nhau ở ràng buộc uq_sign_steps_order. Ưu tiên bản "dùng chung".
            cur.execute(f"""
                select id, gloss, word_vi, object_key from (
                    select distinct on (s.id)
                           s.id, s.gloss, s.word_vi, fa.object_key
                    from sign_videos sv
                    join signs s on s.id = sv.sign_id
                    join file_attachments fa on fa.id = sv.file_id
                    where sv.is_primary and sv.ingest_status = 'READY'
                      {'' if force else 'and not exists (select 1 from sign_steps st where st.sign_id = s.id)'}
                      {'and s.word_vi = %(word)s' if word else ''}
                    order by s.id, (sv.region = 'COMMON') desc, sv.region, sv.created_at
                ) t
                order by word_vi
                {'limit %(limit)s' if limit else ''}""",
                        {"word": word, "limit": limit})
            return cur.fetchall()
    finally:
        con.close()


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--properties", type=Path,
                    default=REPO_ROOT / "backend" / "src" / "main" / "resources" / "application.properties")
    ap.add_argument("--limit", type=int, help="Chỉ xử lý N từ đầu (để thử)")
    ap.add_argument("--word", help="Chỉ dựng cho đúng một từ tiếng Việt")
    ap.add_argument("--workers", type=int, default=3,
                    help="Số luồng chạy song song (mặc định 3 — MediaPipe ăn CPU)")
    ap.add_argument("--force", action="store_true", help="Dựng lại cả những từ ĐÃ CÓ bước (ghi đè)")
    ap.add_argument("--dry-run", action="store_true", help="In thử chữ của từng bước, không ghi gì")
    args = ap.parse_args()

    cfg = build_config(args.properties)
    viec = lay_danh_sach(cfg, args.limit, args.word, args.force)
    if not viec:
        print("Không còn từ nào cần dựng bước.")
        return 0

    print(f"Sẽ dựng bước cho {len(viec)} từ "
          f"({'chạy thử, không ghi' if args.dry_run else f'{args.workers} luồng'})")
    t0 = time.time()
    dem = {"xong": 0, "bo_qua": 0, "loi": 0}
    bo_qua_chi_tiet: list[str] = []

    # Mỗi luồng một Extractor: MediaPipe giữ trạng thái theo dõi giữa các khung,
    # dùng chung một bộ cho nhiều video sẽ "dính" tay của video trước.
    cuc_bo = threading.local()

    def chay(row):
        sign_id, gloss, word, key = row
        if not hasattr(cuc_bo, "ex"):
            cuc_bo.ex = Extractor()
        try:
            return dung_mot_tu(cfg, cuc_bo.ex, sign_id, gloss, word, key, args.dry_run)
        except Exception as e:                       # noqa: BLE001
            return KetQua(gloss, "loi", f"{type(e).__name__}: {e}")

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = [pool.submit(chay, r) for r in viec]
        for i, fu in enumerate(as_completed(futures), 1):
            kq = fu.result()
            dem[kq.trang_thai] += 1
            if kq.trang_thai != "xong":
                bo_qua_chi_tiet.append(f"{kq.gloss}: {kq.ghi_chu}")
            if i % 25 == 0 or i == len(futures):
                troi = time.time() - t0
                in_ra(f"  {i}/{len(futures)} — xong {dem['xong']}, bỏ qua {dem['bo_qua']}, "
                      f"lỗi {dem['loi']} — {troi/i:.1f}s/từ, còn ~{(len(futures)-i)*troi/i/60:.0f} phút")

    print(f"\nXong sau {(time.time()-t0)/60:.1f} phút: "
          f"{dem['xong']} từ có bước, {dem['bo_qua']} bỏ qua, {dem['loi']} lỗi")
    for d in bo_qua_chi_tiet[:15]:
        print("   ", d)
    if len(bo_qua_chi_tiet) > 15:
        print(f"    … và {len(bo_qua_chi_tiet) - 15} từ nữa")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
