"""Cấu hình đọc từ biến môi trường, có mặc định chạy được ngay trên máy dev."""

from __future__ import annotations

import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

MODEL_DIR = Path(os.environ.get("AI_MODEL_DIR", ROOT / "models"))
POSE_MODEL = MODEL_DIR / "pose_landmarker_lite.task"
HAND_MODEL = MODEL_DIR / "hand_landmarker.task"

# Bí mật dùng chung giữa Spring Boot và service này (xác thực máy-với-máy).
# Để trống = không kiểm tra, chỉ nên như vậy khi chạy local.
SHARED_SECRET = os.environ.get("AI_SERVICE_SECRET", "")

# Trần kích thước video gửi lên /extract. Video từ điển ~0,5MB; 30MB dư rộng.
MAX_VIDEO_BYTES = int(os.environ.get("AI_MAX_VIDEO_BYTES", 30 * 1024 * 1024))

# Số khung hình tối đa nhận ở /verify — chặn payload phình to vô ích
MAX_FRAMES = int(os.environ.get("AI_MAX_FRAMES", 400))

# Trích landmark ở tần số này khi đọc video mẫu (khớp với tần số lấy mẫu ở trình duyệt)
EXTRACT_FPS = float(os.environ.get("AI_EXTRACT_FPS", 15))

# Số video được trích song song. Mỗi lượt trích tự tạo landmarker riêng nên chạy song song được;
# giới hạn để không nuốt hết CPU của máy (mỗi lượt đã dùng vài luồng bên trong MediaPipe).
MAX_CONCURRENCY = int(os.environ.get("AI_MAX_CONCURRENCY", 4))
