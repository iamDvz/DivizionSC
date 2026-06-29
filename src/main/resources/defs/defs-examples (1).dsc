// DivizionSC 1.0.0 — примеры способностей (.dsc)
// plugins/DivizionSC/defs/defs-examples.dsc
// /dsc validate  →  /dsc reload  →  /dsc list

// ── Модули ─────────────────────────────────────────────────────────────────

module pain(dmg) {
  target entity
  do {
    dmg $dmg target
    ptl crit 12
  }
}

module hearts_vfx {
  target self
  do {
    fx sphere heart 1.2 20 15 2
  }
}

module boss_slam_vfx {
  do {
    fx circle cloud 3.0 40 20 3
    snd entity_ravager_roar 1.0 0.8
    ptl explosion 8
  }
}

module boss_announce(msg) {
  do {
    msg $msg
    snd entity_wither_spawn 0.5 0.7
  }
}

effect spark {
  ptl electric_spark 25
  snd entity_lightning_bolt_impact 0.8 1.0
}

// ── Базовые ────────────────────────────────────────────────────────────────

def heal {
  name "&aЛечение"
  cd 8
  key rclick
  target self
  perm divizionsc.def.heal
  item GOLDEN_APPLE | &aЛечение | &7ПКМ — 6 HP

  do {
    heal 6
    snd entity_player_levelup 0.7 1.2
    wait 5t {
      @hearts_vfx
    }
  }

  on done {
    @spark
  }
}

def fireball {
  name "&cОгненный шар"
  cd 6
  key rclick
  target block
  range 32
  item BLAZE_ROD | &cОгненный шар | &7ПКМ — выстрел

  do {
    snd entity_blaze_shoot 1.0 0.9
    proj fireball 1.4
    wait 10t {
      @spark
    }
  }

  on hit {
    @pain(5)
  }
}

def blink {
  name "&bМерцание"
  cd 4
  key shift
  target self

  do {
    snd entity_enderman_teleport 0.6 1.4
    blink 8
    ptl portal 30
  }
}

def bump {
  name "&eПодброс"
  cd 5
  key cmd
  target self

  do {
    vel 0 0.4
    snd entity_slime_jump
  }
}

// ── Условия / переменные / шанс ───────────────────────────────────────────

def combo_strike {
  name "&6Комбо-удар"
  cd 12
  key shift
  target entity
  range 16

  do {
    require health > 4
    if distance < 8 {
      @pain(4)
      dash 1.6
      wait 8t {
        @pain(6)
      }
    } else {
      msg "&7Цель слишком далеко"
    }
    chance 35% {
      lit
    }
  }

  on done {
    @hearts_vfx
  }
}

def arcane_combo {
  name "&5Арканное комбо"
  cd 20
  key cmd
  target entity
  range 24

  do {
    shield 4
    @pain(3)
    wait 15t {
      @pain(5)
      push 1.2
    }
    chance 50% {
      potion speed 5s 1
    }
  }

  on cast {
    @spark
  }
}

def hybrid_demo {
  name "&dГибрид"
  cd 3
  key cmd
  target entity
  range 16

  do {
    require has-target
    @pain(2)
    wait 10t {
      @pain(5)
    }
  }
}

def ground_slam {
  name "&4Удар по земле"
  cd 10
  key rclick
  target entity
  range 10

  do {
    require has-target
    dash 1.2
    wait 6t {
      @pain(8)
      fx circle cloud 2.5 30 15 2
    }
  }
}

// ── Босс ─────────────────────────────────────────────────────────────────

def boss_slam {
  name "&4&lУдар босса"
  cd 8
  key cmd
  target entity
  range 14

  do {
    require has-target
    @pain(14)
    wait 5t {
      @boss_slam_vfx
    }
  }

  on cast {
    @boss_announce("&4&l⚠ Удар!")
  }
}

def boss_volley {
  name "&cЗалп"
  cd 12
  key cmd
  target entity
  range 32

  do {
    snd entity_skeleton_shoot
    proj snowball 1.2
    wait 8t {
      proj snowball 1.2
    }
    wait 16t {
      proj snowball 1.4
    }
  }

  on hit {
    @pain(4)
  }
}

def boss_enrage {
  name "&c&lЯрость"
  cd 30
  key cmd
  target self

  do {
    potion strength 10s 1
    potion speed 10s 0
    shield 6
    snd entity_ender_dragon_growl
    msg "&c&lБосс в ярости!"
    fx helix flame 1.5 50 30 2
  }
}

def boss_chain_lightning {
  name "&eЦепная молния"
  cd 18
  key cmd
  target entity
  range 20

  do {
    require has-target
    lit
    @pain(6)
    wait 10t {
      chance 70% {
        lit
        @pain(4)
      }
    }
  }
}

// ── ModelEngine VFX (DSC_MEG + ModelEngine) ────────────────────────────────

def meg_flamethrower {
  name "&6Огнемёт VFX"
  cd 15
  key cmd
  target block
  range 40

  do {
    snd item_firecharge_use
    proj snowball 0.1 {
      vfx {
        position projectile
        model-name vfx_flamethrower
        model-scale 1.0
        remove-delay 0
        bones {
          flame {
            tint ffaa00
            glow ff6600
          }
        }
        animations {
          skill {
            delay 0
            speed 1.0
            loop true
          }
        }
      }
    }
  }
}

def meg_ground_slam {
  name "&4VFX удар"
  cd 12
  key cmd
  target entity
  range 12

  do {
    require has-target
    vfx {
      model-name ground_slam
      position target
      model-scale 1.5
      remove-delay 40
      animations {
        slam {
          speed 1.2
          delay 0
        }
      }
    }
    wait 10t {
      @pain(10)
    }
  }
}

// ── Пассивки ────────────────────────────────────────────────────────────────

def passive_thorns {
  name "&cШипы"
  passive true
  on damage
  cd 2
  target entity
  perm divizionsc.def.passive_thorns

  do {
    require has-target
    @pain(3)
    ptl crit 8
    snd entity_player_hurt 0.5 1.2
  }
}

def passive_lifesteal {
  name "&4Вампиризм"
  passive true
  on attack
  cd 1
  target self
  perm divizionsc.def.passive_lifesteal

  do {
    heal 2
    ptl heart 6
  }
}

def passive_second_wind {
  name "&aВторое дыхание"
  passive true
  on damage
  cd 30
  target self
  perm divizionsc.def.passive_second_wind

  do {
    require health < 6
    heal 4
    shield 2
    snd entity_player_levelup 0.6 1.0
    msg "&aВторое дыхание!"
  }
}

def passive_regen_aura {
  name "&dАура регенерации"
  passive true
  on interval
  interval 60t
  cd 3
  target self
  perm divizionsc.def.passive_regen_aura

  do {
    heal 1
    ptl heart 4
  }
}

def passive_dash_combo {
  name "&bРывок (комбо)"
  passive true
  key shift
  presses 3
  press-window 2s
  cd 5
  target self
  perm divizionsc.def.passive_dash_combo

  do {
    vel forward 1.2
    ptl cloud 12
    snd entity_enderman_teleport 0.5 1.4
    msg "&bРывок!"
  }
}

def passive_parry {
  name "&eПарирование"
  passive true
  key rclick
  cd 3
  target self
  perm divizionsc.def.passive_parry

  do {
    shield 2
    ptl crit 10
    snd item_shield_block 0.8 1.0
  }
}
