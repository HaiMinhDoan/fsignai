#!/usr/bin/env python3
"""Thử nghiệm: chấm TƯƠNG ĐỐI (so với một nhóm từ khác) có tách đúng/sai tốt hơn chấm tuyệt đối không.

Ý tưởng (giống "cohort normalization" trong xác thực giọng nói): khung webcam lệch hay người học
đứng gần làm MỌI khoảng cách phình lên như nhau. So khoảng cách tới từ đúng với khoảng cách tới
vài chục từ ngẫu nhiên khác thì phần phình chung đó triệt tiêu, chỉ còn lại câu hỏi đáng hỏi:
"người này làm GIỐNG từ này hơn hẳn các từ khác không?"

Dùng lại cache mẫu của tools/eval_exemplars.py (chạy công cụ đó trước).
"""

from __future__ import annotations

import pickle
import random
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from app import matcher as M  # noqa: E402

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

CACHE = Path(__file__).resolve().parent / "_exemplar_cache.pkl"
N_COHORT = 16


def kc(a, b) -> float:
    return M.match_one(a, "x", b).distance


def auc(cung, khac) -> float:
    cung, khac = np.asarray(cung), np.asarray(khac)
    return float((khac[None, :] > cung[:, None]).mean())


def far_tai(cung, khac, giu) -> float:
    """Tỉ lệ cặp khác từ lọt qua khi đặt ngưỡng để giữ `giu` phần người đúng (giá trị nhỏ = tốt)."""
    nguong = float(np.quantile(cung, giu))
    return float((np.asarray(khac) <= nguong).mean())


def main() -> None:
    kho = pickle.loads(CACHE.read_bytes())
    rng = random.Random(11)
    ds = list(kho)
    # Nhóm đối chiếu dùng chung: mẫu đầu tiên của 120 từ ngẫu nhiên
    cohort_ids = rng.sample(ds, 120)

    tuyet_cung, tuyet_khac, tg_cung, tg_khac, hang_cung, hang_khac = [], [], [], [], [], []
    for n, sid in enumerate(ds, 1):
        a = kho[sid][0]
        # Nhóm đối chiếu của lượt này: N_COHORT từ khác, không trùng từ đích
        nhom = [kho[c][0] for c in rng.sample([c for c in cohort_ids if c != sid], N_COHORT)]
        dc = np.array([kc(a, x) for x in nhom])
        mu, sd = dc.mean(), dc.std() + 1e-6

        def chuan(d):
            return (d - mu) / sd       # âm = gần từ đích hơn nhóm đối chiếu

        def hang(d):
            return float((dc <= d).mean())   # 0 = gần hơn MỌI từ đối chiếu

        d_dung = min(kc(a, b) for b in kho[sid][1:])
        tuyet_cung.append(d_dung); tg_cung.append(chuan(d_dung)); hang_cung.append(hang(d_dung))

        for t in rng.sample([x for x in ds if x != sid], 3):
            d_sai = kc(a, kho[t][0])
            tuyet_khac.append(d_sai); tg_khac.append(chuan(d_sai)); hang_khac.append(hang(d_sai))
        if n % 100 == 0:
            print(f"  {n}/{len(ds)}", flush=True)

    print(f"\n{len(tuyet_cung)} lượt cùng từ (người khác làm), {len(tuyet_khac)} lượt khác từ, nhóm đối chiếu {N_COHORT} từ\n")
    for ten, c, k in (("Tuyệt đối (hiện hành)", tuyet_cung, tuyet_khac),
                      ("Tương đối z-score", tg_cung, tg_khac),
                      ("Tương đối theo hạng", hang_cung, hang_khac)):
        print(f"  {ten:<24} AUC {auc(c, k):.3f}   nhận nhầm khi giữ 80%/90% người đúng: "
              f"{far_tai(c, k, 0.80) * 100:4.1f}% / {far_tai(c, k, 0.90) * 100:4.1f}%")

    # Kết hợp: đạt nếu đủ gần tuyệt đối HOẶC gần hơn hẳn nhóm đối chiếu
    tg_c, tg_k = np.array(tg_cung), np.array(tg_khac)
    print("\nPhân vị z-score:  cùng từ p50 {:.2f} p80 {:.2f} p90 {:.2f}   khác từ p5 {:.2f} p10 {:.2f}".format(
        *np.quantile(tg_c, [0.5, 0.8, 0.9]), *np.quantile(tg_k, [0.05, 0.10])))
    pickle.dump({"tc": tuyet_cung, "tk": tuyet_khac, "zc": tg_cung, "zk": tg_khac},
                open(Path(__file__).resolve().parent / "_cohort_result.pkl", "wb"))


if __name__ == "__main__":
    main()
