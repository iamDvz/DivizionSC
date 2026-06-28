// DivizionSC script — псевдо-язык для def, module и effect
// Файлы: defs/defs-*.dsc

// --- модули (переиспользуемые блоки эффектов) ---

module hearts_vfx {
  target self
  do {
    fx sphere heart 1.2 20 15 2
  }
}

module pain(dmg) {
  target entity
  do {
    dmg $dmg
    ptl crit 12
    snd player_attack_crit 0.7 1.3
  }
}

effect spark {
  ptl electric_spark 25
  snd lightning_bolt_impact 0.4 1.3
}

// --- def (кастуемые способности) ---

def heal {
  name "&aЛечение"
  cd 8
  key rclick
  target self
  item GOLDEN_APPLE | &aЛечение | &7ПКМ — 6 HP

  do {
    heal 6
    snd player_levelup 0.6 1.4
  }

  on done {
    @hearts_vfx
  }
}

def blink {
  name "&bРывок"
  cd 4
  key shift
  target self

  do {
    ptl portal 30
    snd enderman_teleport 0.5 1.2
    tp 8
  }
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
  }

  on hit {
    @pain(5)
    @spark
  }
}

def combo_strike {
  name "&6Комбо"
  cd 10
  key cmd
  target entity
  range 16

  do {
    @pain(4)
    wait 8t {
      @pain(7)
    }
    @spark
  }
}

def arcane_combo {
  name "&9Арканное комбо"
  cd 15
  key cmd
  target entity
  range 20

  on cast {
    @spark
  }

  do {
    @pain(3)
    wait 10t {
      @pain(6)
    }
    lit
  }

  on done {
    @hearts_vfx
  }
}
