package ru.iamdvz.divizionsc.def.loader.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.config.settings.Settings;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

class DscTargetDirectiveTest {

    private final DscParser parser = new DscParser();

    @Test
    void parsesDualRouteOnEffectCall() {
        DefDefinition def = compile("""
                ability test {
                  cast {
                    heal(6) >> self
                    damage(4) >> self >> target
                    sound(entity_player_levelup) >> caster >> location
                  }
                }
                """);

        EffectDefinition heal = def.effects().get(0);
        assertEquals("heal", heal.type());
        assertEquals("self", heal.text("target", null));
        assertNull(heal.text("from", null));

        EffectDefinition damage = def.effects().get(1);
        assertEquals("damage", damage.type());
        assertEquals("target", damage.text("target", null));
        assertEquals("caster", damage.text("from", null));

        EffectDefinition sound = def.effects().get(2);
        assertEquals("sound", sound.type());
        assertEquals("effect", sound.text("at", null));
        assertEquals("caster", sound.text("from", null));
        assertNull(sound.text("target", null));
    }

    @Test
    void parsesRussianRouteAliases() {
        DefDefinition def = compile("""
                ability test {
                  cast {
                    damage(3) >> начало >> цель
                    particles(FLAME, count=5) >> конец
                  }
                }
                """);

        EffectDefinition damage = def.effects().get(0);
        assertEquals("target", damage.text("target", null));
        assertEquals("caster", damage.text("from", null));

        EffectDefinition particles = def.effects().get(1);
        assertEquals("target", particles.text("at", null));
        assertNull(particles.text("from", null));
    }

    @Test
    void singleTokenIsTargetNotStart() {
        DefDefinition def = compile("""
                ability test {
                  cast {
                    damage(4) >> target
                    damage(2) >> target >>
                    damage(1) >> >> entity
                  }
                }
                """);

        for (EffectDefinition damage : def.effects()) {
            assertEquals("damage", damage.type());
            assertEquals("target", damage.text("target", null));
            assertNull(damage.text("from", null));
        }
    }

    @Test
    void parsesRouteOnUseAndAfterBlock() {
        DscScript script = parser.parse("""
                ability test {
                  cast {
                    @pain(dmg=5) >> target
                    after(5t) >> target {
                      particles(FLAME, count=10) >> target
                    }
                  }
                }
                """, "test.dsc");

        DscBlock block = script.blocks().getFirst();
        DscStatement.ModuleCall use = (DscStatement.ModuleCall) block.sections().get("cast").statements().get(0);
        assertNull(use.route().from());
        assertEquals("target", use.route().to());

        DscStatement.WaitBlock wait = (DscStatement.WaitBlock) block.sections().get("cast").statements().get(1);
        assertNull(wait.route().from());
        assertEquals("target", wait.route().to());
        assertNotNull(wait.body());
    }

    private DefDefinition compile(String source) {
        DscScript script = parser.parse(source, "test.dsc");
        List<DefDefinition> defs = new DscCompiler(new PluginConfig(new Settings())).compile(script);
        return defs.getFirst();
    }
}
