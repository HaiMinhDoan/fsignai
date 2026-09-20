"""Sinh landmark tổng hợp để test pipeline mà không cần video hay MediaPipe."""

from __future__ import annotations

import numpy as np

ASPECT = 16 / 9


def hand_points(wrist_xy, openness: float, size: float = 0.05):
    """21 điểm bàn tay. openness 1 = xoè, 0 = nắm. Gốc ngón giữa (điểm 9) cách cổ tay đúng `size`."""
    pts = [[wrist_xy[0], wrist_xy[1], 0.0]]
    for finger in range(5):
        base_ang = np.deg2rad(-40 + finger * 20)
        for seg in range(1, 5):
            reach = size * (0.5 + 0.35 * seg) * (0.3 + 0.7 * openness)
            curl = (1 - openness) * 0.35 * seg
            ang = base_ang + curl * (1 if finger % 2 else -1)
            pts.append([wrist_xy[0] + reach * np.sin(ang) / ASPECT,
                        wrist_xy[1] - reach * np.cos(ang),
                        -0.01 * seg * (1 - openness)])
    a = np.array(pts)
    # ép điểm 9 (gốc ngón giữa) cách cổ tay đúng size để chuẩn hoá cỡ tay ổn định
    v = a[9] - a[0]
    a[9] = a[0] + v / np.linalg.norm(v * [ASPECT, 1, ASPECT]) * size
    return a.tolist()


def make_frames(traj, n=40, openness=lambda t: 1.0, both=True, pad=4, hands_detected=True):
    """traj(t) → (x, y) của cổ tay TRÁI giải phẫu (bên phải ảnh) theo độ rộng vai, t ∈ [0,1].

    Cổ tay phải là ảnh gương qua trục người nếu both=True, ngược lại buông thõng.
    Vai đặt ở giữa khung, rộng 0.2 chiều rộng ảnh (theo đơn vị chuẩn hoá).
    """
    cx, cy = 0.5, 0.3   # vai rộng 0.2 chiều rộng ảnh
    frames = []
    total = n + 2 * pad
    for k in range(total):
        t = min(max((k - pad) / (n - 1), 0), 1)
        active = pad <= k < pad + n
        lx, ly = traj(t) if active else (0.7, 1.35)
        rx, ry = (-lx, ly) if (both and active) else (-0.7, 1.35)

        # đổi (x,y) tính theo độ rộng vai (đẳng hướng) về toạ độ chuẩn hoá của ảnh
        def px(x, y):
            return [cx + x * 0.2, cy + y * 0.2 * ASPECT]

        pose = np.zeros((33, 4))
        pose[:, 3] = 0.0
        pose[11] = [cx + 0.1, cy, 0, 1.0]   # vai trái giải phẫu: bên phải ảnh
        pose[12] = [cx - 0.1, cy, 0, 1.0]
        lw, rw = px(lx, ly), px(rx, ry)
        pose[15] = [lw[0], lw[1], 0, 0.95]
        pose[16] = [rw[0], rw[1], 0, 0.95]

        hands = []
        if active and hands_detected:
            op = openness(t)
            hands.append(hand_points(lw, op))
            if both:
                hands.append(hand_points(rw, op))
        frames.append({"pose": pose.tolist(), "hands": hands})
    return frames


def arc(t):
    """Vẽ một cung từ cằm sang ngang ngực"""
    ang = np.pi * t
    return (0.7 - 0.5 * np.sin(ang) * 0.4 + 0.2 * t, 0.9 - 0.9 * np.sin(ang))


def circle(t):
    ang = 2 * np.pi * t
    return (0.5 + 0.35 * np.cos(ang), 0.2 + 0.35 * np.sin(ang))


def swipe_down(t):
    return (0.6, -0.2 + 1.1 * t)
