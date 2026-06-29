# DivizionSC — шпаргалка

Paper **1.21+** / Folia · Java 21 · способности на языке **`.dsc`**

> **Канон синтаксиса v2:** [SYNTAX.md](SYNTAX.md) · **Шпаргалка:** [SHPARGALKA.md](SHPARGALKA.md)

```
написать defs/defs-*.dsc  →  /dsc validate  →  /dsc reload  →  выдать perm / give item
```

---

## Структура

```
plugins/DivizionSC/
├── config.yml
├── lang/ru.yml, en.yml
├── defs/defs-*.dsc      ← только .dsc загружаются
│   ├── defs-bricks.dsc  ← библиотека module-кирпичей
│   ├── defs-examples.dsc
│   └── defs-mine.dsc    ← ваши способности
└── data.db              ← кулдауны и привязки хотбара (БД)
```

Дубли **id** — побеждает последний загруженный файл. **Module** из любого `defs-*.dsc` — в общем индексе, видны везде. Addon-паки: `defs/defs-*.dsc` в JAR других плагинов.

---

## Команды

Алиасы: `/dsc`, `/divizionsc`, `/ability`

| Команда | Право | Действие |
|---------|-------|----------|
| `/dsc list [фильтр]` | — | Список def |
| `/dsc info <id>` | — | Детали |
| `/dsc cast <id>` | `divizionsc.def.<id>` | Принудительный каст |
| `/dsc give <id> [игрок]` | `divizionsc.admin` | Выдать cast-item |
| `/dsc skills` | `divizionsc.skills` | Skill Bar (или **F**) |
| `/dsc bind <1-9> <id\|clear>` | `divizionsc.skills` | Привязка к хотбару |
| `/dsc reload` | `divizionsc.admin` | Перезагрузка |
| `/dsc validate` | `divizionsc.admin` | Проверка defs без применения |

---

## Права

| Нода | По умолчанию | Назначение |
|------|--------------|------------|
| `divizionsc.admin` | op | reload, give |
| `divizionsc.skills` | true | Skill Bar, bind |
| `divizionsc.def.<id>` | — | Каст / пассивка (если не задан `perm`) |
| `divizionsc.def.*` | true | Все def |
| `divizionsc.passive.*` | — | Все пассивки |
| `divizionsc.cast.*` | op | Каст через команду |

Префикс прав настраивается: `cast-permission-prefix` в config.yml.

---

## Как кастовать

| Способ | Условие |
|--------|---------|
| Предмет + триггер (`rclick`, `shift`…) | Задан `item` |
| `/dsc cast <id>` | `key cmd` или любой def с правом |
| Skill Bar / клавиши 1–9 | Привязка + `bind-triggers` в config |
| ПКМ пустой рукой | Привязка к слоту хотбара |

| Пассивки **не кастуются** вручную — срабатывают по **событию** или **клавише** + permission.

---

## Минимальная способность

```dsc
ability heal {
  meta {
    name: "&aЛечение"
    cd: 8
    key: rclick
    target: self
    perm: divizionsc.def.heal
    item: GOLDEN_APPLE | &aЛечение | &7ПКМ
  }

  cast {
    heal(6) >> self
    sound(entity_player_levelup) >> self
    @fx_hearts >> self
  }
}
```

**Алиасы блоков (устаревшие):** `def` → `ability`, `effect` → `module`.

---

## meta { } — все свойства способности

Формат: **`key: value`** (двоеточие обязательно).

| Поле | Алиасы | Пример |
|------|--------|--------|
| `name` | — | `"&aЛечение"` |
| `desc` | `description` | текст |
| `cd` | `cooldown` | `8` (сек) |
| `key` | `trigger` | `rclick` — каст; у пассивки — клавиша |
| `target` | `tgt`, `to` | `entity` / `self` / `block` / `none` |
| `range` | `rng` | `32` |
| `perm` | `permission` | `divizionsc.def.heal` |
| `mana` | `mp`, `cost` | `15` |
| `item` | `cast-item` | `BLAZE_ROD \| имя \| lore` |
| `event` | `on`, `passive-trigger` | `damage` — событие пассивки |
| `presses` | `combo` | `3` — комбо по клавише |
| `press-window` | `press-interval` | `2s` |
| `interval` | `passive-interval` | `60t` — для `event: interval` |

`target: none` — каст **не требует** цель; эффекты с `>> target` сами делают raycast по `range`.

---

## Секции блока

| Секция | Назначение | Алиасы |
|--------|------------|--------|
| `meta { }` | Свойства | — |
| `cast { }` | Основное тело каста | `do { }` |
| `effects { }` | Тело module (или cast) | — |
| `start { }` | Chain до основного каста | `on cast { }` |
| `hit { }` | Chain при попадании снаряда | `on hit { }` |
| `done { }` | Chain после каста | `on done { }`, `complete`, `end` |

Порядок выполнения: **start → cast → done**. `hit` — при попадании `projectile`.

Chain-секции (`start` / `done`) принимают только **`@module(args) >> route`**.

Эффекты при попадании снаряда — в **`projectile(...) { hit { … } }`**, не в ability `hit { }`.

---

## Маршрут `>>` / `->`

| Запись | Смысл |
|--------|--------|
| `effect(...)` | **Авто-цель** по типу эффекта и `meta.target` |
| `effect(...) >> target` | Явная цель; начало по умолчанию `self` |
| `effect(...) at target` | То же, читаемый алиас |
| `effect(...) -> target` | То же, короткая запись |
| `effect(...) >> self >> target` | Явно: от кастера к цели |

**Авто-цель (если `>>` / `->` не указан):**

| Эффект | Куда |
|--------|------|
| `heal`, `shield`, `dash`, `blink`, `require`, `set`, `message` | `self` |
| `damage`, `push`, `pull`, `stun`, `raycast`, `lightning` | `target` |
| `sound`, `particles` | `self` (или по `meta.target`) |
| `projectile` | `target` / `location` при `target: block` |

Токены: `self`/`caster`, `target`/`entity`/`цель`, `location`/`block`, `eyes`/`глаза`.

**Не путать:** `meta { target: entity }` — режим прицеливания способности; `>> target` — куда применить конкретный эффект.

---

## Краткая запись эффектов

```dsc
heal 6                    // heal(6)
damage 5                  // damage(5)
require has-target        // require(has-target)
lightning                 // lightning()
when (distance < 8) { }  // if (...)
@pain 5 -> target         // @pain(dmg=5) >> target
@spark                    // @spark()
```

Сложные аргументы — по-прежнему через скобки: `sound(entity_blaze_shoot, volume=0.8)`.

---

## Вызов модулей

```dsc
@pain(dmg=5) >> target
@pain 5 -> target
@fx_hearts
@pain(5) >> target          // один аргумент → dmg/amount
@pain(3), @fx_spark >> target   // цепочка @ на одной строке
@strike(5) with slow_pack >> target   // with — доп. кирпич
```

`module pain(dmg) { … }` — параметр `$dmg` в теле. `module pain(dmg=3)` — `@pain` без скобок.

`use()` / `call()` **не поддерживаются** — только `@`.

### Композиция (LEGO)

| Синтаксис | Пример |
|-----------|--------|
| **stack** | `stack >> target { @strike(4) @slow_pack }` |
| **with** | `@strike(5) with slow_pack >> target` |
| **цепочка `@`** | `@pain(3), @fx_spark >> target` |
| **extends** | `module heavy(dmg) extends strike { … }` |
| **дефолты** | `module pain(dmg=3) { }` → `@pain` |

Алиасы `stack`: `compose`, `pipe`, `batch`.

Кирпичи — **`defs-bricks.dsc`**. Способности собираются в **`defs-examples.dsc`**.

---

## Эффекты (function-call)

| Эффект | Пример |
|--------|--------|
| `heal` | `heal(6) >> self` |
| `damage` | `damage(5) >> target` |
| `sound` | `sound(entity_blaze_shoot, volume=0.8, pitch=0.9) >> self` |
| `particles` | `particles(portal, count=30) >> target` |
| `potion` | `potion(speed, duration=5s, amplifier=1) >> self` |
| `lightning` | `lightning() >> target` |
| `message` | `message("&aГотово!") >> self` |
| `projectile` | `projectile(FIREBALL, speed=1.4) >> target` |
| `after` | `after(10t) >> self { … }` |
| `if` | `if (distance < 8) >> target { … } else { … }` |
| `chance` | `chance(35%) { … }` или `chance 35% { … }` |
| `require` | `require(has-target) >> self` |
| `set` | `set(power, 5) >> self` |
| `area` | `area(radius=5) >> self { … }` |
| `loop` | `loop(times=3, interval=5) >> self { … }` |
| `dash` / `blink` | `dash(1.6) >> self`, `blink(7) >> self` |
| `push` / `pull` | `push(1.2) >> target` |
| `shield` | `shield(4) >> self` |
| `raycast` / `stun` | `raycast(20) >> target`, `stun(2s) >> target` |
| `chain` | `chain(18, hits=3) { damage(4) >> target }` |
| `ignite` / `glow` | `ignite(80) >> target`, `glow(120) >> target` |
| `swap` / `root` | `swap >> target`, `root(60) >> target` |
| `explosion` | `explosion(1.5, fire=true) >> location` |
| `cleanse` / `launch` | `cleanse >> self`, `launch(1.2) >> target` |
| `title` | `title(title="&6Удар!", subtitle="&7Крит") >> self` |
| `command` | `command("say %player%") >> self` |
| `give` / `summon` | `give(DIAMOND, count=1) >> self` |
| `money` | `give_money(10) >> self` (Vault) |

### EffectLib — блок `fx { }`

```dsc
fx >> target {
  shape helix
  particle flame
  at self
  helix { radius 1.5 strands 2 particles 50 }
  timing { iterations 30 period 2 }
  base { visible-range 64 offset 0,1,0 }
}
```

Короткая форма: `fx(sphere, particle=heart, radius=1.2, particles=20) >> self`

### ModelEngine — блок `vfx { }` (DSC_MEG)

```dsc
projectile(SNOWBALL, speed=0.1) >> target {
  tick {
    vfx {
      position projectile
      model-name vfx_flamethrower
      animations { skill { loop true speed 1.0 } }
    }
  }
}
```

---

## Триггеры каста (`key`)

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

---

## Цели (`target`)

| Значение | Поведение |
|----------|-----------|
| `self` | Кастер |
| `entity` / `mob` | Существо в прицеле |
| `block` | Блок в прицеле |
| `none` | Точка по взгляду + range |

---

## Пассивки

Только **скрипт + permission**. Команд выдачи нет. Не кастуются через `/dsc cast`, give, bind.

### Поля пассивки

| Поле | Алиасы | Назначение |
|------|--------|------------|
| `on` | `passive-trigger`, `ptrigger`, `trigger`* | **Событие** игры |
| `key` | `passive-key`, `pkey` | **Клавиша** (те же значения, что в таблице триггеров) |
| `presses` | `combo`, `press-count` | Сколько раз подряд нажать (только для `key`, по умолчанию `1`) |
| `press-window` | `press-interval` | Макс. пауза между нажатиями: `2s`, `40t` (по умолчанию `40t` при `presses > 1`) |
| `interval` | `passive-interval` | Период тиков для `on interval`: `60t`, `3s` |

\* `trigger` — устаревший общий алиас.  
Не путать `event: damage` (свойство) с `start { }` / `on cast { }` (chain).

### Событие (`event: damage`)

```dsc
passive passive_thorns {
  meta {
    name: "&cШипы"
    event: damage
    cd: 2
    target: none
    range: 16
    perm: divizionsc.def.passive_thorns
  }

  cast {
    require(has-target) >> self
    @pain(dmg=3) >> target
    particles(CRIT, count=8) >> target
  }
}
```

| `on …` | Когда |
|--------|-------|
| `damage` | Получил урон |
| `attack` | Нанёс урон (`hit` — тот же смысл) |
| `kill` | Убил существо |
| `death` | Смерть |
| `join` | Вход на сервер |
| `break` | Сломал блок |
| `fall` | Урон от падения |
| `interval` | Периодически — нужен `interval 60t` |

Кулдаун `cd` ограничивает частоту. В контексте урона: `{damage}`, `{event_damage}`.

### Клавиша (`key: rclick`)

```dsc
passive passive_parry {
  meta {
    name: "&9Парирование"
    key: rclick
    cd: 3
    target: self
    perm: divizionsc.def.passive_parry
  }

  cast {
    shield(4) >> self
    particles(CRIT, count=10) >> self
  }
}
```

Срабатывает после каста с предмета/bind, если тот не перехватил нажатие.  
`attack` в `on`/`trigger` — всегда **событие** (урон), не ЛКМ. Для ЛКМ: `key lclick`.

### Комбо (`presses` + `press-window`)

```dsc
passive passive_dash_combo {
  meta {
    name: "&bКомбо-рывок"
    key: shift
    presses: 3
    press-window: 2s
    cd: 5
    target: self
    perm: divizionsc.def.passive_dash_combo
  }

  cast {
    dash(1.8) >> self
    message("&bРывок!") >> self
  }
}
```

Три нажатия Shift подряд, пауза между нажатиями не больше 2 сек. Иначе счётчик сбрасывается.

Активна при: `perm` def · `divizionsc.def.*` · `divizionsc.passive.*`

---

## Условия

Типы: `health`, `distance`, `permission`, `holding`, `sneaking`, `on-ground`, `has-target`, `world`, `region` (WG), `money` (Vault), `variable`, `chance`.

Операторы: `< <= > >= == !=`, `!`/`not`, проценты (`health > 50%`).

```dsc
require(has-target) >> self
require(health > 4) >> self
if (distance < 10) >> target { damage(8) >> target } else { message("&7Далеко") >> self }
chance(30%) >> target { lightning() >> target }
```

---

## Плейсхолдеры

| Токен | Значение |
|-------|----------|
| `{caster_health}` | HP кастера |
| `{target_distance}` | Дистанция до цели |
| `{def_id}` | id def |
| `{var_NAME}` | Переменная |
| `{random}` | 0..1 |
| `{damage}` | Урон (пассивки) |
| `%player%` | Имя (в `cmd`) |
| `%def%` | id (в `cmd`) |
| `%papi_placeholder%` | PlaceholderAPI |

---

## Skill Bar

- Открыть: `/dsc skills` или **F** (`open-on-swap-hands`)
- Клик слот 1–9 → клик скилл = привязка
- **Shift+клик** скилл = первый свободный слот
- **Shift+клик** слот = очистить
- Привязки в БД, синхрон между серверами (MySQL)

---

## config.yml (ключевое)

```yaml
locale: ru
defs-folder: defs
default-range: 32.0
cast-permission-prefix: divizionsc.def.
cooldown-messages: true
cast-messages: false

op-bypass:
  enabled: true
  ignore-cooldown: true
  ignore-mana: true

mana:
  enabled: true
  default-max: 100.0

skill-bar:
  enabled: true
  open-on-swap-hands: true
  require-empty-hand: false
  bind-triggers: [right_click, 1, 2, 3, 4, 5, 6, 7, 8, 9]

database:
  type: sqlite          # sqlite | mysql | mariadb
  sqlite-file: data.db
```

---

## Интеграции (soft-depend)

| Плагин | Что даёт |
|--------|----------|
| PlaceholderAPI | `%placeholder%` в строках |
| Vault | `money`, условие `balance` |
| WorldGuard | условие `region` |
| EffectLib | `fx { }` / `fx(...)` (встроен в JAR) |
| DSC_MEG + ModelEngine | `vfx { }` блок |
| DSC_MM + MythicMobs | `dscdef{def=fireball} @target` |

---

## MythicMobs (DSC_MM)

```yaml
Skills:
  - dscdef{def=fireball} @target
  - dscdef{def=heal_vfx;helper=true} @self
```

Параметры: `def`/`ability`/`id`, `helper`, `caster` (`self`, `target`, `trigger`, `auto`).

---

## API (кратко)

```java
DivizionSCApi.cast(player, "heal");
DivizionSCApi.findDef("fireball");
DivizionSCApi.reloadDefs();
```

События: `DefPreCastEvent` (Cancellable), `DefCastEvent`, `DefLoadEvent`.

---

## Частые проблемы

| Проблема | Решение |
|----------|---------|
| Def не грузится | `/dsc validate`, консоль, `debug-load-errors: true` |
| Нет каста | `key`, permission, кулдаун, не `helper`/`passive` |
| Нет цели | `target entity` + цель в range |
| Пассивка молчит | `passive true`, `on damage` или `key shift`, выдать `perm` |
| `on damage` не парсится | Используй `event: damage` в `meta { }`, не путать с `start { }` |
| `use()` не парсится | Только `@module(args)` |
| Дубли id | Последний файл побеждает |

---

## Примеры

| Файл | Содержимое |
|------|------------|
| **`defs-bricks.dsc`** | Кирпичи: `fx_spark`, `strike`, `smite`, `heavy_strike` (extends), … |
| **`defs-examples.dsc`** | `heal`, `fireball`, `smite_basic`, `tri_combo`, `heavy_sword`, `passive_thorns`, … |
| **`defs-advanced.dsc`** | `arcane_meteor`, `boss_enrage`, `chain_lightning`, `passive_lifesteal`, … |
| **`defs-fx-examples.dsc`** | EffectLib / ModelEngine VFX |

```
/dsc reload
/lp user <ник> permission set divizionsc.def.passive_thorns true
```

---

Подробная документация: [README.md](README.md)
