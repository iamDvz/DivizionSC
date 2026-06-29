// =====================================================================
//  defs-advanced.dsc — сложные способности
//  after · area · loop · aura · if/when · chance · set · chain · projectile
//  Канон: SYNTAX.md · база: defs-examples.dsc
// =====================================================================


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
    sound(entity_lightning_bolt_impact, volume=0.7) >> target
  }
}


// --- Задержка + зона ---------------------------------------------------

ability arcane_meteor {
  name: "&5Метеор"
  cooldown: 14
  key: rclick
  target: none
  range: 40
  mana: 25
  item: FIRE_CHARGE | &5Метеор

  cast {
    sound(entity_blaze_shoot, volume=1.2, pitch=0.6) >> self
    @meteor_trail >> location
    after(30t) {
      area(4) {
        damage(10) >> target
        @burn(3) >> target
      }
      particle(explosion, count=6) >> location
      sound(entity_generic_explode, volume=1.5) >> location
    }
  }
}


// --- Условия -----------------------------------------------------------

ability boss_enrage {
  name: "&4Ярость"
  cooldown: 30
  key: cmd
  target: self

  cast {
    if (health < 40%) {
      potion(strength, duration=15s, amplifier=2) >> self
      potion(speed, duration=15s, amplifier=1) >> self
      potion(resistance, duration=15s, amplifier=1) >> self
      message("&4Босс приходит в ярость!") >> self
    } else {
      message("&7Слишком много здоровья") >> self
    }
    particle(villager_angry, count=30) >> self
    sound(entity_wither_spawn, volume=1.0) >> self
  }
}

ability holy_smite {
  name: "&eСвятой удар"
  cooldown: 10
  key: rclick
  target: entity
  range: 20
  mana: 12
  item: GOLDEN_SWORD | &eСвятой удар

  cast {
    require(has-target)
    if (distance < 6) {
      damage(12) >> target
      message("&eБлизкий удар!") >> self
    } else {
      damage(6) >> target
      lightning(fx=true) >> target
    }
  }
}


// --- chain · loop · aura -----------------------------------------------

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
      lightning() >> target
    }
    sound(entity_lightning_bolt_impact, volume=0.8) >> self
  }
}

ability meteor_storm {
  name: "&cМетеоритный дождь"
  cooldown: 40
  key: cmd
  target: none
  range: 30
  mana: 50

  cast {
    message("&cНебеса разверзлись!") >> self
    loop(times=5, interval=10) {
      area(5) {
        damage(6) >> target
        particle(flame, count=20) >> target
      }
      sound(entity_generic_explode, volume=1.0, pitch=0.8) >> self
    }
  }
}

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
    sound(entity_wither_ambient, volume=0.8) >> self
  }
}


// --- Зона · переменные · комбо -----------------------------------------

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
    velocity(0, 0.5, 0) >> self
    particle(campfire_cosy_smoke, count=60) >> self
    sound(entity_generic_explode, volume=1.4, pitch=0.5) >> self
  }
}

ability teleport_combo {
  name: "&dТелепорт-комбо"
  cooldown: 12
  key: rclick
  target: entity
  range: 15
  mana: 10
  item: CHORUS_FRUIT | &dТелепорт-комбо

  cast {
    require(has-target)
    blink(10) >> self
    @shock(5) >> target
    after(5t) {
      blink(6) >> self
    }
  }
}

ability power_strike {
  name: "&cСиловой удар"
  cooldown: 8
  key: rclick
  target: entity
  range: 5
  item: IRON_AXE | &cСиловой удар

  cast {
    require(has-target)
    set(base, 4) >> self
    set(total, base * 2 + level) >> self
    damage(total) >> target
    message("&cУрон: &e{var_total}") >> self
    particle(crit, count=20) >> target
  }
}

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
    sound(entity_warden_sonic_boom, volume=0.6) >> self
  }
}

ability boss_volley {
  name: "&5Залп"
  cooldown: 20
  key: cmd
  target: none

  cast {
    loop(times=3, interval=8) {
      projectile(FIREBALL, speed=1.2) {
        hit {
          @burn(4) >> target
        }
      }
      sound(entity_blaze_shoot, volume=0.8) >> self
    }
  }
}

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
      ignite(ticks=100) >> target
      damage(5) >> target
    }
    particle(flame, count=40) >> location
    sound(entity_generic_explode, volume=0.8, pitch=1.2) >> location
  }
}

ability shadow_swap {
  name: "&5Теневой обмен"
  cooldown: 14
  key: shift
  target: entity
  range: 12
  mana: 12
  item: ENDER_EYE | &5Теневой обмен

  cast {
    require(has-target)
    swap() >> target
    @shock(6) >> target
    root(50) >> target
    particle(portal, count=30) >> self
    sound(entity_enderman_teleport) >> self
  }
}

ability hunters_mark {
  name: "&eМетка охотника"
  cooldown: 8
  key: rclick
  target: entity
  range: 32
  item: SPYGLASS | &eМетка

  cast {
    require(has-target)
    glow(duration=160) >> target
    title(title="&e☠ МЕТКА", subtitle="&7Цель отмечена") >> target
    particle(glow, count=12) >> target
  }
}

ability cleansing_wave {
  name: "&aОчищающая волна"
  cooldown: 22
  key: rclick
  target: self
  mana: 15
  item: TOTEM_OF_UNDYING | &aОчищение

  cast {
    cleanse() >> self
    heal(4) >> self
    area(6) {
      cleanse() >> target
      heal(2) >> target
      particle(happy_villager, count=6) >> target
    }
    sound(block_beacon_activate, volume=0.7) >> self
  }
}

ability sky_launch {
  name: "&bНебесный рывок"
  cooldown: 10
  key: space
  target: self
  item: ELYTRA | &bНебесный рывок

  cast {
    launch(1.1) >> self
    after(3t) {
      dash(0.8) >> self
    }
    particle(cloud, count=20) >> self
    sound(entity_ender_dragon_flap, volume=0.4) >> self
  }
}


// --- Пассивки ----------------------------------------------------------

passive passive_lifesteal {
  name: "&4Вампиризм"
  on: attack
  cooldown: 1
  target: entity
  perm: divizionsc.def.passive_lifesteal

  cast {
    heal(2) >> self
    particle(heart, count=4) >> self
  }
}

passive passive_berserk {
  name: "&cБерсерк"
  on: damage
  cooldown: 5
  target: self
  perm: divizionsc.def.passive_berserk

  cast {
    if (health < 30%) {
      potion(strength, duration=6s, amplifier=1) >> self
      particle(villager_angry, count=10) >> self
    }
  }
}

passive passive_guardian {
  name: "&9Страж"
  on: damage
  cooldown: 12
  target: self
  perm: divizionsc.def.passive_guardian

  cast {
    if (health < 25%) {
      shield(6) >> self
      cleanse() >> self
      title(actionbar="&9Щит стража!") >> self
      particle(totem_of_undying, count=8) >> self
    }
  }
}
