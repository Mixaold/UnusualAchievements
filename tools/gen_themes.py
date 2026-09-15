"""Generates the achievement-card background themes as real PNG textures.

Everything is placed pixel by pixel on purpose: banded dithered gradients (Bayer 4x4, quantized to a
handful of steps) instead of smooth ones, hard 1px highlights and shadows, and per-theme motifs built
from blocks - that is what makes it read as Minecraft pixel art rather than a CSS gradient.
"""
import os
import random

from PIL import Image

W, H = 260, 210
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "themes")

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


def gradient(img, top, bottom, steps=6, strength=1.0, horizontal=False):
    """Dithered, banded vertical (or horizontal) gradient - the backbone of every theme."""
    px = img.load()
    for y in range(H):
        for x in range(W):
            t = (x / (W - 1)) if horizontal else (y / (H - 1))
            d = (BAYER[y % 4][x % 4] + 0.5) / 16.0 - 0.5
            q = round((t + d * strength / steps) * (steps - 1)) / (steps - 1)
            q = max(0.0, min(1.0, q))
            px[x, y] = lerp(top, bottom, q)


def speckle(img, colors, density, rnd, size=1):
    px = img.load()
    for _ in range(int(W * H * density)):
        x = rnd.randrange(0, W - size)
        y = rnd.randrange(0, H - size)
        c = rnd.choice(colors)
        for dy in range(size):
            for dx in range(size):
                px[x + dx, y + dy] = c


def rect(img, x0, y0, x1, y1, color):
    px = img.load()
    for y in range(max(0, y0), min(H, y1)):
        for x in range(max(0, x0), min(W, x1)):
            px[x, y] = color


def frame(img, outer, inner, accent):
    """MC-style chiselled border: dark outer line, accent, then a lit inner edge."""
    rect(img, 0, 0, W, 2, outer)
    rect(img, 0, H - 2, W, H, outer)
    rect(img, 0, 0, 2, H, outer)
    rect(img, W - 2, 0, W, H, outer)
    rect(img, 2, 2, W - 2, 3, accent)
    rect(img, 2, H - 3, W - 2, H - 2, shade(accent, 0.6))
    rect(img, 2, 2, 3, H - 2, accent)
    rect(img, W - 3, 2, W - 2, H - 2, shade(accent, 0.6))
    rect(img, 3, 3, W - 3, 4, inner)
    rect(img, 3, 3, 4, H - 3, inner)


DIRS8 = [(1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0), (-1, -1), (0, -1), (1, -1)]


def veins(img, color, glow, count, rnd, life=90, branch=0.06, seeds=None):
    """
    Organic crawling cracks - lava seams, sculk veins, roots. Steps one pixel at a time and only
    ever turns to a neighbouring direction, so the line stays continuous and wanders; stepping
    several pixels at once (as this used to) leaves gaps that read as circuit traces, not cracks.
    """
    px = img.load()
    stack = []
    for _ in range(count):
        start = (rnd.randrange(4, W - 4), rnd.randrange(4, H - 4)) if seeds is None else rnd.choice(seeds)
        stack.append((start[0], start[1], rnd.randrange(8), life))
    while stack:
        x, y, d, remaining = stack.pop()
        for _ in range(remaining):
            if not (2 < x < W - 3 and 2 < y < H - 3):
                break
            px[x, y] = color
            if rnd.random() < 0.3:
                px[x, y - 1] = glow
            if rnd.random() < 0.28:  # nudge to an adjacent heading, never a hard right angle
                d = (d + rnd.choice([-1, 1])) % 8
            if rnd.random() < branch and remaining > 20:
                stack.append((x, y, (d + rnd.choice([-2, 2])) % 8, remaining // 2))
            x += DIRS8[d][0]
            y += DIRS8[d][1]
            remaining -= 1


# ---------------------------------------------------------------- themes

def nether(rnd):
    img = Image.new("RGB", (W, H))
    gradient(img, (40, 18, 18), (14, 8, 10), steps=5)
    speckle(img, [(54, 26, 26), (26, 14, 16), (62, 32, 28)], 0.10, rnd, size=2)
    # Cracks seeded along the bottom, so they crawl upward out of the lava like cooling seams.
    veins(img, (188, 62, 16), (255, 150, 40), 10, rnd, life=120, branch=0.10,
          seeds=[(rnd.randrange(10, W - 10), H - 34) for _ in range(10)])
    speckle(img, [(255, 190, 70), (255, 120, 30)], 0.003, rnd)
    for i in range(26):  # lava lake, brightest at the very bottom
        y = H - 4 - i
        c = lerp((255, 196, 70), (128, 38, 12), i / 26)
        for x in range(4, W - 4):
            if rnd.random() < 0.12 * (i / 26) + 0.02:
                continue  # ragged shoreline instead of a ruler-straight edge
            img.load()[x, y] = c
    frame(img, (10, 5, 6), (92, 40, 26), (208, 84, 24))
    return img


def end(rnd):
    img = Image.new("RGB", (W, H))
    gradient(img, (12, 9, 20), (30, 22, 46), steps=5)
    speckle(img, [(230, 226, 250), (170, 165, 200), (255, 255, 255)], 0.0035, rnd)
    for _ in range(4):  # chunky floating endstone islands with ragged undersides
        cx, cy = rnd.randrange(40, W - 40), rnd.randrange(34, H - 60)
        w = rnd.randrange(30, 62)
        rect(img, cx - w // 2, cy, cx + w // 2, cy + 4, (232, 232, 178))
        rect(img, cx - w // 2, cy + 4, cx + w // 2, cy + 7, (198, 198, 148))
        depth = rnd.randrange(12, 24)
        left, right = cx - w // 2, cx + w // 2
        for i in range(depth):
            left += rnd.randrange(0, 3)
            right -= rnd.randrange(0, 3)
            if left >= right:
                break
            rect(img, left, cy + 7 + i, right, cy + 8 + i, lerp((170, 170, 126), (78, 76, 60), i / depth))
    for _ in range(60):  # violet void haze
        x, y = rnd.randrange(4, W - 6), rnd.randrange(4, H - 6)
        rect(img, x, y, x + rnd.randrange(2, 5), y + 1, (58, 38, 84))
    frame(img, (8, 6, 14), (86, 60, 130), (168, 118, 224))
    return img


def ocean(rnd):
    img = Image.new("RGB", (W, H))
    gradient(img, (26, 92, 116), (6, 24, 46), steps=7)
    for y in range(0, H, 8):  # prismarine brick courses
        rect(img, 3, y, W - 3, y + 1, (44, 122, 140))
        off = 0 if (y // 8) % 2 == 0 else 12
        for x in range(3 + off, W - 3, 24):
            rect(img, x, y, x + 1, y + 8, (40, 112, 130))
    speckle(img, [(96, 176, 168), (18, 58, 82)], 0.02, rnd, size=2)
    for _ in range(22):  # bubble columns rising off the sea floor
        x = rnd.randrange(6, W - 6)
        y = rnd.randrange(H - 60, H - 6)
        while y > 6:
            r = rnd.choice([1, 2, 2])
            rect(img, x + rnd.randrange(-1, 2), y, x + r, y + r, (206, 244, 248))
            y -= rnd.randrange(7, 15)
    frame(img, (4, 16, 30), (54, 140, 150), (110, 200, 196))
    return img


def sculk(rnd):
    img = Image.new("RGB", (W, H))
    gradient(img, (12, 20, 26), (6, 12, 16), steps=4)
    speckle(img, [(18, 30, 36), (10, 16, 20)], 0.14, rnd, size=2)
    sensors = [(rnd.randrange(14, W - 16), rnd.randrange(14, H - 16)) for _ in range(11)]
    veins(img, (16, 58, 66), (54, 190, 180), 11, rnd, life=130, branch=0.16, seeds=sensors)
    for x, y in sensors:  # sculk sensors, each the hub its veins crawl out from
        rect(img, x - 5, y - 5, x + 6, y + 6, (20, 44, 50))
        rect(img, x - 4, y - 4, x + 5, y + 5, (28, 92, 98))
        rect(img, x - 2, y - 2, x + 3, y + 3, (96, 240, 220))
        rect(img, x - 1, y - 1, x + 2, y + 2, (216, 255, 250))
    frame(img, (4, 8, 10), (22, 76, 82), (58, 200, 190))
    return img


def enchant(rnd):
    img = Image.new("RGB", (W, H))
    gradient(img, (32, 16, 58), (8, 5, 20), steps=6)
    speckle(img, [(220, 210, 255), (150, 130, 220)], 0.0035, rnd)
    glyphs = [  # blocky standard-galactic-ish marks, big enough to actually read as writing
        ["X...X", "XX.XX", "X.X.X", "X...X", "X...X"],
        [".XXX.", "X....", ".XXX.", "....X", ".XXX."],
        ["X...X", ".X.X.", "..X..", ".X.X.", "X...X"],
        ["XXXXX", "..X..", "..X..", "X.X..", ".XX.."],
        [".XXX.", "X...X", "XXXXX", "X...X", "X...X"],
        ["XX...", "X.X..", "X..X.", "X...X", "XXXXX"],
    ]
    for _ in range(30):
        gx, gy = rnd.randrange(4, W - 22), rnd.randrange(4, H - 22)
        s = rnd.choice([2, 2, 3])
        c = rnd.choice([(150, 118, 230), (120, 92, 196), (186, 160, 255)])
        for r, row in enumerate(rnd.choice(glyphs)):
            for cc, ch in enumerate(row):
                if ch == "X":
                    rect(img, gx + cc * s, gy + r * s, gx + cc * s + s, gy + r * s + s, c)
    # The open book on its lectern, front and centre - the thing all that writing comes off.
    bx, by = W // 2 - 30, H // 2 - 10
    rect(img, bx, by, bx + 60, by + 26, (146, 44, 40))
    rect(img, bx + 2, by + 2, bx + 29, by + 24, (238, 232, 208))
    rect(img, bx + 31, by + 2, bx + 58, by + 24, (238, 232, 208))
    rect(img, bx + 29, by, bx + 31, by + 26, (92, 26, 24))
    for i in range(5):
        rect(img, bx + 5, by + 6 + i * 4, bx + 26, by + 7 + i * 4, (176, 168, 148))
        rect(img, bx + 34, by + 6 + i * 4, bx + 55, by + 7 + i * 4, (176, 168, 148))
    frame(img, (8, 4, 16), (96, 70, 160), (178, 150, 255))
    return img


def redstone(rnd):
    img = Image.new("RGB", (W, H))
    gradient(img, (44, 44, 46), (24, 24, 26), steps=4)
    speckle(img, [(52, 52, 54), (32, 32, 34)], 0.12, rnd, size=2)
    px = img.load()
    for _ in range(16):  # right-angle dust runs, like a redstone board
        x, y = rnd.randrange(10, W - 10), rnd.randrange(10, H - 10)
        for seg in range(rnd.randrange(2, 5)):
            ln = rnd.randrange(14, 50)
            horiz = seg % 2 == 0
            for i in range(ln):
                if horiz:
                    x = min(W - 4, x + 1)
                else:
                    y = min(H - 4, y + 1)
                px[x, y] = (190, 30, 24)
                px[x, min(H - 1, y + 1)] = (110, 16, 14)
    for _ in range(12):  # torches, with a lit halo so they actually read as sources
        x, y = rnd.randrange(10, W - 10), rnd.randrange(10, H - 16)
        for r in range(6, 1, -1):
            for yy in range(y - r, y + r):
                for xx in range(x - r, x + r):
                    if 0 <= xx < W and 0 <= yy < H and (xx - x) ** 2 + (yy - y) ** 2 < r * r:
                        if rnd.random() < 0.16:
                            px[xx, yy] = lerp((80, 30, 26), (150, 50, 36), 1 - r / 6)
        rect(img, x - 1, y + 2, x + 2, y + 12, (104, 74, 48))
        rect(img, x - 1, y + 2, x, y + 12, (138, 100, 64))
        rect(img, x - 2, y - 3, x + 3, y + 2, (206, 44, 34))
        rect(img, x - 1, y - 2, x + 2, y + 1, (255, 108, 76))
    frame(img, (12, 12, 14), (100, 26, 22), (190, 30, 24))
    return img


def copper(rnd):
    img = Image.new("RGB", (W, H))
    gradient(img, (196, 108, 62), (58, 148, 126), steps=7, horizontal=True)
    speckle(img, [(224, 140, 84), (44, 128, 108), (168, 88, 52)], 0.10, rnd, size=2)
    for _ in range(30):  # oxidation blotches eating across the plate
        cx, cy = rnd.randrange(W), rnd.randrange(H)
        r = rnd.randrange(4, 16)
        for y in range(cy - r, cy + r):
            for x in range(cx - r, cx + r):
                if 0 <= x < W and 0 <= y < H and (x - cx) ** 2 + (y - cy) ** 2 < r * r:
                    if rnd.random() < 0.55:
                        img.load()[x, y] = (74, 160, 134)
    for y in range(0, H, 10):  # cut-copper seams
        rect(img, 3, y, W - 3, y + 1, (40, 30, 26))
    frame(img, (30, 20, 16), (150, 92, 54), (224, 140, 84))
    return img


def library(rnd):
    img = Image.new("RGB", (W, H))
    gradient(img, (74, 50, 30), (44, 30, 18), steps=4)
    spines = [(150, 40, 36), (52, 84, 140), (60, 120, 60), (170, 130, 40), (110, 60, 130), (190, 180, 160)]
    for shelf in range(5):  # rows of books on plank shelves
        y0 = 8 + shelf * 40
        rect(img, 4, y0 + 30, W - 4, y0 + 36, (96, 66, 38))
        rect(img, 4, y0 + 30, W - 4, y0 + 31, (128, 92, 54))
        x = 8
        while x < W - 12:
            bw = rnd.randrange(4, 9)
            bh = rnd.randrange(18, 29)
            c = rnd.choice(spines)
            rect(img, x, y0 + 30 - bh, x + bw, y0 + 30, c)
            rect(img, x, y0 + 30 - bh, x + 1, y0 + 30, shade(c, 1.35))
            rect(img, x, y0 + 30 - bh + 3, x + bw, y0 + 30 - bh + 4, shade(c, 0.6))
            x += bw + 1
    frame(img, (24, 16, 10), (128, 92, 54), (196, 152, 88))
    return img


def taiga(rnd):
    img = Image.new("RGB", (W, H))
    gradient(img, (120, 162, 204), (226, 236, 246), steps=6)
    ground = H - 40
    # Two ranks of spruce, the far one smaller and paler, all rooted on the snow line - drawn
    # widest at the base and tapering up, which is the way a spruce actually goes.
    for far in (True, False):
        for _ in range(9 if far else 7):
            bx = rnd.randrange(6, W - 6)
            by = ground - (10 if far else 0) + rnd.randrange(-2, 3)
            h = rnd.randrange(26, 40) if far else rnd.randrange(42, 66)
            wide = rnd.randrange(9, 13) if far else rnd.randrange(13, 19)
            dark = (58, 96, 84) if far else (24, 62, 46)
            light = (92, 130, 116) if far else (46, 98, 70)
            for i in range(h):
                t = i / h
                tier = 0.72 + 0.28 * (((i // 7) % 2))  # stepped tiers, not a smooth cone
                half = max(1, int(wide * (1 - t) * tier))
                rect(img, bx - half, by - i, bx + half + 1, by - i + 1, lerp(dark, light, t))
            rect(img, bx - 1, by, bx + 2, by + 6, (62, 44, 30) if not far else (86, 74, 62))
    rect(img, 0, ground + 4, W, H, (246, 250, 253))  # snow
    rect(img, 0, ground + 4, W, ground + 6, (208, 222, 238))
    for _ in range(40):  # drifts
        x = rnd.randrange(0, W - 12)
        rect(img, x, ground + rnd.randrange(1, 5), x + rnd.randrange(6, 14), ground + 6, (238, 244, 251))
    speckle(img, [(255, 255, 255), (236, 243, 251)], 0.016, rnd)
    frame(img, (54, 74, 96), (168, 196, 220), (240, 248, 255))
    return img


def mushroom(rnd):
    img = Image.new("RGB", (W, H))
    gradient(img, (92, 78, 96), (52, 44, 56), steps=5)
    speckle(img, [(112, 96, 116), (70, 58, 74), (128, 110, 128)], 0.16, rnd, size=2)
    for _ in range(9):  # huge red caps with mycelium stems
        cx = rnd.randrange(20, W - 20)
        cy = rnd.randrange(40, H - 24)
        r = rnd.randrange(12, 26)
        rect(img, cx - 4, cy, cx + 4, cy + 20, (224, 216, 196))
        for y in range(r):
            half = int((1 - (y / r) ** 2) ** 0.5 * r)
            rect(img, cx - half, cy - y, cx + half, cy - y + 1, (186, 42, 38))
        for _ in range(rnd.randrange(4, 9)):
            sx = rnd.randrange(cx - r + 2, cx + r - 2)
            sy = rnd.randrange(cy - r + 2, cy)
            rect(img, sx, sy, sx + 3, sy + 3, (236, 228, 214))
    frame(img, (32, 26, 34), (128, 110, 128), (186, 42, 38))
    return img


def cherry(rnd):
    img = Image.new("RGB", (W, H))
    gradient(img, (146, 196, 232), (214, 234, 246), steps=6)  # sky, so the pink reads as blossom
    rect(img, 0, H - 34, W, H, (96, 150, 76))  # grass
    rect(img, 0, H - 34, W, H - 31, (124, 180, 96))

    def blossom(cx, cy, r):
        for _ in range(int(r * r * 1.6)):
            a = rnd.random() * 6.2832
            d = rnd.random() ** 0.5 * r
            x = int(cx + d * (a % 2 - 0.5) * 2.4 + rnd.randrange(-2, 3))
            y = int(cy + d * ((a * 1.7) % 2 - 1) + rnd.randrange(-2, 3))
            s = rnd.choice([2, 2, 3])
            rect(img, x, y, x + s, y + s,
                 rnd.choice([(250, 186, 214), (255, 214, 232), (238, 152, 190), (255, 240, 246)]))

    trunk_x = W // 2 + rnd.randrange(-24, 25)
    base = H - 30
    for i in range(74):  # trunk, leaning slightly as cherry trunks do
        off = i // 12
        rect(img, trunk_x - 5 + off, base - i, trunk_x + 6 + off, base - i + 1, (88, 58, 62))
        rect(img, trunk_x - 5 + off, base - i, trunk_x - 2 + off, base - i + 1, (116, 78, 82))
    for ang, ln in ((-1, 46), (1, 52), (-1, 30), (1, 28)):  # limbs
        x, y = trunk_x + 6, base - rnd.randrange(40, 68)
        for i in range(ln):
            x += ang
            y -= 1 if i % 2 else 0
            rect(img, x, y, x + 3, y + 3, (88, 58, 62))
        blossom(x, y, rnd.randrange(16, 24))
    blossom(trunk_x + 6, base - 84, 34)
    for _ in range(150):  # petals drifting down
        x, y = rnd.randrange(4, W - 6), rnd.randrange(4, H - 6)
        rect(img, x, y, x + 2, y + 2, rnd.choice([(255, 226, 238), (250, 198, 220)]))
    frame(img, (110, 62, 78), (232, 168, 198), (255, 238, 246))
    return img


def amethyst(rnd):
    img = Image.new("RGB", (W, H))
    gradient(img, (52, 52, 58), (28, 28, 34), steps=4)
    speckle(img, [(62, 62, 70), (38, 38, 44)], 0.14, rnd, size=2)
    # Crystals grow in geode pockets, not scattered evenly - a budding wall lined with clusters.
    for _ in range(7):
        px_, py_ = rnd.randrange(24, W - 24), rnd.randrange(24, H - 24)
        pr = rnd.randrange(16, 30)
        for y in range(py_ - pr, py_ + pr):
            for x in range(px_ - pr, px_ + pr):
                if 0 <= x < W and 0 <= y < H and (x - px_) ** 2 + (y - py_) ** 2 < pr * pr:
                    img.load()[x, y] = (46, 38, 58) if rnd.random() < 0.7 else (60, 48, 74)
        for _ in range(rnd.randrange(7, 13)):
            a = rnd.random() * 6.2832
            d = rnd.random() ** 0.5 * (pr - 4)
            cx = int(px_ + d * ((a % 2) - 0.5) * 2)
            cy = int(py_ + d * (((a * 1.7) % 2) - 1))
            h = rnd.randrange(7, 17)
            w = rnd.randrange(2, 6)
            for i in range(h):
                half = max(1, int(w * (1 - i / h)))
                rect(img, cx - half, cy - i, cx + half, cy - i + 1,
                     lerp((110, 66, 172), (214, 176, 255), i / h))
            rect(img, cx - 1, cy - h, cx, cy - h + 2, (240, 226, 255))
    frame(img, (16, 16, 20), (104, 68, 156), (190, 150, 245))
    return img


THEMES = [
    ("nether", "Пепел и лава", nether),
    ("end", "Пустота Края", end),
    ("ocean", "Морская пучина", ocean),
    ("sculk", "Древний город", sculk),
    ("enchant", "Стол зачарований", enchant),
    ("redstone", "Красный камень", redstone),
    ("copper", "Патина", copper),
    ("library", "Библиотека", library),
    ("taiga", "Снежная тайга", taiga),
    ("mushroom", "Грибной остров", mushroom),
    ("cherry", "Цветущая сакура", cherry),
    ("amethyst", "Аметистовая жеода", amethyst),
]

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for i, (key, _title, fn) in enumerate(THEMES):
        img = fn(random.Random(1000 + i))
        img.save(os.path.join(OUT, f"{key}.png"))
        print("wrote", key)
