package ru.iamdvz.divizionsc.def.loader.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.config.settings.Settings;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

class DscConditionBlockTest {

    private final DscParser parser = new DscParser();

    private DefDefinition compileSingle(String source) {
        DscScript script = parser.parse(source, "test.dsc");
        List<DefDefinition> defs = new DscCompiler(new PluginConfig(new Settings())).compile(script);
        assertEquals(1, defs.size());
        return defs.getFirst();
    }

    @Test
    void parsesIfElseChanceSetRequire() {
        String source = """
                ability combo {
                  meta { key: cmd }
                  cast {
                    set(combo, 1)
                    require(health > 4)
                    if (distance < 10) {
                      damage(6, target)
                    } else {
                      message("too far")
                    }
                    chance(30%) {
                      lightning()
                    }
                  }
                }
                """;

        DefDefinition def = compileSingle(source);
        List<EffectDefinition> effects = def.effects();

        EffectDefinition set = effects.get(0);
        assertEquals("set", set.type());
        assertEquals("combo", set.text("var", null));

        EffectDefinition require = effects.get(1);
        assertEquals("require", require.type());
        assertEquals("health > 4", require.text("condition", null));

        EffectDefinition conditional = effects.get(2);
        assertEquals("if", conditional.type());
        Object conditions = conditional.data().get("conditions");
        assertTrue(conditions instanceof List<?>);
        assertEquals("distance < 10", ((List<?>) conditions).getFirst());
        assertNotNull(conditional.data().get("effects"));
        assertNotNull(conditional.data().get("else"));

        EffectDefinition chance = effects.get(3);
        assertEquals("chance", chance.type());
        assertEquals(0.3, chance.number("chance", 0), 1e-9);
    }
}
