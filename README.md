# DivizionSC

Paper-плагин **1.21+** для кастомных способностей через YAML (**def**) или скриптовый язык **`.dsc`**. Вдохновлён MagicSpells, но проще в настройке: короткий синтаксис для быстрых скиллов и полный verbose-формат для сложных комбо.

**Версия:** `0.1.0-SNAPSHOT` · **Java:** 21 · **Сервер:** Paper 1.21+

---

## Содержание

1. [Установка](#установка)
2. [Структура папок](#структура-папок)
3. [Быстрый старт](#быстрый-старт)
4. [Команды](#команды)
5. [Права](#права)
6. [config.yml](#configyml)
7. [Панель скиллов (Skill Bar)](#панель-скиллов-skill-bar)
8. [Файлы def](#файлы-def)
9. [Простой синтаксис YAML](#простой-синтаксис-yaml)
10. [Язык `.dsc`](#язык-dsc)
11. [Продвинутый синтаксис (verbose YAML)](#продвинутый-синтаксис-verbose-yaml)
12. [Эффекты](#эффекты)
13. [Chain (цепочки)](#chain-цепочки)
14. [Helper-def и аргументы](#helper-def-и-аргументы)
15. [Триггеры и цели](#триггеры-и-цели)
16. [Cast-item (предмет способности)](#cast-item-предмет-способности)
17. [EffectLib](#effectlib)
18. [API для разработчиков](#api-для-разработчиков)
19. [Встроенные наборы def](#встроенные-наборы-def)
20. [Сборка из исходников](#сборка-из-исходников)
21. [Устранение неполадок](#устранение-неполадок)

---

## Установка

1. Соберите или скачайте JAR: `build/libs/DivizionSC-0.1.0-SNAPSHOT.jar`
2. Положите в `plugins/` Paper-сервера **1.21+**
3. Запустите сервер — создадутся `config.yml`, `defs/`, `lang/`, `binds/`
4. Перезагрузите def: `/dsc reload`

EffectLib **встроен в JAR** (shaded) — отдельно ставить не нужно.

---

## Структура папок

```
plugins/DivizionSC/
├── config.yml              # глобальные настройки
├── lang/
│   ├── ru.yml              # сообщения (создаётся при первом запуске)
│   └── en.yml
├── defs/                   # все способности — здесь
│   ├── defs-examples.yml   # ← создаётся автоматически
│   ├── defs-advanced.yml   # ← создаётся автоматически (~62 def)
│   ├── defs-script.dsc     # ← создаётся автоматически
│   ├── defs-combat.yml     # пример в репозитории (скопируйте вручную)
│   └── defs-magic.yml      # пример в репозитории (скопируйте вручную)
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
/dsc cast arcane_meteor
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

В папке `defs/` принимаются файлы:

| Паттерн | Пример |
|---------|--------|
| `defs-*.yml` / `defs-*.yaml` | `defs-combat.yml`, `defs-magic.yml` |
| `defs-*.dsc` | `defs-script.dsc` |
| Legacy | `examples.yml`, `advanced.yml` |

### Порядок загрузки

1. Все файлы из `plugins/DivizionSC/defs/`
2. **Addon-паки** из других плагинов: `defs/defs-*.yml` внутри их JAR или dev-папки

При дублировании id **последний загруженный перезаписывает** предыдущий. Addon-паки грузятся после data-папки.

### Автоматически создаваемые файлы

При первом запуске копируются из JAR:

- `defs-examples.yml` — простые примеры (heal, fireball, blink…)
- `defs-advanced.yml` — ~62 продвинутых def (chain, delay, EffectLib)
- `defs-script.dsc` — те же примеры на языке DSC

Файлы `defs-combat.yml` и `defs-magic.yml` лежат в репозитории как готовые наборы — скопируйте их в `defs/` вручную.

### Addon-паки для других плагинов

Положите в JAR зависимого плагина:

```
defs/
├── defs-myspells.yml
└── defs-boss.dsc
```

При `/dsc reload` или старте сервера DivizionSC подхватит их автоматически.

---

## Простой синтаксис YAML

### Минимальный def

```yaml
heal:
  name: "&aЛечение"
  cd: 8
  key: rclick
  tgt: self
  item: GOLDEN_APPLE | &aЛечение | &7ПКМ — 6 HP
  do:
    - heal 6
    - snd player_levelup
  done: heal_vfx
```

### Поля def

| Коротко | Полное | Описание | Пример |
|---------|--------|----------|--------|
| `cd` | `cooldown` | Перезарядка (сек) | `8` |
| `key` / `on` | `trigger` | Триггер каста | `rclick` |
| `tgt` / `to` | `target` | Режим цели | `self` |
| `rng` | `range` | Дальность (блоки) | `32` |
| `desc` | `description` | Описание | текст |
| `perm` | `permission` | Permission-нода | `divizionsc.def.heal` |
| `item` | `cast-item` | Предмет каста | см. ниже |
| `do` | `effects` | Список эффектов | строки или `{type: ...}` |
| `hit` | `on_hit` | Chain при попадании снаряда | `@helper` |
| `cast` | `on_cast` | Chain при начале каста | helper id |
| `done` / `after` | `on_complete` | Chain после эффектов | helper id |
| `helper` | — | Только для chain, не кастуется | `true` |

**Предмет (компактно):** `MATERIAL | имя | lore | lore2`

**Предмет (verbose):**

```yaml
cast-item:
  material: BLAZE_ROD
  name: "&cОгненный шар"
  lore:
    - "&7ПКМ — выпустить"
  custom-model-data: 1001
```

### Helper

```yaml
heal_vfx:
  helper: true
  tgt: self
  do:
    - fx sphere heart 1.2 20 15 2
```

---

## Язык `.dsc`

Псевдо-язык для **def**, **module** (переиспользуемые блоки) и **effect** (alias module).

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
    wait 10t {             // задержка + блок
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
| `wait 1s { }` / `wait 20t { }` | Задержка (сек / тики) |

Свойства внутри блока — как в YAML: `cd 8`, `key rclick`, `target self`, `item MAT | name | lore`.

Эффекты в `do` — те же строки: `heal 6`, `dmg 5`, `snd pop`, `fx sphere heart 1.2`, …

YAML и `.dsc` можно смешивать в одной папке. Module из `.dsc` видны другим def **в том же файле** (сверху вниз).

---

## Продвинутый синтаксис (verbose YAML)

Полный формат для многофазных способностей: вложенные **chain**, **delay**, **projectile** с `on_hit`, **EffectLib**, вызов helper-def с **args**.

```yaml
arcane_meteor:
  name: "&5&l☄ Арканный метеор"
  cooldown: 28
  trigger: right_click
  target: block
  range: 64
  cast-item:
    material: NETHER_STAR
    name: "&5&l☄ Арканный метеор"
  chain:
    on_cast:
      - meteor_channel_announce
      - meteor_channel_rune_ring
    on_hit:
      - def: meteor_impact_damage
        args:
          amount: 14
      - meteor_impact_knockback
      - meteor_scorch_aura
    on_complete:
      - meteor_channel_finish
  effects:
    - type: sound
      sound: ENTITY_WITHER_SHOOT
    - type: delay
      ticks: 40
      effects:
        - type: projectile
          projectile: FIREBALL
          speed: 1.6
          incendiary: true
          on_hit:
            - type: particle
              particle: EXPLOSION
              count: 2

meteor_impact_damage:
  helper: true
  target: entity
  effects:
    - type: damage
      amount: 10
```

### Поля def (verbose)

| Поле | Альтернатива | Пример |
|------|--------------|--------|
| `cooldown` | `cd` | `28` |
| `trigger` | `key`, `on` | `right_click`, `sneak`, `command` |
| `target` | `tgt`, `to` | `self`, `entity`, `block`, `none` |
| `range` | `rng` | `64` |
| `effects` | `do` | список `{type: ...}` или строк |
| `cast-item` | `item` | `{material, name, lore}` или `MAT \| name` |
| `chain.on_cast` | `cast` | helper id или `{def:, args:}` |
| `chain.on_hit` | `hit` | helper id или список |
| `chain.on_complete` | `done`, `after` | helper id или список |

Оба формата (простой и verbose) можно смешивать **в одном файле**.

---

## Эффекты

Каждая строка в `do:` — одна команда. Старый verbose-формат `{type: heal, amount: 6}` тоже работает.

### Простой синтаксис (строки)

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
| `cmd say %player% cast %def%` | Выполнить команду от консоли |
| `cmd tp %player% ~ ~1 ~` | `as_player: true` в verbose |

### Задержка

```yaml
do:
  - heal 6
  - wait: 1s
    do:
      - heal 3
      - snd orb_pickup
```

Или inline: `wait 1s: heal 3, snd pop`

В DSC: `wait 20t { heal 3 }` (t = тики, s = секунды).

### Verbose-типы эффектов

| type | Ключевые поля |
|------|---------------|
| `damage` | `amount`, `target` |
| `heal` | `amount`, `target` |
| `sound` | `sound`, `volume`, `pitch` |
| `particle` | `particle`, `count`, `offset`, `speed` |
| `potion` | `effect`, `duration`, `amplifier` |
| `velocity` | `power`, `y` или `x`, `y`, `z` |
| `teleport` | `forward`, `safe` |
| `lightning` | `damage` (false = без урона) |
| `delay` | `ticks`, `effects` |
| `projectile` | `projectile`, `speed`, `incendiary`, `yield`, `on_hit` |
| `effectlib` | `class`, `particle`, `radius`, `at`, `particles`, `iterations`, `period` |
| `def` / `chain` / `ability` | `def`, `args` |
| `message` | `text` |
| `command` | `command`, `as_player` |

### Плейсхолдеры в command

| Плейсхолдер | Значение |
|-------------|----------|
| `%player%` | Имя кастера |
| `%uuid%` | UUID кастера |
| `%def%` / `%ability%` | id def |

---

## Chain (цепочки)

Chain вызывает **helper-def** на определённой фазе каста. Максимальная глубина вложенности — **8**.

| Триггер | Когда срабатывает |
|---------|-------------------|
| `on_cast` / `cast` | В начале каста, до основных эффектов |
| `on_hit` / `hit` | При попадании снаряда (из `proj` / `projectile`) |
| `on_complete` / `done` / `after` | После выполнения основных эффектов |

```yaml
fireball:
  key: rclick
  do:
    - proj fireball 1.4
  hit: fireball_pain          # один helper

combo:
  do:
    - call fireball_pain
  done:
    - def: fireball_pain_heavy
      args:
        amount: 10
```

Порядок выполнения def: **on_cast → effects (do) → on_complete**. `on_hit` срабатывает отдельно при попадании projectile.

---

## Helper-def и аргументы

**Helper** — def с `helper: true`. Не кастуется напрямую, только через chain или `@call`.

Аргументы передаются в поля эффектов helper-def:

```yaml
fireball_pain_heavy:
  helper: true
  tgt: entity
  do:
    - dmg 12        # amount перезапишется из args

combo_strike:
  done:
    - def: fireball_pain_heavy
      args:
        amount: 10   # → dmg/amount в helper = 10
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

Встроенные формы в простом синтаксисе: `sphere`, `helix`, `circle`, `line`.

Verbose-пример:

```yaml
- type: effectlib
  at: target          # self | target | entity | eyes | effect
  class: SphereEffect
  particle: heart
  radius: 1.5
  particles: 30
  iterations: 20
  period: 2
```

При `/dsc reload` активные EffectLib-эффекты отменяются.

---

## API для разработчиков

Зависимость: артефакт `DivizionSC-*-api.jar` (Gradle configuration `pluginApi`).

### DivizionSCApi

```java
if (DivizionSCApi.isAvailable()) {
    DivizionSCApi.cast(player, "heal");
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

В def: `- type: my_effect` или зарегистрируйте парсер через addon.

### CastResult

`SUCCESS`, `NOT_FOUND`, `NOT_BOUND`, `NO_PERMISSION`, `COOLDOWN`, `NO_TARGET`, `WRONG_TRIGGER`, `HELPER_ONLY`, `CANCELLED`

---

## Встроенные наборы def

### defs-examples.yml (простой синтаксис)

`heal`, `blink`, `leap`, `fireball`, `combo_strike`, `lightning_strike` + helpers

### defs-advanced.yml (~62 def)

**Кастуемые:**

| id | Описание |
|----|----------|
| `arcane_meteor` | Канал → метеор → взрыв |
| `shadow_assassination` | Теневое убийство (shift по цели) |
| `storm_caller` | Многофазная молния |
| `blood_pact` | Кровавый пакт (самоповреждение → усиление) |
| `phantom_volley` | Серия призрачных снарядов |
| `void_rift` | Разлом пустоты |
| `elemental_convergence` | 4 стихии по очереди |
| `solar_avatar` | Солнечный аватар |
| `chrono_rift` | Временной разлом |
| `necrotic_plague` | Некротическая чума |
| `arcane_overload` | Перегрузка арканой |

Тест: `/dsc cast arcane_meteor`

### defs-combat.yml (в репозитории)

`combat_slash`, `combat_dash`, `combat_riposte`, `combat_execute`, `combat_whirlwind`, `combat_shield_bash`

### defs-magic.yml (в репозитории)

`magic_frost_nova`, `magic_soul_drain`, `magic_arcane_barrier`, `magic_arcane_bolt`, `magic_meteor_strike`

### defs-script.dsc

Те же базовые примеры на языке DSC.

---

## Сборка из исходников

```bash
./gradlew build          # JAR + DSC_MEG (если настроен local.plugins.dir)
./gradlew test           # unit-тесты парсеров
./gradlew shadowJar      # только DivizionSC JAR
```

Структура Java-пакетов:

```
ru.iamdvz.divizionsc/
├── command/             # /dsc
├── config/              # config.yml
├── def/
│   ├── loader/
│   │   ├── simple/      # SimpleDefCompiler, SimpleEffectParser
│   │   ├── verbose/     # VerboseEffectParser
│   │   └── dsl/         # DscParser, DscCompiler
│   ├── effect/          # EffectExecutor, BuiltinEffectRegistry
│   ├── model/           # DefDefinition, TriggerType, …
│   └── service/         # DefService, CooldownService, ChainService
├── bind/                # Skill Bar привязки
├── gui/                 # SkillBarMenu (InventoryHolder)
├── listener/            # CastTrigger, KeyInput, SkillBar
├── lang/                # ru/en сообщения
└── api/                 # DivizionSCApi, события, EffectHandler
```

### DSC_MEG (опционально)

Отдельный плагин в `DSC_MEG/` — VFX через ModelEngine. Зависит от DivizionSC + ModelEngine. Собирается вместе с `./gradlew build`.

---

## Устранение неполадок

| Проблема | Решение |
|----------|---------|
| Def не загружается | `/dsc reload` — смотрите ошибки в чате и консоли; `debug-load-errors: true` |
| «Def не найден» | Проверьте id и имя файла (`defs-*.yml`) |
| Каст не срабатывает | Проверьте `key`, permission, кулдаун, `helper: true` |
| «Нет цели в прицеле» | `tgt: entity` требует живую цель в range |
| «Нельзя кастовать так» | Триггер def не совпадает с действием; используйте `/dsc give` |
| Skill Bar не открывается | `divizionsc.skills`, `skill-bar.enabled: true` |
| Дубли id | Последний загруженный файл побеждает; addon грузится после data-папки |
| EffectLib не виден | Только в verbose или `fx …` в простом синтаксисе |

---

## Лицензия и автор

**Автор:** iamdvz · **Описание:** script-based abilities inspired by MagicSpells
