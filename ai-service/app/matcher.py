"""So khớp DTW giữa chuỗi đặc trưng của người học và exemplar (Tầng 4).

Service này KHÔNG biết ngưỡng "đạt" và không quyết định đạt/không. Nó chỉ trả khoảng cách đã
hiệu chỉnh thang đo; Spring Boot tra ngưỡng theo từng từ (verify_thresholds) rồi so sánh.

Thang đo: `distance` được nhân hằng số K sao cho cùng thang với bảng `verify_threshold_groups`
đã seed sẵn trong CSDL (từ 1 tay ≈ 0,24, từ 2 tay ≈ 0,30 ...). Hằng số lấy từ đo đạc, xem
`tools/calibrate.py` — đổi thuật toán thì phải chạy lại và cập nhật ở đây.
"""

from __future__ import annotations

from dataclasses import dataclass, field

import numpy as np

from .features import (
    LOC_L, LOC_R, N_HAND_PTS, PRES, SL, SR, SVALID, mirror,
)

# Trọng số trong chi phí từng khung (đơn vị chưa hiệu chỉnh)
W_SHAPE = 1.0
W_LOC = 0.6
W_VEL = 0.6
W_PRES = 1.0

# Vận tốc tính theo "độ rộng vai / bước", rất nhỏ so với hình dạng → nhân hệ số để cân bằng
VEL_GAIN = 6.0

# Băng Sakoe-Chiba: cho phép lệch pha tối đa ±BAND bước trên chuỗi 32 bước
BAND = 10

# Hằng số hiệu chỉnh thang đo (tools/calibrate.py). Nhân vào khoảng cách thô.
#
# K_TOTAL: đo trên 60 video từ điển thật (tools/calibrate.py) — người học giả lập (đổi tốc độ, tỉ lệ,
# xoay, nhiễu, mất khung tay) có khoảng cách thô p99 ≈ 0,47; ký hiệu khác từ có p1 ≈ 0,81.
# Đặt K = 0,42 sao cho ngưỡng nhóm đã seed trong verify_threshold_groups ứng với khoảng cách thô:
#   từ 1 tay 0,24 ↔ 0,57     từ 2 tay 0,30 ↔ 0,71 (gần như mọi từ, xem handCount ở main.py)
# Ngưỡng 2 tay hơn p99 giả lập ~1,5 lần (chừa chỗ cho người học thật kém hơn người giả lập) mà vẫn
# dưới p1 của khác-từ. Đó là điểm cân bằng, không phải điểm giữa (0,64): dịch lên thì dễ nhận nhầm từ
# gần giống nhau, dịch xuống thì dễ từ chối oan người học thật.
#
# K_SHAPE/K_LOC/K_MOVE: chia K_TOTAL cho tỉ trọng của thành phần đó ở các cặp gần ngưỡng
# (hình tay 51%, vị trí 25%, chuyển động 24%), để điểm từng phần đọc cùng thang với điểm tổng:
# một phần "chiếm hết lượng sai" thì điểm phần đó bằng điểm tổng.
K_TOTAL = 0.42
K_SHAPE = 0.82
K_LOC = 1.68
K_MOVE = 1.75


@dataclass
class Match:
    exemplar_id: str
    mirrored: bool
    distance: float                 # đã nhân K_TOTAL
    handshape: float                # đã nhân K_SHAPE
    location: float                 # đã nhân K_LOC (gồm phạt tay giơ/không giơ)
    movement: float                 # đã nhân K_MOVE
    hints: list[str] = field(default_factory=list)
    diag: dict = field(default_factory=dict)


def _velocity(f: np.ndarray) -> np.ndarray:
    loc = np.concatenate([f[:, LOC_L], f[:, LOC_R]], axis=1)
    v = np.gradient(loc, axis=0)
    return v * VEL_GAIN


def _frame_costs(a: np.ndarray, b: np.ndarray):
    """Ma trận chi phí (T×T) cho từng thành phần giữa mọi cặp khung (i của a, j của b)."""
    T = len(a)
    va, vb = _velocity(a), _velocity(b)
    pa, pb = a[:, PRES], b[:, PRES]
    both = pa[:, None, :] * pb[None, :, :]                    # (T, T, 2) tay nào cùng giơ

    # hình dạng: RMS chênh lệch 63 chiều mỗi tay, chỉ tính khi cả hai cùng có hình tay đo được
    shape = np.zeros((T, T))
    weight = np.zeros((T, T))
    for h, sl in enumerate((SL, SR)):
        d = a[:, None, sl] - b[None, :, sl]
        rms = np.sqrt((d ** 2).mean(axis=2))
        m = a[:, None, SVALID][:, :, h] * b[None, :, SVALID][:, :, h] * both[:, :, h]
        shape += rms * m
        weight += m
    shape = np.divide(shape, weight, out=np.zeros_like(shape), where=weight > 0)

    # vị trí: khoảng cách Euclid của cổ tay, chỉ tay cùng giơ
    loc = np.zeros((T, T))
    lw = np.zeros((T, T))
    for h, sl in enumerate((LOC_L, LOC_R)):
        d = np.linalg.norm(a[:, None, sl] - b[None, :, sl], axis=2)
        loc += d * both[:, :, h]
        lw += both[:, :, h]
    loc = np.divide(loc, lw, out=np.zeros_like(loc), where=lw > 0)

    # vận tốc: cùng tay cùng giơ
    vel = np.zeros((T, T))
    vw = np.zeros((T, T))
    for h in (0, 1):
        d = np.linalg.norm(va[:, None, 2 * h:2 * h + 2] - vb[None, :, 2 * h:2 * h + 2], axis=2)
        vel += d * both[:, :, h]
        vw += both[:, :, h]
    vel = np.divide(vel, vw, out=np.zeros_like(vel), where=vw > 0)

    pres = np.abs(pa[:, None, :] - pb[None, :, :]).mean(axis=2)
    return shape, loc, vel, pres


def _dtw_path(cost: np.ndarray, band: int):
    T = len(cost)
    acc = np.full((T, T), np.inf)
    acc[0, 0] = cost[0, 0]
    for i in range(T):
        for j in range(max(0, i - band), min(T, i + band + 1)):
            if i == 0 and j == 0:
                continue
            best = min(
                acc[i - 1, j] if i > 0 else np.inf,
                acc[i, j - 1] if j > 0 else np.inf,
                acc[i - 1, j - 1] if i > 0 and j > 0 else np.inf,
            )
            acc[i, j] = cost[i, j] + best
    i = j = T - 1
    path = [(i, j)]
    while (i, j) != (0, 0):
        cands = []
        if i > 0 and j > 0:
            cands.append((acc[i - 1, j - 1], i - 1, j - 1))
        if i > 0:
            cands.append((acc[i - 1, j], i - 1, j))
        if j > 0:
            cands.append((acc[i, j - 1], i, j - 1))
        _, i, j = min(cands)
        path.append((i, j))
    path.reverse()
    return np.array(path)


def compare(learner: np.ndarray, exemplar: np.ndarray) -> tuple[dict, np.ndarray]:
    """Khoảng cách thô giữa hai chuỗi đã chuẩn hoá. Trả (các thành phần, đường DTW)."""
    S, L, V, P = _frame_costs(learner, exemplar)
    cost = W_SHAPE * S + W_LOC * L + W_VEL * V + W_PRES * P
    path = _dtw_path(cost, BAND)
    ii, jj = path[:, 0], path[:, 1]
    comp = {
        "shape": float(S[ii, jj].mean()),
        "loc": float(L[ii, jj].mean()),
        "vel": float(V[ii, jj].mean()),
        "pres": float(P[ii, jj].mean()),
    }
    comp["total"] = W_SHAPE * comp["shape"] + W_LOC * comp["loc"] + W_VEL * comp["vel"] + W_PRES * comp["pres"]
    return comp, path


def match_one(learner: np.ndarray, exemplar_id: str, exemplar: np.ndarray) -> Match:
    """So với một exemplar, thử cả hai chiều thuận tay và lấy chiều gần hơn."""
    best = None
    for mirrored in (False, True):
        cand = mirror(learner) if mirrored else learner
        comp, path = compare(cand, exemplar)
        if best is None or comp["total"] < best[0]["total"]:
            best = (comp, path, cand, mirrored)
    comp, path, cand, mirrored = best
    diag = _diagnostics(cand, exemplar, path)
    return Match(
        exemplar_id=exemplar_id,
        mirrored=mirrored,
        distance=comp["total"] * K_TOTAL,
        handshape=comp["shape"] * W_SHAPE * K_SHAPE,
        location=(comp["loc"] * W_LOC + comp["pres"] * W_PRES) * K_LOC,
        movement=comp["vel"] * W_VEL * K_MOVE,
        hints=_geometric_hints(diag),
        diag=diag,
    )


def best_match(learner: np.ndarray, exemplars: list[tuple[str, np.ndarray]]) -> Match:
    matches = [match_one(learner, eid, feats) for eid, feats in exemplars]
    return min(matches, key=lambda m: m.distance)


# ---------------------------------------------------------------------------
# Gợi ý hình học — không phụ thuộc ngưỡng của từng từ
# ---------------------------------------------------------------------------

DY_HINT = 0.30        # lệch dọc (độ rộng vai) trung bình mới đáng nói
MOVE_LOW, MOVE_HIGH = 0.6, 1.6
MIN_EXEMPLAR_TRAVEL = 0.5   # exemplar gần như đứng yên → không gợi ý về biên độ


def _diagnostics(a: np.ndarray, b: np.ndarray, path: np.ndarray) -> dict:
    ii, jj = path[:, 0], path[:, 1]
    pa, pb = a[:, PRES], b[:, PRES]
    both = (pa[ii] * pb[jj]) > 0.5                              # (len, 2)

    dys, dxs = [], []
    for h, sl in enumerate((LOC_L, LOC_R)):
        m = both[:, h]
        if m.any():
            d = a[ii[m], sl] - b[jj[m], sl]
            dxs.append(d[:, 0])
            dys.append(d[:, 1])
    dy = float(np.concatenate(dys).mean()) if dys else 0.0
    dx = float(np.concatenate(dxs).mean()) if dxs else 0.0

    def travel(f):
        t = 0.0
        for h, sl in enumerate((LOC_L, LOC_R)):
            m = f[:, PRES][:, h] > 0.5
            pts = f[:, sl]
            step = np.linalg.norm(np.diff(pts, axis=0), axis=1)
            t += float(step[(m[1:] & m[:-1])].sum())
        return t

    ta, tb = travel(a), travel(b)
    return {
        "dy": dy, "dx": dx,
        "travelLearner": ta, "travelExemplar": tb,
        "handsLearner": float((pa > 0.5).mean(axis=0).max()),
        "handsExemplar": float((pb > 0.5).mean(axis=0).max()),
        "twoHandsLearner": bool(((pa > 0.5).mean(axis=0) > 0.3).all()),
        "twoHandsExemplar": bool(((pb > 0.5).mean(axis=0) > 0.3).all()),
    }


def _geometric_hints(d: dict) -> list[str]:
    hints: list[str] = []
    if d["twoHandsExemplar"] and not d["twoHandsLearner"]:
        hints.append("HAND_MISSING")
    elif d["twoHandsLearner"] and not d["twoHandsExemplar"]:
        hints.append("EXTRA_HAND")
    if abs(d["dy"]) >= DY_HINT:
        # y ảnh tăng dần xuống dưới: dy > 0 nghĩa là tay người học thấp hơn mẫu
        hints.append("LOCATION_TOO_LOW" if d["dy"] > 0 else "LOCATION_TOO_HIGH")
    if d["travelExemplar"] >= MIN_EXEMPLAR_TRAVEL:
        ratio = d["travelLearner"] / d["travelExemplar"]
        if ratio < MOVE_LOW:
            hints.append("MOVEMENT_TOO_SMALL")
        elif ratio > MOVE_HIGH:
            hints.append("MOVEMENT_TOO_LARGE")
    return hints
