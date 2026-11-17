#!/usr/bin/env python3
"""Generate WoouSU launcher/splash icons from one source image.

Usage:
    python3 scripts/woousu_brand_icon.py [source-image]

If no path is given, the script looks for branding/icon_source.(png|jpg|jpeg|webp),
then branding/icon_placeholder.jpg. Drop the original artwork there and rerun to
replace the placeholder everywhere (mipmaps, adaptive foreground, round icon, splash).
"""
from __future__ import annotations

import glob
import os
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "manager/app/src/main/res"
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
LEGACY_PX = 48
FOREGROUND_DP = 108
SPLASH_DP = 288
BG = (247, 245, 238, 255)


def find_source() -> Path:
    if len(sys.argv) > 1:
        return Path(sys.argv[1]).expanduser()
    for pattern in (str(ROOT / "branding/icon_source.*"),):
        hits = sorted(glob.glob(pattern))
        if hits:
            return Path(hits[0])
    fallback = ROOT / "branding/icon_placeholder.jpg"
    if fallback.exists():
        return fallback
    raise SystemExit("No source image found; pass one as the first argument.")


def square(img: Image.Image, size: int) -> Image.Image:
    img = img.convert("RGB")
    w, h = img.size
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    return img.crop((left, top, left + side, top + side)).resize((size, size), Image.LANCZOS)


def write(path: Path, img: Image.Image) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "PNG", optimize=True)
    print(f"wrote {path.relative_to(ROOT)}")


def main() -> None:
    source = find_source()
    print(f"source: {source}")
    art = square(Image.open(source), 2048)

    for name, scale in DENSITIES.items():
        legacy_px = int(LEGACY_PX * scale)
        base = art.resize((legacy_px, legacy_px), Image.LANCZOS)

        write(RES / f"mipmap-{name}/ic_launcher.png", base)

        round_img = Image.new("RGBA", (legacy_px, legacy_px), (0, 0, 0, 0))
        mask = Image.new("L", (legacy_px * 4, legacy_px * 4), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, legacy_px * 4 - 1, legacy_px * 4 - 1), fill=255)
        mask = mask.resize((legacy_px, legacy_px), Image.LANCZOS)
        round_img.paste(base, (0, 0), mask)
        write(RES / f"mipmap-{name}/ic_launcher_round.png", round_img)

        # Adaptive foreground: 108dp canvas, artwork fitted into the 72dp safe zone.
        fg_px = int(FOREGROUND_DP * scale)
        safe = int(fg_px * 2 / 3)
        canvas = Image.new("RGBA", (fg_px, fg_px), (0, 0, 0, 0))
        fitted = art.resize((safe, safe), Image.LANCZOS)
        offset = (fg_px - safe) // 2
        canvas.paste(fitted, (offset, offset))
        write(RES / f"mipmap-{name}/ic_launcher_foreground.png", canvas)

    # Source-resolution art kept for the splash screen on any density.
    splash_px = SPLASH_DP * 2
    splash = Image.new("RGBA", (splash_px, splash_px), (0, 0, 0, 0))
    circle = Image.new("L", (splash_px * 2, splash_px * 2), 0)
    ImageDraw.Draw(circle).ellipse((0, 0, splash_px * 2 - 1, splash_px * 2 - 1), fill=255)
    circle = circle.resize((splash_px, splash_px), Image.LANCZOS)
    splash.paste(art.resize((splash_px, splash_px), Image.LANCZOS), (0, 0), circle)
    write(RES / "drawable-nodpi/ic_launcher_splash.png", splash)


if __name__ == "__main__":
    main()
