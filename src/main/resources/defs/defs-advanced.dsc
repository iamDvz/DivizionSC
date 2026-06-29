// =====================================================================
//  DivizionSC — defs-advanced.dsc
//  Сложные способности: задержки, зоны, циклы, условия, переменные,
//  многофазные боссы и пассивки. Все примеры компилируются «как есть».
// =====================================================================
//
//  Дополнительно к базе (см. defs-examples.dsc):
//    after(30t) { }          — отложенный блок (t = тики, s = секунды)
//    area(R) { }             — применить вложенное к каждой цели в радиусе R
//    loop(times=N, interval=T) { }  — повторить N раз с паузой T тиков
//    aura(radius=R, duration=D, interval=T) { }  — периодическая аура
//    if (cond) { } else { }  — ветвление (when — псевдоним if)
//    chance (30%) { }        — вероятностный блок
//    set(name, выражение)    — переменная каста; формулы exp4j (health, level…)
//    require <условие>       — прерывает каст, если ложно
//  Условия: has-target · health < 40% · distance < 6 · sneaking · on-ground …
// =====================================================================


// ---------------------------------------------------------------------
//  МОДУЛИ
// ---------------------------------------------------------------------

module meteor_trail {
  effects {
    particle(flame, count=40) >> location
    particle(lava, count=20) >> location
  }
}

module burn(dmg) {
  target: entity
  effects {
    damage($dmg) >> target
    potion(wither, duration=3s, amplifier=0) >> target
    particle(flame, count=8) >> target
  }
}

module shock(dmg) {
  target: entity
  effects {
    damage($dmg) >> target
    particle(electric_spark, count=20) >> target
    sound(lightning_bolt_impact, volume=0.7)
  }
}


// ---------------------------------------------------------------------
//  1. Метеор: каст → след → через 1.5с урон по зоне в точке падения
// ---------------------------------------------------------------------
ability arcane_meteor {
  name: "&5Метеор"
  cooldown: 14
  key: rclick
  target: none
  range: 40
  mana: 25
  item: FIRE_CHARGE | &5Метеор | &7Призывает метеор в точку взгляда

  cast {
    sound(blaze_shoot, volume=1.2, pitch=0.6)
    @meteor_trail >> target
    after(30t) {
      area(4) {
        damage(10) >> target
        @burn(3) >> target
      }
      particle(explosion, count=6) >> location
      sound(generic_explode, volume=1.5)
    }
  }
}


// ---------------------------------------------------------------------
//  2. Ярость босса: бафы только при низком здоровье (if/else)
// ---------------------------------------------------------------------
ability boss_enrage {
  name: "&4Ярость"
  cooldown: 30
  key: cmd
  target: self

  cast {
    if (health < 40%) {
      potion(strength, duration=15s, amplifier=2)
      potion(speed, duration=15s, amplifier=1)
      potion(resistance, duration=15s, amplifier=1)
      message("&4Босс приходит в ярость!")
    } else {
      message("&7Слишком много здоровья для ярости")
    }
    particle(angry_villager, count=30) >> self
    sound(wither_spawn, volume=1.0)
  }
}


// ---------------------------------------------------------------------
//  3. Цепная молния: raycast сквозь врагов
// ---------------------------------------------------------------------
ability chain_lightning {
  name: "&bЦепная молния"
  cooldown: 9
  key: rclick
  target: none
  range: 20
  mana: 15
  item: PRISMARINE_SHARD | &bЦепная молния

  cast {
    chain(18, hits=3, hit_radius=1.5) {
      @shock(4) >> target
      lightning >> target
    }
    sound(lightning_bolt_impact, volume=0.8)
  }
}


// ---------------------------------------------------------------------
//  4. Метеоритный дождь: цикл из 5 ударов по зоне
// ---------------------------------------------------------------------
ability meteor_storm {
  name: "&cМетеоритный дождь"
  cooldown: 40
  key: cmd
  target: none
  range: 30
  mana: 50

  cast {
    message("&cНебеса разверзлись!")
    loop(times=5, interval=10) {
      area(5) {
        damage(6) >> target
        particle(flame, count=20) >> target
      }
      sound(generic_explode, volume=1.0, pitch=0.8)
    }
  }
}


// ---------------------------------------------------------------------
//  5. Аура вампира: периодически бьёт врагов и лечит себя
// ---------------------------------------------------------------------
ability vampire_aura {
  name: "&4Аура вампира"
  cooldown: 25
  key: shift
  target: self
  mana: 20
  item: WITHER_ROSE | &4Аура вампира

  cast {
    aura(radius=5, duration=6s, interval=20) {
      damage(2) >> target
      heal(1) >> self
      particle(damage_indicator, count=3) >> target
    }
    sound(wither_ambient, volume=0.8)
  }
}


// ---------------------------------------------------------------------
//  6. Землетрясение: зона урона + оглушение + подброс
// ---------------------------------------------------------------------
ability earthquake {
  name: "&6Землетрясение"
  cooldown: 18
  key: rclick
  target: none
  range: 8
  mana: 20
  item: NETHERITE_PICKAXE | &6Землетрясение

  cast {
    area(7) {
      damage(8) >> target
      stun(40) >> target
    }
    velocity(0, 0.5, 0)
    particle(campfire_cosy_smoke, count=60) >> self
    sound(generic_explode, volume=1.4, pitch=0.5)
  }
}


// ---------------------------------------------------------------------
//  7. Телепорт-комбо: рывок к цели, удар, отскок назад
// ---------------------------------------------------------------------
ability teleport_combo {
  name: "&dТелепорт-комбо"
  cooldown: 12
  key: rclick
  target: entity
  range: 15
  mana: 10
  item: CHORUS_FRUIT | &dТелепорт-комбо

  cast {
    require has-target
    blink(10)
    @shock(5) >> target
    after(5t) {
      blink(6)
    }
  }
}


// ---------------------------------------------------------------------
//  8. Переменные и формулы: урон = base*2 + уровень игрока
//     set вычисляет выражение (exp4j), {var_...} — в сообщениях
// ---------------------------------------------------------------------
ability power_strike {
  name: "&cСиловой удар"
  cooldown: 8
  key: rclick
  target: entity
  range: 5
  item: IRON_AXE | &cСиловой удар

  cast {
    require has-target
    set(base, 4)
    set(total, base * 2 + level)
    damage(total) >> target
    message("&cУрон: &e{var_total}")
    particle(crit, count=20) >> target
  }
}


// ---------------------------------------------------------------------
//  9. Гравитационный колодец: притягивает врагов в радиусе
// ---------------------------------------------------------------------
ability gravity_well {
  name: "&8Гравитация"
  cooldown: 15
  key: rclick
  target: none
  range: 12
  mana: 15
  item: ECHO_SHARD | &8Гравитация

  cast {
    area(8) {
      pull(1.2) >> target
      particle(portal, count=10) >> target
    }
    sound(warden_sonic_boom, volume=0.6)
  }
}


// ---------------------------------------------------------------------
//  10. Святой удар: ближе цель — больше урон (условие по дистанции)
// ---------------------------------------------------------------------
ability holy_smite {
  name: "&eСвятой удар"
  cooldown: 10
  key: rclick
  target: entity
  range: 20
  mana: 12
  item: GOLDEN_SWORD | &eСвятой удар

  cast {
    require has-target
    if (distance < 6) {
      damage(12) >> target
      message("&eБлизкий удар!")
    } else {
      damage(6) >> target
      lightning(fx=true) >> target
    }
  }
}


// ---------------------------------------------------------------------
//  11. Залп босса: цикл из снарядов с эффектом при попадании
// ---------------------------------------------------------------------
ability boss_volley {
  name: "&5Залп"
  cooldown: 20
  key: cmd
  target: none

  cast {
    loop(times=3, interval=8) {
      projectile(FIREBALL, speed=1.2) {
        hit {
          @burn(4)
        }
      }
      sound(blaze_shoot, volume=0.8)
    }
  }
}


// ---------------------------------------------------------------------
//  12. ПАССИВКА: вампиризм — лечит при атаке (on: attack)
// ---------------------------------------------------------------------
passive passive_lifesteal {
  name: "&4Вампиризм"
  on: attack
  cooldown: 1
  target: entity
  perm: divizionsc.def.passive_lifesteal

  cast {
    heal(2)
    particle(heart, count=4) >> self
  }
}


// ---------------------------------------------------------------------
//  13. ПАССИВКА: берсерк — при получении урона на низком HP даёт силу
// ---------------------------------------------------------------------
passive passive_berserk {
  name: "&cБерсерк"
  on: damage
  cooldown: 5
  target: self
  perm: divizionsc.def.passive_berserk

  cast {
    if (health < 30%) {
      potion(strength, duration=6s, amplifier=1)
      particle(angry_villager, count=10) >> self
    }
  }
}


// ---------------------------------------------------------------------
//  14. Солнечная вспышка: взрыв + поджог в зоне
// ---------------------------------------------------------------------
ability solar_flare {
  name: "&6Солнечная вспышка"
  cooldown: 16
  key: rclick
  target: none
  range: 24
  mana: 18
  item: GLOWSTONE | &6Солнечная вспышка

  cast {
    explosion(0, fire=true) >> location
    area(5) {
      ignite(100) >> target
      damage(5) >> target
    }
    particle(flame, count=40) >> location
    sound(generic_explode, volume=0.8, pitch=1.2)
  }
}


// ---------------------------------------------------------------------
//  15. Теневой обмен: swap + удар + root
// ---------------------------------------------------------------------
ability shadow_swap {
  name: "&5Теневой обмен"
  cooldown: 14
  key: shift
  target: entity
  range: 12
  mana: 12
  item: ENDER_EYE | &5Теневой обмен

  cast {
    require has-target
    swap >> target
    @shock(6) >> target
    root(50) >> target
    particle(portal, count=30) >> self
    sound(enderman_teleport)
  }
}


// ---------------------------------------------------------------------
//  16. Метка охотника: glow + title на цель
// ---------------------------------------------------------------------
ability hunters_mark {
  name: "&eМетка охотника"
  cooldown: 8
  key: rclick
  target: entity
  range: 32
  item: SPYGLASS | &eМетка

  cast {
    require has-target
    glow(160) >> target
    title(title="&e☠ МЕТКА", subtitle="&7Цель отмечена") >> target
    particle(glow, count=12) >> target
  }
}


// ---------------------------------------------------------------------
//  17. Очищение: снять дебаффы + лечение в зоне
// ---------------------------------------------------------------------
ability cleansing_wave {
  name: "&aОчищающая волна"
  cooldown: 22
  key: rclick
  target: self
  mana: 15
  item: TOTEM_OF_UNDYING | &aОчищение

  cast {
    cleanse >> self
    heal(4)
    area(6) {
      cleanse >> target
      heal(2) >> target
      particle(happy_villager, count=6) >> target
    }
    sound(beacon_activate, volume=0.7)
  }
}


// ---------------------------------------------------------------------
//  18. Небесный рывок: launch + dash
// ---------------------------------------------------------------------
ability sky_launch {
  name: "&bНебесный рывок"
  cooldown: 10
  key: space
  target: self
  item: ELYTRA | &bНебесный рывок

  cast {
    launch(1.1)
    after(3t) {
      dash(0.8)
    }
    particle(cloud, count=20) >> self
    sound(enderdragon_flap, volume=0.4)
  }
}


// ---------------------------------------------------------------------
//  19. ПАССИВКА: страж — щит при низком HP
// ---------------------------------------------------------------------
passive passive_guardian {
  name: "&9Страж"
  on: damage
  cooldown: 12
  target: self
  perm: divizionsc.def.passive_guardian

  cast {
    if (health < 25%) {
      shield(6)
      cleanse >> self
      title(actionbar="&9Щит стража!") >> self
      particle(totem_of_undying, count=8) >> self
    }
  }
}
