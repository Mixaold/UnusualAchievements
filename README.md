# Unusual Achievements

Fabric mod for Minecraft 26.2.

50 hidden achievements. No list, no hints, no progress bars. Nobody tells you what they are: you do
something odd, the game counts it, and only then do you find out what it was.

Everything you unlock goes on a card. **J** opens yours, aiming at another player and pressing it
again opens theirs. 12 card backgrounds are locked behind single achievements, and every unlock says
how rare it is in your own game, counted from your vanilla stats.

## Install

Needs **Fabric API**. Nothing else.

Put the jar on **both the client and the server**, same version on both sides — the mod has its own
network packets, so a client without it cannot open cards and a server without it has nothing to send.

Settings: `config/unusualachievements.json` (`overlayEnabled` turns the hotbar marker off). The key is
rebindable in Options → Controls, under **Unusual Achievements**.

`/uadev` lists every achievement with its condition spelled out and force-unlocks entries for testing.
It requires operator permission, and it spoils the entire mod — open it only if you want to know.

## Build

```
./gradlew build
```

The jar lands in `build/libs/` — the plain one, not `-sources`.

The card backgrounds, the avatar frames and the mod icon are generated PNGs. Regenerate them through
`tools/gen_themes.py`, `tools/gen_frames.py` and `tools/gen_icon.py` (Python + Pillow) rather than
editing the files by hand.

## License

MIT.

---

# Unusual Achievements (Русский)

Мод под Fabric для Minecraft 26.2.

50 скрытых достижений. Ни списка, ни подсказок, ни полосок прогресса. Никто не скажет, какие они: ты
делаешь что-то странное, игра это засчитывает, и только тогда ты узнаёшь, что это было.

Всё открытое ложится на карточку. Своя — на **J**, наведись на другого игрока и нажми ещё раз —
увидишь его. 12 фонов карточки закрыты за отдельными достижениями, и у каждого достижения написано,
насколько оно редкое в твоей игре, по обычной ванильной статистике.

## Установка

Нужен **Fabric API**. Больше ничего.

Ставить jar **и на клиент, и на сервер**, одной и той же версии: у мода свои сетевые пакеты, без него
клиент не откроет карточки, а серверу нечего отдавать.

Настройки: `config/unusualachievements.json` (`overlayEnabled` выключает значок у хотбара). Клавишу
можно переназначить в «Настройки → Управление», раздел **Необычные достижения**.

`/uadev` показывает все достижения с расписанными условиями и выдаёт любое для проверки. Нужны права
оператора, и она раскрывает весь мод целиком — открывай, только если готов узнать.

## Сборка

```
./gradlew build
```

Готовый jar появится в `build/libs/` — обычный, не `-sources`.

Фоны карточки, рамки аватара и иконка мода — сгенерированные PNG. Правки вносить через
`tools/gen_themes.py`, `tools/gen_frames.py` и `tools/gen_icon.py` (нужны Python и Pillow), а не
редактированием файлов руками.

## Лицензия

MIT.
