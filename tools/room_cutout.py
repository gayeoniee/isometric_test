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

  3. **테두리 두르기** — 오려낸 자리가 계단(픽셀 아트라 45도가 계단이 된다)이라
     그대로 두면 "오린 티"가 난다. 실루엣을 [OUTLINE_WIDTH] 만큼 부풀려 그 띠를
     [OUTLINE_COLOR] 로 칠하면 계단이 띠 안에 먹혀서 의도한 테두리로 읽힌다.

  4. **외톨이 털어내기** — 경계가 디더링(체크무늬)이라 배경만 지우면 반쪽이 점으로
     남아 희끗희끗해진다. 이웃 넷 중 셋 이상이 빈 픽셀은 그 찌꺼기다.

## 그림자는 남기지 않는다

처음엔 그림자를 반투명 검정으로 살려뒀는데, **분홍 배경 위에서 뿌연 회색 얼룩으로
보였다.** 배경색이 달라지면 같은 그림자가 다르게 읽힌다. 그래서 배경과 함께 지운다.
필요해지면 방 밑에 코드로 그리는 편이 배경색을 따라가므로 낫다.

## 테마 방은 이 방법이 안 통한다

파스텔 테마의 방은 **벽 색이 배경색과 거의 같다**(벚꽃: 벽 #F4D7DF / 배경 #DCB5C0).
색으로 가르는 flood 는 벽을 배경으로 보고 그대로 타고 들어간다 — 실제로 오른쪽
벽이 통째로 뜯겼다.

테마는 같은 그림을 리컬러한 것이라 **실루엣이 전부 같다**(저쪽 README 도 "retain
the transparent silhouettes of their source assets" 라고 적어뒀다). 그래서 배경과
벽이 충분히 다른 **원본 방 한 장에서 실루엣을 구해** 나머지에 씌운다.
[mask_of] 와 [apply_mask] 가 그 일을 한다.

사용:
    uv run tools/room_cutout.py <입력.png> [출력.png]

출력을 안 주면 입력 파일을 덮어쓴다. 저쪽에서 색만 다른 방 PNG 가 오면
그때마다 이걸 한 번 돌리면 된다.
"""

import sys
from collections import deque

from pathlib import Path

from PIL import Image, ImageFilter

# 바탕색으로 볼 색 거리. 픽셀 아트라 배경이 거의 단색이지만 노이즈가 조금 있다.
TOLERANCE = 26

# 그림자로 인정할 최대 어두움. 알파를 이 값으로 정규화한다.
SHADOW_DEPTH = 70

# 그림자를 벗겨낼 최대 두께(px). 그림자는 얇게 깔리므로 이 정도면 넉넉하다.
SHADOW_MAX_PEEL = 10

# "바탕색이 어두워진 것"으로 볼 색 오차. 넉넉하면 벽면까지 먹는다.
SHADE_TOLERANCE = 16

# 테두리 두께(px)와 색.
#
# 색은 원본 그림자 톤에서 가져왔다. 잉크 검정은 픽셀 아트 원본에 없던 선이라 세고,
# 크림색은 티는 제일 잘 가리지만 방이 종이 스티커처럼 붕 뜬다. 이 톤이 원본에
# 원래 있던 색이라 제일 자연스럽다.
OUTLINE_WIDTH = 3
OUTLINE_COLOR = (150, 122, 96, 255)

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
    image = add_outline(image)

    image.save(dst_path)
    print(f"바탕색 {base}  ->  투명 {cleared:,}px, 외톨이 {speckles:,}px")


def add_outline(image: Image.Image) -> Image.Image:
    """
    실루엣 바깥에 테두리를 두른다.

    오려낸 자리는 계단이다 — 픽셀 아트에서 45도 선은 계단으로 그려지고, 배경을
    지우면 그 계단이 그대로 윤곽이 되어 "오린 티"가 난다.

    알파를 [OUTLINE_WIDTH] 만큼 부풀려 띠를 만들고 그 위에 원본을 얹는다.
    계단이 띠 안쪽에 묻혀서 의도한 테두리로 읽힌다.
    """
    if OUTLINE_WIDTH <= 0:
        return image
    grown = image.getchannel("A").filter(ImageFilter.MaxFilter(OUTLINE_WIDTH * 2 + 1))
    ring = Image.new("RGBA", image.size, OUTLINE_COLOR)
    ring.putalpha(grown)
    return Image.alpha_composite(ring, image)


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



# ---------------------------------------------------------------------------
# 스프라이트 시트 — **몸통에서 떨어진 조각 털기**
#
# 저쪽 워크 시트에 몸과 이어지지 않은 발·다리 조각이 섞여 들어온 칸이 있다.
# 보더콜리와 웰시코기는 하필 **정지 프레임(1번)** 에 붙어 있어서, 방 안에서
# 강아지가 멈출 때마다 앞쪽에 흰 점이 떠 있었다.
#
# 저쪽 아트 규약이 "칸마다 오른쪽을 보는 강아지 하나"이므로, 칸에서 **가장 큰
# 덩어리 하나만 남기면** 된다. 지운 조각은 크기와 함께 찍는다 — 진짜 몸의 일부를
# 지우는 사고가 나면 로그에 보이게 하려는 것이다.
# ---------------------------------------------------------------------------

# 이 비율을 넘는 조각을 지우면 몸의 일부일 수 있다. 지우되 눈에 띄게 알린다.
LOOSE_WARN_RATIO = 0.05

# 이 알파부터 그림으로 친다. 가장자리 반투명이 조각을 몸에 이어주기도 해서 낮게 잡는다.
ALPHA_FLOOR = 8


def _blobs(alpha, width, height):
    """알파가 있는 픽셀의 4-연결 덩어리들. 큰 것부터."""
    seen = bytearray(width * height)
    found = []
    for sy in range(height):
        row = sy * width
        for sx in range(width):
            i = row + sx
            if seen[i] or alpha[i] < ALPHA_FLOOR:
                continue
            stack = [i]
            seen[i] = 1
            blob = []
            while stack:
                j = stack.pop()
                blob.append(j)
                x, y = j % width, j // width
                if x > 0 and not seen[j - 1] and alpha[j - 1] >= ALPHA_FLOOR:
                    seen[j - 1] = 1; stack.append(j - 1)
                if x + 1 < width and not seen[j + 1] and alpha[j + 1] >= ALPHA_FLOOR:
                    seen[j + 1] = 1; stack.append(j + 1)
                if y > 0 and not seen[j - width] and alpha[j - width] >= ALPHA_FLOOR:
                    seen[j - width] = 1; stack.append(j - width)
                if y + 1 < height and not seen[j + width] and alpha[j + width] >= ALPHA_FLOOR:
                    seen[j + width] = 1; stack.append(j + width)
            found.append(blob)
    found.sort(key=len, reverse=True)
    return found


def strip_loose_bits(src_path: str, dst_path: str, columns: int = 4) -> int:
    """시트의 각 칸에서 가장 큰 덩어리만 남긴다. 지운 조각 수를 돌려준다."""
    sheet = Image.open(src_path).convert("RGBA")
    frame_w = sheet.width // columns
    height = sheet.height
    removed = 0

    for col in range(columns):
        box = (col * frame_w, 0, (col + 1) * frame_w, height)
        frame = sheet.crop(box)
        alpha = bytearray(frame.split()[3].tobytes())
        parts = _blobs(alpha, frame_w, height)
        if len(parts) < 2:
            continue

        main = len(parts[0])
        pixels = frame.load()
        for blob in parts[1:]:
            ratio = len(blob) / main
            flag = "  ★ 몸의 일부일 수 있다" if ratio > LOOSE_WARN_RATIO else ""
            print(
                "    %s 칸%d: 조각 %d px (몸의 %.1f%%) 지움%s"
                % (Path(src_path).stem, col, len(blob), ratio * 100, flag)
            )
            for j in blob:
                pixels[j % frame_w, j // frame_w] = (0, 0, 0, 0)
            removed += 1

        sheet.paste(frame, box)

    sheet.save(dst_path)
    return removed

def mask_of(src_path: str) -> bytearray:
    """
    [src_path] 의 **실루엣**만 구한다. 1 이면 방, 0 이면 배경.

    색이 뚜렷이 다른 원본에서 뽑아 테마 방들에 씌우는 용도다.
    """
    image = Image.open(src_path).convert("RGBA")
    width, height = image.size
    pixels = image.load()
    corners = [pixels[0, 0], pixels[width - 1, 0], pixels[0, height - 1], pixels[width - 1, height - 1]]
    base = tuple(sum(c[i] for c in corners) // len(corners) for i in range(3))

    outside = flood_from_border(pixels, width, height, base)
    shadow = peel_shadow(pixels, width, height, outside, base)

    keep = bytearray(width * height)
    for i in range(width * height):
        keep[i] = 0 if (outside[i] or shadow[i]) else 1

    # 실루엣에서도 디더링 찌꺼기를 턴다. 마스크 단계에서 털어야 씌우는 쪽이 깨끗하다.
    for _ in range(3):
        doomed = []
        for y in range(height):
            for x in range(width):
                i = y * width + x
                if not keep[i]:
                    continue
                empty = 0
                for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                    nx, ny = x + dx, y + dy
                    if not (0 <= nx < width and 0 <= ny < height) or not keep[ny * width + nx]:
                        empty += 1
                if empty >= 3:
                    doomed.append(i)
        if not doomed:
            break
        for i in doomed:
            keep[i] = 0
    return keep


def apply_mask(src_path: str, dst_path: str, keep: bytearray) -> None:
    """[keep] 실루엣을 [src_path] 에 씌운다. 테마 방들이 이 경로를 탄다."""
    image = Image.open(src_path).convert("RGBA")
    width, height = image.size
    if len(keep) != width * height:
        raise ValueError(f"마스크 크기가 다르다: {len(keep)} vs {width * height}")
    pixels = image.load()
    for y in range(height):
        for x in range(width):
            if not keep[y * width + x]:
                pixels[x, y] = (0, 0, 0, 0)
    add_outline(image).save(dst_path)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        raise SystemExit(2)
    source = sys.argv[1]
    cutout(source, sys.argv[2] if len(sys.argv) > 2 else source)
