"""Trích landmark từ video bằng MediaPipe Tasks (Pose + Hand).

Dùng đúng hai model mà trình duyệt cũng dùng (`@mediapipe/tasks-vision`), nên
phân phối landmark của mẫu chuẩn và của người học cùng một nguồn gốc.

Vì sao không dùng Holistic Landmarker như tài liệu ban đầu: bản Python của Tasks
không có Holistic (chỉ `mediapipe.solutions.holistic` đã bị deprecate), còn bản
JS thì ở mức preview. Pose + Hand đều ổn định ở cả hai nền tảng và đủ cho M0:
khuôn mặt chỉ giúp đọc khẩu hình, chưa dùng ở mức này.
"""

from __future__ import annotations

import logging
import tempfile
import threading
from dataclasses import dataclass
from pathlib import Path

import cv2
import mediapipe as mp
from mediapipe.tasks.python import BaseOptions, vision

from . import config

log = logging.getLogger("ai.landmarks")

# Mỗi lượt trích tự tạo landmarker riêng (xem extract_file) nên chạy song song được; semaphore chỉ
# để chặn trần số lượt đồng thời, vì job sinh mẫu hàng loạt sẽ bắn nhiều yêu cầu một lúc và mỗi lượt
# đã dùng vài luồng bên trong MediaPipe.
_slots = threading.Semaphore(config.MAX_CONCURRENCY)


@dataclass
class RawFrame:
    """Một khung hình: pose 33 điểm (x, y, z, visibility) và 0-2 bàn tay, mỗi tay 21 điểm (x, y, z).

    Toạ độ x, y chuẩn hoá theo [0, 1] của khung hình (x theo chiều rộng, y theo chiều cao).
    Bàn tay KHÔNG gắn nhãn trái/phải: nhãn của MediaPipe đảo theo việc ảnh có bị lật gương
    hay không, nên gán trái/phải dựa vào vị trí cổ tay so với pose ở bước sau.
    """

    pose: list[list[float]] | None
    hands: list[list[list[float]]]


@dataclass
class ExtractedClip:
    frames: list[RawFrame]
    aspect: float
    fps: float
    duration_s: float


class Extractor:
    """Giữ hai model MediaPipe trong RAM. Không thread-safe → khoá khi dùng."""

    def __init__(self) -> None:
        for m in (config.POSE_MODEL, config.HAND_MODEL):
            if not Path(m).exists():
                raise FileNotFoundError(
                    f"Thiếu model MediaPipe: {m}. Chạy `python scripts/fetch_models.py` để tải."
                )
        self._pose_path = str(config.POSE_MODEL)
        self._hand_path = str(config.HAND_MODEL)

    def _new_landmarkers(self):
        pose = vision.PoseLandmarker.create_from_options(
            vision.PoseLandmarkerOptions(
                base_options=BaseOptions(model_asset_path=self._pose_path),
                running_mode=vision.RunningMode.VIDEO,
                num_poses=1,
            )
        )
        hand = vision.HandLandmarker.create_from_options(
            vision.HandLandmarkerOptions(
                base_options=BaseOptions(model_asset_path=self._hand_path),
                running_mode=vision.RunningMode.VIDEO,
                num_hands=2,
            )
        )
        return pose, hand

    def extract_file(self, path: str | Path, target_fps: float | None = None) -> ExtractedClip:
        target_fps = target_fps or config.EXTRACT_FPS
        cap = cv2.VideoCapture(str(path))
        if not cap.isOpened():
            raise ValueError("Không mở được video")
        try:
            src_fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
            width = cap.get(cv2.CAP_PROP_FRAME_WIDTH)
            height = cap.get(cv2.CAP_PROP_FRAME_HEIGHT)
            if not width or not height:
                raise ValueError("Video không có kích thước hợp lệ")
            step = max(1, round(src_fps / target_fps))

            frames: list[RawFrame] = []
            # Mỗi lần trích tạo landmarker mới: chế độ VIDEO giữ trạng thái theo dõi giữa các khung,
            # dùng lại cho video khác sẽ "dính" tay của video trước.
            with _slots:
                pose, hand = self._new_landmarkers()
                try:
                    idx = 0
                    while True:
                        ok, bgr = cap.read()
                        if not ok:
                            break
                        if idx % step == 0:
                            ts = int(idx / src_fps * 1000)
                            rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
                            image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
                            p = pose.detect_for_video(image, ts)
                            h = hand.detect_for_video(image, ts)
                            frames.append(_to_raw(p, h))
                        idx += 1
                finally:
                    pose.close()
                    hand.close()
            duration = idx / src_fps
            return ExtractedClip(frames=frames, aspect=width / height, fps=src_fps / step, duration_s=duration)
        finally:
            cap.release()

    def extract_bytes(self, data: bytes, suffix: str = ".mp4") -> ExtractedClip:
        # OpenCV không đọc được từ bộ nhớ → ghi tạm ra đĩa
        with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as f:
            f.write(data)
            name = f.name
        try:
            return self.extract_file(name)
        finally:
            Path(name).unlink(missing_ok=True)


def _to_raw(pose_res, hand_res) -> RawFrame:
    pose = None
    if pose_res.pose_landmarks:
        pose = [[lm.x, lm.y, lm.z, lm.visibility or 0.0] for lm in pose_res.pose_landmarks[0]]
    hands = [[[lm.x, lm.y, lm.z] for lm in hl] for hl in (hand_res.hand_landmarks or [])]
    return RawFrame(pose=pose, hands=hands)
