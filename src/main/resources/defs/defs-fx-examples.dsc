// =====================================================================
//  DivizionSC — defs-fx-examples.dsc
//  Визуальные эффекты: EffectLib (fx) и ModelEngine VFX (vfx).
//  fx  требует плагин EffectLib · vfx требует DSC_MEG + ModelEngine.
//  Файл компилируется даже без них — эффект просто не отыграет в рантайме.
// =====================================================================
//
//  EffectLib — две формы записи:
//    • короткая:  fx(sphere, heart, radius=1.2, particles=20, iterations=15, period=2)
//    • подробный блок:  fx { shape ...  particle ...  <shape> { } timing { } }
//  Фигуры: sphere · helix(spiral) · circle(ring) · line · tornado
//
//  ModelEngine VFX — тоже две формы:
//    • короткая:  vfx <model> <animation> [scale] [remove-delay] [at]
//    • подробный блок:  vfx { model-name ...  position ...  bones { } animations { } }
// =====================================================================


// ---------------------------------------------------------------------
//  1. Короткая запись EffectLib — сфера из сердечек на себе
// ---------------------------------------------------------------------
ability fx_sparkle {
  name: "&dИскры"
  cooldown: 3
  key: rclick
  target: self
  item: NETHER_STAR | &dИскры

  cast {
    fx(sphere, heart, radius=1.2, particles=20, iterations=15, period=2) >> self
    sound(amethyst_block_chime, volume=0.8)
  }
}


// ---------------------------------------------------------------------
//  2. Подробный блок сферы (полный контроль формы и тайминга)
// ---------------------------------------------------------------------
ability fx_sphere_aura {
  name: "&bАура"
  cooldown: 6
  key: shift
  target: self
  item: HEART_OF_THE_SEA | &bАура

  cast {
    fx {
      shape sphere
      particle end_rod
      at self
      sphere {
        radius 1.5
        particles 30
        y-offset 0.4
        radius-increase 0.02
      }
      timing {
        iterations 20
        period 2
      }
    }
  }
}


// ---------------------------------------------------------------------
//  3. Спираль (helix) на цели — с маршрутом >> target
// ---------------------------------------------------------------------
ability fx_helix {
  name: "&aВихрь"
  cooldown: 7
  key: rclick
  target: entity
  range: 24
  item: BREEZE_ROD | &aВихрь

  cast {
    require has-target
    fx >> target {
      shape helix
      particle flame
      helix {
        radius 1.5
        strands 2
        curve 5
        particles 50
        rotation 180
      }
      timing {
        iterations 30
        period 2
      }
    }
  }
}


// ---------------------------------------------------------------------
//  4. Кольцо (circle) + линия (line) в одном касте
// ---------------------------------------------------------------------
ability fx_runes {
  name: "&5Руны"
  cooldown: 8
  key: rclick
  target: none
  item: ENCHANTED_BOOK | &5Руны

  cast {
    fx {
      shape circle
      particle cloud
      at self
      circle {
        radius 3.0
        particles 40
        whole-circle true
        orient true
      }
    }
    fx {
      shape line
      particle electric_spark
      at self
      line {
        length 8.0
        particles 30
        is-zig-zag true
        zig-zags 4
      }
    }
  }
}


// ---------------------------------------------------------------------
//  5. ModelEngine VFX — короткая запись (модель, анимация, масштаб,
//     remove-delay в тиках, точка спавна)
// ---------------------------------------------------------------------
ability vfx_ground_slam {
  name: "&6Удар по земле"
  cooldown: 10
  key: rclick
  target: none
  item: MACE | &6Удар по земле

  cast {
    vfx ground_slam slash 1.5 60 target
    sound(generic_explode, volume=1.2, pitch=0.7)
    area(5) {
      damage(8) >> target
    }
  }
}


// ---------------------------------------------------------------------
//  6. ModelEngine VFX — подробный блок (на цели)
// ---------------------------------------------------------------------
ability vfx_slash {
  name: "&cРассечение"
  cooldown: 6
  key: rclick
  target: entity
  range: 6
  item: DIAMOND_SWORD | &cРассечение

  cast {
    require has-target
    vfx {
      model-name slash_effect
      position target
      model-scale 1.0
      remove-delay 40
      animations {
        slash {
          speed 1.2
        }
      }
    }
    damage(5) >> target
  }
}


// ---------------------------------------------------------------------
//  7. VFX на снаряде (как vfx-effect в MagicSpells):
//     модель спавнится один раз и летит вместе со снарядом
// ---------------------------------------------------------------------
ability meg_flamethrower {
  name: "&6Огнемёт"
  key: cmd
  target: none

  cast {
    projectile(SNOWBALL, speed=0.1) {
      tick {
        vfx {
          position projectile
          model-name vfx_flamethrower
          model-color ffffff
          model-scale 1.0
          remove-delay 0
          bones {
            flame {
              tint ffaa00
              glow ff6600
              block-light 15
              sky-light 0
            }
            shadow_plane {
              hidden true
            }
          }
          animations {
            skill {
              delay 0
              speed 1.0
              loop true
              lerp-in 0.0
              lerp-out 0.0
            }
          }
        }
      }
      hit {
        area(3) {
          damage(6) >> target
        }
      }
    }
  }
}
