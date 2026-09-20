#!/usr/bin/env python3
"""Tải hai model MediaPipe (Apache-2.0) về thư mục models/. Chạy lại được, bỏ qua file đã có."""

from __future__ import annotations

import sys
import urllib.request
from pathlib import Path

BASE = "https://storage.googleapis.com/mediapipe-models"
FILES = {
    "pose_landmarker_lite.task": f"{BASE}/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task",
    "hand_landmarker.task": f"{BASE}/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task",
}

dest = Path(__file__).resolve().parent.parent / "models"
dest.mkdir(exist_ok=True)
for name, url in FILES.items():
    target = dest / name
    if target.exists() and target.stat().st_size > 1_000_000:
        print(f"đã có   {name}")
        continue
    print(f"đang tải {name} ...")
    urllib.request.urlretrieve(url, target)
    print(f"xong    {name} ({target.stat().st_size // 1024} KB)")
sys.exit(0)
