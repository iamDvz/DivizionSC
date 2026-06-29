// =====================================================================
//  DivizionSC — defs-examples.dsc
//  Базовые способности: от самых простых к составным.
//  Синтаксис .dsc v2. Перезагрузка: /dsc reload · Выдать: /dsc give <id>
// =====================================================================
//
//  ШПАРГАЛКА ПО СИНТАКСИСУ
//  -----------------------
//  ability <id> { ... }   — кастуемая способность
//  module  <id> { ... }   — helper-блок, вызывается через @<id> (сам не кастуется)
//  module  <id>(a, b) { } — module с параметрами ($a, $b в теле)
//
//  Свойства пишутся как `ключ: значение` прямо в блоке (можно и в meta { }):
//    name / cooldown(cd) / key(trigger) / target(tgt) / range(rng) /
//    perm(permission) / mana(mp/cost) / item / desc
//
//  Тело: cast { ... } (псевдонимы: do, effects)
//  Эффекты — вызовы вида verb(args):  heal(6), damage(5), sound(blaze_shoot)
//  Маршрут цели:  effect(args) >> target   |   effect(args) >> from >> to
//      self/caster · target/entity · block/location · eyes
//  Цепочки фаз:   on cast { @mod }   on hit { @mod }   on done { @mod }
//                 (принимают ТОЛЬКО вызовы @module)
//  Блоки:  after(20t) { } · chance (30%) { } · if (cond) { } else { } ·
//          area(6) { } · loop(times=3, interval=10) { } · projectile(...) { hit { } }
// =====================================================================


// ---------------------------------------------------------------------
//  МОДУЛИ (переиспользуемые блоки)
// ---------------------------------------------------------------------

module spark {
  effects {
    particle(electric_spark, count=25)
    sound(lightning_bolt_impact, volume=0.6, pitch=1.4)
  }
}

module hearts {
  effects {
    particle(heart, count=12, offset=0.6) >> self
  }
}

// параметр $dmg подставляется при вызове: @pain(8) или @pain(dmg=8)
module pain(dmg) {
  target: entity
  effects {
    damage($dmg) >> target
    particle(crit, count=12) >> target
  }
}


// ---------------------------------------------------------------------
//  1. Простейшее: вылечить себя
// ---------------------------------------------------------------------
ability heal {
  name: "&aЛечение"
  cooldown: 8
  key: rclick
  target: self
  item: GOLDEN_APPLE | &aЛечение | &7ПКМ — восстановить здоровье

  cast {
    heal(6)
    sound(player_levelup)
    @hearts
  }
}


// ---------------------------------------------------------------------
//  2. Урон по существу в прицеле (+ проверка цели)
// ---------------------------------------------------------------------
ability smite_basic {
  name: "&eУдар"
  cooldown: 4
  key: rclick
  target: entity
  range: 24
  item: IRON_SWORD | &eУдар

  cast {
    require has-target
    damage(5) >> target
    @spark >> target
  }
}


// ---------------------------------------------------------------------
//  3. Молния по цели (требуется обязательная цель)
// ---------------------------------------------------------------------
ability instant_smite {
  name: "&bКара"
  cooldown: 6
  key: rclick
  target: entity
  range: 30
  item: TRIDENT | &bКара

  cast {
    require has-target
    lightning >> target
    damage(4) >> target
  }
}


// ---------------------------------------------------------------------
//  4. Снаряд с эффектом при попадании + цепочки фаз
// ---------------------------------------------------------------------
ability fireball {
  name: "&cОгненный шар"
  cooldown: 6
  key: rclick
  target: block
  range: 32
  item: BLAZE_ROD | &cОгненный шар

  cast {
    sound(blaze_shoot)
    projectile(FIREBALL, speed=1.4) {
      hit {
        @pain(6)
        particle(flame, count=30)
      }
    }
  }
  on cast { @spark }
  on done { @hearts }
}


// ---------------------------------------------------------------------
//  5. Рывок по направлению взгляда (shift)
// ---------------------------------------------------------------------
ability dash_strike {
  name: "&fРывок"
  cooldown: 5
  key: shift
  target: none
  item: FEATHER | &fРывок

  cast {
    dash(1.6)
    sound(enderdragon_flap, volume=0.5)
    particle(cloud, count=15) >> self
  }
}


// ---------------------------------------------------------------------
//  6. Телепорт по взгляду (blink)
// ---------------------------------------------------------------------
ability blink {
  name: "&dМерцание"
  cooldown: 7
  key: rclick
  target: none
  item: ENDER_PEARL | &dМерцание

  cast {
    blink(8)
    particle(portal, count=40) >> self
    sound(enderman_teleport)
  }
}


// ---------------------------------------------------------------------
//  7. Прыжок (вектор скорости x y z)
// ---------------------------------------------------------------------
ability leap {
  name: "&aПрыжок"
  cooldown: 4
  key: space
  target: self
  item: RABBIT_FOOT | &aПрыжок

  cast {
    velocity(0, 0.9, 0)
    sound(slime_squish)
  }
}


// ---------------------------------------------------------------------
//  8. Зона эффекта вокруг точки (area)
// ---------------------------------------------------------------------
ability frost_nova {
  name: "&bЛедяная вспышка"
  cooldown: 12
  key: rclick
  target: none
  range: 8
  item: PACKED_ICE | &bЛедяная вспышка

  cast {
    sound(glass_break, volume=1.2, pitch=0.7)
    area(6) {
      potion(slowness, duration=4s, amplifier=2) >> target
      particle(snowflake, count=10) >> target
    }
  }
}


// ---------------------------------------------------------------------
//  9. Комбо урон + лечение (переиспользуем модули)
// ---------------------------------------------------------------------
ability lifesteal {
  name: "&4Похищение жизни"
  cooldown: 8
  key: rclick
  target: entity
  range: 6
  item: NETHERITE_SWORD | &4Похищение жизни

  cast {
    require has-target
    damage(6) >> target
    heal(3)
    @hearts
  }
}


// ---------------------------------------------------------------------
//  10. Бафы себе (несколько зелий)
// ---------------------------------------------------------------------
ability war_cry {
  name: "&6Боевой клич"
  cooldown: 20
  key: shift
  target: self
  item: GOLDEN_HORSE_ARMOR | &6Боевой клич

  cast {
    potion(strength, duration=8s, amplifier=1)
    potion(speed, duration=8s, amplifier=0)
    sound(raid_horn, volume=1.0)
    particle(angry_villager, count=15) >> self
  }
}


// ---------------------------------------------------------------------
//  11. Призыв существ
// ---------------------------------------------------------------------
ability summon_wolf {
  name: "&7Призыв волков"
  cooldown: 30
  key: rclick
  target: none
  item: BONE | &7Призыв волков

  cast {
    summon(wolf, count=2)
    sound(wolf_howl)
  }
}


// ---------------------------------------------------------------------
//  12. Задержка (after) + шанс (chance)
// ---------------------------------------------------------------------
ability thunder_combo {
  name: "&eГромовое комбо"
  cooldown: 10
  key: rclick
  target: none
  item: LIGHTNING_ROD | &eГромовое комбо

  cast {
    @spark
    after(10t) {
      lightning(fx=true) >> self
      sound(thunder)
    }
    chance (35%) {
      lightning >> self
    }
  }
}


// ---------------------------------------------------------------------
//  13. ПАССИВКА: шипы — отвечают уроном при получении урона
//      passive-блок: триггер `on: damage`, кулдаун защищает от спама
// ---------------------------------------------------------------------
passive passive_thorns {
  name: "&8Шипы"
  on: damage
  cooldown: 2
  target: entity
  perm: divizionsc.def.passive_thorns

  cast {
    require has-target
    @pain(3)
  }
}


// ---------------------------------------------------------------------
//  14. Метка цели — glow на враге
// ---------------------------------------------------------------------
ability mark_target {
  name: "&eМетка"
  cooldown: 5
  key: lclick
  target: entity
  range: 28
  item: GLOW_INK_SAC | &eМетка

  cast {
    require has-target
    glow(120) >> target
    message("&eЦель отмечена") >> self
  }
}


// ---------------------------------------------------------------------
//  15. Огненный взрыв — explosion без разрушения блоков
// ---------------------------------------------------------------------
ability fire_blast {
  name: "&cОгненный взрыв"
  cooldown: 9
  key: rclick
  target: none
  range: 20
  item: FIRE_CHARGE | &cОгненный взрыв

  cast {
    explosion(1.5, fire=true) >> location
    area(4) {
      ignite(60) >> target
    }
    particle(lava, count=16) >> location
  }
}
