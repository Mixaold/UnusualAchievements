"""Generates the mod icon: a locked achievement slot - a gold "?" on the Ocean Depths card background.

Nobody is told what the achievements are, and the "?" is what the mod keeps showing you instead of a
list: the question-mark button on the card, the slot you have not earned yet. The background and the
frame are the Ocean Depths theme (same colours as gen_themes.py / gen_frames.py), so the icon reads as
a piece of the card itself. The "?" is the vanilla font glyph at 6x, in the same yellow the card uses
for achievement titles, with the vanilla drop shadow.

Drawn on a 64x64 grid and scaled up with nearest-neighbour, the same way the card themes are built -
pixels stay pixels at 512. Bayer-dithered gradients and hard 1px bevels, matching gen_themes.py.
"""
import os
import random

from PIL import Image

N = 64  # native pixel grid; everything below is in these units
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "icon")

# Ocean Depths, from gen_themes.ocean()
SEA_TOP = (26, 92, 116)
SEA_BOTTOM = (6, 24, 46)
BRICK_LINE = (44, 122, 140)
BRICK_JOINT = (40, 112, 130)
FRAME_OUTER = (4, 16, 30)
FRAME_INNER = (54, 140, 150)
FRAME_ACCENT = (110, 200, 196)

YELLOW = (255, 255, 85)       # title colour on the card (vanilla YELLOW)
YELLOW_SHADOW = (63, 63, 21)  # vanilla text shadow = colour / 4
SLOT_DARK = (8, 22, 34)

BAYER = [
    [0, 8, 2, 10],
    [12, 4, 14, 6],
    [3, 11, 1, 9],
    [15, 7, 13, 5],
]

# Vanilla "?" glyph, 5x7
QMARK = [
    ".###.",
    "#...#",
    "....#",
    "...#.",
    "..#..",
    ".....",
    "..#..",
]


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


def rect(px, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(N, y1)):
        for x in range(max(0, x0), min(N, x1)):
            px[x, y] = color


def gradient(px, top, bottom, steps=6):
    for y in range(N):
        for x in range(N):
            t = y / (N - 1)
            d = (BAYER[y % 4][x % 4] + 0.5) / 16.0 - 0.5
            q = max(0.0, min(1.0, round((t + d / steps) * (steps - 1)) / (steps - 1)))
            px[x, y] = lerp(top, bottom, q)


def bricks(px):
    """Prismarine brick courses, like the Ocean Depths theme."""
    for y in range(4, N, 8):
        rect(px, 0, y, N, y + 1, BRICK_LINE)
        off = 0 if (y // 8) % 2 == 0 else 12
        for x in range(off % 24, N, 24):
            rect(px, x, y, x + 1, y + 8, BRICK_JOINT)


def speckle(px, colors, density, rnd):
    for _ in range(int(N * N * density)):
        px[rnd.randrange(0, N), rnd.randrange(0, N)] = rnd.choice(colors)


def bubbles(px, rnd):
    for x in (9, 53, 47):
        y = rnd.randrange(46, 56)
        while y > 8:
            r = rnd.choice([1, 1, 2])
            rect(px, x, y, x + r, y + r, (206, 244, 248))
            y -= rnd.randrange(6, 12)


def slot(px):
    """The locked slot: a sunken dark square with the vanilla inventory bevel (dark top-left, lit bottom-right)."""
    x0, y0, x1, y1 = 10, 8, 54, 56
    rect(px, x0, y0, x1, y1, SLOT_DARK)
    rect(px, x0, y0, x1, y0 + 1, shade(SLOT_DARK, 0.5))
    rect(px, x0, y0, x0 + 1, y1, shade(SLOT_DARK, 0.5))
    rect(px, x0, y1 - 1, x1, y1, FRAME_INNER)
    rect(px, x1 - 1, y0, x1, y1, FRAME_INNER)


def glow(px, rnd):
    """Faint teal light in the slot, strongest behind the glyph."""
    cx, cy = 32.0, 32.0
    for y in range(9, 55):
        for x in range(11, 53):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d > 22:
                continue
            if (BAYER[y % 4][x % 4] + 0.5) / 16.0 < (1 - d / 22) * 0.9:
                px[x, y] = lerp(SLOT_DARK, (20, 58, 72), 1 - d / 22)


def qmark(px):
    s = 6
    sh = 2  # drop shadow, kept short so the diagonal steps of the glyph stay readable
    w, h = len(QMARK[0]) * s, len(QMARK) * s
    ox, oy = (N - w - sh) // 2 + 1, (N - h - sh) // 2 + 1
    cells = {(cx, cy) for cy, row in enumerate(QMARK) for cx, v in enumerate(row) if v == "#"}
    filled = set()
    for cx, cy in cells:
        for y in range(s):
            for x in range(s):
                filled.add((ox + cx * s + x, oy + cy * s + y))
    for x, y in filled:
        px[x + sh, y + sh] = YELLOW_SHADOW
    for x, y in filled:
        px[x, y] = YELLOW


def frame(px):
    """Ocean Depths frame: dark outer line, bright lip, shaded far edge, lit inner edge."""
    for d in (0, 1):
        rect(px, d, d, N - d, d + 1, FRAME_OUTER)
        rect(px, d, N - d - 1, N - d, N - d, FRAME_OUTER)
        rect(px, d, d, d + 1, N - d, FRAME_OUTER)
        rect(px, N - d - 1, d, N - d, N - d, FRAME_OUTER)
    rect(px, 2, 2, N - 2, 3, FRAME_ACCENT)
    rect(px, 2, 2, 3, N - 2, FRAME_ACCENT)
    rect(px, 2, N - 3, N - 2, N - 2, shade(FRAME_ACCENT, 0.55))
    rect(px, N - 3, 2, N - 2, N - 2, shade(FRAME_ACCENT, 0.55))
    rect(px, 3, 3, N - 3, 5, FRAME_INNER)
    rect(px, 3, 3, 5, N - 3, FRAME_INNER)
    rect(px, 3, N - 5, N - 3, N - 3, shade(FRAME_INNER, 0.62))
    rect(px, N - 5, 3, N - 3, N - 3, shade(FRAME_INNER, 0.62))
    rect(px, 5, 5, N - 5, 6, shade(FRAME_INNER, 0.40))
    rect(px, 5, 5, 6, N - 5, shade(FRAME_INNER, 0.40))


def build():
    rnd = random.Random(7)
    img = Image.new("RGB", (N, N))
    px = img.load()
    gradient(px, SEA_TOP, SEA_BOTTOM)
    bricks(px)
    speckle(px, [(96, 176, 168), (18, 58, 82)], 0.05, rnd)
    bubbles(px, rnd)
    slot(px)
    glow(px, rnd)
    qmark(px)
    frame(px)
    return img


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    base = build()
    for size in (64, 256, 512):
        base.resize((size, size), Image.NEAREST).save(os.path.join(OUT, f"icon-{size}.png"))
        print("wrote", f"icon-{size}.png")
