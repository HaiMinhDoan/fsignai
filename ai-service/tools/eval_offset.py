#!/usr/bin/env python3
"""Thử nghiệm: bù độ lệch vị trí TỔNG THỂ (người ngồi lệch so với khung) trước khi so vị trí.

Số liệu thật (19 lượt): điểm hình tay 74–100 nhưng điểm vị trí gần như luôn < 20. Ngồi trước
webcam, cả người thường dịch lên/xuống/ngang so với người mẫu đứng quay video — mọi cổ tay lệch
cùng một đoạn, và cách so hiện tại phạt nguyên đoạn lệch đó ở MỌI khung hình.

Bù có giới hạn (MAX_OFFSET độ rộng vai): đủ để hết phạt oan vì ngồi lệch, vẫn giữ khác biệt
giữa ký hiệu ở trán và ký hiệu ở ngực (cách nhau > 0,5 độ rộng vai).
"""

from __future__ import annotations

import pickle
import random
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from app import features as F, matcher as M  # noqa: E402

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

CACHE = Path(__file__).resolve().parent / "_exemplar_cache.pkl"


def dich(f: np.ndarray, dx: float, dy: float) -> np.ndarray:
    """Giả lập người ngồi lệch: dịch mọi vị trí cổ tay một đoạn cố định."""
    g = f.copy()
    for sl in (F.LOC_L, F.LOC_R):
        g[:, sl] += np.array([dx, dy])
    return g


def kc(a, b, bu: bool) -> float:
    goc = M.OFFSET_COMPENSATION
    M.OFFSET_COMPENSATION = bu
    try:
        return M.match_one(a, "x", b).distance
    finally:
        M.OFFSET_COMPENSATION = goc


def auc(c, k):
    c, k = np.asarray(c), np.asarray(k)
    return float((k[None, :] > c[:, None]).mean())


def main() -> None:
    kho = pickle.loads(CACHE.read_bytes())
    rng = random.Random(5)
    ds = list(kho)
    for bu in (False, True):
        for ten, dx, dy in (("khung chuẩn", 0, 0), ("ngồi thấp (cổ tay cao hơn 0,25)", 0, -0.25),
                            ("ngồi lệch ngang 0,2", 0.2, 0)):
            cung, khac = [], []
            r2 = random.Random(5)
            for sid in ds[:300]:
                a = dich(kho[sid][0], dx, dy)
                cung.append(min(kc(a, b, bu) for b in kho[sid][1:]))
                for t in r2.sample([x for x in ds if x != sid], 2):
                    khac.append(kc(a, kho[t][0], bu))
            c, k = np.array(cung), np.array(khac)
            print(f"  bù={'CÓ ' if bu else 'KHÔNG'} | {ten:<32} AUC {auc(c, k):.3f}   cùng từ p50 {np.median(c):.3f}"
                  f"   đạt@0,30: {(c <= 0.30).mean() * 100:4.1f}%   từ khác lọt@0,30: {(k <= 0.30).mean() * 100:4.1f}%", flush=True)


if __name__ == "__main__":
    main()
