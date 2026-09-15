# Unusual Achievements — для выкладки на Modrinth

---

# ЧАСТЬ 1. Поля формы (не часть описания)

| Поле | Значение |
|---|---|
| **Name** | Unusual Achievements |
| **Slug** | `unusual-achievements` |
| **Summary** | Fifty hidden achievements with no list and no hints — you only find out what one was after you have already done it. Everything you unlock lives on a card other players can read. |
| **Type / Loader** | Mod / Fabric |
| **Game versions** | 26.2 |
| **Client side** | **Required** |
| **Server side** | **Required** |
| **License** | MIT |
| **Categories** | Adventure, Game Mechanics, Social |

**Зависимости:** Fabric API — Required. Больше ничего: ни Cloth Config, ни Mod Menu — настройка идёт файлом, клавиша живёт в ванильном «Управлении».

**Ссылки в форме:** Source code — `https://github.com/Mixaold/UnusualAchievements`, Issue tracker — тот же адрес с `/issues`. Wiki и Discord — пусто. Donation — DonationAlerts через «Other».

**Client и Server оба Required** потому, что мод регистрирует свои сетевые пакеты и хранит всё на сервере. Клиент без мода не откроет карточку, сервер без мода её не отдаст. При разных версиях ничего не упадёт — мод просто не будет работать.

**Скриншоты в галерею:** карточка с надетой темой, окно выбора тем с замочками на закрытых, золотой значок у хотбара в момент анимации, чужая карточка (наведение на другого игрока).

**Иконка проекта:** `icons/icon-512.png`.

**Файл версии:** канал **Release**, номер `1.0`, загружать `build/libs/unusualachievements-1.0.jar` — **не** `-sources`.

---
---

# ЧАСТЬ 2. Описание (English)

## Unusual Achievements

Vanilla advancements tell you what to do next. This one never does.

Fifty hidden achievements: no list, no progress bars, no "3 of 10". You do something strange —
usually something you would never do on purpose — and the game quietly counts it. Beat a creeper to
death with your bare hands while standing in lava in a thunderstorm: that is one of them. Type a
goodbye in chat and die within thirty seconds: that is another.

Nobody tells you how to get them. That is the whole mod.

### The card

Everything you have unlocked lives on a card. **J** opens yours.

Aim at another player and press the same key to open **theirs** — their unlocks, their chosen look,
written in your language rather than in theirs.

### Themes you earn, not pick

Twelve pixel-art backgrounds, each locked behind one specific achievement and chosen so that the
backdrop says something about what its owner did. Nether ash and lava goes to whoever fought a
creeper barehanded in a lava pool. The Ancient City one goes to whoever killed a Warden without
taking a single hit from it.

Every theme also brings its own frame for your player head, so an equipped card reads as one piece.

Locked themes are still shown in the wardrobe — dimmed, padlocked and named. A look you cannot wear
yet is a hint that something out there is still unfound.

### A rarity that is actually about you

Every unlock carries a line like *"once in 3418 mob kills"* — how often it happened in **your** game,
read from the vanilla statistics Minecraft already keeps for you. Where vanilla has no counter for
what the achievement asked (nothing tracks kills made mid-fall), it falls back to hours played
instead of inventing a percentage. On a populated server it also shows how many players hold the
thing at all.

### Quiet by design

No chat spam, no toast across half the screen. An unlock is a small gold marker beside your hotbar
that flips into a red "!", plus a soft sound. Once per game, at the very first unlock, it also tells
you which key opens the card.

### Multiplayer

Unlocks are detected and stored **on the server**, so your card is the same wherever you log in from,
and a client cannot hand itself something it did not earn. Your chosen theme is stored there too —
otherwise nobody but you would ever see it.

> ### ⚠️ Install it on the client AND the server
> The mod has its own network packets: a client without it cannot open cards, a server without it
> has nothing to send. Same version on both sides.

### Settings

`config/unusualachievements.json` — `overlayEnabled` turns the hotbar marker off entirely. The key is
rebindable in Options → Controls, under **Unusual Achievements**.

### For server operators

`/uadev` lists the whole pool with every condition spelled out and can force-unlock entries for
testing. Operator only: it does not even tab-complete for anyone else, and the server re-checks the
permission before granting anything.

It spoils every secret in the mod. Open it only if you are willing to know.

Fabric · Minecraft 26.2 · Fabric API

MIT license — use it, fork it, put it in your modpack.

---
---

# ЧАСТЬ 3. Описание (Русский)

## Необычные достижения

Ванильные достижения всегда говорят, что делать дальше. Это — никогда.

Пятьдесят скрытых достижений: ни списка, ни полосок прогресса, ни «пройдено 3 из 10». Ты делаешь
что-то странное — обычно то, что специально делать бы не стал, — и игра молча это засчитывает. Забить
крипера голыми руками, стоя в лаве во время грозы, — это одно из них. Написать в чат прощание и
умереть в течение тридцати секунд — другое.

Как их получить, никто не расскажет. В этом весь мод.

### Карточка

Всё открытое лежит на карточке. Своя открывается на **J**.

Наведись на другого игрока и нажми ту же клавишу — откроется **его**: его достижения, его оформление,
и текст на твоём языке, а не на его.

### Темы, которые зарабатывают, а не выбирают

Двенадцать пиксельных фонов, каждый закрыт за конкретным достижением и подобран так, чтобы фон говорил
о том, что человек сделал. Пепел и лава достаётся тому, кто дрался с крипером голыми руками в луже
лавы. Древний город — тому, кто убил Стража, не получив от него ни одного удара.

К каждой теме идёт своя рамка для головы игрока, так что надетая карточка смотрится цельной.

Закрытые темы всё равно видны в выборе — затемнённые, с замком и названием. Оформление, которое ещё
нельзя надеть, само по себе намекает, что где-то есть что искать.

### Редкость, которая правда про тебя

У каждого достижения есть строка вроде «один раз на 3418 убийств» — насколько редко это случалось
именно **в твоей** игре, посчитано по обычной ванильной статистике, которую майн и так ведёт. Там, где
подходящего счётчика в ваниле нет (убийства в падении никто не считает), берутся часы в игре, а не
выдуманный процент. На живом сервере рядом показано ещё и то, у скольких игроков оно вообще есть.

### Ненавязчиво

Никакого спама в чат и окон на пол-экрана. Получение — небольшой золотой значок у хотбара, который
переворачивается в красный восклицательный знак, и тихий звук. Один раз за всю игру, на самом первом
достижении, он ещё и подскажет, какой клавишей открывается карточка.

### Мультиплеер

Достижения определяются и хранятся **на сервере**: карточка одинаковая, откуда бы ты ни зашёл, а
клиент не может выдать себе то, чего не заработал. Выбранная тема тоже лежит на сервере — иначе её
видел бы только ты сам.

> ### ⚠️ Ставить и на клиент, и на сервер
> У мода свои сетевые пакеты: без него клиент не откроет карточку, а серверу нечего отдавать. Версия
> одна и та же с обеих сторон.

### Настройки

`config/unusualachievements.json` — `overlayEnabled` полностью выключает значок у хотбара. Клавишу
можно переназначить в «Настройки → Управление», раздел **Необычные достижения**.

### Для администраторов

`/uadev` показывает весь пул достижений с расписанными условиями и выдаёт любое для проверки. Только
для операторов: у остальных команда даже не появляется в подсказках, а сервер отдельно перепроверяет
право перед выдачей.

Она раскрывает все секреты мода. Открывай, только если готов их узнать.

Fabric · Minecraft 26.2 · Fabric API

Лицензия MIT — пользуйтесь, форкайте, кладите в сборки.

---
---

# ЧАСТЬ 4. Что осознанно НЕ выносим на страницу (не часть описания)

Записано, чтобы это не добавили обратно по ошибке:

- **Список достижений.** Публикация условий убила бы единственную механику мода. На странице названы
  ровно два, и оба уже используются как примеры на экране «?» внутри самого мода.
- **Скриншоты.** Их надо снять в игре — список кадров в ЧАСТИ 1. Это единственное, что остаётся
  сделать перед публикацией.
