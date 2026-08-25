#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# /// script
# requires-python = ">=3.10"
# dependencies = ["pillow"]
# ///
"""방 그림에서 문짝 윤곽을 떠서 [DoorSpec.TOP] / [DoorSpec.BOTTOM] 값을 뽑는다.

사용:
    uv run tools/trace_door.py [확인용_이미지.png]

찍힌 배열을 RoomShellShapes.kt 의 DoorSpec 에 붙여 넣으면 된다. 확인용 이미지에
뽑은 윤곽이 분홍 선으로 얹혀 나오므로, 문에 붙었는지 눈으로 보고 넣는다.

문 색을 밝은 올리브로만 잡으면 **위쪽 그늘을 통째로 놓친다** (아치 꼭대기는
(60,58,15) 까지 어둡다). 문의 진짜 표식은 밝기가 아니라 r≈g 이면서 파랑이
빠졌다는 것 — 벽·문틀은 r>g>b 로 붉은기가 돈다.
"""
import sys
from collections import deque

from PIL import Image, ImageDraw

SRC = "app/src/main/res/drawable-nodpi/theme_sage_room.webp"
CHECK = sys.argv[1] if len(sys.argv) > 1 else "door_trace.png"
N = 21

im = Image.open(SRC).convert("RGBA")
W, H = im.size
px = im.load()
x0, x1 = int(0.03 * W), int(0.24 * W)
y0, y1 = int(0.30 * H), int(0.75 * H)


def door(c):
    r, g, b, a = c
    return a > 128 and abs(r - g) < 18 and r - b > 20 and r < 175 and b < 110


seen = [[False] * (y1 - y0) for _ in range(x1 - x0)]
best = []
for sx in range(x1 - x0):
    for sy in range(y1 - y0):
        if seen[sx][sy] or not door(px[x0 + sx, y0 + sy]):
            continue
        blob, q = [], deque([(sx, sy)])
        seen[sx][sy] = True
        while q:
            cx, cy = q.popleft()
            blob.append((cx, cy))
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = cx + dx, cy + dy
                if 0 <= nx < x1 - x0 and 0 <= ny < y1 - y0 and not seen[nx][ny] \
                        and door(px[x0 + nx, y0 + ny]):
                    seen[nx][ny] = True
                    q.append((nx, ny))
        if len(blob) > len(best):
            best = blob

cols = {}
for cx, cy in best:
    x, y = x0 + cx, y0 + cy
    lo, hi = cols.get(x, (10 ** 9, -1))
    cols[x] = (min(lo, y), max(hi, y))

xs = sorted(cols)
L, R = xs[0], xs[-1] + 1
T = min(c[0] for c in cols.values())
B = max(c[1] for c in cols.values()) + 1
print("leaf  Rect(%.2f, %.2f, %.2f, %.2f)" % (L / W * 100, T / H * 100, R / W * 100, B / H * 100))
print("덩어리 %d px, 열 %d개 (%d..%d)" % (len(best), len(xs), L, R))

top, bot = [], []
half = max(1, (R - L) // (N - 1) // 2)
for i in range(N):
    xc = L + (R - 1 - L) * i / (N - 1)
    win = [cols[x] for x in xs if abs(x - xc) <= half] or [cols[min(xs, key=lambda x: abs(x - xc))]]
    top.append((min(p[0] for p in win) - T) / (B - T))
    bot.append((max(p[1] for p in win) + 1 - T) / (B - T))

fmt = lambda a: ", ".join("%.4ff" % v for v in a)
print("TOP    = floatArrayOf(%s)" % fmt(top))
print("BOTTOM = floatArrayOf(%s)" % fmt(bot))

crop = im.crop((L - 26, T - 26, R + 26, B + 26)).convert("RGB")
z = 4
crop = crop.resize((crop.width * z, crop.height * z), Image.NEAREST)
d = ImageDraw.Draw(crop)
pts = [((L + (R - L) * i / (N - 1) - (L - 26)) * z, (T + (B - T) * top[i] - (T - 26)) * z)
       for i in range(N)]
pts += [((L + (R - L) * i / (N - 1) - (L - 26)) * z, (T + (B - T) * bot[i] - (T - 26)) * z)
        for i in range(N - 1, -1, -1)]
d.line(pts + [pts[0]], fill=(255, 0, 128), width=3)
crop.save(CHECK)
print("check ->", CHECK)
