#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# /// script
# requires-python = ">=3.10"
# dependencies = ["pillow", "numpy"]
# ///
"""
미니룸 에셋 도우미.

  check   : 스프라이트가 우리 격자(2:1)에 맞는 각도인지 측정
  pastel  : 채도를 낮추고 명도를 올려 파스텔로 리컬러

사용 예 (팀 규칙: 파이썬은 uv 로 통일)
  uv run tools/isoasset.py check  assets/*.png
  uv run tools/isoasset.py pastel assets/*.png -o app/src/main/res/drawable-nodpi

위 /// script 블록에 의존성이 적혀 있어서, uv run 이 알아서 받아 쓴다.
가상환경을 따로 만들거나 설치할 필요가 없다.

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


def write_svg(path, cols, rows, W, H, ax, ay, scale):
    """피그마/일러스트/잉크스케이프에 그대로 열리는 벡터 템플릿.

    viewBox 단위 = 기본 단위(타일 64x32)라서, 도형 좌표를 그대로 읽어
    ItemCatalog 의 ArtBox 값으로 옮길 수 있다.
    """
    TW, TH = 64.0, 32.0
    fw, fh = cols * TW, rows * TH
    ch = TW / 2  # 한 칸 높이

    def dia(cx, cy, w, h):
        return "%g,%g %g,%g %g,%g %g,%g" % (
            cx, cy - h / 2, cx + w / 2, cy, cx, cy + h / 2, cx - w / 2, cy)

    def line(a, b):
        return '<line x1="%g" y1="%g" x2="%g" y2="%g"/>' % (a[0], a[1], b[0], b[1])

    N = (ax, ay - TH / 2)
    E = (ax + TW / 2, ay)
    S = (ax, ay + TH / 2)
    Wv = (ax - TW / 2, ay)

    def up(p):
        return (p[0], p[1] - ch)

    cube = [line(Wv, S), line(S, E)]
    cube += [line(p, up(p)) for p in (Wv, S, E)]
    cube += [line(up(a), up(b)) for a, b in ((N, E), (E, S), (S, Wv), (Wv, N))]

    # 2:1 기울기 안내선 (화면 각도 26.57도)
    guides = []
    for k in range(-4, 5):
        oy = ay + k * TH
        guides.append(line((0, oy + W / 4), (W, oy - W / 4)))
        guides.append(line((0, oy - W / 4), (W, oy + W / 4)))

    out = []
    out.append('<svg xmlns="http://www.w3.org/2000/svg" width="%g" height="%g" viewBox="0 0 %g %g">'
               % (W * scale, H * scale, W, H))
    out.append('  <title>iso template %dx%d, tile 64x32 (2:1)</title>' % (cols, rows))
    out.append('  <!-- viewBox 1 unit = base unit 1. ArtBox size=(%g,%g) anchor=(%g,%g) -->'
               % (W, H, ax, ay))
    out.append('  <g id="guides-angle" stroke="#c9d8ef" stroke-width="0.4" fill="none">')
    out.append("    " + "".join(guides))
    out.append('  </g>')
    out.append('  <g id="reference-cube" stroke="#7aa0d8" stroke-width="0.7" fill="none">')
    out.append("    " + "".join(cube))
    out.append('  </g>')
    out.append('  <g id="footprint">')
    out.append('    <polygon points="%s" fill="#f5c6d3" fill-opacity="0.25" stroke="#e2547f" stroke-width="0.9"/>'
               % dia(ax, ay, fw, fh))
    out.append('  </g>')
    out.append('  <g id="anchor" stroke="#ff2856" stroke-width="0.9">')
    out.append("    " + line((ax - 3, ay), (ax + 3, ay)) + line((ax, ay - 3), (ax, ay + 3)))
    out.append('  </g>')
    out.append('  <g id="labels" font-family="sans-serif" font-size="3.4" fill="#555">')
    out.append('    <text x="1.5" y="5">footprint %dx%d, tile 64x32 (2:1)</text>' % (cols, rows))
    out.append('    <text x="1.5" y="10">ArtBox size = (%g, %g)</text>' % (W, H))
    out.append('    <text x="1.5" y="15" fill="#e2547f">anchor = (%g, %g)</text>' % (ax, ay))
    out.append('  </g>')
    out.append('</svg>')
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(out) + "\n")


def cmd_template(outdir, cols, rows, height_tiles, scale):
    """2D로 직접 그릴 때 밑에 깔 아이소메트릭 격자 템플릿.

    기본 단위에서 타일은 64x32 (2:1). scale 배로 확대해 내보낸다.
    """
    from PIL import ImageDraw

    TW, TH = 64.0 * scale, 32.0 * scale
    fw, fh = cols * TW, rows * TH          # 발자국 다이아몬드 크기
    W = int(fw + TW)                        # 좌우 여유 반 칸씩
    top_room = height_tiles * TH * 2        # 위로 쓸 수 있는 높이
    H = int(top_room + fh + TH * 0.6)
    ax, ay = W / 2.0, H - fh / 2.0 - TH * 0.3   # 기준점 = 발자국 중심

    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)

    # 투명 확인용 체커보드
    c = int(8 * scale)
    for y in range(0, H, c):
        for x in range(0, W, c):
            if (x // c + y // c) % 2 == 0:
                d.rectangle([x, y, x + c, y + c], fill=(244, 244, 244, 255))

    GUIDE = (232, 122, 150, 255)
    GHOST = (150, 180, 230, 120)

    def dia(cx, cy, w, h, **kw):
        d.polygon([(cx, cy - h / 2), (cx + w / 2, cy), (cx, cy + h / 2), (cx - w / 2, cy)], **kw)

    # 발자국 다이아몬드 (= 이 아이템이 차지하는 바닥)
    dia(ax, ay, fw, fh, outline=GUIDE, width=max(2, int(scale)))
    # 칸 구분선
    for i in range(1, cols):
        t = i / cols
        d.line([(ax - fw / 2 + fw * t / 2, ay - fh / 2 + fh * t / 2),
                (ax + fw * t / 2, ay + fh / 2 - fh * (1 - t) / 2)],
               fill=(232, 122, 150, 90), width=1)
    for i in range(1, rows):
        t = i / rows
        d.line([(ax + fw / 2 - fw * t / 2, ay - fh / 2 + fh * t / 2),
                (ax - fw * t / 2, ay + fh / 2 - fh * (1 - t) / 2)],
               fill=(232, 122, 150, 90), width=1)

    # 1x1x1 참고 정육면체 — 이 각도에 맞춰 그리면 격자에 맞는다.
    # 밑면은 발자국 다이아몬드와 정확히 겹친다.
    ch = TW / 2                      # 한 칸 높이 = 타일 가로의 절반
    N = (ax, ay - TH / 2)            # 뒤
    E = (ax + TW / 2, ay)            # 오른쪽
    S = (ax, ay + TH / 2)            # 앞 (보이는 모서리)
    W_ = (ax - TW / 2, ay)           # 왼쪽
    up = lambda p: (p[0], p[1] - ch)

    # 보이는 밑면 두 변
    d.line([W_, S], fill=GHOST, width=2)
    d.line([S, E], fill=GHOST, width=2)
    # 수직 모서리 — 왼쪽/앞/오른쪽만 보인다
    for p_ in (W_, S, E):
        d.line([p_, up(p_)], fill=GHOST, width=2)
    # 윗면 마름모
    for p_, q_ in ((N, E), (E, S), (S, W_), (W_, N)):
        d.line([up(p_), up(q_)], fill=GHOST, width=2)

    # 높이 눈금 (한 칸 = TH)
    for k in range(1, int(height_tiles * 2) + 1):
        y = ay - fh / 2 - k * TH
        if y < 4:
            break
        d.line([(6, y), (18, y)], fill=(120, 120, 120, 160), width=1)
        d.text((22, y - 6), "%d" % (k * 32), fill=(120, 120, 120, 200))

    # 기준점 십자
    r = 9 * scale / 4
    d.line([(ax - r, ay), (ax + r, ay)], fill=(255, 40, 90, 255), width=max(2, int(scale / 2)))
    d.line([(ax, ay - r), (ax, ay + r)], fill=(255, 40, 90, 255), width=max(2, int(scale / 2)))

    d.text((6, 6), "footprint %dx%d  tile 64x32 (2:1)  scale x%d" % (cols, rows, scale),
           fill=(60, 60, 60, 255))
    d.text((6, 20), "ArtBox size = (%d, %d) base units" % (W / scale, H / scale),
           fill=(60, 60, 60, 255))
    d.text((6, 36), "anchor = (%d, %d)  <- 빨간 십자" % (ax / scale, ay / scale),
           fill=(200, 30, 70, 255))

    os.makedirs(outdir, exist_ok=True)
    base = os.path.join(outdir, "iso-template-%dx%d" % (cols, rows))
    im.save(base + ".png")
    write_svg(base + ".svg", cols, rows, W / scale, H / scale, ax / scale, ay / scale, scale)
    print("  -> %s.png / .svg   ArtBox size=(%d,%d)  anchor=(%d,%d)"
          % (base, W / scale, H / scale, ax / scale, ay / scale))


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

    t = sub.add_parser("template", help="2D로 직접 그릴 때 쓸 격자 템플릿 생성")
    t.add_argument("-o", "--outdir", default="docs/templates")
    t.add_argument("--cols", type=int, default=1)
    t.add_argument("--rows", type=int, default=1)
    t.add_argument("--height", type=float, default=1.5, help="위로 확보할 높이 (타일 수)")
    t.add_argument("--scale", type=int, default=4)

    a = ap.parse_args()
    if a.cmd == "check":
        cmd_check(a.paths)
    elif a.cmd == "template":
        cmd_template(a.outdir, a.cols, a.rows, a.height, a.scale)
    else:
        cmd_pastel(a.paths, a.outdir, a.sat, a.lift, a.warm)


if __name__ == "__main__":
    sys.exit(main())
