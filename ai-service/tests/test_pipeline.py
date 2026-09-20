import os
import sys

import numpy as np
import pytest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app import features as F, matcher as M  # noqa: E402
from synth import ASPECT, arc, circle, make_frames, swipe_down  # noqa: E402


def clip(traj, **kw):
    return F.build_clip(make_frames(traj, **kw), ASPECT).features


def dist(a, b):
    return M.best_match(a, [("x", b)]).distance


def test_feature_shape_and_finite():
    f = clip(arc)
    assert f.shape == (F.N_STEPS, F.N_FEATURES)
    assert np.isfinite(f).all()


def test_self_match_is_near_zero():
    a = clip(arc)
    assert dist(a, a) < 0.02


def test_speed_and_rest_padding_do_not_matter():
    slow = clip(arc, n=60, pad=2)
    fast = clip(arc, n=25, pad=9)
    assert dist(slow, fast) < 0.10


def test_different_trajectory_is_far():
    assert dist(clip(arc), clip(circle)) > 0.12
    assert dist(clip(arc), clip(swipe_down)) > 0.12


def test_different_handshape_is_far():
    open_hand = clip(arc, openness=lambda t: 1.0)
    fist = clip(arc, openness=lambda t: 0.0)
    assert dist(open_hand, fist) > dist(open_hand, clip(arc, openness=lambda t: 0.9))


def test_opposite_dominant_hand_still_matches():
    # Người ký thuận tay trái: chỉ tay trái giải phẫu ký; mẫu chỉ tay phải. Phải khớp nhờ thử chiều gương.
    def one_hand(traj, side):
        frames = make_frames(traj, both=False)
        for f in frames:
            f["hands"] = f["hands"][:1]
        return frames

    left = F.build_clip(one_hand(arc, 0), ASPECT).features
    # dựng tay phải bằng cách lật gương chuỗi trái
    right = M.mirror(left)
    assert dist(left, right) < 0.02


def test_one_hand_vs_two_hands_is_penalised():
    two = clip(arc, both=True)
    one = clip(arc, both=False)
    assert dist(two, one) > dist(two, clip(arc, both=True, n=30))
    assert "HAND_MISSING" in M.best_match(one, [("x", two)]).hints


def test_location_hint_when_hand_too_low():
    ref = clip(arc)
    low = clip(lambda t: (arc(t)[0], arc(t)[1] + 0.5))
    hints = M.best_match(low, [("x", ref)]).hints
    assert "LOCATION_TOO_LOW" in hints
    high = clip(lambda t: (arc(t)[0], arc(t)[1] - 0.5))
    assert "LOCATION_TOO_HIGH" in M.best_match(high, [("x", ref)]).hints


def test_movement_hint_when_barely_moving():
    ref = clip(arc)
    tiny = clip(lambda t: (0.6 + 0.02 * t, 0.4))
    assert "MOVEMENT_TOO_SMALL" in M.best_match(tiny, [("x", ref)]).hints


def test_best_of_several_exemplars():
    ref_far, ref_near = clip(circle), clip(arc)
    learner = clip(arc, n=32)
    best = M.best_match(learner, [("far", ref_far), ("near", ref_near)])
    assert best.exemplar_id == "near"


def test_no_body_raises():
    frames = make_frames(arc)
    for f in frames:
        f["pose"] = None
    with pytest.raises(F.FeatureError) as e:
        F.build_clip(frames, ASPECT)
    assert e.value.code == "NO_BODY"


def test_no_raised_hand_raises():
    frames = make_frames(lambda t: (0.7, 1.35))   # tay luôn buông
    with pytest.raises(F.FeatureError) as e:
        F.build_clip(frames, ASPECT)
    assert e.value.code == "NO_SIGN"


def test_survives_hand_detector_dropouts():
    # Hand Landmarker bỏ sót một nửa số khung: vị trí lấy từ pose, hình tay điền từ khung gần nhất
    frames = make_frames(arc)
    for i, f in enumerate(frames):
        if i % 2:
            f["hands"] = []
    dropped = F.build_clip(frames, ASPECT).features
    assert dist(dropped, clip(arc)) < 0.15


def test_scale_and_shift_invariance():
    ref = make_frames(arc)
    # dời và phóng to cả người trong khung hình (camera gần hơn)
    moved = []
    for f in ref:
        pose = np.array(f["pose"])
        pose[:, :2] = (pose[:, :2] - 0.5) * 1.3 + 0.55
        hands = [((np.array(h) - [0.5, 0.5, 0]) * [1.3, 1.3, 1.3] + [0.55, 0.55, 0]).tolist() for h in f["hands"]]
        moved.append({"pose": pose.tolist(), "hands": hands})
    assert dist(F.build_clip(moved, ASPECT).features, F.build_clip(ref, ASPECT).features) < 0.10
