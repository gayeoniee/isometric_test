#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
미니룸 에셋 도우미.

  check   : 스프라이트가 우리 격자(2:1)에 맞는 각도인지 측정
  pastel  : 채도를 낮추고 명도를 올려 파스텔로 리컬러

사용 예
  python tools/isoasset.py check  assets/*.png
  python tools/isoasset.py pastel assets/*.png -o app/src/main/res/drawable-nodpi

직교 투영 기하 (이걸 알아야 check 결과를 읽을 수 있다)
  지면 축의 화면 기울기 = sin(고도각)
  타일 가로:세로        = 1 / sin(고도각)

  고도 30도  -> 타일 2.000 : 1   <- 우리 격자 (64x32)
  고도 35.26도 -> 타일 1.732 : 1  (true isometric)
  고도 45도  -> 타일 1.414 : 1   (Kenney 프리렌더가 이것)

주의: 흔히 쓰는 "26.565도"는 2:1 타일의 **화면상 모서리 각도**(atan 1/2)이지
카메라 고도각이 아니다. 카메라 고도는 30도다.
"""
import argparse
import math
import os
import sys

import numpy as np
from PIL import Image

TARGET_RATIO = 2.0  # 타일 64x32


def load(path):
    return Image.open(path).convert("RGBA")


def measure_slope(im, alpha_min=200):
    """윗면 왼쪽 모서리의 화면 기울기를 잰다. = sin(고도각)

    alpha_min 을 높게 잡는 이유: 그림자를 구워 넣은 PNG 는 물체 바깥으로
    반투명 픽셀이 번져 있어서, 낮은 임계값을 쓰면 그림자 가장자리를
    윗면 모서리로 착각한다 (실제로 그랬다).
    """
    a = np.asarray(im)
    solid = a[:, :, 3] >= alpha_min
    w = solid.shape[1]
    top = []
    for x in range(w):
        col = np.nonzero(solid[:, x])[0]
        top.append(col.min() if len(col) else None)
    xs = [x for x in range(w) if top[x] is not None]
    if len(xs) < 12:
        return None
    lo, hi = xs[0], xs[-1]
    seg = [(x, top[x]) for x in range(lo + 2, (lo + hi) // 2) if top[x] is not None]
    if len(seg) < 6:
        return None
    X = np.array([p[0] for p in seg], float)
    Y = np.array([p[1] for p in seg], float)
    return abs(np.polyfit(X, Y, 1)[0])


def cmd_check(paths):
    print("스프라이트 투영 각도 측정 (우리 격자는 2:1 = 고도 30도)\n")
    for p in paths:
        im = load(p)
        s = measure_slope(im)
        name = os.path.basename(p)
        if s is None or s <= 0 or s >= 1:
            print(f"  {name:34s} 측정 불가 (윗면 모서리가 뚜렷하지 않음)")
            continue
        elev = math.degrees(math.asin(min(s, 1.0)))
        ratio = 1.0 / s
        ok = abs(ratio - TARGET_RATIO) < 0.08
        print(
            f"  {name:34s} 기울기 {s:.3f}  고도 {elev:5.1f}도  타일 {ratio:.2f}:1  "
            + ("맞음" if ok else "안 맞음 -> 재렌더 필요")
        )


def pastelize(im, sat=0.42, lift=0.16, warm=0.02):
    a = np.asarray(im).astype(np.float32) / 255.0
    rgb, al = a[..., :3], a[..., 3:]
    mx = rgb.max(-1)
    mn = rgb.min(-1)
    v = mx
    d = mx - mn
    s = np.where(mx > 0, d / np.maximum(mx, 1e-6), 0)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    h = np.zeros_like(v)
    m = d > 1e-6
    i = (mx == r) & m
    h[i] = ((g - b)[i] / d[i]) % 6
    i = (mx == g) & m
    h[i] = ((b - r)[i] / d[i]) + 2
    i = (mx == b) & m
    h[i] = ((r - g)[i] / d[i]) + 4
    h = h / 6.0

    h2 = (h + warm) % 1.0
    s2 = s * sat
    v2 = np.clip(v * (1 - lift) + lift + 0.06, 0, 1)

    idx = np.floor(h2 * 6).astype(int) % 6
    f = h2 * 6 - np.floor(h2 * 6)
    p = v2 * (1 - s2)
    q = v2 * (1 - f * s2)
    t = v2 * (1 - (1 - f) * s2)
    out = np.zeros_like(rgb)
    combos = [(v2, t, p), (q, v2, p), (p, v2, t), (p, q, v2), (t, p, v2), (v2, p, q)]
    for k, (R, G, B) in enumerate(combos):
        sel = idx == k
        out[..., 0][sel] = R[sel]
        out[..., 1][sel] = G[sel]
        out[..., 2][sel] = B[sel]
    return Image.fromarray(
        (np.concatenate([out, al], -1) * 255).astype(np.uint8), "RGBA"
    )


def cmd_pastel(paths, outdir, sat, lift, warm):
    os.makedirs(outdir, exist_ok=True)
    for p in paths:
        out = pastelize(load(p), sat, lift, warm)
        dst = os.path.join(outdir, os.path.basename(p))
        out.save(dst)
        print("  ->", dst)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)

    c = sub.add_parser("check", help="격자에 맞는 각도인지 측정")
    c.add_argument("paths", nargs="+")

    p = sub.add_parser("pastel", help="파스텔로 리컬러")
    p.add_argument("paths", nargs="+")
    p.add_argument("-o", "--outdir", required=True)
    p.add_argument("--sat", type=float, default=0.42, help="채도 비율 (낮을수록 연함)")
    p.add_argument("--lift", type=float, default=0.16, help="명도 올림")
    p.add_argument("--warm", type=float, default=0.02, help="색상을 따뜻한 쪽으로")

    a = ap.parse_args()
    if a.cmd == "check":
        cmd_check(a.paths)
    else:
        cmd_pastel(a.paths, a.outdir, a.sat, a.lift, a.warm)


if __name__ == "__main__":
    sys.exit(main())
