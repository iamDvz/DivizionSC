// =====================================================================
//  defs-bricks.dsc — библиотека module-кирпичей
//  Подключаются через @id из любого defs-*.dsc (глобальный индекс)
//  Канон: SYNTAX.md
// =====================================================================


// --- VFX (частицы) ---------------------------------------------------

module fx_spark {
  effects {
    particle(electric_spark, count=18) >> target
  }
}

module fx_hearts {
  effects {
    particle(heart, count=10, offset=0.5) >> self
  }
}

module fx_blood {
  effects {
    particle(damage_indicator, count=8) >> target
  }
}

module fx_smoke {
  effects {
    particle(campfire_cosy_smoke, count=12) >> self
  }
}

module fx_frost {
  effects {
    particle(snowflake, count=16) >> target
  }
}

module fx_fire {
  effects {
    particle(flame, count=14) >> target
  }
}

module fx_holy {
  effects {
    particle(end_rod, count=20) >> self
  }
}

module fx_portal {
  effects {
    particle(portal, count=24) >> self
  }
}


// --- SFX (звуки) -----------------------------------------------------

module sfx_cast {
  effects {
    sound(entity_blaze_shoot, volume=0.9, pitch=1.1) >> self
  }
}

module sfx_hit {
  effects {
    sound(entity_player_attack_strong, volume=0.8) >> target
  }
}

module sfx_heal {
  effects {
    sound(entity_player_levelup, volume=0.7) >> self
  }
}

module sfx_thunder {
  effects {
    sound(entity_lightning_bolt_thunder, volume=0.9) >> self
  }
}

module sfx_horn {
  effects {
    sound(item_goat_horn_sound_0, volume=1.0) >> self
  }
}


// --- Атомы (один эффект + параметр) ----------------------------------

module pain(dmg) {
  target: entity
  effects {
    require(has-target)
    damage($dmg) >> target
    @fx_blood >> target
  }
}

module mend(amt) {
  target: self
  effects {
    heal($amt) >> self
    @fx_hearts >> self
    @sfx_heal >> self
  }
}


// --- Паки (2–3 связанных эффекта) ------------------------------------

module slow_pack {
  target: entity
  effects {
    potion(slowness, duration=4s, amplifier=1) >> target
    @fx_frost >> target
  }
}

module burn_pack {
  target: entity
  effects {
    ignite(ticks=80) >> target
    @fx_fire >> target
  }
}

module mark_pack {
  target: entity
  effects {
    require(has-target)
    glow(duration=160) >> target
    message("&eЦель отмечена") >> self
  }
}

module lift_pack {
  target: entity
  effects {
    require(has-target)
    velocity(0, 1.2, 0) >> target
    @fx_spark >> target
  }
}

module purge_pack {
  target: self
  effects {
    cleanse() >> self
    @fx_holy >> self
    sound(block_beacon_activate, volume=0.6) >> self
  }
}


// --- Композиты (удар, магия, движение) -------------------------------

module strike(dmg) {
  target: entity
  effects {
    require(has-target)
    @pain($dmg) >> target
    @fx_spark >> target
    @sfx_hit >> target
  }
}

module smite(dmg) {
  target: entity
  effects {
    require(has-target)
    lightning() >> target
    @pain($dmg) >> target
    @sfx_thunder >> target
  }
}

module heavy_strike(dmg) extends strike {
  effects {
    @slow_pack >> target
  }
}

module vamp_bite(dmg, heal_amt) {
  target: entity
  effects {
    require(has-target)
    @pain($dmg) >> target
    @mend($heal_amt) >> self
  }
}

module dash_pack(power) {
  target: self
  effects {
    dash($power) >> self
    @fx_smoke >> self
    sound(entity_ender_dragon_flap, volume=0.5) >> self
  }
}

module blink_pack(range) {
  target: self
  effects {
    blink($range) >> self
    @fx_portal >> self
  }
}

module leap_pack {
  target: self
  effects {
    velocity(0, 0.9, 0) >> self
    sound(entity_slime_squish, volume=0.8) >> self
  }
}

module frost_burst(radius) {
  effects {
    @fx_frost >> self
    area($radius) {
      @slow_pack >> target
    }
  }
}

module fire_burst(power, radius) {
  effects {
    explosion($power, fire=true) >> location
    area($radius) {
      @burn_pack >> target
    }
    @fx_fire >> location
  }
}

module war_shout {
  effects {
    potion(strength, duration=8s, amplifier=1) >> self
    potion(speed, duration=8s, amplifier=0) >> self
    @sfx_horn >> self
    particle(angry_villager, count=15) >> self
  }
}

module thunder_roll {
  effects {
    @fx_spark >> self
    after(10t) {
      lightning(fx=true) >> self
      @sfx_thunder >> self
    }
    chance(35%) {
      lightning() >> self
    }
  }
}

module pain_default(dmg=3) {
  target: entity
  effects {
    @pain($dmg) >> target
  }
}
