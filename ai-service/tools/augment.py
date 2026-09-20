"""Giả lập người học từ một clip mẫu bằng cách biến dạng landmark thô.

Dùng để hiệu chỉnh ngưỡng khi CHƯA có người học thật: nếu một mẫu bị biến dạng kiểu này mà vẫn không
khớp được với chính nó thì chắc chắn ngưỡng quá chặt. Đây là cận DƯỚI của độ biến thiên thật — người
học thật còn khác ở tỉ lệ cơ thể, cỡ bàn tay, độ nhoè webcam — nên ngưỡng phải rộng hơn kết quả ở đây.
"""

from __future__ import annotations

import numpy as np

from app.landmarks import RawFrame


def augment(frames, aspect, rng, noise=0.004, drop=0.1):
    speed = rng.uniform(0.7, 1.4)                       # ký nhanh/chậm hơn mẫu
    rot = np.deg2rad(rng.uniform(-7, 7))                # camera nghiêng
    scale = rng.uniform(0.8, 1.25)                      # ngồi gần/xa camera
    shift = rng.uniform(-0.08, 0.08, 2)                 # lệch khỏi giữa khung
    n = len(frames)
    m = max(6, int(round(n / speed)))
    src = np.linspace(0, n - 1, m).round().astype(int)
    c, s = np.cos(rot), np.sin(rot)
    rot_m = np.array([[c, -s], [s, c]]).T

    def tf(xy):
        p = (xy - 0.5) * np.array([aspect, 1.0])
        p = p @ rot_m * scale
        p = p / np.array([aspect, 1.0]) + 0.5 + shift
        return p + rng.normal(0, noise, p.shape)

    out = []
    for idx in src:
        f = frames[idx]
        pose = None
        if f.pose is not None:
            a = np.array(f.pose)
            a[:, :2] = tf(a[:, :2])
            a[:, 2] *= scale
            pose = a.tolist()
        hands = []
        if rng.random() > drop:                         # Hand Landmarker thỉnh thoảng bỏ sót
            for h in f.hands:
                a = np.array(h)
                a[:, :2] = tf(a[:, :2])
                a[:, 2] *= scale
                hands.append(a.tolist())
        out.append(RawFrame(pose=pose, hands=hands))
    pre = [out[0]] * int(rng.integers(0, 8))            # đoạn nghỉ trước/sau khi ký
    post = [out[-1]] * int(rng.integers(0, 8))
    return pre + out + post
