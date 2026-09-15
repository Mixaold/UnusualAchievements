"""Generates the avatar frames that go with each card theme.

Each frame reuses the exact three colours its theme's own border was drawn from, so a frame can never
drift out of step with the background it sits on. 48x48 RGBA with a 32x32 transparent hole in the
middle - the player head is drawn underneath and shows through.
"""
import os
import random

from PIL import Image

SIZE = 48
HOLE = 32
BORDER = (SIZE - HOLE) // 2  # 8px
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "frames")

# theme id -> (outer outline, main band, bright accent) - copied from each theme's frame() call.
PALETTES = {
    "nether": ((10, 5, 6), (92, 40, 26), (208, 84, 24)),
    "end": ((8, 6, 14), (86, 60, 130), (168, 118, 224)),
    "ocean": ((4, 16, 30), (54, 140, 150), (110, 200, 196)),
    "sculk": ((4, 8, 10), (22, 76, 82), (58, 200, 190)),
    "enchant": ((8, 4, 16), (96, 70, 160), (178, 150, 255)),
    "redstone": ((12, 12, 14), (100, 26, 22), (190, 30, 24)),
    "copper": ((30, 20, 16), (150, 92, 54), (224, 140, 84)),
    "library": ((24, 16, 10), (128, 92, 54), (196, 152, 88)),
    "taiga": ((54, 74, 96), (168, 196, 220), (240, 248, 255)),
    "mushroom": ((32, 26, 34), (128, 110, 128), (186, 42, 38)),
    "cherry": ((110, 62, 78), (232, 168, 198), (255, 238, 246)),
    "amethyst": ((16, 16, 20), (104, 68, 156), (190, 150, 245)),
}


def shade(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def in_ring(x, y):
    return not (BORDER <= x < SIZE - BORDER and BORDER <= y < SIZE - BORDER)


def build(key, rnd):
    outer, band, accent = PALETTES[key]
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()

    # The theme's border colour is meant to sit against a full-screen background, so on its own it
    # reads almost black. Pulling it most of the way to the accent gives the frame its own material
    # while keeping it unmistakably the same family as the theme.
    base = mix(band, accent, 0.55)

    for y in range(SIZE):
        for x in range(SIZE):
            if not in_ring(x, y):
                continue
            # Bevel: lit from the top-left, like every vanilla GUI frame.
            lit = (x + y) / (2.0 * SIZE)
            c = shade(base, 1.24 - 0.40 * lit)
            if rnd.random() < 0.18:
                c = shade(c, rnd.choice([0.90, 1.10]))
            px[x, y] = c + (255,)

    for i in range(SIZE):  # hard outer outline
        for d in (0, SIZE - 1):
            px[i, d] = outer + (255,)
            px[d, i] = outer + (255,)

    for i in range(1, SIZE - 1):  # lit top/left edge, shaded bottom/right - reads as raised
        px[i, 1] = shade(base, 1.45) + (255,)
        px[1, i] = shade(base, 1.45) + (255,)
        px[i, SIZE - 2] = shade(base, 0.66) + (255,)
        px[SIZE - 2, i] = shade(base, 0.66) + (255,)

    for i in range(BORDER - 1, SIZE - BORDER + 1):  # bright lip around the opening
        for d in (BORDER - 1, SIZE - BORDER):
            if in_ring(i, d):
                px[i, d] = accent + (255,)
            if in_ring(d, i):
                px[d, i] = accent + (255,)

    for i in range(BORDER - 2, SIZE - BORDER + 2):  # dark seat just outside the lip, for depth
        for d in (BORDER - 2, SIZE - BORDER + 1):
            if in_ring(i, d):
                px[i, d] = shade(base, 0.5) + (255,)
            if in_ring(d, i):
                px[d, i] = shade(base, 0.5) + (255,)

    return img


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for i, key in enumerate(PALETTES):
        build(key, random.Random(500 + i)).save(os.path.join(OUT, f"{key}.png"))
        print("wrote", key)
