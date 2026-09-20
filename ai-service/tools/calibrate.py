#!/usr/bin/env python3
"""Đo phân bố khoảng cách để chọn hằng số K trong app/matcher.py.

    python tools/calibrate.py DIR_VIDEO [--per-video 4]

DIR_VIDEO chứa các file .mp4 kiểu từ điển Bộ GD&ĐT (W00367B.mp4 ...). Hai video cùng từ khi tên
giống nhau sau khi bỏ hậu tố vùng miền B/T/N. Kết quả trích landmark được cache cạnh thư mục để chạy
lại nhanh (trích MediaPipe mất ~2 giây/video).

In ra ba thứ cần để chọn ngưỡng:
  1. GIẢ LẬP người học   — mẫu bị biến dạng (tools/augment.py) so với chính nó       → phải đạt
  2. KHÁC TỪ             — mọi cặp video khác từ                                     → phải trượt
  3. TOP-1               — người học giả lập so với tất cả mẫu, có ra đúng từ không?

Chọn ngưỡng thô nằm giữa p99 của (1) và p1 của (2), rồi thiên về khoan dung. Người học thật sẽ tệ hơn
người học giả lập, đó là lý do không đặt ngưỡng sát p99 của (1).
"""

from __future__ import annotations

import argparse
import pickle
import re
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
sys.path.insert(0, str(Path(__file__).resolve().parent))

from app import features as F, matcher as M  # noqa: E402
from app.landmarks import Extractor  # noqa: E402
from augment import augment  # noqa: E402


def sign_key(path: Path) -> str:
    return re.sub(r"[BTN]$", "", path.stem)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("videos", type=Path)
    ap.add_argument("--per-video", type=int, default=4)
    ap.add_argument("--seed", type=int, default=7)
    args = ap.parse_args()

    # Thang đo THÔ: tắt K để in ra số chưa nhân hằng số
    for name in ("K_TOTAL", "K_SHAPE", "K_LOC", "K_MOVE"):
        setattr(M, name, 1.0)

    cache = args.videos / "_raw_cache.pkl"
    raw = pickle.loads(cache.read_bytes()) if cache.exists() else {}
    ex = None
    for p in sorted(args.videos.glob("*.mp4")):
        if p.name in raw:
            continue
        ex = ex or Extractor()
        print("trích", p.name, flush=True)
        raw[p.name] = ex.extract_file(p)
        cache.write_bytes(pickle.dumps(raw))

    names = sorted(raw)
    feats = {n: F.build_clip(raw[n].frames, raw[n].aspect).features for n in names}
    key = {n: sign_key(Path(n)) for n in names}
    rng = np.random.default_rng(args.seed)

    # Nhóm theo SỐ TAY của mẫu: ngưỡng 1 tay (0,24) và 2 tay (0,30) khác nhau nên phải đo riêng từng nhóm
    def hands(n: str) -> int:
        return int((feats[n][:, F.PRES].mean(axis=0) > 0.3).sum()) or 1

    genuine = {1: [], 2: []}
    top1_ok = top1_n = 0
    for n in names:
        for _ in range(args.per_video):
            learner = F.build_clip(augment(raw[n].frames, raw[n].aspect, rng), raw[n].aspect).features
            ds = sorted((M.match_one(learner, o, feats[o]).distance, key[o] == key[n]) for o in names)
            genuine[hands(n)].append(next(d for d, same in ds if same))
            top1_ok += ds[0][1]
            top1_n += 1

    impostor = {1: [], 2: []}
    for i, a in enumerate(names):
        for b in names[i + 1:]:
            if key[a] != key[b]:
                impostor[hands(b)].append(M.match_one(feats[a], b, feats[b]).distance)
                impostor[hands(a)].append(M.match_one(feats[b], a, feats[a]).distance)

    print(f"\nTOP-1 (người học giả lập so với mọi mẫu)  {top1_ok}/{top1_n} = {100 * top1_ok / top1_n:.1f}%")
    seeded = {1: 0.24, 2: 0.30}   # verify_threshold_groups (WORD); thang đo SAU khi nhân K_TOTAL
    k_total = 0.42
    for h in (1, 2):
        g, im = np.array(genuine[h]), np.array(impostor[h])
        if not len(g):
            continue
        raw_thr = seeded[h] / k_total
        print(f"\n=== Mẫu {h} TAY ===  (ngưỡng seed {seeded[h]} ↔ thô {raw_thr:.2f})")
        print(f"  GIẢ LẬP người học n={len(g):4d}  p50 {np.percentile(g, 50):.3f}  p95 {np.percentile(g, 95):.3f}"
              f"  p99 {np.percentile(g, 99):.3f}  max {g.max():.3f}")
        print(f"  KHÁC TỪ          n={len(im):4d}  p1 {np.percentile(im, 1):.3f}  p5 {np.percentile(im, 5):.3f}"
              f"  p50 {np.percentile(im, 50):.3f}")
        print(f"  Ở ngưỡng đó: từ chối oan người học giả lập {100 * (g > raw_thr).mean():.1f}%,"
              f"  nhận nhầm từ khác {100 * (im <= raw_thr).mean():.2f}%")
    print("\nĐổi K_TOTAL trong app/matcher.py nếu tỉ lệ ở trên không chấp nhận được (và cập nhật `k_total` ở đây).")


if __name__ == "__main__":
    main()
