#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# /// script
# requires-python = ">=3.10"
# dependencies = ["pillow"]
# ///
"""
방 PNG의 **바깥 배경을 투명하게** 뚫는다.

저쪽(frankie516c/dog-training-rag)에서 받은 방 그림은 알파가 없는 RGB 라서,
방 실루엣 바깥이 황토색으로 꽉 차 있다. 그대로 쓰면 앱 배경(우리 테마색) 위에
황토색 카드가 얹힌 것처럼 보인다.

그래서 **가장자리에서 flood fill** 로 바깥 영역을 찾아 알파를 0 으로 만든다.
방 안쪽(창밖 하늘처럼 우연히 배경색과 비슷한 부분)은 가장자리와 이어져 있지
않으므로 안전하다 — 색만 보고 지우면 그런 곳까지 뚫린다.

## 두 단계로 나눈 이유

한 번의 flood fill 로 "바탕색이거나 바탕보다 어두운 픽셀"을 따라가게 했더니
**방 안으로 새어 들어갔다.** 바닥 나무와 크림색 벽이 바탕 황토색과 밝기가
비슷해서 그대로 타고 들어간다.

그래서 나눈다.

  1. **엄격한 flood** — 바탕색과 거의 같은 픽셀만 따라간다. 방 근처의 그림자에서
     멈추므로 방 안은 절대 안 건드린다
  2. **그림자만 벗겨내기** — 1번이 남긴 경계에서 안쪽으로, **바탕보다 어두운**
     픽셀만 한 겹씩 먹어 들어간다. 방의 테두리(크림색 몰딩·바닥 가장자리)는
     바탕보다 **밝아서** 여기서 저절로 멈춘다

그림자는 지우지 않고 반투명 검정으로 바꿔 남긴다. 통째로 지우면 방이 허공에 뜬다.

사용:
    uv run tools/room_cutout.py <입력.png> [출력.png]

출력을 안 주면 입력 파일을 덮어쓴다. 저쪽에서 색만 다른 방 PNG 가 오면
그때마다 이걸 한 번 돌리면 된다.
"""

import sys
from collections import deque

from PIL import Image

# 바탕색으로 볼 색 거리. 픽셀 아트라 배경이 거의 단색이지만 노이즈가 조금 있다.
TOLERANCE = 26

# 그림자로 인정할 최대 어두움. 알파를 이 값으로 정규화한다.
SHADOW_DEPTH = 70

# 그림자를 벗겨낼 최대 두께(px). 폭주 방지용 상한이고 보통 그 전에 멈춘다.
SHADOW_MAX_PEEL = 40

# 그림자 최대 진하기. 1.0 이면 원본만큼 진해져서 배경이 밝을 때 튄다.
SHADOW_ALPHA = 0.55


def cutout(src_path: str, dst_path: str) -> None:
    image = Image.open(src_path).convert("RGBA")
    width, height = image.size
    pixels = image.load()

    # 네 모서리의 평균을 바탕색으로 본다. 한 점만 보면 노이즈에 휘둘린다.
    corners = [pixels[0, 0], pixels[width - 1, 0], pixels[0, height - 1], pixels[width - 1, height - 1]]
    base = tuple(sum(c[i] for c in corners) // len(corners) for i in range(3))
    base_luma = luma(base)

    outside = flood_from_border(pixels, width, height, base)
    shadow = peel_shadow(pixels, width, height, outside, base_luma)

    cleared = 0
    shaded = 0
    for y in range(height):
        for x in range(width):
            i = y * width + x
            if shadow[i]:
                r, g, b, _ = pixels[x, y]
                darkness = base_luma - luma((r, g, b))
                a = min(1.0, max(0.0, darkness) / SHADOW_DEPTH) * SHADOW_ALPHA
                pixels[x, y] = (36, 26, 18, int(a * 255))
                shaded += 1
            elif outside[i]:
                pixels[x, y] = (0, 0, 0, 0)
                cleared += 1

    image.save(dst_path)
    print(f"바탕색 {base}  ->  투명 {cleared:,}px, 그림자 {shaded:,}px")


def peel_shadow(pixels, width, height, outside, base_luma):
    """
    바깥 경계에서 안쪽으로 **바탕보다 어두운 픽셀**만 한 겹씩 벗겨낸다.

    방의 테두리는 크림색 몰딩이라 바탕보다 밝다 — 그래서 여기서 저절로 멈춘다.
    [SHADOW_MAX_PEEL] 은 폭주 방지용 상한이고, 보통 그 전에 멈춘다.
    """
    shadow = bytearray(width * height)
    frontier = [
        (x, y)
        for y in range(height)
        for x in range(width)
        if outside[y * width + x]
    ]
    for _ in range(SHADOW_MAX_PEEL):
        nxt = []
        for x, y in frontier:
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < width and 0 <= ny < height):
                    continue
                i = ny * width + nx
                if outside[i] or shadow[i]:
                    continue
                r, g, b, _ = pixels[nx, ny]
                if luma((r, g, b)) >= base_luma:      # 방 쪽 밝은 테두리 -> 멈춤
                    continue
                shadow[i] = 1
                nxt.append((nx, ny))
        if not nxt:
            break
        frontier = nxt
    return shadow


def luma(rgb) -> float:
    r, g, b = rgb[:3]
    return 0.299 * r + 0.587 * g + 0.114 * b


def near(a, b) -> bool:
    return abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2]) <= TOLERANCE * 3


def flood_from_border(pixels, width, height, base):
    """
    가장자리에서 시작해 바탕색으로 이어진 영역을 찾는다.

    **바탕색과 거의 같은 픽셀만** 따라간다. 어두운 픽셀까지 따라가게 했더니
    바닥 나무와 크림색 벽이 밝기가 비슷해서 방 안으로 새어 들어갔다.
    그림자는 [peel_shadow] 가 따로 처리한다.
    """
    seen = bytearray(width * height)
    queue = deque()

    def push(x, y):
        if 0 <= x < width and 0 <= y < height and not seen[y * width + x]:
            r, g, b, _ = pixels[x, y]
            if near((r, g, b), base):
                seen[y * width + x] = 1
                queue.append((x, y))

    for x in range(width):
        push(x, 0)
        push(x, height - 1)
    for y in range(height):
        push(0, y)
        push(width - 1, y)

    while queue:
        x, y = queue.popleft()
        push(x - 1, y)
        push(x + 1, y)
        push(x, y - 1)
        push(x, y + 1)

    return seen


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        raise SystemExit(2)
    source = sys.argv[1]
    cutout(source, sys.argv[2] if len(sys.argv) > 2 else source)
