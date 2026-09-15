# Unusual Achievements — для выкладки на Modrinth

---

# ЧАСТЬ 1. Поля формы (не часть описания)

| Поле | Значение |
|---|---|
| **Name** | Unusual Achievements |
| **Slug** | `unusual-achievements` |
| **Summary** | Adds 50 hidden achievements, shown on a card other players can read. |
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

50 hidden achievements. No list, no hints, no progress bars.

Nobody tells you what they are. You do something odd, the game counts it, and only then do you find
out what it was. Most of them you hit by accident.

Everything you unlock goes on a card. **J** opens yours. Aim at another player and press it again to
see theirs.

12 card backgrounds, each locked behind one achievement, so a card says something about who is
holding it. The locked ones are shown too, greyed out under a padlock.

Every unlock says how rare it is in your own game: "once in 3418 mob kills", counted from your
vanilla stats.

No chat spam, no window across half the screen. Just a small gold marker by the hotbar that flips
into a red "!".

Settings in `config/unusualachievements.json`. The key is rebindable in Options → Controls.

Fabric · Minecraft 26.2 · Fabric API. MIT.

---

## Unusual Achievements

50 скрытых достижений. Ни списка, ни подсказок, ни полосок прогресса.

Никто не скажет, какие они. Ты делаешь что-то странное, игра это засчитывает, и только тогда ты
узнаёшь, что это было. Большинство ловятся случайно.

Всё открытое ложится на карточку. Своя — на **J**. Наведись на другого игрока и нажми ещё раз,
увидишь его.

12 фонов для карточки, каждый закрыт за одним достижением, так что по карточке видно, что за человек.
Закрытые тоже показаны — серые, под замком.

У каждого достижения написано, насколько оно редкое в твоей игре: «один раз на 3418 убийств», по
обычной ванильной статистике.

Никакого спама в чат и окон на пол-экрана. Просто маленький золотой значок у хотбара, который
переворачивается в красный восклицательный знак.

Настройки в `config/unusualachievements.json`. Клавиша переназначается в «Управлении».

Fabric · Minecraft 26.2 · Fabric API. MIT.

---
---

# ЧАСТЬ 4. Что осознанно НЕ выносим на страницу (не часть описания)

Записано, чтобы это не добавили обратно по ошибке:

- **Ни одного условия, даже как пример.** Раньше на странице стояли два («забить крипера голыми руками
  в лаве», «написать прощание и умереть») — с оправданием, что они и так есть на экране «?» внутри
  мода. Оправдание не работает: внутри мода их видит тот, кто уже играет, а на странице их читает
  каждый, ещё до установки. Мод целиком держится на том, что условий не знают. Убрано.
- **`/uadev`.** На странице о нём не пишем: это указатель на то, что существует способ прочитать все
  50 условий разом. Описан в README репозитория, этого достаточно.
- **Скриншоты.** Их надо снять в игре — список кадров в ЧАСТИ 1. Это единственное, что остаётся
  сделать перед публикацией.
- **Блок «ставить и на клиент, и на сервер».** Убран по просьбе — на странице это и так сказано полями
  Client / Server Required, а в описании читалось как инструкция посреди рассказа о моде.
