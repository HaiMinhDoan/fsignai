"""Chuẩn hoá chuỗi landmark thành chuỗi đặc trưng cố định độ dài (Tầng 2 trong 01-ai-model-research.md).

Đầu vào là chuỗi khung hình thô — từ trình duyệt (người học) hoặc từ video mẫu (`landmarks.py`).
Hai nguồn đi qua CÙNG một hàm này, đó là điều kiện để so sánh được.

Bất biến mà bước này tạo ra:
  • vị trí trong khung hình  → gốc toạ độ ở giữa hai vai
  • khoảng cách tới camera   → chia cho độ rộng vai
  • tỉ lệ khung hình         → nhân x với aspect trước khi tính khoảng cách
  • tốc độ ký hiệu           → cắt bỏ đoạn nghỉ rồi nội suy về N_STEPS khung
  • kích thước bàn tay       → toạ độ ngón tính tương đối cổ tay, chia cho cỡ bàn tay

Vị trí tay (đặc trưng "vị trí") lấy từ POSE chứ không từ Hand Landmarker: pose bám cổ tay liên tục
kể cả khi bàn tay bị mờ, còn Hand Landmarker hay bỏ sót khung. Hình dạng ngón thì chỉ Hand
Landmarker có.

Bố cục vector mỗi khung (N_FEATURES = 134):
    [ shapeL(63) | shapeR(63) | locL(2) | locR(2) | presL, presR | shapeValidL, shapeValidR ]
"L"/"R" là trái/phải GIẢI PHẪU của người ký (không phải trái/phải trong ảnh).
"""

from __future__ import annotations

from dataclasses import dataclass, field

import numpy as np

N_STEPS = 32
N_HAND_PTS = 21
SHAPE_DIM = N_HAND_PTS * 3

SL = slice(0, SHAPE_DIM)
SR = slice(SHAPE_DIM, 2 * SHAPE_DIM)
LOC_L = slice(2 * SHAPE_DIM, 2 * SHAPE_DIM + 2)
LOC_R = slice(2 * SHAPE_DIM + 2, 2 * SHAPE_DIM + 4)
PRES = slice(2 * SHAPE_DIM + 4, 2 * SHAPE_DIM + 6)
SVALID = slice(2 * SHAPE_DIM + 6, 2 * SHAPE_DIM + 8)
N_FEATURES = 2 * SHAPE_DIM + 8

# Chỉ số điểm trong pose 33 điểm của MediaPipe
P_L_SHOULDER, P_R_SHOULDER = 11, 12
P_L_WRIST, P_R_WRIST = 15, 16

# Ngưỡng theo "độ rộng vai". Khi nghỉ, cổ tay nằm quanh y ≈ 1,35 (đo trên video từ điển).
REST_X, REST_Y = 0.7, 1.35   # vị trí cổ tay khi buông thõng
RAISED_Y = 1.10          # cổ tay cao hơn mức này = tay đang tham gia ký hiệu
MIN_VIS = 0.4            # độ tin cậy tối thiểu của điểm pose
MIN_ACTIVE_FRAMES = 4    # ít hơn thế coi như không có ký hiệu
MIN_SHOULDER_VIS = 0.5


class FeatureError(ValueError):
    """Lỗi do dữ liệu đầu vào (không thấy người / không thấy tay), có mã để client hiển thị."""

    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


@dataclass
class Clip:
    """Chuỗi đặc trưng đã chuẩn hoá: features (N_STEPS, N_FEATURES) + chất lượng theo dõi 0..1."""

    features: np.ndarray
    quality: float
    active_frames: int
    meta: dict = field(default_factory=dict)


def _lm_array(lms, dims: int) -> np.ndarray:
    return np.asarray([lm[:dims] for lm in lms], dtype=np.float64)


def build_clip(frames: list, aspect: float) -> Clip:
    """frames: list các dict/đối tượng có `pose` (33×4 hoặc None) và `hands` (0-2 × 21×3)."""
    if not frames:
        raise FeatureError("NO_FRAMES", "Chưa có khung hình nào")
    aspect = float(aspect) if aspect and aspect > 0 else 16 / 9
    n = len(frames)

    pose = np.full((n, 33, 4), np.nan)
    for i, f in enumerate(frames):
        p = _get(f, "pose")
        if p:
            pose[i] = _lm_array(p, 4)

    # ---- mốc cơ thể: trung vị trên các khung nhìn rõ hai vai (ổn định hơn từng khung) ----
    sh_vis = np.minimum(pose[:, P_L_SHOULDER, 3], pose[:, P_R_SHOULDER, 3])
    good = np.nan_to_num(sh_vis) >= MIN_SHOULDER_VIS
    if good.mean() < 0.6:
        raise FeatureError("NO_BODY", "Không thấy rõ hai vai trong khung hình")

    def xy(a):  # nhân x với aspect → toạ độ đẳng hướng
        return a[..., :2] * np.array([aspect, 1.0])

    ls, rs = xy(pose[good, P_L_SHOULDER]), xy(pose[good, P_R_SHOULDER])
    centre = np.median((ls + rs) / 2, axis=0)
    width = float(np.median(np.linalg.norm(ls - rs, axis=1)))
    if width < 1e-3:
        raise FeatureError("NO_BODY", "Không thấy rõ hai vai trong khung hình")

    # ---- từng khung: vị trí cổ tay (pose), tay nào đang giơ, hình dạng ngón ----
    loc = np.full((n, 2, 2), np.nan)       # [khung, tay(L,R), x/y]
    raised = np.zeros((n, 2), dtype=bool)
    shape = np.full((n, 2, SHAPE_DIM), np.nan)

    for i, f in enumerate(frames):
        wrist_pose = [None, None]
        for h, idx in enumerate((P_L_WRIST, P_R_WRIST)):
            row = pose[i, idx]
            if not np.isnan(row[0]) and row[3] >= MIN_VIS:
                wrist_pose[h] = (xy(row) - centre) / width

        hands = _get(f, "hands") or []
        assigned = _assign_hands(hands, wrist_pose, centre, width, aspect)
        for h in (0, 1):
            hand = assigned[h]
            if hand is not None:
                # Nếu pose không thấy cổ tay nhưng Hand Landmarker thấy → dùng cổ tay bàn tay
                if wrist_pose[h] is None:
                    wrist_pose[h] = (xy(np.asarray(hand[0][:3])) - centre) / width
                shape[i, h] = _hand_shape(hand, aspect)
            if wrist_pose[h] is not None:
                loc[i, h] = wrist_pose[h]
                raised[i, h] = wrist_pose[h][1] < RAISED_Y

    active = raised.any(axis=1)
    if active.sum() < MIN_ACTIVE_FRAMES:
        raise FeatureError("NO_SIGN", "Chưa thấy tay giơ lên để ký hiệu")

    first, last = int(np.argmax(active)), int(n - 1 - np.argmax(active[::-1]))
    first, last = max(0, first - 1), min(n - 1, last + 1)
    seg = slice(first, last + 1)
    loc, raised, shape = loc[seg], raised[seg], shape[seg]
    m = last - first + 1

    # ---- điền khuyết: vị trí nội suy theo thời gian, hình dạng lấy khung gần nhất còn thấy ----
    shape_valid = ~np.isnan(shape).any(axis=2)                 # (m, 2)
    for h in (0, 1):
        # Không bao giờ thấy cổ tay này → coi như buông thõng ở vị trí nghỉ (trái giải phẫu ở bên phải ảnh)
        rest = (REST_X if h == 0 else -REST_X, REST_Y)
        for d in (0, 1):
            loc[:, h, d] = _interp_nan(loc[:, h, d], fill=rest[d])
        shape[:, h] = _nearest_fill(shape[:, h], shape_valid[:, h])

    # ---- ghép vector khung rồi nội suy về N_STEPS ----
    feats = np.zeros((m, N_FEATURES))
    feats[:, SL] = shape[:, 0]
    feats[:, SR] = shape[:, 1]
    feats[:, LOC_L] = loc[:, 0]
    feats[:, LOC_R] = loc[:, 1]
    feats[:, PRES] = raised.astype(float)
    feats[:, SVALID] = (shape_valid & raised).astype(float)

    out = _resample(feats, N_STEPS)
    # Làm mượt nhẹ: khử rung của webcam mà không xoá chuyển động thật
    out = _smooth(out, window=3)

    tracked = float(np.mean([shape_valid[raised[:, h], h].mean() if raised[:, h].any() else 1.0 for h in (0, 1)]))
    quality = float(np.clip(0.5 * good.mean() + 0.5 * tracked, 0, 1))
    return Clip(features=out, quality=quality, active_frames=int(active.sum()),
                meta={"segment_frames": m, "input_frames": n})


# ---------------------------------------------------------------------------
# Các hàm phụ
# ---------------------------------------------------------------------------

def _get(frame, key):
    return frame.get(key) if isinstance(frame, dict) else getattr(frame, key, None)


def _assign_hands(hands, wrist_pose, centre, width, aspect):
    """Gán tối đa 2 bàn tay vào (trái, phải) giải phẫu, theo khoảng cách tới cổ tay của pose."""
    out = [None, None]
    if not hands:
        return out
    hands = hands[:2]
    wrists = [(xy_(np.asarray(h[0][:2]), aspect) - centre) / width for h in hands]

    def dist(hi, side):
        wp = wrist_pose[side]
        if wp is None:
            # Pose không thấy cổ tay → dựa vào phía của cơ thể (ảnh không lật: trái giải phẫu nằm bên phải ảnh)
            return 0.0 if (wrists[hi][0] > 0) == (side == 0) else 5.0
        return float(np.linalg.norm(wrists[hi] - wp))

    if len(hands) == 1:
        side = 0 if dist(0, 0) <= dist(0, 1) else 1
        out[side] = hands[0]
        return out
    straight = dist(0, 0) + dist(1, 1)
    crossed = dist(0, 1) + dist(1, 0)
    if straight <= crossed:
        out[0], out[1] = hands[0], hands[1]
    else:
        out[0], out[1] = hands[1], hands[0]
    return out


def xy_(a, aspect):
    return a * np.array([aspect, 1.0])


def _hand_shape(hand, aspect) -> np.ndarray:
    """21 điểm → (63,) tương đối cổ tay, chia cho cỡ bàn tay. Nhân x và z với aspect cho đẳng hướng."""
    a = _lm_array(hand, 3) * np.array([aspect, 1.0, aspect])
    rel = a - a[0]
    size = np.linalg.norm(rel[9])  # cổ tay → gốc ngón giữa
    if size < 1e-6:
        return np.full(SHAPE_DIM, np.nan)
    return (rel / size).ravel()


def _interp_nan(y: np.ndarray, fill: float) -> np.ndarray:
    bad = np.isnan(y)
    if not bad.any():
        return y
    if bad.all():
        return np.full_like(y, fill)
    idx = np.arange(len(y))
    return np.interp(idx, idx[~bad], y[~bad])


def _nearest_fill(a: np.ndarray, valid: np.ndarray) -> np.ndarray:
    if not valid.any():
        return np.zeros_like(a)
    idx = np.arange(len(a))
    vi = idx[valid]
    nearest = vi[np.abs(idx[:, None] - vi[None, :]).argmin(axis=1)]
    return a[nearest]


def _resample(x: np.ndarray, n: int) -> np.ndarray:
    m = len(x)
    if m == 1:
        return np.repeat(x, n, axis=0)
    src = np.linspace(0, m - 1, n)
    idx = np.arange(m)
    return np.stack([np.interp(src, idx, x[:, k]) for k in range(x.shape[1])], axis=1)


def _smooth(x: np.ndarray, window: int) -> np.ndarray:
    if window <= 1:
        return x
    pad = window // 2
    padded = np.pad(x, ((pad, pad), (0, 0)), mode="edge")
    kernel = np.ones(window) / window
    return np.stack([np.convolve(padded[:, k], kernel, mode="valid") for k in range(x.shape[1])], axis=1)


def mirror(features: np.ndarray) -> np.ndarray:
    """Đổi vai trò tay trái ↔ phải và lật trục x: so khớp với người ký thuận tay ngược lại."""
    out = np.zeros_like(features)
    for a, b in ((SL, SR), (SR, SL)):
        blk = features[:, a].reshape(len(features), N_HAND_PTS, 3).copy()
        blk[:, :, 0] *= -1
        out[:, b] = blk.reshape(len(features), SHAPE_DIM)
    out[:, LOC_L] = features[:, LOC_R] * np.array([-1, 1])
    out[:, LOC_R] = features[:, LOC_L] * np.array([-1, 1])
    out[:, PRES] = features[:, PRES][:, ::-1]
    out[:, SVALID] = features[:, SVALID][:, ::-1]
    return out
