package ru.iamdvz.divizionsc.def.loader.dsl;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.config.settings.Settings;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private DefDefinition compile(String source) {
        DscScript script = parser.parse(source, "test.dsc");
        List<DefDefinition> defs = new DscCompiler(new PluginConfig(new Settings())).compile(script);
        return defs.getFirst();
    }
}
