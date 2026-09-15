# Unusual Achievements

Fabric mod for Minecraft 26.2.

50 hidden achievements with no list, no progress bars and no hints. You do something strange —
usually something you would never do on purpose — and the game quietly counts it. Everything you
unlock lives on a personal card: press **J** to open yours, aim at another player and press it again
to read theirs.

Twelve pixel-art card backgrounds are locked behind specific achievements, so a card's look says
something about what its owner actually did. Every unlock also carries how rare it was in your own
game, counted from the vanilla statistics Minecraft already keeps.

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

50 скрытых достижений: без списка, без полосок прогресса, без подсказок. Ты делаешь что-то странное —
обычно то, что специально делать бы не стал, — и игра молча это засчитывает. Всё открытое лежит на
личной карточке: **J** открывает свою, наведись на другого игрока и нажми ту же клавишу — откроется его.

Двенадцать пиксельных фонов карточки закрыты за конкретными достижениями, так что по оформлению видно,
что человек сделал. У каждого достижения показано, насколько редко оно случалось именно в твоей игре —
посчитано по обычной ванильной статистике, которую майн и так ведёт.

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
