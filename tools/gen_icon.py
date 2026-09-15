"""Generates the mod icon: the gold unlock marker, mid-flip, sitting on a card.

The mark is not a generic trophy on purpose - it is the thing the mod actually puts on your screen.
An unlock draws a small gold badge beside the hotbar that coin-flips into a red "!", so the icon is
that badge with the "!" already on it: both signature colours (0xFFD700 gold, 0xFF2B22 red) in one
shape, readable down to the 32px Modrinth search thumbnail.

Drawn on a 64x64 grid and scaled up with nearest-neighbour, the same way the card themes are built -
pixels stay pixels at 512. Bayer-dithered gradients and hard 1px bevels, matching gen_themes.py.
"""
import os
import random

from PIL import Image

N = 64  # native pixel grid; everything below is in these units
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "icon")

GOLD = (255, 215, 0)          # UnlockDotHudElement.GOLD_ARGB
RED = (255, 43, 34)           # UnlockDotHudElement.RED_ARGB
FRAME_OUTER = (12, 10, 14)
FRAME_BAND = (110, 78, 44)
FRAME_ACCENT = (214, 168, 86)

BAYER = [
    [0, 8, 2, 10],
    [12, 4, 14, 6],
    [3, 11, 1, 9],
    [15, 7, 13, 5],
]


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


def gradient(px, top, bottom, steps=5, strength=0.35):
    """Banded, dithered - a smooth gradient reads as a web button, not as a Minecraft texture.

    Strength is deliberately low: at 1.0 the Bayer matrix stays visible as a regular grid, which at
    icon sizes reads as a halftone print screen rather than as a dark panel.
    """
    for y in range(N):
        for x in range(N):
            t = y / (N - 1)
            d = (BAYER[y % 4][x % 4] + 0.5) / 16.0 - 0.5
            q = max(0.0, min(1.0, round((t + d * strength / steps) * (steps - 1)) / (steps - 1)))
            px[x, y] = lerp(top, bottom, q)


def speckle(px, colors, density, rnd):
    """Irregular grain over the panel - breaks up the bands without printing a pattern."""
    for _ in range(int(N * N * density)):
        px[rnd.randrange(0, N), rnd.randrange(0, N)] = rnd.choice(colors)


def rect(px, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(N, y1)):
        for x in range(max(0, x0), min(N, x1)):
            px[x, y] = color


def frame(px):
    """The card's chiselled border: dark outer line, bright lip, shaded far edge, lit inner edge."""
    for d in (0, 1):
        rect(px, d, d, N - d, d + 1, FRAME_OUTER)
        rect(px, d, N - d - 1, N - d, N - d, FRAME_OUTER)
        rect(px, d, d, d + 1, N - d, FRAME_OUTER)
        rect(px, N - d - 1, d, N - d, N - d, FRAME_OUTER)
    rect(px, 2, 2, N - 2, 3, FRAME_ACCENT)
    rect(px, 2, 2, 3, N - 2, FRAME_ACCENT)
    rect(px, 2, N - 3, N - 2, N - 2, shade(FRAME_ACCENT, 0.55))
    rect(px, N - 3, 2, N - 2, N - 2, shade(FRAME_ACCENT, 0.55))
    rect(px, 3, 3, N - 3, 5, FRAME_BAND)
    rect(px, 3, 3, 5, N - 3, FRAME_BAND)
    rect(px, 3, N - 5, N - 3, N - 3, shade(FRAME_BAND, 0.62))
    rect(px, N - 5, 3, N - 3, N - 3, shade(FRAME_BAND, 0.62))
    rect(px, 5, 5, N - 5, 6, shade(FRAME_BAND, 0.40))
    rect(px, 5, 5, 6, N - 5, shade(FRAME_BAND, 0.40))


CX, CY = 31.5, 31.5
R = 19.0


def glow(px, rnd):
    """Halo under the badge - the marker is lit, and a flat disc on flat dark looks pasted on.

    Scattered at random rather than through the Bayer matrix: an ordered dither on a ring draws a
    visible dotted circle around the badge, which is the one thing a halo must not do.
    """
    for y in range(4, N - 4):
        for x in range(4, N - 4):
            d = ((x - CX) ** 2 + (y - CY) ** 2) ** 0.5
            if d <= R or d > R + 7:
                continue
            t = (d - R) / 7.0
            if rnd.random() < (1.0 - t) ** 2 * 0.75:
                px[x, y] = lerp(px[x, y], shade(GOLD, 0.45), 0.42 * (1.0 - t) + 0.06)


def badge(px):
    """Gold disc, lit from the top-left like every vanilla GUI element, hard dark rim on the far side."""
    for y in range(N):
        for x in range(N):
            d = ((x - CX) ** 2 + (y - CY) ** 2) ** 0.5
            if d > R:
                continue
            if d > R - 1.6:
                px[x, y] = (54, 38, 6)  # outline, so the badge holds its shape on any background
                continue
            if d > R - 3.4:
                # Far edge darkens, near edge keeps a bright lip - that is what makes it a sphere
                lit = ((CX - x) + (CY - y)) / (2.0 * R)
                px[x, y] = shade(GOLD, 0.68 + 0.42 * max(0.0, lit))
                continue
            lit = ((CX - x) + (CY - y)) / (2.0 * R)
            c = shade(GOLD, 0.86 + 0.30 * lit)
            if (BAYER[y % 4][x % 4] + 0.5) / 16.0 < 0.12:
                c = shade(c, 1.06)
            px[x, y] = c
    # Specular: two stacked runs rather than a circle, the way hand-drawn MC highlights are done.
    rect(px, 22, 19, 29, 21, (255, 246, 196))
    rect(px, 20, 21, 25, 23, (255, 246, 196))


MARK_TOP, MARK_BOTTOM = 18, 35
DOT_TOP, DOT_BOTTOM = 39, 45


def mark(px):
    """The red "!" - tapered bar plus dot, outlined so it cannot smear into the gold underneath."""
    body = []
    for y in range(MARK_TOP, MARK_BOTTOM):
        t = (y - MARK_TOP) / (MARK_BOTTOM - MARK_TOP - 1)
        half = 3 - int(round(t))  # 6px wide at the top, 4px at the base
        for x in range(int(round(CX)) - half, int(round(CX)) - half + half * 2):
            body.append((x, y))
    for y in range(DOT_TOP, DOT_BOTTOM):
        for x in range(int(round(CX)) - 3, int(round(CX)) + 3):
            body.append((x, y))

    filled = set(body)
    for x, y in body:  # 1px dark outline all round, drawn first so the fill paints over nothing
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if (x + dx, y + dy) not in filled:
                    px[x + dx, y + dy] = (104, 16, 12)
    for x, y in body:
        px[x, y] = RED
    for x, y in body:  # inner shading: lit left column, darker right, same light as the badge
        if (x - 1, y) not in filled:
            px[x, y] = shade(RED, 1.18)
        elif (x + 1, y) not in filled:
            px[x, y] = shade(RED, 0.72)


def sparkles(px):
    """Three four-point glints in the dead corners - says "something was just unlocked"."""
    for cx, cy, arm in ((12, 12, 3), (52, 16, 2), (14, 51, 2)):
        for i in range(-arm, arm + 1):
            f = 1.0 - abs(i) / (arm + 1.0)
            c = lerp((120, 96, 40), GOLD, f)
            if ((cx + i - CX) ** 2 + (cy - CY) ** 2) ** 0.5 > R:
                px[cx + i, cy] = c
            if ((cx - CX) ** 2 + (cy + i - CY) ** 2) ** 0.5 > R:
                px[cx, cy + i] = c


def build():
    rnd = random.Random(7)
    img = Image.new("RGB", (N, N))
    px = img.load()
    gradient(px, (40, 34, 56), (13, 11, 19))
    speckle(px, [(52, 44, 70), (22, 19, 30), (46, 39, 62)], 0.09, rnd)
    glow(px, rnd)
    sparkles(px)
    badge(px)
    mark(px)
    frame(px)
    return img


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    base = build()
    for size in (64, 256, 512):
        base.resize((size, size), Image.NEAREST).save(os.path.join(OUT, f"icon-{size}.png"))
        print("wrote", f"icon-{size}.png")
