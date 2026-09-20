"""SignAI AI service — FastAPI, STATELESS.

Không có CSDL, không biết người dùng là ai, không biết ngưỡng "đạt" là bao nhiêu.
Spring Boot là bên duy nhất gọi service này (xác thực bằng bí mật dùng chung), đưa vào
landmark của người học cùng exemplar của từ mục tiêu và nhận lại khoảng cách + gợi ý.

    GET  /health    tình trạng + phiên bản đặc trưng (để Spring biết exemplar nào đã cũ)
    POST /extract   video mẫu → chuỗi đặc trưng chuẩn hoá (dùng khi sinh exemplar)
    POST /verify    landmark người học + exemplar → khoảng cách, điểm từng phần, gợi ý
"""

from __future__ import annotations

import asyncio
import hmac
import logging
import time
from contextlib import asynccontextmanager

import numpy as np
from fastapi import Depends, FastAPI, File, Header, HTTPException, UploadFile
from pydantic import BaseModel, Field, field_validator

from . import config, features as F, matcher as M
from .landmarks import Extractor

log = logging.getLogger("ai.main")

# Đổi bất cứ điều gì làm thay đổi đặc trưng (features.py) hoặc thang đo (matcher.py) thì tăng số này:
# Spring so sánh với `model_version` của exemplar và coi exemplar cũ là hết hạn, cần sinh lại.
FEATURE_VERSION = "pose-hand-features-v1"
VERIFY_VERSION = "verify-dtw-v1"

_extractor: Extractor | None = None


@asynccontextmanager
async def lifespan(_: FastAPI):
    global _extractor
    try:
        _extractor = Extractor()
    except FileNotFoundError as e:
        # /verify vẫn chạy được không cần MediaPipe; chỉ /extract báo lỗi
        log.warning("%s", e)
    yield


app = FastAPI(title="SignAI AI service", version="1.0.0", lifespan=lifespan)


def require_secret(x_ai_secret: str | None = Header(default=None)) -> None:
    if not config.SHARED_SECRET:
        return
    if not x_ai_secret or not hmac.compare_digest(x_ai_secret, config.SHARED_SECRET):
        raise HTTPException(status_code=401, detail="Sai bí mật dịch vụ")


# ---------------------------------------------------------------------------
# Schema
# ---------------------------------------------------------------------------

class FrameIn(BaseModel):
    # 33 điểm × [x, y, z, visibility]; null khi khung này không thấy người
    pose: list[list[float]] | None = None
    # 0-2 bàn tay, mỗi tay 21 điểm × [x, y, z], KHÔNG gắn nhãn trái/phải
    hands: list[list[list[float]]] = Field(default_factory=list, max_length=2)

    @field_validator("pose")
    @classmethod
    def _check_pose(cls, v):
        if v is not None and (len(v) != 33 or any(len(p) != 4 for p in v)):
            raise ValueError("pose cần đúng 33 điểm, mỗi điểm [x, y, z, visibility]")
        return v

    @field_validator("hands")
    @classmethod
    def _check_hands(cls, v):
        for h in v:
            if len(h) != 21 or any(len(p) != 3 for p in h):
                raise ValueError("mỗi bàn tay cần đúng 21 điểm, mỗi điểm [x, y, z]")
        return v


class ExemplarIn(BaseModel):
    id: str
    features: list[list[float]]


class VerifyRequest(BaseModel):
    aspect: float = Field(gt=0.2, lt=5)   # rộng / cao của khung hình đã quay
    frames: list[FrameIn]
    exemplars: list[ExemplarIn] = Field(min_length=1, max_length=12)


class ExtractResponse(BaseModel):
    features: list[list[float]]
    quality: float
    activeFrames: int
    handCount: int
    durationSec: float
    featureVersion: str


class Components(BaseModel):
    handshape: float
    location: float
    movement: float


class VerifyResponse(BaseModel):
    distance: float
    components: Components
    bestExemplarId: str
    mirrored: bool
    hints: list[str]
    trackingQuality: float
    activeFrames: int
    handCount: int
    modelVersion: str
    processingMs: int


# ---------------------------------------------------------------------------
# Endpoint
# ---------------------------------------------------------------------------

@app.get("/health")
def health():
    return {
        "status": "ok",
        "mediapipe": _extractor is not None,
        "featureVersion": FEATURE_VERSION,
        "verifyVersion": VERIFY_VERSION,
    }


@app.post("/extract", response_model=ExtractResponse, dependencies=[Depends(require_secret)])
async def extract(video: UploadFile = File(...)):
    if _extractor is None:
        raise HTTPException(503, {"code": "MODEL_MISSING", "message": "Chưa có model MediaPipe (chạy scripts/fetch_models.py)"})
    data = await video.read()
    if not data:
        raise HTTPException(400, {"code": "EMPTY", "message": "Tệp video rỗng"})
    if len(data) > config.MAX_VIDEO_BYTES:
        raise HTTPException(413, {"code": "TOO_LARGE", "message": "Video quá lớn"})
    try:
        # Trích landmark là việc nặng và đồng bộ: chạy thẳng trong hàm async sẽ khoá vòng lặp sự kiện
        # và mọi yêu cầu khác phải xếp hàng sau nó — job sinh mẫu hàng loạt sẽ chậm đúng bằng tuần tự.
        clip, result = await asyncio.to_thread(_extract_sync, data)
    except F.FeatureError as e:
        raise HTTPException(422, {"code": e.code, "message": str(e)})
    except ValueError as e:
        raise HTTPException(400, {"code": "BAD_VIDEO", "message": str(e)})
    return ExtractResponse(
        features=np.round(result.features, 4).tolist(),
        quality=round(result.quality, 3),
        activeFrames=result.active_frames,
        handCount=_hand_count(result.features),
        durationSec=round(clip.duration_s, 2),
        featureVersion=FEATURE_VERSION,
    )


@app.post("/verify", response_model=VerifyResponse, dependencies=[Depends(require_secret)])
def verify(req: VerifyRequest):
    started = time.perf_counter()
    if len(req.frames) > config.MAX_FRAMES:
        raise HTTPException(413, {"code": "TOO_MANY_FRAMES", "message": f"Tối đa {config.MAX_FRAMES} khung hình"})

    exemplars: list[tuple[str, np.ndarray]] = []
    for ex in req.exemplars:
        arr = np.asarray(ex.features, dtype=np.float64)
        if arr.shape != (F.N_STEPS, F.N_FEATURES) or not np.isfinite(arr).all():
            raise HTTPException(400, {"code": "BAD_EXEMPLAR", "message": f"Exemplar {ex.id} sai kích thước {arr.shape}"})
        exemplars.append((ex.id, arr))

    try:
        clip = F.build_clip([f.model_dump() for f in req.frames], req.aspect)
    except F.FeatureError as e:
        raise HTTPException(422, {"code": e.code, "message": str(e)})

    best = M.best_match(clip.features, exemplars)
    ex_features = dict(exemplars)[best.exemplar_id]
    return VerifyResponse(
        distance=round(best.distance, 4),
        components=Components(
            handshape=round(best.handshape, 4),
            location=round(best.location, 4),
            movement=round(best.movement, 4),
        ),
        bestExemplarId=best.exemplar_id,
        mirrored=best.mirrored,
        hints=best.hints,
        trackingQuality=round(clip.quality, 3),
        activeFrames=clip.active_frames,
        handCount=_hand_count(ex_features),
        modelVersion=VERIFY_VERSION,
        processingMs=int((time.perf_counter() - started) * 1000),
    )


def _extract_sync(data: bytes):
    clip = _extractor.extract_bytes(data)
    return clip, F.build_clip(clip.frames, clip.aspect)


def _hand_count(features: np.ndarray) -> int:
    """Số tay tham gia ký hiệu, đo từ mẫu: tay nào giơ > 30% thời gian thì tính.

    Cột signs.hand_count trong CSDL đang là 1 cho cả 3322 từ (giá trị mặc định chưa ai nhập),
    trong khi đa số ký hiệu dùng hai tay — nên số đo được từ exemplar mới là số đáng tin.
    """
    return int((features[:, F.PRES].mean(axis=0) > 0.3).sum()) or 1
