package ru.iamdvz.divizionsc.def.loader.dsl;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.config.settings.Settings;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DscConvenienceSyntaxTest {

    private final DscParser parser = new DscParser();

    @Test
    void appliesSmartDefaultRoutes() {
        DefDefinition def = compile("""
                ability heal {
                  meta { target: self cd: 0 key: cmd }
                  cast {
                    heal(6)
                    sound(entity_player_levelup)
                  }
                }
                """);

        EffectDefinition heal = def.effects().get(0);
        assertEquals("self", heal.text("target", null));

        EffectDefinition sound = def.effects().get(1);
        assertEquals("caster", sound.text("at", null));
    }

    @Test
    void parsesShorthandEffectCalls() {
        DefDefinition def = compile("""
                ability test {
                  meta { target: none cd: 0 key: cmd range: 16 }
                  cast {
                    damage 5
                    require has-target
                    lightning
                  }
                }
                """);

        assertEquals("damage", def.effects().get(0).type());
        assertEquals("target", def.effects().get(0).text("target", null));
        assertEquals("require", def.effects().get(1).type());
        assertEquals("lightning", def.effects().get(2).type());
    }

    @Test
    void parsesArrowRouteAndWhenAlias() {
        DscScript script = parser.parse("""
                ability test {
                  cast {
                    when (distance < 8) -> target {
                      damage(4)
                    } else if (distance < 14) -> target {
                      damage(2)
                    } else {
                      message("far")
                    }
                  }
                }
                """, "test.dsc");

        DscStatement.IfBlock block = (DscStatement.IfBlock) script.blocks().getFirst()
                .sections().get("cast").statements().getFirst();
        assertEquals("distance < 8", block.condition());
        assertEquals("target", block.route().to());
        assertEquals(1, block.elseIfBranches().size());
        assertNotNull(block.elseBody());
    }

    @Test
    void parsesModuleCallWithoutParens() {
        DscScript script = parser.parse("""
                ability test {
                  cast {
                    @pain 5 -> target
                    @spark
                  }
                }
                """, "test.dsc");

        DscSection cast = script.blocks().getFirst().sections().get("cast");
        DscStatement.ModuleCall pain = (DscStatement.ModuleCall) cast.statements().get(0);
        assertEquals("pain", pain.moduleId());
        assertEquals("5", pain.named().get("dmg"));
        assertEquals("target", pain.route().to());

        DscStatement.ModuleCall spark = (DscStatement.ModuleCall) cast.statements().get(1);
        assertEquals("spark", spark.moduleId());
    }

    @Test
    void parsesAtRouteSyntax() {
        DefDefinition def = compile("""
                ability test {
                  meta { target: entity cd: 0 key: cmd range: 24 }
                  cast {
                    damage(5) at target
                    particle(flame, count=10) >> target
                  }
                }
                """);

        assertEquals("target", def.effects().get(0).text("target", null));
        assertEquals("target", def.effects().get(1).text("at", null));
    }

    @Test
    void glowDefaultsToTargetOnEntityAbility() {
        DefDefinition def = compile("""
                ability mark {
                  meta { target: entity cd: 0 key: cmd range: 24 }
                  cast {
                    require has-target
                    glow(120)
                  }
                }
                """);

        EffectDefinition glow = def.effects().get(1);
        assertEquals("glow", glow.type());
        assertEquals("target", glow.text("target", null));
    }

    @Test
    void parsesBareChanceAndWhenBlocks() {
        DscScript script = parser.parse("""
                ability test {
                  cast {
                    chance 40% {
                      damage(2)
                    }
                    when has-target {
                      heal(1)
                    }
                  }
                }
                """, "test.dsc");

        DscSection cast = script.blocks().getFirst().sections().get("cast");
        assertInstanceOf(DscStatement.ChanceBlock.class, cast.statements().get(0));
        assertEquals("40%", ((DscStatement.ChanceBlock) cast.statements().get(0)).chance());
        assertInstanceOf(DscStatement.IfBlock.class, cast.statements().get(1));
        assertEquals("has-target", ((DscStatement.IfBlock) cast.statements().get(1)).condition());
    }

    @Test
    void moduleTargetPropertyAppliesToInnerEffects() {
        List<DefDefinition> all = new DscCompiler(new PluginConfig(new Settings()))
                .compile(parser.parse("""
                        module pain(dmg) {
                          target: entity
                          effects {
                            damage($dmg)
                          }
                        }
                        ability test {
                          meta { target: self cd: 0 key: cmd }
                          cast {
                            @pain(5)
                          }
                        }
                        """, "test.dsc"));
        EffectDefinition damage = all.stream()
                .filter(def -> "test".equals(def.id()))
                .findFirst()
                .orElseThrow()
                .effects()
                .getFirst();
        assertEquals("damage", damage.type());
        assertEquals("target", damage.text("target", null));
    }

    private DefDefinition compile(String source) {
        DscScript script = parser.parse(source, "test.dsc");
        List<DefDefinition> defs = new DscCompiler(new PluginConfig(new Settings())).compile(script);
        return defs.getFirst();
    }
}
