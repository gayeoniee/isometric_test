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
  2. **그림자만 벗겨내기** — 1번이 남긴 경계에서 안쪽으로 한 겹씩 먹어 들어간다.
     조건은 "어두운가"가 **아니라** "바탕색이 어두워진 것인가"다.

     처음엔 그냥 "바탕보다 어두우면 그림자"로 봤다가 **방의 왼쪽 벽 바깥면을 갉아
     먹었다.** 그 면은 그늘져서 바탕보다 어둡지만 회색빛이라 그림자가 아니다.
     결과는 벽 모서리가 희끗희끗 반투명해지는 것이었다.

     그래서 색조까지 본다. 그림자는 바탕색에 밝기만 곱한 것이므로
     `픽셀 ~= 바탕색 x k` 가 성립한다. 회색빛 벽면은 이 식에서 크게 벗어난다.

  3. **외톨이 털어내기** — 경계가 디더링(체크무늬)이라 배경만 지우면 반쪽이 점으로
     남아 희끗희끗해진다. 이웃 넷 중 셋 이상이 빈 픽셀은 그 찌꺼기다.

## 그림자는 남기지 않는다

처음엔 그림자를 반투명 검정으로 살려뒀는데, **분홍 배경 위에서 뿌연 회색 얼룩으로
보였다.** 배경색이 달라지면 같은 그림자가 다르게 읽힌다. 그래서 배경과 함께 지운다.
필요해지면 방 밑에 코드로 그리는 편이 배경색을 따라가므로 낫다.

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

# 그림자를 벗겨낼 최대 두께(px). 그림자는 얇게 깔리므로 이 정도면 넉넉하다.
SHADOW_MAX_PEEL = 10

# "바탕색이 어두워진 것"으로 볼 색 오차. 넉넉하면 벽면까지 먹는다.
SHADE_TOLERANCE = 16

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
    shadow = peel_shadow(pixels, width, height, outside, base)

    cleared = 0
    for y in range(height):
        for x in range(width):
            i = y * width + x
            if shadow[i] or outside[i]:
                pixels[x, y] = (0, 0, 0, 0)
                cleared += 1

    speckles = despeckle(pixels, width, height)

    image.save(dst_path)
    print(f"바탕색 {base}  ->  투명 {cleared:,}px, 외톨이 {speckles:,}px")


def despeckle(pixels, width, height, rounds: int = 3) -> int:
    """
    가장자리에 남은 **디더링 찌꺼기**를 털어낸다.

    픽셀 아트는 경계를 체크무늬로 섞어 부드럽게 보이게 한다. 배경만 골라 지우면
    그 체크무늬의 반쪽이 공중에 뜬 점으로 남아 희끗희끗해진다.

    이웃 넷 중 **셋 이상**이 비어 있으면 찌꺼기로 본다. 1px 짜리 얇은 선(벽 모서리
    몰딩)은 위아래 이웃이 살아 있어 둘만 비므로 살아남는다.
    """
    removed = 0
    for _ in range(rounds):
        doomed = []
        for y in range(height):
            for x in range(width):
                if pixels[x, y][3] == 0:
                    continue
                empty = 0
                for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                    nx, ny = x + dx, y + dy
                    if not (0 <= nx < width and 0 <= ny < height) or pixels[nx, ny][3] == 0:
                        empty += 1
                if empty >= 3:
                    doomed.append((x, y))
        if not doomed:
            break
        for x, y in doomed:
            pixels[x, y] = (0, 0, 0, 0)
        removed += len(doomed)
    return removed


def peel_shadow(pixels, width, height, outside, base):
    """
    바깥 경계에서 안쪽으로 **바탕색이 어두워진 픽셀**만 한 겹씩 벗겨낸다.

    "어두우면 그림자"로 보면 안 된다 — 방의 왼쪽 벽 바깥면도 그늘져서 어둡다.
    거기까지 먹으면 벽 모서리가 희끗희끗 반투명해진다. [is_shade_of] 참조.
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
                if not is_shade_of(( r, g, b), base):
                    continue
                shadow[i] = 1
                nxt.append((nx, ny))
        if not nxt:
            break
        frontier = nxt
    return shadow


def is_shade_of(rgb, base) -> bool:
    """
    [rgb] 가 [base] 에 밝기만 곱한 색인가 — 즉 **그림자인가**.

    그림자는 바탕색을 어둡게 만든 것이라 `픽셀 ~= 바탕 x k` 가 성립한다.
    회색빛 벽면처럼 색조가 다른 어두움은 이 식에서 크게 벗어나므로 걸러진다.
    """
    base_l = luma(base)
    l = luma(rgb)
    if l >= base_l or l <= 0:
        return False
    k = l / base_l
    return sum(abs(rgb[i] - base[i] * k) for i in range(3)) <= SHADE_TOLERANCE * 3


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
