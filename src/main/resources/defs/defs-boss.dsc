// defs-boss.dsc — босс-способности через модули DSC
// Тест: /dsc cast boss_slam · /dsc cast boss_volley · /dsc cast boss_enrage

// --- переиспользуемые модули ---

module boss_hit(dmg) {
  target entity
  do {
    dmg $dmg
    ptl crit 14
    snd player_attack_crit 0.6 1.1
  }
}

module boss_slam_vfx {
  target self
  do {
    fx circle cloud 2.5 36 10 2
    snd entity_iron_golem_attack 0.8 0.6
    ptl block 30
  }
}

module boss_roar {
  target self
  do {
    msg &c&l⚠ &4Босс ревёт!
    snd entity_ender_dragon_growl 0.6 0.8
    fx sphere dragon_breath 2.0 40 20 2
    pot weakness 3s 0
  }
}

module boss_pulse(dmg) {
  target entity
  do {
    @boss_hit($dmg)
    fx sphere smoke 1.2 16 8 1
  }
}

effect boss_spark {
  ptl electric_spark 20
  snd entity_lightning_bolt_thunder 0.3 1.5
}

// --- босс-способности ---

def boss_slam {
  name "&c&l▼ Удар по земле"
  desc "&7Мощный удар с отбрасыванием"
  cd 12
  key cmd
  target entity
  range 10
  item NETHERITE_AXE | &cУдар по земле | &7Команда — AoE удар

  on cast {
    @boss_roar
  }

  do {
    @boss_hit(10)
  }

  on done {
    @boss_slam_vfx
    vel 0.0 0.6
  }
}

def boss_volley {
  name "&f&l◎ Залп теней"
  desc "&7Пять снарядов с нарастающим уроном"
  cd 18
  key cmd
  target none
  range 32
  item BOW | &fЗалп теней | &7Команда — серия выстрелов

  on cast {
    @boss_spark
    msg &8◎ &7Тени сжимаются в снаряды...
  }

  do {
    snd entity_skeleton_shoot 0.5 1.2
    proj snowball 1.6
    wait 6t {
      snd entity_skeleton_shoot 0.5 1.3
      proj snowball 1.7
    }
    wait 12t {
      snd entity_skeleton_shoot 0.5 1.4
      proj snowball 1.8
    }
    wait 18t {
      snd entity_skeleton_shoot 0.5 1.5
      proj snowball 1.9
    }
    wait 24t {
      snd entity_wither_shoot 0.6 0.9
      proj fireball 1.2
    }
  }

  on hit {
    @boss_hit(4)
    @boss_spark
  }

  on done {
    fx helix smoke 1.5 24 15 2
  }
}

def boss_enrage {
  name "&4&l☠ Ярость босса"
  desc "&7Комбо: три импульса урона + финишер"
  cd 25
  key cmd
  target entity
  range 16
  item NETHERITE_SWORD | &4Ярость | &7Команда — комбо

  on cast {
    @boss_roar
    pot strength 5s 1
    pot speed 5s 0
  }

  do {
    @boss_pulse(4)
    wait 10t {
      @boss_pulse(6)
    }
    wait 20t {
      @boss_pulse(8)
    }
    wait 30t {
      @boss_hit(14)
      lit fx
      @boss_slam_vfx
    }
  }

  on done {
    msg &4☠ &cЯрость утихает...
    pot weakness 4s 0
  }
}

def boss_chain_lightning {
  name "&b&l⚡ Цепная молния"
  desc "&7Молния без урона + серия ударов"
  cd 15
  key cmd
  target entity
  range 24
  item TRIDENT | &bЦепная молния | &7Команда — шок по цели

  do {
    lit fx
    @boss_spark
    wait 5t {
      @boss_hit(5)
    }
    wait 12t {
      lit
      @boss_hit(7)
    }
    wait 20t {
      @boss_hit(9)
      fx circle electric_spark 2.0 30 12 2
    }
  }

  on done {
    snd weather_rain 0.3 1.0
  }
}
