#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# /// script
# requires-python = ">=3.10"
# dependencies = ["pillow"]
# ///
"""
저쪽 레포에서 받은 에셋 묶음을 안드로이드 리소스로 들여온다.

frankie516c/dog-training-rag 의 `ui-experiments/main-screen/assets` 아래에서
테마 팩과 견종 워크 시트를 받아오면, 이 스크립트가 세 가지를 한 번에 한다.

  1. **방 그림 컷아웃** — 테마의 `room.png` 는 알파가 없는 불투명 그림이다.
     바깥을 뚫고 테두리를 두른다. 소품과 강아지는 이미 투명해서 건드리지 않는다.

     실루엣은 **참조 방 한 장에서만 구해** 전 테마에 씌운다. 파스텔 테마는 벽 색이
     배경색과 거의 같아서 색으로 가르면 벽이 통째로 뜯긴다. 테마는 같은 그림을
     리컬러한 것이라 실루엣이 전부 같으므로, 배경과 벽이 뚜렷이 다른 원본
     (`reference-room.png`, 황토색 배경)에서 한 번 구하면 된다.
  2. **WebP 변환** — PNG 그대로 넣으면 55MB 다. 손실 WebP(q92)면 6MB 로 줄고,
     2배 확대해 비교해도 눈에 띄는 차이가 없다. 부드럽게 셰이딩된 그림이라
     손실 압축이 잘 먹는다. (2색 도트였다면 무손실을 써야 한다)
  3. **이름 짓기** — 안드로이드 리소스 이름은 소문자·숫자·밑줄만 된다.
     `themes/sky-blue/rug-cream.png` -> `theme_sky_blue_rug_cream.webp`

사용:
    uv run tools/import_room_assets.py <받은폴더> [출력폴더]

받은폴더 구조는 저쪽 레포와 같아야 한다.

    <받은폴더>/themes/<테마>/{room,ball,basket,bowls,cabinet,doghouse,plant,rug,rug-cream}.png
    <받은폴더>/dogs/<견종>.png

출력폴더 기본값은 `app/src/main/res/drawable-nodpi` 다.
"""

import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from room_cutout import apply_mask, mask_of  # noqa: E402

# 손실 압축 품질. 92 는 원본과 육안 차이가 없으면서 10분의 1 로 줄어드는 지점이다.
WEBP_QUALITY = 92

# 인코딩 노력. 6 이 가장 느리고 가장 작다. 한 번 굽고 마는 파일이라 느려도 된다.
WEBP_METHOD = 6

DEFAULT_OUT = Path("app/src/main/res/drawable-nodpi")


def resource_name(*parts: str) -> str:
    """안드로이드 리소스 이름 규칙: 소문자·숫자·밑줄."""
    joined = "_".join(parts).lower()
    return "".join(c if c.isalnum() else "_" for c in joined)


def to_webp(src: Path, dst: Path) -> tuple[int, int]:
    image = Image.open(src).convert("RGBA")
    dst.parent.mkdir(parents=True, exist_ok=True)
    image.save(dst, "WEBP", quality=WEBP_QUALITY, method=WEBP_METHOD)
    return src.stat().st_size, dst.stat().st_size


def main(drop: Path, out: Path) -> None:
    before = after = 0
    made = 0

    reference = drop / "reference-room.png"
    if not reference.exists():
        raise SystemExit(
            f"참조 방이 없다: {reference}\n"
            "배경과 벽이 뚜렷이 다른 원본 방 PNG 가 필요하다."
        )
    silhouette = mask_of(str(reference))

    themes = sorted(p for p in (drop / "themes").iterdir() if p.is_dir())
    for theme in themes:
        for png in sorted(theme.glob("*.png")):
            staged = png
            if png.stem == "room":
                # 방만 불투명하다. 참조 실루엣을 씌우고 테두리를 둘러 임시 파일로.
                staged = png.with_name("room.cut.png")
                apply_mask(str(png), str(staged), silhouette)
            name = resource_name("theme", theme.name, png.stem)
            a, b = to_webp(staged, out / f"{name}.webp")
            before += png.stat().st_size
            after += b
            made += 1
            if staged != png:
                staged.unlink()

    for png in sorted((drop / "dogs").glob("*.png")):
        name = resource_name("dog", png.stem)
        a, b = to_webp(png, out / f"{name}.webp")
        before += a
        after += b
        made += 1

    print(f"{made}개  {before / 1e6:.1f}MB -> {after / 1e6:.1f}MB  ({after / before * 100:.0f}%)")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        raise SystemExit(2)
    main(Path(sys.argv[1]), Path(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_OUT)
