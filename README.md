# DivizionSC

Paper-плагин **1.21+** для кастомных способностей на скриптовом языке **`.dsc`**. Вдохновлён MagicSpells, но проще в настройке.

**Версия:** `1.0.0` · **Java:** 21 · **Сервер:** Paper 1.21+ · **Folia:** поддерживается

---

## Шпаргалка

> **Канон синтаксиса v2:** [SYNTAX.md](SYNTAX.md)  
> **Полная шпаргалка:** [SHPARGALKA.md](SHPARGALKA.md)  
> Ниже — краткий дубликат для быстрого доступа из README.

### Команды

| Команда | Право | Что делает |
|---------|-------|------------|
| `/dsc list [фильтр]` | — | Список способностей |
| `/dsc info <id>` | — | Детали def |
| `/dsc cast <id>` | `divizionsc.def.<id>` | Принудительный каст |
| `/dsc give <id> [игрок]` | `divizionsc.admin` | Выдать cast-item |
| `/dsc skills` | `divizionsc.skills` | Панель скиллов (F — swap hands) |
| `/dsc bind <1-9> <id>` | `divizionsc.skills` | Привязать к слоту |
| `/dsc reload` | `divizionsc.admin` | Перезагрузить config + defs |
| `/dsc validate` | `divizionsc.admin` | Проверка defs без применения |

Алиасы: `/divizionsc`, `/ability`

### Минимальный def

**DSC** (`defs/defs-mine.dsc`):

```dsc
ability heal {
  name: "&aЛечение"
  cooldown: 8
  key: rclick
  target: self
  item: GOLDEN_APPLE | &aЛечение

  cast {
    @mend(6) >> self
  }
}
```

> Кирпич `@mend` — из `defs-bricks.dsc`. Без него: `heal(6) >> self`.

### Поля def

| Коротко | Полное | Пример |
|---------|--------|--------|
| `cd` | `cooldown` | `8` (сек) |
| `key` / `on` | `trigger` | `rclick` |
| `tgt` / `to` | `target` | `entity` |
| `rng` | `range` | `32` |
| `perm` | `permission` | `divizionsc.def.heal` |
| `mana` / `mp` / `cost` | — | `15` |
| `item` | `cast-item` | `BLAZE_ROD \| имя \| lore` |
| `do` | `effects` | список эффектов |
| `hit` | `on_hit` | chain при попадании снаряда |
| `cast` | `on_cast` | chain при начале каста |
| `done` / `after` | `on_complete` | chain после эффектов |
| `helper` | — | `true` — не кастуется напрямую |
| `passive` | — | `true` — пассивка |
| `on` | `passive-trigger`, `ptrigger` | Событие пассивки: `damage`, `attack`, `interval`… |
| `key` | `passive-key`, `pkey` | Клавиша пассивки (как у каста): `shift`, `rclick`… |
| `presses` | `combo`, `press-count` | Комбо: число нажатий (для `key`) |
| `press-window` | `press-interval` | Комбо: пауза между нажатиями (`2s`, `40t`) |
| `interval` | `passive-interval` | `60t` — период для `on interval` |

### Триггеры (`key`)

| Значение | Действие |
|----------|----------|
| `rclick` | ПКМ |
| `lclick` | ЛКМ |
| `shift` | Shift |
| `ctrl` | Ctrl (бег) |
| `space` | Пробел |
| `w` `s` `a` `d` | Движение |
| `q` | Выбросить |
| `f` | Swap hands |
| `1`–`9` | Слот хотбара |
| `cmd` | Только `/dsc cast` |
| `any` | Любой (Skill Bar) |

### Цели (`tgt`)

| Значение | Поведение |
|----------|-----------|
| `self` | Кастер |
| `entity` / `mob` | Существо в прицеле |
| `block` | Блок в прицеле |
| `none` | Без сущности, точка по взгляду |

### Эффекты (строки в `do`)

| Команда | Пример |
|---------|--------|
| `heal` / `dmg` | `heal 6`, `dmg 5 entity` |
| `tp` / `vel` | `tp 8`, `vel 0 0.4` |
| `snd` | `snd blaze_shoot 0.8 1.0` |
| `ptl` | `ptl portal 30` |
| `pot` | `pot speed 5s 1` |
| `lit` | `lit` / `lit fx` (без урона) |
| `msg` | `msg &aПривет` |
| `proj` | `proj fireball 1.4` |
| `fx` | `fx sphere heart 1.2 20 15 2` |
| `vfx` / `meg` | `vfx ground_slam slash` (DSC_MEG) |
| `cmd` | `cmd say %player% cast %def%` |
| `call` / `@` | `@pain(5)`, `call heal_vfx` |
| `wait` / `after` | `after(20t) { heal 3 }` |
| `pull` / `push` | `push 1.2` |
| `dash` / `blink` | `dash 1.6`, `blink 8` |
| `shield` | `shield 4` |
| `summon` / `totem` | `summon zombie 1` |
| `give` | `give DIAMOND 1` |
| `repeat` / `aura` / `shape` | см. [Новые эффекты](#новые-эффекты-100) |
| `money` | `take-money 10` (Vault) |
| `set` | `set power 5` |
| `require` | `require health > 4` |
| `if` / `chance` | `if (distance < 8) { … } else { … }`, `chance (35%) { lit }` |

### DSC: модули и chain

```dsc
module pain(dmg) {
  target entity
  do { dmg $dmg target }
}

def fireball {
  cd 6
  key rclick
  target block

  do {
    proj fireball 1.4
    after(10t) { @spark }
  }
  on hit { @pain(5) }
  on cast { @spark }
  on done { @hearts_vfx }
}
```

| Конструкция | Назначение |
|-------------|------------|
| `def id { }` | Кастуемая способность |
| `module id(a) { }` | Helper, `$a` — аргумент |
| `effect id { }` | = module |
| `@mod(5)` / `use mod` | Вызов module |
| `after(1s) { }` / `after(20t) { }` | Задержка (сек / тики) |
| `on hit / cast / done` | Chain-триггеры |

Порядок каста: **on_cast → do → on_complete**. `on_hit` — при попадании `proj`.

### Условия

```dsc
require health > 4
require has-target
if (distance < 10) { dmg 8 } else { msg "&7Далеко" }
chance (30%) { lit }
```

Типы: `health`, `distance`, `permission`, `holding`, `sneaking`, `on-ground`, `has-target`, `world`, `region` (WG), `money` (Vault), `variable`. Операторы: `< <= > >= == !=`, `!`/`not`, `50%`.

### Плейсхолдеры

`{caster_health}`, `{target_distance}`, `{def_id}`, `{var_NAME}`, `{random}` · в cmd: `%player%`, `%uuid%`, `%def%` · PAPI: `%placeholder%`

### Пассивки

```dsc
def passive_thorns {
  passive true
  on damage                 // событие
  cd 2
  target entity
  perm divizionsc.def.passive_thorns

  do {
    require has-target
    @pain(3)
  }
}

def passive_dash_combo {
  passive true
  key shift                 // клавиша (как у активных def)
  presses 3
  press-window 2s
  cd 5
  target self
  perm divizionsc.def.passive_dash_combo
  do { vel forward 1.2 }
}
```

Только **permission** — команд выдачи нет.

События: `on damage`, `on attack`, `kill`, `death`, `join`, `break`, `fall`, `on interval` + `interval 60t`.  
Клавиши: `key shift`, `key rclick`, `1`–`9` и т.д. Комбо: `presses` + `press-window`.

### Права

| Нода | По умолчанию |
|------|--------------|
| `divizionsc.admin` | op |
| `divizionsc.skills` | true |
| `divizionsc.def.<id>` | — (если не задан `perm`) |
| `divizionsc.def.*` | true |

### Workflow

```
написать defs/defs-*.dsc  →  /dsc validate  →  /dsc reload  →  /dsc give <id>
```

Файлы: только `defs-*.dsc`. Дубли id — **последний файл побеждает**.

---

## Что нового в 1.0.0

- **Folia-safe** планировщик: все задержки/повторы идут через region/entity-планировщик (`folia-supported: true`).
- **Типизированный конфиг** на Elytrium Serializer; способности описываются только в `.dsc`.
- **Условия, переменные и формулы:** `if/else`, `chance`, `require`, `set var`, `exp4j`-формулы и плейсхолдеры (PAPI + встроенные).
- **Новые эффекты:** `pull/push`, `dash/blink`, `shield`, `summon`, `totem`, `give-item`, `repeat`, `aura`, `shape`, экономика (`money`).
- **База данных:** SQLite по умолчанию, MySQL/MariaDB для кросс-сервера; привязки и кулдауны хранятся в БД (async, HikariCP). Старые `binds/<uuid>.yml` мигрируют автоматически.
- **GUI:** поиск по способностям и браузер способностей в Skill Bar.
- **Интеграции** (soft-depend, через `ServicesManager`): PlaceholderAPI, Vault, WorldGuard.
- **Сборка без shade:** библиотеки грузятся Paper через `plugin.yml libraries`; EffectLib — soft-depend (отдельный плагин).
- **Команда** `/dsc validate` — dry-run проверки def без применения.

---

## Содержание

0. [Шпаргалка](#шпаргалка)
1. [Установка](#установка)
2. [Структура папок](#структура-папок)
3. [Быстрый старт](#быстрый-старт)
4. [Команды](#команды)
5. [Права](#права)
6. [config.yml](#configyml)
7. [Панель скиллов (Skill Bar)](#панель-скиллов-skill-bar)
8. [Файлы def](#файлы-def)
9. [Язык `.dsc`](#язык-dsc)
10. [Эффекты](#эффекты)
11. [Chain (цепочки)](#chain-цепочки)
12. [Helper-def и аргументы](#helper-def-и-аргументы)
13. [Триггеры и цели](#триггеры-и-цели)
14. [Cast-item (предмет способности)](#cast-item-предмет-способности)
15. [EffectLib](#effectlib)
16. [API для разработчиков](#api-для-разработчиков)
17. [Встроенные наборы def](#встроенные-наборы-def)
18. [Сборка из исходников](#сборка-из-исходников)
19. [Устранение неполадок](#устранение-неполадок)

---

## Установка

1. Соберите или скачайте JAR: `build/libs/DivizionSC-1.0.0.jar`
2. Положите в `plugins/` Paper-сервера **1.21+** (или Folia)
3. Запустите сервер — создадутся `config.yml`, `defs/`, `lang/`, БД (`data.db`)
4. Перезагрузите def: `/dsc reload`

Библиотеки (Elytrium, HikariCP, JDBC, exp4j) Paper подгружает сам через `plugin.yml libraries` — JAR их не встраивает. **EffectLib** теперь soft-depend: для `fx …`/EffectLib-эффектов поставьте плагин EffectLib отдельно. Аналогично опциональны PlaceholderAPI, Vault, WorldGuard.

---

## Структура папок

```
plugins/DivizionSC/
├── config.yml              # глобальные настройки
├── lang/
│   ├── ru.yml              # сообщения (создаётся при первом запуске)
│   └── en.yml
├── defs/                   # все способности — здесь
│   ├── defs-bricks.dsc     # ← библиотека module-кирпичей (создаётся автоматически)
│   ├── defs-examples.dsc   # ← способности из кирпичей
│   └── defs-mine.dsc       # ваши способности
└── binds/
    └── <uuid>.yml          # привязки скиллов к слотам 1–9
```

---

## Быстрый старт

```bash
./gradlew build
# → build/libs/DivizionSC-*.jar
```

После первого запуска:

```
/dsc list
/dsc info heal
/dsc give heal
/dsc cast arcane_combo
/dsc skills
/dsc reload
```

**Как кастовать способность:**

| Способ | Когда |
|--------|-------|
| Предмет в руке + триггер (`rclick`, `shift`, …) | У def задан `item` |
| `/dsc cast <id>` | У def `key: cmd` |
| Слот 1–9 / ПКМ с пустой рукой | Через Skill Bar (`/dsc skills`) |
| Клавиша хотбара | Если def привязан к слоту и настроен `bind-triggers` |

---

## Команды

Алиасы: `/dsc`, `/divizionsc`, `/ability`

| Команда | Право | Описание |
|---------|-------|----------|
| `/dsc list [фильтр] [страница]` | — | Список всех def (пагинация) |
| `/dsc info <id>` | — | Детали def: кулдаун, триггер, цель |
| `/dsc cast <id>` | `divizionsc.def.<id>` | Принудительный каст |
| `/dsc give <id> [игрок]` | `divizionsc.admin` | Выдать cast-item |
| `/dsc skills` | `divizionsc.skills` | Открыть панель скиллов |
| `/dsc bind` | `divizionsc.skills` | Показать привязки слотов 1–9 |
| `/dsc bind <1-9> <id>` | `divizionsc.skills` | Привязать def к слоту |
| `/dsc bind <1-9> clear` | `divizionsc.skills` | Очистить слот |
| `/dsc reload` | `divizionsc.admin` | Перезагрузить config + defs |
| `/dsc validate` | `divizionsc.admin` | Dry-run проверка defs без применения |

---

## Права

| Нода | По умолчанию | Назначение |
|------|--------------|------------|
| `divizionsc.admin` | op | `/dsc reload`, `/dsc give` |
| `divizionsc.skills` | true | Skill Bar и привязки |
| `divizionsc.cast.*` | op | Каст любого def через команду |
| `divizionsc.def.*` | true | Wildcard на все def |
| `divizionsc.def.<id>` | — | Право на конкретный def (задаётся в def) |

Если в def не указан `perm`, используется `divizionsc.def.<id>` (префикс настраивается в `config.yml`).

> ⚠️ **Безопасность.** `divizionsc.def.*` по умолчанию `true` — значит **любой игрок может скастовать любой def без собственного `perm`**.
> Эффект `cmd ...` выполняется от имени **консоли**, поэтому def с `cmd op %player%` или подобным = выдача прав всем.
> Для любого def с `cmd`/`command` **всегда указывайте свой `perm`** (например `perm: divizionsc.def.myspell`) и выдавайте право точечно,
> либо ужесточите дефолт `divizionsc.def.*` до `op` на сервере.

---

## config.yml

Создаётся при первом запуске. Ключевые параметры:

```yaml
locale: ru                    # ru | en — файл из lang/
defs-folder: defs             # папка с def (относительно data-папки)
default-range: 32.0           # дальность прицеливания по умолчанию
admin-permission: divizionsc.admin
cast-permission-prefix: divizionsc.def.
cooldown-messages: true       # сообщение «подождите N сек»
cast-messages: false          # сообщение при успешном касте
debug-load-errors: true       # stack trace ошибок загрузки в консоль
list-page-size: 15            # def на страницу в /dsc list

config-version: 1             # схема конфига (обновляется плагином)

op-bypass:                    # обход для операторов (isOp)
  enabled: true
  ignore-cooldown: true       # без кулдауна
  ignore-mana: true           # без траты/проверки маны

mana:                         # мана в def (поля mana / mp / cost)
  enabled: true
  default-max: 100.0
  default-amount: 100.0

skill-bar:
  enabled: true
  permission: divizionsc.skills
  gui-size: 54
  list-start-slot: 0          # слот GUI, с которого начинается список def
  list-page-size: 27
  bind-row-start-slot: 36     # нижний ряд — слоты 1–9
  bind-slot-count: 9
  require-empty-hand: false   # каст с хотбара только с пустыми руками
  open-on-swap-hands: true    # F (swap hands) открывает панель
  bind-triggers:              # какие действия кастуют привязанный скилл
    - right_click
    - 1
    - 2
    - 3
    - 4
    - 5
    - 6
    - 7
    - 8
    - 9
```

Устаревший ключ `skill-bar.cast-mode` (`right_click` / `select` / `both`) всё ещё поддерживается.

---

## Панель скиллов (Skill Bar)

Открыть: **`/dsc skills`** или клавиша **F** (swap hands), если `open-on-swap-hands: true`.

### Привязка (GUI)

1. Кликните слот **1–9** в нижнем ряду — он станет активным.
2. Кликните скилл в списке сверху — он привяжется к выбранному слоту.

| Действие в GUI | Эффект |
|----------------|--------|
| Клик по слоту 1–9 | Выбрать слот для привязки |
| Клик по скиллу | Привязать к выбранному слоту |
| **Shift+клик** по скиллу | Привязать к первому свободному слоту |
| **Shift+клик** по слоту | Очистить слот |
| Стрелки | Листать страницы списка def |
| Кнопка **Поиск** | ЛКМ — ввести запрос в чат; ПКМ — сбросить фильтр |
| Кнопка **Браузер** | Открыть браузер способностей с описанием |

### Каст в игре

| Действие | Эффект |
|----------|--------|
| Клавиша **1–9** | Каст def из привязанного слота (если в `bind-triggers`) |
| **ПКМ** с пустой рукой | Каст def из текущего слота хотбара |
| Выбор слота хотбара | Каст def, привязанного к этому слоту |

По умолчанию каст работает **с предметом в руке** (`require-empty-hand: false`). Чтобы требовать пустые руки — `require-empty-hand: true`.

Привязки сохраняются в `binds/<uuid>.yml` и переживают перезагрузку сервера.

---

## Файлы def

### Именование

Способности описываются в файлах `defs/defs-*.dsc`. Другие расширения (`.yml`, `.yaml`) **не загружаются**.

### Порядок загрузки

1. Все `defs-*.dsc` из `plugins/DivizionSC/defs/`
2. **Addon-паки** из других плагинов: `defs/defs-*.dsc` внутри их JAR или dev-папки

При дублировании id **последний загруженный перезаписывает** предыдущий. Addon-паки грузятся после data-папки.

### Автоматически создаваемые файлы

При первом запуске копируется из JAR:

- `defs-bricks.dsc` — библиотека **module-кирпичей** (fx, strike, smite, …)
- `defs-examples.dsc` — способности, собранные из кирпичей (heal, fireball, smite, …)
- `defs-advanced.dsc` — сложные комбо, боссы, пассивки
- `defs-fx-examples.dsc` — EffectLib / ModelEngine VFX

### Addon-паки для других плагинов

Положите в JAR зависимого плагина:

```
defs/
├── defs-myspells.dsc
└── defs-combo.dsc
```

При `/dsc reload` или старте сервера DivizionSC подхватит их автоматически.

---

## Язык `.dsc`

> **Канон синтаксиса v2:** [SYNTAX.md](SYNTAX.md) · **Шпаргалка:** [SHPARGALKA.md](SHPARGALKA.md)

Скриптовый синтаксис для **ability**, **module** (переиспользуемые кирпичи) и **passive**.  
Файлы: `defs/defs-*.dsc`. Устаревшие алиасы `def` / `effect` / `do` — см. [SYNTAX.md](SYNTAX.md).

> **Канонические рабочие примеры** (покрыты тестами и создаются при первом запуске):
> `defs/defs-bricks.dsc` (кирпичи), `defs/defs-examples.dsc` (способности),
> `defs/defs-advanced.dsc` (сложное), `defs/defs-fx-examples.dsc` (EffectLib/VFX).
>
> **Module-кирпичи** из любого `defs-*.dsc` попадают в **общий индекс** и доступны
> во всех остальных файлах при `/dsc reload`.
>
> **Свойства** пишутся как `ключ: значение` (можно несколько на строке) — прямо в блоке или в `meta { }`.
> Короткая форма `ключ значение` (без двоеточия) тоже работает.
>
> **Эффекты** — вызовы `verb(args)`: `heal(6)`, `damage(5)`, `sound(blaze_shoot, volume=0.8)`.
> Короткая форма `heal 6` тоже принимается.
>
> **Модули** — `@strike(5) >> target`, цепочки `@a, @b`, `with`, `stack { }`, `extends`.
>
> **Блочные конструкции:** `if (cond) { }`, `when has-target { }`, `chance 35% { }`,
> `after(20t) { }`, `stack >> target { @a @b }`, `area(6) { }`,
> `projectile(FIREBALL, speed=1.4) { hit { } }`.

---

# 📘 Полный гайд по синтаксису `.dsc`

Этот раздел — исчерпывающий справочник. Всё, что здесь описано, реально принимается парсером
(сверено с тестами). Рабочие примеры — в `defs/defs-bricks.dsc`, `defs/defs-examples.dsc`,
`defs/defs-advanced.dsc`, `defs/defs-fx-examples.dsc`.

## 1. Из чего состоит файл

```dsc
// строчный комментарий
# тоже комментарий

ability имя {        // верхнеуровневый блок
  свойство: значение // настройки
  cast {             // тело
    effect(args)     // эффекты
  }
}
```

- Файлы должны называться `defs-*.dsc` (другие расширения не грузятся).
- Один файл — сколько угодно блоков. При совпадении id побеждает последний загруженный файл.
- Регистр id не важен (приводится к нижнему). Кавычки — двойные `"..."`.
- Комментарии: `//` и `#` (внутри кавычек не считаются комментарием).

## 2. Блоки верхнего уровня

| Блок | Назначение |
|------|------------|
| `ability id { }` | Кастуемая способность. Псевдоним: `def id { }` |
| `module id { }` | Helper-блок — вызывается через `@id`, сам не кастуется. Псевдоним: `effect id { }` |
| `module id(a, b) { }` | Module с параметрами — в теле доступны `$a`, `$b` |
| `module id(a=3) { }` | Параметр с умолчанием — `@id` без аргументов |
| `module id extends parent { }` | Наследование: сначала тело `parent`, затем своё |
| `passive id { }` | Пассивная способность (то же, что `ability` + `passive: true`) |

```dsc
module pain(dmg) {
  target: entity
  effects {
    damage($dmg) >> target
  }
}
```

## 3. Свойства блока

Пишутся **прямо в блоке** или внутри `meta { }`. Две формы записи, обе равноценны:

```dsc
ability x {
  cooldown: 8           // с двоеточием (можно несколько на строке)
  key: rclick  cd: 8    // несколько свойств в одну строку — ок
  cd 8                  // короткая форма (одно свойство на строку)
  meta { cd: 8 key: rclick }   // или внутри meta { }
}
```

> Значения с пробелами/двоеточием (`name`, `desc`, `item`) лучше держать на отдельной строке.

| Свойство | Псевдонимы | Пример | Описание |
|----------|-----------|--------|----------|
| `name` | — | `name: "&aЛечение"` | Отображаемое имя |
| `desc` | `description` | `desc: "&7Текст"` | Описание (для `/dsc info`, лора) |
| `cooldown` | `cd` | `cd: 8` | Перезарядка в секундах |
| `key` | `trigger`, `on` | `key: rclick` | Триггер каста (см. §4) |
| `target` | `tgt`, `to` | `target: entity` | Режим цели (см. §5) |
| `range` | `rng` | `range: 32` | Дальность прицеливания (блоки) |
| `permission` | `perm` | `perm: divizionsc.def.heal` | Право на каст |
| `mana` | `mp`, `cost` | `mana: 15` | Стоимость маны |
| `item` | `cast-item` | `item: BLAZE_ROD \| &cИмя \| &7лор` | Предмет каста: `материал \| имя \| лор...` |
| `helper` | — | `helper: true` | Не кастуется напрямую, только через `@` |
| `passive` | — | `passive: true` | Пассивная способность (см. §18) |

## 4. Триггеры (`key` / `trigger`)

| Значение | Псевдонимы | Действие |
|----------|-----------|----------|
| `rclick` | `right_click`, `right`, `use` | ПКМ |
| `lclick` | `left_click`, `left` | ЛКМ |
| `shift` | `sneak` | Приседание |
| `ctrl` | `sprint` | Бег |
| `space` | `jump` | Прыжок |
| `w` | `forward` | Вперёд |
| `s` | `backward`, `back` | Назад |
| `a` | `strafe_left` | Влево |
| `d` | `strafe_right` | Вправо |
| `q` | `drop` | Выбросить предмет |
| `f` | `swap_hands`, `swap`, `offhand` | Смена рук |
| `1`–`9` | `hotbar`, `slot` | Слот хотбара |
| `cmd` | `command` | Только `/dsc cast <id>` |
| `any` | — | Любой триггер (для Skill Bar) |

## 5. Цели (`target` / `tgt`)

| Значение | Псевдонимы | Поведение |
|----------|-----------|-----------|
| `self` | `caster`, `me` | Кастер |
| `entity` | `target`, `mob`, `living` | Существо в прицеле (ray-trace в пределах `range`) |
| `block` | `location` | Блок в прицеле |
| `none` | — | Без сущности; точка = взгляд + `range` |

## 6. Тело и фазы каста

| Секция | Псевдонимы | Когда выполняется |
|--------|-----------|-------------------|
| `cast { }` | `do`, `effects` | Основное тело |
| `on cast { }` | `start` | До основного тела |
| `on hit { }` | `hit` | При попадании снаряда из `projectile`/`proj` |
| `on done { }` | `complete`, `end`, `after` | После основного тела |

```dsc
cast { proj fireball 1.4 }
on cast { @spark }     // фазовые секции принимают ТОЛЬКО вызовы @module
on hit  { @pain(8) }
on done { @hearts }
```

Порядок: **on cast → cast → on done**. `on hit` срабатывает отдельно при попадании снаряда.

## 7. Эффекты: две формы записи

```dsc
heal(6)              // канон: verb(аргументы)
heal 6               // короткая форма — тоже работает
sound(blaze_shoot, volume=0.8, pitch=1.0)   // именованные аргументы
damage(5, target)                            // позиционные аргументы
```

- Аргументы разделяются запятыми; именованные — `имя=значение`.
- Строки с пробелами — в кавычках: `message("&aПривет, мир")`.
- `command(...)` и `message(...)` берут весь текст целиком (запятые сохраняются).

## 8. Справочник эффектов

| Эффект | Псевдонимы | Сигнатура / пример | Описание |
|--------|-----------|--------------------|----------|
| `heal` | — | `heal(6)` | Лечение (по умолч. себя) |
| `damage` | `dmg` | `damage(5, target)` | Урон (амаунт может быть переменной — §12) |
| `teleport` | `tp` | `tp(8)` | Телепорт вперёд (safe) |
| `velocity` | `vel`, `knockback` | `velocity(0, 1, 0)` или `vel(power, y)` | Импульс скорости |
| `sound` | `snd` | `sound(blaze_shoot, volume=0.8, pitch=1.0)` | Звук |
| `particle` | `ptl` | `particle(flame, count=20, offset=0.3)` | Партиклы |
| `potion` | `pot` | `potion(speed, duration=5s, amplifier=1)` | Зелье на цель |
| `lightning` | `lit` | `lightning` / `lightning(fx=true)` | Молния (`fx=true` — без урона) |
| `message` | `msg` | `message("&aТекст {caster_health}")` | Сообщение кастеру (резолвит плейсхолдеры) |
| `command` | `cmd` | `command(give %player% diamond 1)` | Команда от консоли |
| `projectile` | `proj` | `projectile(FIREBALL, speed=1.4) { hit { } }` | Снаряд (FIREBALL/SNOWBALL/POTION) |
| `def` | `call`, `chain`, `ability` | `def(other_spell)` | Вызвать другой def (см. `@` в §10) |
| `set` | `setvar` | `set(power, 5)` | Переменная каста (§12) |
| `require` | — | `require(has-target)` | Прерывает каст, если ложно (§14) |
| `blink` | — | `blink(8)` | Телепорт по взгляду на N блоков |
| `dash` | — | `dash(1.6)` | Рывок по взгляду |
| `pull` | — | `pull(1.2)` | Притянуть цель к кастеру |
| `push` | — | `push(1.2)` | Оттолкнуть цель |
| `shield` | — | `shield(4)` (или `shield 4 10s`) | Поглощение урона (absorption) |
| `stun` | — | `stun(40)` | Оглушение на N тиков |
| `raycast` | `beam` | `raycast(18, hit_radius=2)` | Raycast по взгляду — первый враг на луче |
| `particle_projectile` | `ppj` | `ppj(flame, 0.75, 15)` | Партикловый снаряд (particle, speed, distance) |
| `summon` | — | `summon(wolf, count=2)` | Призыв сущности |
| `give` | `give_item` | `give(DIAMOND, amount=1)` | Выдать предмет |
| `shape` | `shape_particle` | `shape(circle, flame, 2)` | Фигура из партиклов |
| `money` | `give-money`, `take-money` | `give-money(100)` | Экономика (нужен Vault) |
| `fx` | `effectlib` | блок (§16) | EffectLib-эффект |
| `vfx` | `meg`, `model` | блок (§17) или `vfx модель анимация` | ModelEngine VFX |

Блочные эффекты (`if`, `chance`, `after`, `area`, `loop`, `aura`, `repeat`, `projectile`) — см. §11.

## 9. Маршруты цели (`>>` / `at`)

Указывают, на кого/откуда действует эффект. `->` и `→` — псевдонимы `>>`. `at target` — читаемый алиас.

```dsc
heal(6) >> self                 // один токен = НАЗНАЧЕНИЕ
damage(4) >> caster >> target   // два токена = ОТКУДА >> КУДА
damage(5) at target             // то же, что >> target
sound(boom) >> location         // на точку
```

Токены: `self`/`caster` · `target`/`entity` · `block`/`location` · `eyes`.
Русские псевдонимы: `начало` (откуда), `цель`, `конец`.

Если маршрут не указан — действует **умолчание по эффекту** и `target` способности:
`heal`→self, `damage`→target, `glow`→target при `target: entity` и т.д.
Явный `>> target` / `>> self` рекомендуется для наглядности.

## 10. Модули — LEGO-сборка

### Файлы и индекс

- **`defs-bricks.dsc`** — библиотека кирпичей (`fx_spark`, `strike`, `smite`, …).
- Способности в **`defs-examples.dsc`** собираются только из `@кирпичей`.
- Все `module` из **любого** `defs-*.dsc` индексируются глобально и видны во всех файлах.

### Базовый вызов

```dsc
module pain(dmg) {
  target: entity
  effects { damage($dmg) >> target }
}

ability x {
  cast {
    @pain(8)            // позиционно → $dmg = 8
    @pain(dmg=8)        // именованно
    @pain 8             // короткая форма
    @pain(5) >> target  // с маршрутом
    @pain               // если module pain(dmg=3) — dmg=3 по умолчанию
  }
}
```

- Вызов только через `@id` (формы `use id` / `call(...)` **не** поддерживаются).
- Параметры подставляются как `$имя` в теле модуля.
- Свойство `target: entity` на module задаёт цель для эффектов внутри.

### Модульный синтаксис (композиция)

| Конструкция | Пример | Назначение |
|-------------|--------|------------|
| **Цепочка `@`** | `@pain(3), @fx_spark >> target` | Несколько кирпичей на одной строке, общий маршрут |
| **`with`** | `@strike(5) with slow_pack >> target` | Основной кирпич + доп. без нового module |
| **`stack`** | `stack >> target { @strike(4) @slow_pack }` | Группа с общим маршрутом (алиасы: `compose`, `pipe`, `batch`) |
| **`extends`** | `module heavy(dmg) extends strike { … }` | Наследование module (сначала родитель, потом своё тело) |
| **Дефолты** | `module pain(dmg=3) { }` → `@pain` | Параметры по умолчанию |

```dsc
// with — дополнить готовый кирпич
cast { @strike(5) with slow_pack >> target }

// stack — группа на одну цель
cast {
  stack >> target {
    @strike(4)
    @slow_pack
  }
  @mend(2) >> self
}

// extends — heavy_strike = strike + slow (см. defs-bricks.dsc)
module heavy_strike(dmg) extends strike {
  effects { @slow_pack >> target }
}
cast { @heavy_strike(8) >> target }
```

### Уровни кирпичей (рекомендуемая структура)

| Уровень | Пример | Описание |
|---------|--------|----------|
| **Атом** | `fx_spark`, `sfx_heal` | Один эффект / VFX |
| **Пак** | `pain(dmg)`, `mend(amt)`, `slow_pack` | 2–3 связанных эффекта |
| **Сборка** | `strike(dmg)`, `vamp_bite(dmg, heal)` | Вызывает другие `@module` внутри |

## 11. Блочные конструкции

Все — со скобками у заголовка.

```dsc
// Задержка (t = тики, s = секунды)
after(20t) {
  heal(3)
}

// Условие (when — псевдоним if)
if (distance < 8) {
  damage(10) >> target
} else if (distance < 16) {
  damage(5) >> target
} else {
  message("&7Далеко")
}

// Вероятность
chance (30%) {
  lightning >> self
}

// По всем целям в радиусе
area(6) {
  damage(4) >> target
}

// Цикл: N раз с паузой interval тиков
loop(times=5, interval=10) {
  particle(flame, count=20) >> self
}

// Периодическая аура вокруг цели
aura(radius=5, duration=6s, interval=20) {
  damage(2) >> target
}

// Повтор вложенного
repeat(times=3) {
  sound(note_pling)
}

// Снаряд с фазами hit/tick (внутри — любые эффекты)
projectile(FIREBALL, speed=1.4) {
  tick { particle(flame, count=3) }
  hit  { @pain(8) }
}
```

## 12. Переменные и формулы

`set(имя, выражение)` создаёт переменную каста. Выражения считает exp4j; видны имена
других переменных и встроенные `health`, `level`, `distance`, `chain_depth`.

```dsc
set(base, 4)
set(total, base * 2 + level)   // формула
damage(total) >> target        // одиночная переменная как амаунт — работает
message("&cУрон: {var_total}")
```

> Переменная/формула как **амаунт** надёжно работает у `damage` и `heal`. Для остальных
> числовых полей сначала посчитайте в `set`, затем подставьте переменную. Многословные
> формулы (`power * 2`) кладите в `set`, а не прямо в аргумент эффекта.

## 13. Плейсхолдеры

Работают в формулах и сообщениях (`message`):

| Плейсхолдер | Значение |
|-------------|----------|
| `{caster_name}` `{caster_health}` `{caster_max_health}` | О кастере |
| `{caster_level}` `{caster_x}` `{caster_y}` `{caster_z}` `{caster_world}` | О кастере |
| `{target_name}` `{target_health}` `{target_max_health}` `{target_distance}` | О цели |
| `{def_id}` `{def_cooldown}` | О способности |
| `{var_ИМЯ}` | Переменная каста |
| `{random}` | Случайное 0..1 |
| `{chain_depth}` | Глубина цепочки вызовов |
| `%папи%` | Любой PlaceholderAPI (если установлен) |

В эффекте `command` доступны: `%player%`, `%uuid%`, `%def%`, `%ability%`.

## 14. Условия (`require` / `if`)

```dsc
require has-target
require health > 6
if (distance < 10) { ... } else { ... }
```

| Тип | Пример |
|-----|--------|
| `health` | `health > 6`, `health < 50%` |
| `distance` | `distance < 10` |
| `has-target` | `has-target` |
| `sneaking` / `on-ground` | `sneaking`, `on-ground` |
| `holding` | `holding DIAMOND_SWORD` |
| `permission` | `permission divizionsc.vip` |
| `world` | `world world_nether` |
| `region` | `region spawn` (нужен WorldGuard) |
| `money` / `balance` | `money > 100` (нужен Vault) |
| `variable` | `variable power > 3` |

Операторы: `<` `<=` `>` `>=` `==` `!=`, отрицание `!`/`not`, проценты (`> 50%`).

## 15. Cast-item

```dsc
item: BLAZE_ROD | &cОгненный шар | &7ПКМ — каст
```

Формат: `материал | имя | строка лора | ещё строка...`. id способности хранится в предмете (PDC).
Выдать: `/dsc give <id> [игрок]`. Без `item` способность кастуется через команду или Skill Bar.

## 16. EffectLib — блок `fx { }`

Требует плагин EffectLib. Две формы:

```dsc
// короткая
fx(sphere, heart, radius=1.2, particles=20, iterations=15, period=2) >> self

// подробная
fx >> target {
  shape helix          // sphere · helix/spiral · circle/ring · line · tornado
  particle flame       // или: class HelixEffect
  at self              // self · target · block · eyes  (или маршрут >> у заголовка)
  sphere {  radius 1.5  particles 30  y-offset 0.4  radius-increase 0.02 }
  helix  {  radius 1.5  strands 2  curve 5  particles 50  rotation 180 }
  circle {  radius 3.0  particles 40  whole-circle true  orient true }
  line   {  length 8.0  particles 30  is-zig-zag true  zig-zags 4 }
  timing {  iterations 30  period 2  delay 0 }
  base   {  visible-range 64  auto-orient true  async false }
}
```

## 17. ModelEngine — блок `vfx { }`

Требует DSC_MEG + ModelEngine. Формы:

```dsc
// короткая: vfx <модель> <анимация> [масштаб] [remove-delay тиков] [точка]
vfx ground_slam slash 1.5 60 target

// подробная (как vfx-effect в MagicSpells)
vfx {
  model-name vfx_flamethrower
  position projectile       // projectile · target · self · caster · location
  model-color ffffff
  model-scale 1.0
  remove-delay 0            // 0 = жить, пока есть якорь (follow/снаряд); иначе тики
  bones {
    flame  { tint ffaa00  glow ff6600  block-light 15  sky-light 0 }
    shadow { hidden true }
  }
  animations {
    skill { delay 0  speed 1.0  loop true  lerp-in 0.0  lerp-out 0.0 }
  }
}
```

VFX на снаряде — поместите блок `vfx { position projectile ... }` внутрь `projectile(...) { tick { } }`.

## 18. Пассивки

```dsc
passive passive_thorns {
  on: damage            // событие
  cooldown: 2
  target: entity
  perm: divizionsc.def.passive_thorns
  cast { @pain(3) }
}

passive combo_dash {
  key: shift            // или по клавише
  presses: 3            // комбо: 3 нажатия
  press-window: 2s      // в пределах 2 секунд
  cooldown: 5
  target: self
  perm: divizionsc.def.combo_dash
  cast { dash(1.6) }
}
```

| Свойство | Псевдонимы | Описание |
|----------|-----------|----------|
| `on` | `passive-trigger`, `ptrigger`, `event` | Событие: `damage`, `attack`, `kill`, `death`, `join`, `break`, `fall`, `interval` |
| `key` | `passive-key`, `pkey` | Клавиша (как у активных): `shift`, `rclick`, `1`–`9`… |
| `presses` | `combo`, `press-count` | Комбо: число нажатий клавиши |
| `press-window` | `press-interval` | Окно комбо (`2s`, `40t`) |
| `interval` | `passive-interval` | Период для `on: interval` (`60t`) |

Пассивки выдаются **только правом** (`perm`) — команд выдачи нет.

## 19. Частые ошибки и правила

- Блоки `if` / `chance` / `after` / `loop` / `area` / `aura` / `projectile` — **всегда со скобками**:
  `if (cond) { }`, а не `if cond { }`.
- Фазовые секции `on cast/hit/done` принимают **только** `@module`, не обычные эффекты.
- Module вызывается только через `@id` — не `use`/`call(...)`.
- Текст с пробелами/запятыми — в кавычках; для `command` кавычки не нужны (берётся весь текст).
- Многословные формулы — через `set`, а не прямо в аргумент эффекта.
- `divizionsc.def.*` по умолчанию `true` — для def с `cmd` всегда указывайте свой `perm` (см. раздел «Права»).

### Структура

```dsc
// комментарий

module hearts_vfx {
  target self
  do {
    fx sphere heart 1.2 20 15 2
  }
}

module pain(dmg) {          // параметры → $dmg в теле
  target entity
  do {
    dmg $dmg
    ptl crit 12
  }
}

effect spark {              // = module, короткая запись
  ptl electric_spark 25
  snd lightning_bolt_impact
}

def fireball {
  name "&cОгненный шар"
  cd 6
  key rclick
  range 32
  item BLAZE_ROD | &cОгненный шар

  do {
    snd blaze_shoot
    proj fireball 1.4
    @pain(5)               // inline-вызов module с аргументами
    after(10t) {           // задержка + блок
      @spark
    }
  }

  on hit { @pain(8) }       // chain on_hit
  on cast { @spark }        // chain on_cast
  on done { @hearts_vfx }   // chain on_complete
}
```

### Ключевые слова

| Конструкция | Назначение |
|-------------|------------|
| `def id { }` | Кастуемая способность |
| `module id { }` | Helper-блок (переиспользуемый) |
| `module id(a, b) { }` | Module с параметрами `$a`, `$b` |
| `effect id { }` | То же, что module |
| `do { }` | Список эффектов |
| `on hit / cast / done { }` | Chain-триггеры |
| `@module` / `use module` | Вызов module |
| `@module(5)` | Вызов с аргументами |
| `after(1s) { }` / `after(20t) { }` | Задержка (сек / тики) |

### Поля def

| Свойство | Описание | Пример |
|----------|----------|--------|
| `cd` / `cooldown` | Перезарядка (сек) | `cd 8` |
| `key` / `trigger` | Триггер каста | `key rclick` |
| `target` / `tgt` | Режим цели | `target entity` |
| `range` / `rng` | Дальность (блоки) | `range 32` |
| `desc` / `description` | Описание | `desc "&7…"` |
| `perm` / `permission` | Permission-нода | `perm divizionsc.def.heal` |
| `mana` / `mp` / `cost` | Стоимость маны | `mana 15` |
| `item` / `cast-item` | Предмет каста | `item BLAZE_ROD \| имя \| lore` |
| `helper` | Не кастуется напрямую | `helper true` |
| `passive` | Пассивная способность | `passive true` |
| `on` / `passive-trigger` | Событие пассивки | `on damage` |
| `key` / `passive-key` | Клавиша пассивки | `key shift` |
| `presses` / `combo` | Комбо: число нажатий | `presses 3` |
| `press-window` | Комбо: пауза между нажатиями | `press-window 2s` |
| `interval` / `passive-interval` | Период `on interval` | `interval 60t` |

Эффекты в `do` — строковые команды: `heal 6`, `dmg 5`, `snd pop`, `fx sphere heart 1.2`, …

Module из **любого** `defs-*.dsc` попадают в **общий индекс** и доступны во всех файлах при загрузке.

---

## Эффекты

> **Канон v2:** `verb(args) >> route` в секции `cast { }`. Ниже — **legacy-формат строк** (`heal 6`, `snd pop`) для обратной совместимости; в новых defs используйте [SYNTAX.md](SYNTAX.md).

Каждая строка в `cast { }` (или устаревшем `do { }`) — одна команда.

### Синтаксис (legacy-строки)

| Строка | Действие |
|--------|----------|
| `heal 6` | Исцеление (target: self) |
| `dmg 5` | Урон по цели |
| `dmg 5 entity` | Урон с явным target |
| `tp 8` | Телепорт вперёд на 8 блоков (safe) |
| `vel 1.2 0.5` | Velocity: power, y-составляющая |
| `vel 0 1 0` | Velocity: x y z |
| `snd player_levelup` | Звук (короткое имя → `ENTITY_…`) |
| `snd blaze_shoot 0.8 1.0` | Звук + volume + pitch |
| `ptl portal 30` | Частицы |
| `ptl portal 30 0.5` | Частицы + offset |
| `pot speed 5s 1` | Зелье (5s = секунды, amplifier) |
| `lit` | Молния с уроном |
| `lit fx` | Молния без урона |
| `msg &aПривет` | Сообщение игроку |
| `call heal_vfx` / `@heal_vfx` | Вызов helper-def |
| `@pain(5)` | Вызов с аргументом (amount/dmg/damage = 5) |
| `proj fireball 1.4` | Снаряд (FIREBALL, SNOWBALL, POTION) |
| `fx sphere heart 1.2` | EffectLib SphereEffect |
| `fx sphere heart 1.2 20 15 2` | + particles, iterations, period |
| `fx helix flame 1.0` | HelixEffect |
| `fx circle cloud 2.0` | CircleEffect |
| `fx line electric_spark 3.0` | LineEffect |
| `vfx ground_slam slash` | ModelEngine VFX (нужен DSC_MEG) |
| `vfx aura idle 1.0 100 self follow glow loop` | scale, remove-delay, at, флаги |
| `meg ground_slam slam target` | алиасы: `vfx`, `meg`, `modelengine`, `model` |
| `cmd say %player% cast %def%` | Выполнить команду от консоли |

### Задержка

```dsc
do {
  heal 6
  after(1s) {
    heal 3
    snd orb_pickup
  }
}
```

`t` = тики, `s` = секунды. Блок задержки — `after(...)` / `wait(...)` со скобками.

### Плейсхолдеры в command

| Плейсхолдер | Значение |
|-------------|----------|
| `%player%` | Имя кастера |
| `%uuid%` | UUID кастера |
| `%def%` / `%ability%` | id def |

---

## Условия, переменные и формулы (1.0.0)

### Переменные и формулы

Внутри одного каста доступны переменные. Задаются через `set` и используются в формулах (`exp4j`) и плейсхолдерах.

```dsc
do {
  set(power, 5)
  set(power, power * 2)       // формула: power = 10
  damage(power) >> target     // урон = значение переменной
}
```

**Плейсхолдеры** (в строках/формулах): `{caster_health}`, `{caster_max_health}`, `{caster_level}`, `{caster_x/y/z}`, `{target_health}`, `{target_distance}`, `{def_id}`, `{def_cooldown}`, `{var_NAME}`, `{random}`, `{chain_depth}`. Если установлен PlaceholderAPI — работают и `%papi%`-плейсхолдеры.

### Условия

`require <условие>` прерывает цепочку, если условие ложно. `if … { } else { }` и `chance N% { }` — ветвление.

```dsc
do {
  require health > 6
  if (distance < 10) {
    dmg 8 target
  } else {
    msg "&7Слишком далеко"
  }
  chance (30%) {
    lightning
  }
}
```

**Типы условий:** `health`, `distance`, `permission <node>`, `holding <MATERIAL>`, `sneaking`, `on-ground`, `has-target`, `world <name>`, `region <id>` (WorldGuard), `money`/`balance` (Vault), `variable <name>`, `chance <p>`. Поддерживают сравнения `<`, `<=`, `>`, `>=`, `==`, `!=`, отрицание `!`/`not`, проценты (`health > 50%`).

---

## Новые эффекты (1.0.0)

| Эффект | Описание | Ключевые поля |
|--------|----------|---------------|
| `pull` | Притянуть цель к кастеру | `strength`/`power`, `y` |
| `push` | Оттолкнуть цель/себя | `strength`/`power`, `y` |
| `dash` | Рывок кастера по взгляду | `power`/`strength` |
| `blink` | Телепорт по взгляду | `distance` |
| `shield` | Поглощение урона (absorption) | `amount`, `duration` |
| `summon` | Призыв сущности | `entity`, `count`, `duration` |
| `totem` | Тотем-armorstand с эффектом | `duration`, `interval` |
| `give-item` | Выдать предмет | `material`, `amount`, `name` |
| `repeat` | Повтор вложенных эффектов | `times`, `interval`, `effects` |
| `aura` | Периодическая аура вокруг цели | `radius`, `duration`, `interval`, `effects` |
| `shape` | Фигура из партиклов/звука | `shape`, `particle`/`sound`, `radius` |
| `money` / `give-money` / `take-money` | Экономика (Vault) | `amount`, `op` |

Все новые эффекты Folia-safe (мутации сущностей планируются в их region/entity-потоках).

---

## База данных и кросс-сервер (1.0.0)

Привязки хотбара и кулдауны хранятся в БД. Запросы — асинхронные (HikariCP), на main-потоке БД не блокируется.

```yaml
database:
  type: sqlite            # sqlite | mysql | mariadb
  sqlite-file: data.db    # для sqlite (внутри plugins/DivizionSC)
  host: localhost         # для mysql/mariadb
  port: 3306
  name: divizionsc
  user: root
  password: ""
  pool-size: 6
  table-prefix: dsc_
```

- **SQLite** (по умолчанию) — локальный файл, ничего настраивать не нужно.
- **MySQL/MariaDB** — общая БД для нескольких серверов: привязки и кулдауны синхронизируются между ними.
- Миграции идемпотентны (`CREATE TABLE IF NOT EXISTS`), таблицы: `<prefix>binds`, `<prefix>cooldowns`.

### Миграция со старого формата

При первом запуске 1.0.0 старые `binds/<uuid>.yml` автоматически переносятся в БД, после чего папка переименовывается в `binds_migrated`. Удалять её можно вручную после проверки.

---

## Интеграции (1.0.0)

Soft-depend, подключаются автоматически при наличии плагина (через `ServicesManager`):

| Плагин | Что даёт |
|--------|----------|
| PlaceholderAPI | `%papi%`-плейсхолдеры в строках/формулах |
| WorldGuard | условие `region <id>` |
| Vault | условие `money`/`balance`, эффекты `money`/`give-money`/`take-money` |

---

## Chain (цепочки)

Chain вызывает **helper-def** на определённой фазе каста. Максимальная глубина вложенности — **8**.

| Триггер | Когда срабатывает |
|---------|-------------------|
| `on_cast` / `cast` | В начале каста, до основных эффектов |
| `on_hit` / `hit` | При попадании снаряда (из `proj` / `projectile`) |
| `on_complete` / `done` / `after` | После выполнения основных эффектов |

```dsc
def fireball {
  key rclick
  do {
    proj fireball 1.4
  }
  on hit { @fireball_pain }
}

def combo {
  do {
    @fireball_pain
  }
  on done { @fireball_pain_heavy(10) }
}
```

Порядок выполнения def: **on_cast → effects (do) → on_complete**. `on_hit` срабатывает отдельно при попадании projectile.

---

## Helper-def и аргументы

**Helper** — `module` или `def` с `helper true`. Не кастуется напрямую, только через chain или `@call`.

```dsc
module fireball_pain_heavy(dmg) {
  target entity
  do { dmg $dmg target }
}

def combo_strike {
  on done { @fireball_pain_heavy(10) }
}
```

Inline-аргументы: `@pain(5)` → `amount`, `dmg`, `damage` = 5.

В DSC параметры module: `module pain(dmg) { dmg $dmg }` → `@pain(8)`.

---

## Триггеры и цели

### Триггеры (`key` / `trigger`)

| Значение | Действие |
|----------|----------|
| `rclick` / `right_click` | ПКМ |
| `lclick` / `left_click` | ЛКМ |
| `shift` / `sneak` | Shift (присед) |
| `ctrl` / `sprint` | Ctrl (бег) |
| `space` / `jump` | Пробел |
| `w` / `forward` | W |
| `s` / `backward` | S |
| `a` / `strafe_left` | A |
| `d` / `strafe_right` | D |
| `q` / `drop` | Q (выбросить) |
| `f` / `swap_hands` | F (вторая рука) |
| `1`–`9` / `hotbar` | Слот хотбара |
| `cmd` / `command` | Только `/dsc cast` |
| `any` | Любой триггер (для Skill Bar) |

### Цели (`tgt` / `target`)

| Значение | Поведение |
|----------|-----------|
| `self` | Кастер |
| `mob` / `entity` | Живое существо в прицеле (ray trace). Без цели — каст с `NO_TARGET` |
| `block` | Блок в прицеле |
| `none` | Без сущности; позиция — взгляд + range |

---

## Cast-item (предмет способности)

Предмет хранит id def в PDC (`def_id`). При совпадении триггера — каст.

- `/dsc give <id>` — выдать предмет
- Без `item` / `cast-item` def кастуется только через команду или Skill Bar
- Если material не указан при `/dsc give` — `BLAZE_ROD`

---

## EffectLib

Встроенные формы в DSC: `fx sphere`, `fx helix`, `fx circle`, `fx line`.

```dsc
do {
  fx sphere heart 1.5 30 20 2
}
```

При `/dsc reload` активные EffectLib-эффекты отменяются.

---

## API для разработчиков

Зависимость: артефакт основного JAR (Gradle configuration `pluginApi`).

API регистрируется в `Bukkit.getServicesManager()` как `DivizionSCService`. Получить можно напрямую или через статический фасад `DivizionSCApi` (обёртка над ServicesManager, сохранена для совместимости).

```java
DivizionSCService dsc = Bukkit.getServicesManager()
        .load(DivizionSCService.class);
if (dsc != null) {
    dsc.cast(player, "heal");
}
```

### DivizionSCApi (фасад)

```java
if (DivizionSCApi.isAvailable()) {
    DivizionSCApi.cast(player, "heal");
    DivizionSCApi.castIntegration(player, "heal_vfx", target, location, IntegrationCastOptions.mythicDefaults());
    DivizionSCApi.findDef("fireball");
    DivizionSCApi.allDefs();
    DivizionSCApi.reloadDefs();
    DivizionSCApi.lastLoadReport();
}
```

### События

| Событие | Когда | Особенности |
|---------|-------|-------------|
| `DefPreCastEvent` | Перед кастом | `Cancellable` — отмена блокирует каст |
| `DefCastEvent` | После успешного каста | — |
| `DefLoadEvent` | После загрузки defs | `DefLoadReport` с ошибками |

### Кастомные эффекты

```java
DivizionSCApi.effectHandlers().register(new EffectHandler() {
    @Override
    public String type() {
        return "my_effect";
    }

    @Override
    public void execute(EffectContext ctx, EffectDefinition effect) {
        // логика
    }
});
```

В def: `my_effect …` или зарегистрируйте парсер через addon.

### CastResult

`SUCCESS`, `NOT_FOUND`, `NOT_BOUND`, `NO_PERMISSION`, `COOLDOWN`, `NO_TARGET`, `WRONG_TRIGGER`, `HELPER_ONLY`, `CANCELLED`

---

## Встроенные наборы def

### defs-bricks.dsc + defs-examples.dsc

**Кирпичи** (`defs-bricks.dsc`): `fx_spark`, `pain`, `strike`, `smite`, `vamp_bite`, `heavy_strike` (extends), …

**Способности** (`defs-examples.dsc`): `heal`, `fireball`, `smite_basic`, `instant_smite`, `lifesteal`,
`frost_nova`, `tri_combo`, `heavy_sword`, `passive_thorns`, …

Сборка из кирпичей: `@strike(5) >> target`, `@strike with slow_pack`, `stack >> target { … }`.

**Продвинутое:** `defs-advanced.dsc` — `arcane_meteor`, `boss_enrage`, `chain_lightning`, …  
**VFX:** `defs-fx-examples.dsc` — EffectLib / ModelEngine.

---

## Сборка из исходников

```bash
./gradlew build          # JAR + DSC_MEG + DSC_MM (если настроен local.plugins.dir)
./gradlew test           # unit-тесты парсеров, условий, схемы БД
./gradlew :jar           # только DivizionSC JAR (без shade)
```

Сборка **без shade**: внешние библиотеки объявлены в `plugin.yml libraries` и грузятся Paper в рантайме.

Структура Java-пакетов:

```
ru.iamdvz.divizionsc/
├── platform/            # Scheduler, FoliaScheduler, SchedulerProvider (Folia-safe)
├── command/             # /dsc, /dsc validate
├── config/              # Elytrium settings (Settings), PluginConfig-фасад
├── def/
│   ├── loader/
│   │   ├── simple/      # SimpleEffectParser
│   │   └── dsl/         # DscParser, DscCompiler (if/else/chance/set/require)
│   ├── effect/          # EffectExecutor, BuiltinEffectRegistry, ExtraEffects, VariableScope
│   ├── condition/       # ConditionParser, ConditionSpec, ConditionEvaluator, Comparison
│   ├── expr/            # PlaceholderResolver, ExpressionEvaluator (exp4j)
│   ├── model/           # DefDefinition, TriggerType, …
│   └── service/         # DefService, CooldownService, ChainService
├── database/            # DatabaseManager (HikariCP), CooldownRepository
├── bind/                # BindService, BindRepository (БД + кэш)
├── gui/                 # SkillBarMenu, AbilityBrowserMenu, GuiInputService
├── integration/         # PlaceholderApiHook, VaultHook, WorldGuardHook, IntegrationManager
├── listener/            # CastTrigger, KeyInput, SkillBar, PlayerData
├── lang/                # ru/en сообщения
└── api/                 # DivizionSCService (ServicesManager), DivizionSCApi-фасад, события
```

### DSC_MEG (опционально)

Отдельный плагин в `DSC_MEG/` — VFX через ModelEngine. Зависит от DivizionSC + ModelEngine. Собирается вместе с `./gradlew build`.

В `.dsc` (нужен `DSC_MEG.jar`):

```dsc
// короткая строка
vfx ground_slam slash 1.5 60 target

// полный блок (как MagicSpells vfx effect)
proj snowball 0.1 {
  vfx {
    position projectile
    model-name vfx_flamethrower
    model-color ffffff
    model-scale 1.0
    remove-delay 0
    bones {
      flame { tint ffaa00 glow ff6600 block-light 15 sky-light 0 }
      shadow_plane { hidden true }
    }
    animations {
      skill { delay 0 speed 1.0 loop true lerp-in 0.0 lerp-out 0.0 }
    }
  }
}
```

`position projectile` — VFX на снаряде (спавн один раз, следует за ним до удаления).  
Пример: `defs/defs-meg.dsc` → `/dsc cast meg_flamethrower`

### DSC_MM (опционально)

Отдельный плагин в `DSC_MM/` — интеграция с MythicMobs. Зависит от DivizionSC + MythicMobs. Собирается вместе с `./gradlew build`.

Основной путь — **MythicMobs → DivizionSC** через механику `dscdef` в скиллах MM:

```yaml
Skills:
  - dscdef{def=fireball} @target
  - dscdef{def=heal_vfx;helper=true} @self
  - dscskill{ability=combo_strike;caster=trigger} @trigger
```

| Механика | Алиасы | Описание |
|----------|--------|----------|
| `dscdef` | `dscskill`, `dsc`, `divizionsc` | Выполнить def DivizionSC |

Параметры механики: `def` / `ability` / `id`, `helper`, `permission`, `cooldown`, `require-target`, `caster` (`auto`, `self`, `trigger`, `target`).

Для скиллов моба `caster=auto` берёт игрока-триггера или цель, если кастер — не игрок.

---

## Устранение неполадок

| Проблема | Решение |
|----------|---------|
| Def не загружается | `/dsc reload` — смотрите ошибки в чате и консоли; `debug-load-errors: true` |
| «Def не найден» | Проверьте id и имя файла (`defs-*.dsc`) |
| Каст не срабатывает | Проверьте `key`, permission, кулдаун, `helper true` |
| «Нет цели в прицеле» | `target entity` требует живую цель в range |
| «Нельзя кастовать так» | Триггер def не совпадает с действием; используйте `/dsc give` |
| Skill Bar не открывается | `divizionsc.skills`, `skill-bar.enabled: true` |
| Дубли id | Последний загруженный файл побеждает; addon грузится после data-папки |
| EffectLib не виден | Установите плагин EffectLib (теперь soft-depend, не встроен) |
| Привязки/кулдауны не сохраняются | Проверьте `database` в config.yml и логи инициализации БД |
| Интеграция не подключилась | Плагин (PAPI/Vault/WorldGuard) должен быть установлен; смотрите «Hooked into …» в логе |

---

## Лицензия и автор

**Автор:** iamdvz · **Описание:** script-based abilities inspired by MagicSpells
