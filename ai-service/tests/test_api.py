import os
import sys

import numpy as np
import pytest
from fastapi.testclient import TestClient

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app import config, features as F  # noqa: E402
from app.main import app  # noqa: E402
from synth import ASPECT, arc, circle, make_frames  # noqa: E402

client = TestClient(app)


def exemplar(traj, eid="ex1"):
    feats = F.build_clip(make_frames(traj), ASPECT).features
    return {"id": eid, "features": np.round(feats, 4).tolist()}


def body(frames, exemplars, aspect=ASPECT):
    return {"aspect": aspect, "frames": frames, "exemplars": exemplars}


def test_health():
    r = client.get("/health").json()
    assert r["status"] == "ok"
    assert r["featureVersion"]


def test_verify_happy_path():
    r = client.post("/verify", json=body(make_frames(arc), [exemplar(circle, "bad"), exemplar(arc, "good")]))
    assert r.status_code == 200, r.text
    j = r.json()
    assert j["bestExemplarId"] == "good"
    assert j["distance"] < 0.05
    assert set(j["components"]) == {"handshape", "location", "movement"}
    assert j["handCount"] == 2
    assert 0 <= j["trackingQuality"] <= 1


def test_verify_wrong_trajectory_is_far():
    j = client.post("/verify", json=body(make_frames(circle), [exemplar(arc)])).json()
    assert j["distance"] > 0.12


def test_verify_no_body_is_422_with_code():
    frames = make_frames(arc)
    for f in frames:
        f["pose"] = None
    r = client.post("/verify", json=body(frames, [exemplar(arc)]))
    assert r.status_code == 422
    assert r.json()["detail"]["code"] == "NO_BODY"


def test_verify_rejects_malformed_exemplar():
    r = client.post("/verify", json=body(make_frames(arc), [{"id": "x", "features": [[0.0] * 5]}]))
    assert r.status_code == 400
    assert r.json()["detail"]["code"] == "BAD_EXEMPLAR"


def test_verify_rejects_too_many_frames(monkeypatch):
    monkeypatch.setattr(config, "MAX_FRAMES", 10)
    r = client.post("/verify", json=body(make_frames(arc), [exemplar(arc)]))
    assert r.status_code == 413


def test_verify_requires_empty_exemplar_list_rejected():
    r = client.post("/verify", json=body(make_frames(arc), []))
    assert r.status_code == 422


def test_secret_enforced_when_configured(monkeypatch):
    monkeypatch.setattr(config, "SHARED_SECRET", "s3cret")
    payload = body(make_frames(arc), [exemplar(arc)])
    assert client.post("/verify", json=payload).status_code == 401
    assert client.post("/verify", json=payload, headers={"X-AI-Secret": "wrong"}).status_code == 401
    assert client.post("/verify", json=payload, headers={"X-AI-Secret": "s3cret"}).status_code == 200
