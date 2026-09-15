"""Composites a mock of the real card for every theme: panel, full-bleed background, scrim,
themed avatar frame and sample rows - laid out with the same numbers the mod uses, so the preview
shows what the screen will actually look like rather than the textures in isolation."""
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "mockups")

# Mirrors AchievementCardScreen's constants.
CANVAS_W, CANVAS_H = 260, 210
MARGIN = 8
PANEL_W, PANEL_H = CANVAS_W + MARGIN * 2, CANVAS_H + MARGIN * 2
AVATAR = 32
TITLE_AREA = 18
SCRIM = 0x70
PLATE = 0xB8

FONT = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 11)
FONT_B = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 11)

SAMPLE = {
    "nether": ("Рукопожатие пиромансера", "Забей крипера голыми руками, стоя в лаве.", "«Обе стороны остались недовольны.»"),
    "end": ("На честном слове", "Добей Дракона Края с полусердцем и без тотема.", "«Статистика была против тебя.»"),
    "ocean": ("Похоронен заживо (почти)", "Выживи после удушения с двумя сердцами.", "«Клаустрофобия — теперь личное.»"),
    "sculk": ("Тише, чем тишина", "Убей Стража, не получив от него ни удара.", "«Он тебя даже не услышал.»"),
    "enchant": ("Бумажный демон", "Зачаруй 25 предметов на столе зачарования.", "«Библиотекарь сменил номер.»"),
    "redstone": ("Сам себе сапёр", "Подожги TNT и погибни от его взрыва.", "«Инструкция лежала рядом.»"),
    "copper": ("Молния на заказ", "Убей моба молнией, стоя на блоке золота.", "«Личный бог грома, законно.»"),
    "library": ("Последнее слово", "Напиши «прощайте» и умри за 30 секунд.", "«Ты явно это распланировал.»"),
    "taiga": ("Снежный убийца", "Столкни моба снежком в пропасть.", "«Технически ты его не бил.»"),
    "mushroom": ("Грозовая корова", "Будь рядом, когда молния ударит в мушрума.", "«Наука бессильна. Молния — нет.»"),
    "cherry": ("Пчелиный гнев", "Получи урон от трёх пчёл без шлема.", "«Пчёлы запомнили твоё лицо.»"),
    "amethyst": ("Дно мира", "Сломай блок на глубине Y ≤ -60 в одиночестве.", "«Здесь тебя никто не услышит.»"),
}

SECOND = ("Живая статуя", "Не двигайся 60 секунд подряд ночью.", "«Рекорд не засчитан: никто не мерил.»")

TITLES = {
    "nether": "Пепел и лава", "end": "Пустота Края", "ocean": "Морская пучина",
    "sculk": "Древний город", "enchant": "Стол зачарований", "redstone": "Красный камень",
    "copper": "Патина", "library": "Библиотека", "taiga": "Снежная тайга",
    "mushroom": "Грибной остров", "cherry": "Цветущая сакура", "amethyst": "Аметистовая жеода",
}


def steve_head(size=32):
    """A stand-in player head - the real screen draws the viewer's own skin here."""
    head = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(head)
    d.rectangle([0, 0, size - 1, size - 1], fill=(174, 139, 110))
    d.rectangle([0, 0, size - 1, 8], fill=(74, 51, 38))
    d.rectangle([0, 0, 4, size - 1], fill=(74, 51, 38))
    d.rectangle([size - 5, 0, size - 1, size - 1], fill=(74, 51, 38))
    d.rectangle([7, 13, 12, 17], fill=(240, 240, 240))
    d.rectangle([19, 13, 24, 17], fill=(240, 240, 240))
    d.rectangle([9, 14, 11, 17], fill=(60, 90, 160))
    d.rectangle([20, 14, 22, 17], fill=(60, 90, 160))
    d.rectangle([12, 22, 19, 24], fill=(122, 92, 72))
    return head


def build(key):
    card = Image.new("RGBA", (PANEL_W, PANEL_H), (16, 16, 16, 255))
    bg = Image.open(os.path.join(HERE, "themes", f"{key}.png")).convert("RGBA")
    card.alpha_composite(bg.resize((PANEL_W, PANEL_H), Image.NEAREST))
    card.alpha_composite(Image.new("RGBA", (PANEL_W, PANEL_H), (0, 0, 0, SCRIM)))

    d = ImageDraw.Draw(card)
    card_left, card_top = MARGIN, MARGIN

    def text(x, y, s, fill, font=FONT):
        d.text((x + 1, y + 1), s, font=font, fill=(0, 0, 0, 190))  # the mod's drop shadow
        d.text((x, y), s, font=font, fill=fill)

    text(card_left, card_top - 1, "Необычные достижения", (224, 224, 224))

    avatar_top = card_top + TITLE_AREA
    card.alpha_composite(steve_head(), (card_left + MARGIN, avatar_top))
    frame = Image.open(os.path.join(HERE, "frames", f"{key}.png")).convert("RGBA")
    card.alpha_composite(frame, (card_left + MARGIN - 8, avatar_top - 8))

    text(card_left + MARGIN + AVATAR + MARGIN + 4, avatar_top + AVATAR // 2 - 7, "Mixaold", (255, 255, 255), FONT_B)

    # Each achievement gets one plate behind its whole block, not a strip per line.
    y = avatar_top + AVATAR + MARGIN + 2
    row_left, row_right = card_left + MARGIN - 2, card_left + CANVAS_W - MARGIN - 6
    for name, desc, flavor in (SAMPLE[key], SECOND):
        d.rectangle([row_left, y - 3, row_right, y + 42], fill=(0, 0, 0, PLATE))
        d.rectangle([row_left, y - 3, row_right, y - 3], fill=(255, 255, 255, 0x66))
        text(row_left + 5, y, name, (255, 215, 0))
        text(row_left + 5, y + 13, desc[:56], (176, 176, 176))
        text(row_left + 5, y + 26, flavor, (143, 168, 184))
        y += 54

    d.rectangle([card_left + CANVAS_W - 62, card_top + CANVAS_H - 22, card_left + CANVAS_W - 10,
                 card_top + CANVAS_H - 6], fill=(60, 60, 60, 220), outline=(140, 140, 140))
    text(card_left + CANVAS_W - 52, card_top + CANVAS_H - 20, "Закрыть", (230, 230, 230))
    d.rectangle([card_left, card_top + CANVAS_H - 22, card_left + 52, card_top + CANVAS_H - 6],
                fill=(60, 60, 60, 220), outline=(140, 140, 140))
    text(card_left + 14, card_top + CANVAS_H - 20, "Темы", (230, 230, 230))
    return card


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for key in TITLES:
        img = build(key)
        img.resize((PANEL_W * 2, PANEL_H * 2), Image.NEAREST).save(os.path.join(OUT, f"{key}.png"))
        print("wrote", key)
