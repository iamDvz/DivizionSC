# DSC — канонический синтаксис

Единый стандарт `.dsc` v2. Примеры: `defs-bricks.dsc`, `defs-examples.dsc`.  
Шпаргалка: [SHPARGALKA.md](SHPARGALKA.md)

DivizionSC **вдохновлён** MagicSpells (модули, фазы, VFX), но **свой язык** — не копия YAML MS.

## 1. Файл

```
defs/defs-*.dsc     — только такие файлы загружаются
defs-bricks.dsc     — module-кирпичи (глобальный индекс)
defs-examples.dsc   — способности из @кирпичей
```

Комментарии: `//` и `#`. Id блоков — в нижнем регистре.

---

## 2. Блоки

```dsc
ability id { … }              // кастуемая способность
passive id { … }              // пассивка
module id { … }               // helper, только через @id
module id(a, b=3) extends parent { … }
```

| Устарело | Канон |
|----------|--------|
| `def` | `ability` |
| `effect` | `module` |
| `do { }` | `cast { }` |
| `use()` / `call()` | `@module` |

---

## 3. Свойства

**Канон:** `ключ: значение` (можно несколько на строке).

```dsc
ability heal {
  name: "&aЛечение"
  cooldown: 8
  key: rclick
  target: self
  range: 24
  perm: divizionsc.def.heal
  item: GOLDEN_APPLE | &aЛечение | &7лор
}
```

Короткая форма `cd 8` (одно на строку) — поддерживается, но в примерах не используется.

| Свойство | Алиасы |
|----------|--------|
| `cooldown` | `cd` |
| `key` | `trigger` |
| `target` | `tgt`, `to` |
| `range` | `rng` |
| `permission` | `perm` |
| `mana` | `mp`, `cost` |
| `on` | `event` (пассивка) |

---

## 4. Секции

| Секция | Алиас | Назначение |
|--------|-------|------------|
| `cast { }` | `do`, `effects` | Основное тело |
| `effects { }` | — | Тело module |
| `on cast { }` | `start` | До cast — **только `@module`** |
| `on hit { }` | `hit` | Попадание projectile — **только `@module`** |
| `on done { }` | `done`, `complete` | После cast — **только `@module`** |

Порядок: **on cast → cast → on done**. Hit — от `projectile { hit { } }`.

---

## 5. Эффекты

**Канон:** `verb(аргументы) >> маршрут`

```dsc
heal(6) >> self
damage(5) >> target
sound(blaze_shoot, volume=0.8) >> self
require(has-target)
lightning >> target
```

Короткие формы (совместимость): `heal 6`, `damage 5`, `require has-target`, `lightning`.

---

## 6. Маршрут

**Канон:** `>> target` · `>> self` · `>> location`

| Алиас | Пример |
|-------|--------|
| `->` | `damage(5) -> target` |
| `at` | `damage(5) at target` |
| `to` | `damage(5) to target` |

Два токена: `effect >> self >> target` (откуда → куда).

Токены: `self`/`caster` · `target`/`entity` · `block`/`location` · `eyes`.

**Всегда указывайте маршрут явно** в своих defs — так код читается однозначно.

---

## 7. Модули (@)

```dsc
@strike(5) >> target
@pain(dmg=8) >> target
@pain 8 >> target
@strike(5) with slow_pack >> target
@pain(3), @fx_spark >> target
@pain(3) + @fx_spark >> target

stack >> target {
  @strike(4)
  @slow_pack
}
```

| Конструкция | Назначение |
|-------------|------------|
| `@id(args) >> route` | Базовый вызов |
| `@a, @b` / `@a + @b` | Несколько кирпичей, общий маршрут |
| `with` | `@main with extra >> route` |
| `stack { }` | Группа с общим маршрутом |
| `extends` | `module x extends y { }` — наследование |
| `a=3` | Дефолт параметра → `@id` без аргументов |

Module из **любого** `defs-*.dsc` доступны **везде** (глобальный индекс).

---

## 8. Блочные конструкции

```dsc
after(20t) { … }              // t = тики, s = секунды
if (distance < 8) { … } else { … }
when has-target { … }         // if без скобок для простых условий
chance(35%) { … }             // или chance 35% { … }
area(6) { … }
loop(times=3, interval=10) { … }
projectile(FIREBALL, speed=1.4) {
  hit { @strike(6) >> target }
}
```

---

## 9. Шаблон способности

```dsc
ability my_spell {
  name: "&aМоя способность"
  cooldown: 5
  key: rclick
  target: entity
  range: 24
  perm: divizionsc.def.my_spell

  cast {
    require(has-target)
    @strike(6) >> target
    @mend(2) >> self
  }
}
```

---

## 10. Шаблон module-кирпича

```dsc
module strike(dmg) {
  target: entity
  effects {
    require(has-target)
    damage($dmg) >> target
    @fx_spark >> target
  }
}

module heavy_strike(dmg) extends strike {
  effects {
    @slow_pack >> target
  }
}
```
