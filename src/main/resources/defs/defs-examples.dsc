// =====================================================================
//  defs-examples.dsc — базовые способности из кирпичей
//  Кирпичи: defs-bricks.dsc · Канон: SYNTAX.md
//  /dsc reload · /dsc give <id>
// =====================================================================


// --- 1. Простые ------------------------------------------------------

ability heal {
  name: "&aЛечение"
  cooldown: 8
  key: rclick
  target: self
  item: GOLDEN_APPLE | &aЛечение | &7@mend(6)

  cast {
    @mend(6) >> self
  }
}

ability smite_basic {
  name: "&eУдар"
  cooldown: 4
  key: rclick
  target: entity
  range: 24
  item: IRON_SWORD | &eУдар | &7@strike(5)

  cast {
    @strike(5) >> target
  }
}

ability instant_smite {
  name: "&bКара"
  cooldown: 6
  key: rclick
  target: entity
  range: 30
  item: TRIDENT | &bКара | &7@smite(4)

  cast {
    @smite(4) >> target
  }
}


// --- 2. Кирпичи: with, extends ---------------------------------------

ability smite_with_slow {
  name: "&eТяжёлый удар"
  cooldown: 6
  key: lclick
  target: entity
  range: 20
  item: IRON_AXE | &eТяжёлый удар

  cast {
    @strike(5) with slow_pack >> target
  }
}

ability heavy_sword {
  name: "&4Сокрушительный удар"
  cooldown: 7
  key: rclick
  target: entity
  range: 18
  item: DIAMOND_SWORD | &4Сокрушительный

  cast {
    @heavy_strike(8) >> target
  }
}

ability lifesteal {
  name: "&4Похищение жизни"
  cooldown: 8
  key: rclick
  target: entity
  range: 6
  item: NETHERITE_SWORD | &4Похищение

  cast {
    @vamp_bite(6, 3) >> target
  }
}


// --- 3. Движение -----------------------------------------------------

ability dash_strike {
  name: "&fРывок"
  cooldown: 5
  key: shift
  target: none
  item: FEATHER | &fРывок

  cast {
    @dash_pack(1.6) >> self
  }
}

ability blink {
  name: "&dМерцание"
  cooldown: 7
  key: rclick
  target: none
  item: ENDER_PEARL | &dМерцание

  cast {
    @blink_pack(8) >> self
  }
}

ability leap {
  name: "&aПрыжок"
  cooldown: 4
  key: space
  target: self
  item: RABBIT_FOOT | &aПрыжок

  cast {
    @leap_pack >> self
  }
}


// --- 4. Зона и бафы --------------------------------------------------

ability frost_nova {
  name: "&bЛедяная вспышка"
  cooldown: 12
  key: rclick
  target: none
  range: 8
  item: PACKED_ICE | &bЛедяная вспышка

  cast {
    @frost_burst(6) >> self
  }
}

ability fire_blast {
  name: "&cОгненный взрыв"
  cooldown: 9
  key: rclick
  target: none
  range: 20
  item: FIRE_CHARGE | &cОгненный взрыв

  cast {
    @fire_burst(1.5, 4) >> location
  }
}

ability war_cry {
  name: "&6Боевой клич"
  cooldown: 20
  key: shift
  target: self
  item: GOLDEN_HORSE_ARMOR | &6Боевой клич

  cast {
    @war_shout >> self
  }
}


// --- 5. Снаряд + фазы ------------------------------------------------

ability fireball {
  name: "&cОгненный шар"
  cooldown: 6
  key: rclick
  target: block
  range: 32
  item: BLAZE_ROD | &cОгненный шар

  cast {
    @sfx_cast >> self
    projectile(FIREBALL, speed=1.4) {
      hit {
        stack >> target {
          @strike(6)
          @fx_fire
        }
      }
    }
  }
  on cast { @fx_spark >> self }
  on done { @fx_hearts >> self }
}


// --- 6. Утилиты ------------------------------------------------------

ability mark_target {
  name: "&eМетка"
  cooldown: 5
  key: lclick
  target: entity
  range: 28
  item: GLOW_INK_SAC | &eМетка

  cast {
    require(has-target)
    @mark_pack >> target
  }
}

ability sky_hook {
  name: "&9Небесный крюк"
  cooldown: 7
  key: rclick
  target: entity
  range: 18
  item: FISHING_ROD | &9Небесный крюк

  cast {
    require(has-target)
    @lift_pack >> target
  }
}

ability purify {
  name: "&fОчищение"
  cooldown: 14
  key: shift
  target: self
  item: MILK_BUCKET | &fОчищение

  cast {
    @purge_pack >> self
  }
}

ability summon_wolf {
  name: "&7Призыв волков"
  cooldown: 30
  key: rclick
  target: none
  item: BONE | &7Призыв волков

  cast {
    summon(wolf, count=2) >> self
    @sfx_horn >> self
  }
}

ability thunder_combo {
  name: "&eГромовое комбо"
  cooldown: 10
  key: rclick
  target: none
  item: LIGHTNING_ROD | &eГромовое комбо

  cast {
    @thunder_roll >> self
  }
}

ability conditional_strike {
  name: "&cУсловный удар"
  cooldown: 3
  key: lclick
  target: entity
  range: 20
  item: STONE_AXE | &cУсловный удар

  cast {
    when has-target {
      @strike(7) >> target
    } else {
      message("&7Нет цели") >> self
    }
  }
}


// --- 7. Комбо ----------------------------------------------------------

ability tri_combo {
  name: "&6Тройное комбо"
  cooldown: 11
  key: rclick
  target: entity
  range: 20
  item: GOLDEN_SWORD | &6Тройное комбо

  cast {
    stack >> target {
      @strike(4)
      @slow_pack
    }
    @mend(2) >> self
  }
  on cast { @sfx_cast >> self }
  on done { @fx_hearts >> self }
}

ability double_tap {
  name: "&cДвойной удар"
  cooldown: 5
  key: lclick
  target: entity
  range: 16
  item: STONE_SWORD | &cДвойной удар

  cast {
    require(has-target)
    @pain(3) + @fx_spark >> target
    after(5t) {
      @pain(3) + @fx_spark >> target
    }
  }
}


// --- 8. Пассивка -------------------------------------------------------

passive passive_thorns {
  name: "&8Шипы"
  on: damage
  cooldown: 2
  target: entity
  perm: divizionsc.def.passive_thorns

  cast {
    require(has-target)
    @pain_default >> target
  }
}
