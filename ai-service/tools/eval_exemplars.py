#!/usr/bin/env python3
"""Đo độ chính xác của bộ chấm điểm trên MẪU THẬT trong kho, không cần video.

Vì sao có công cụ này (2026-10-04)
    19 lượt chấm của người học thật: điểm trung bình 32,8 và 0% đạt. Điểm hình bàn tay
    thường cao (74–100) nhưng điểm VỊ TRÍ gần như luôn dưới 20. Hằng số cũ (calibrate.py)
    hiệu chỉnh trên người học GIẢ LẬP = chính video mẫu bị bóp méo nhẹ — quá dễ so với
    một người khác thật sự ngồi trước webcam.

    Ở đây dùng cặp "cùng một từ, NGƯỜI KHÁC làm" có sẵn trong kho (video vùng miền B/T/N
    của cùng một từ, quay bởi người khác nhau) để làm đại diện cho người học thật, và cặp
    "hai từ khác nhau" để đo nguy cơ chấm nhầm.

    Lưu ý: video vùng miền của cùng một từ đôi khi là KÝ HIỆU KHÁC HẲN (phương ngữ), nên tập
    "cùng từ" có lẫn cặp thực chất khác nhau — số liệu "cùng từ" vì thế hơi bi quan.

Cách chạy (cần venv của ai-service, đọc CSDL + MinIO theo application.properties):
    python tools/eval_exemplars.py               # tải mẫu (lần đầu), đo, in bảng so sánh
"""

from __future__ import annotations

import gzip
import json
import pickle
import random
import re
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from app import matcher as M  # noqa: E402

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

REPO = Path(__file__).resolve().parents[2]
CACHE = Path(__file__).resolve().parent / "_exemplar_cache.pkl"


def _props() -> dict[str, str]:
    p = {}
    for dong in (REPO / "backend/src/main/resources/application.properties").read_text(encoding="utf-8").splitlines():
        dong = dong.strip()
        if dong and not dong.startswith("#") and "=" in dong:
            k, _, v = dong.partition("=")
            p[k.strip()] = v.strip()
    return p


def tai_mau() -> dict[str, list[np.ndarray]]:
    """{sign_id: [features 32x134, ...]} cho các từ có từ 2 mẫu trở lên, cache ra đĩa."""
    if CACHE.exists():
        return pickle.loads(CACHE.read_bytes())
    import psycopg2
    from minio import Minio

    p = _props()
    m = re.match(r"jdbc:postgresql://([^:/]+):(\d+)/([^?]+)", p["spring.datasource.url"])
    con = psycopg2.connect(host=m.group(1), port=int(m.group(2)), dbname=m.group(3),
                           user=p["spring.datasource.username"], password=p["spring.datasource.password"])
    cur = con.cursor()
    cur.execute("""
        select se.sign_id, fa.object_key from sign_exemplars se
        join file_attachments fa on fa.id = se.landmark_file_id
        where se.build_status = 'READY' and se.is_active
          and se.sign_id in (select sign_id from sign_exemplars where build_status = 'READY' and is_active
                             group by sign_id having count(*) >= 2)""")
    hang = cur.fetchall()
    con.close()

    mc = Minio(re.sub(r"^https?://", "", p["minio.endpoint"]), access_key=p["minio.accessKey"],
               secret_key=p["minio.secretKey"], secure=False)
    kho: dict[str, list[np.ndarray]] = {}
    for i, (sid, key) in enumerate(hang, 1):
        d = json.loads(gzip.decompress(mc.get_object(p["minio.bucketName"], key).read()))
        kho.setdefault(str(sid), []).append(np.asarray(d["features"], dtype=np.float64))
        if i % 200 == 0:
            print(f"  đã tải {i}/{len(hang)} mẫu", flush=True)
    CACHE.write_bytes(pickle.dumps(kho))
    return kho


def thanh_phan(a: np.ndarray, b: np.ndarray) -> dict:
    """Bốn thành phần THÔ theo đường DTW của trọng số hiện hành, thử cả hai chiều tay."""
    best = None
    for mirrored in (False, True):
        cand = M.mirror(a) if mirrored else a
        comp, _ = M.compare(cand, b)
        if best is None or comp["total"] < best["total"]:
            best = comp
    return best


def danh_gia(cung: np.ndarray, khac: np.ndarray, ten: str) -> dict:
    """AUC + tỉ lệ nhận nhầm (từ khác lọt qua) tại các mức chấp nhận người đúng."""
    auc = float((khac[None, :] > cung[:, None]).mean())
    out = {"ten": ten, "auc": auc}
    for giu in (0.80, 0.90, 0.95):
        nguong = float(np.quantile(cung, giu))
        out[f"far@{int(giu * 100)}"] = float((khac <= nguong).mean())
    return out


def main() -> None:
    kho = tai_mau()
    print(f"Mẫu: {sum(len(v) for v in kho.values())} của {len(kho)} từ có ≥2 người làm\n")

    rng = random.Random(7)
    ds = list(kho)
    cap_cung, cap_khac = [], []
    for sid in ds:
        mau = kho[sid]
        cap_cung.append((mau[0], mau[1]))
        for t in rng.sample([x for x in ds if x != sid], 3):
            cap_khac.append((mau[0], kho[t][0]))

    print(f"Đo {len(cap_cung)} cặp cùng từ, {len(cap_khac)} cặp khác từ (đường DTW theo trọng số hiện hành)…", flush=True)
    C = [thanh_phan(a, b) for a, b in cap_cung]
    K = [thanh_phan(a, b) for a, b in cap_khac]

    def mang(lst, k):
        return np.array([x[k] for x in lst])

    for k in ("shape", "loc", "vel", "pres"):
        print(f"  {k:<5} cùng từ: trung vị {np.median(mang(C, k)):.3f}   khác từ: trung vị {np.median(mang(K, k)):.3f}"
              f"   (tách biệt ×{np.median(mang(K, k)) / max(np.median(mang(C, k)), 1e-9):.2f})")

    print("\nSo sánh trọng số (càng AUC cao, nhận nhầm càng thấp càng tốt):")
    cau_hinh = [
        ("hiện hành  S1.0 L0.6 V0.6 P1.0", 1.0, 0.6, 0.6, 1.0),
        ("giảm vị trí S1.0 L0.3 V0.6 P1.0", 1.0, 0.3, 0.6, 1.0),
        ("giảm vị trí S1.0 L0.2 V0.4 P1.0", 1.0, 0.2, 0.4, 1.0),
        ("tăng hình  S1.4 L0.3 V0.5 P1.0", 1.4, 0.3, 0.5, 1.0),
        ("tăng hình  S1.6 L0.25 V0.4 P0.8", 1.6, 0.25, 0.4, 0.8),
        ("chỉ hình+pres S1.0 L0 V0 P1.0", 1.0, 0.0, 0.0, 1.0),
    ]
    for ten, ws, wl, wv, wp in cau_hinh:
        tong = lambda x: ws * x["shape"] + wl * x["loc"] + wv * x["vel"] + wp * x["pres"]  # noqa: E731
        r = danh_gia(np.array([tong(x) for x in C]), np.array([tong(x) for x in K]), ten)
        print(f"  {r['ten']:<34} AUC {r['auc']:.3f}   nhận nhầm khi giữ 80%/90%/95% người đúng: "
              f"{r['far@80'] * 100:4.1f}% / {r['far@90'] * 100:4.1f}% / {r['far@95'] * 100:4.1f}%")

    # Phân vị khoảng cách tổng (trọng số hiện hành, đã nhân K_TOTAL) để đặt lại ngưỡng
    tong_hh = np.array([x["total"] for x in C]) * M.K_TOTAL
    tong_kh = np.array([x["total"] for x in K]) * M.K_TOTAL
    print("\nKhoảng cách đã nhân K (thang của ngưỡng), trọng số hiện hành:")
    print("  cùng từ  p50 {:.3f}  p80 {:.3f}  p90 {:.3f}".format(*np.quantile(tong_hh, [0.5, 0.8, 0.9])))
    print("  khác từ  p1 {:.3f}  p5 {:.3f}  p10 {:.3f}".format(*np.quantile(tong_kh, [0.01, 0.05, 0.10])))


if __name__ == "__main__":
    main()
