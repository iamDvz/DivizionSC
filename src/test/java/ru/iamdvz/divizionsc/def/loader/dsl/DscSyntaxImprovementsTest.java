package ru.iamdvz.divizionsc.def.loader.dsl;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.config.settings.Settings;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.divizionsc.def.model.TargetMode;
import ru.iamdvz.divizionsc.def.model.TriggerType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DscSyntaxImprovementsTest {

    private final DscParser parser = new DscParser();

    @Test
    void topLevelColonPropertiesWithoutMeta() {
        DefDefinition def = compile("""
                ability heal {
                  name: "&aHeal"
                  cooldown: 8
                  key: rclick
                  target: self
                  cast {
                    heal(6)
                  }
                }
                """);

        assertEquals("&aHeal", def.name());
        assertEquals(8.0, def.cooldown());
        assertEquals(TriggerType.RIGHT_CLICK, def.trigger());
        assertEquals(TargetMode.SELF, def.targetMode());
    }

    @Test
    void bareTopLevelPropertiesReadmeStyle() {
        DefDefinition def = compile("""
                ability dash_move {
                  cd 5
                  key shift
                  target none
                  cast {
                    dash(1.6)
                  }
                }
                """);

        assertEquals(5.0, def.cooldown());
        assertEquals(TriggerType.SNEAK, def.trigger());
        assertEquals(TargetMode.NONE, def.targetMode());
    }

    @Test
    void singleLineMetaKeepsEveryProperty() {
        // Регрессия: раньше `meta { a: x b: y }` сохранял только первый ключ.
        DefDefinition def = compile("""
                ability combo {
                  meta { cooldown: 12 key: rclick target: entity range: 24 }
                  cast {
                    damage(5)
                  }
                }
                """);

        assertEquals(12.0, def.cooldown());
        assertEquals(TriggerType.RIGHT_CLICK, def.trigger());
        assertEquals(TargetMode.ENTITY, def.targetMode());
        assertEquals(24.0, def.range());
    }

    @Test
    void commandKeepsCommas() {
        EffectDefinition command = firstEffect("""
                ability runner {
                  meta { key: cmd }
                  cast {
                    command(minecraft:give @p diamond 1, 64)
                  }
                }
                """);

        assertEquals("command", command.type());
        assertEquals("minecraft:give @p diamond 1, 64", command.text("command", ""));
    }

    @Test
    void messageKeepsCommasAndStripsQuotes() {
        EffectDefinition message = firstEffect("""
                ability talker {
                  meta { key: cmd }
                  cast {
                    message("&aHello, brave world")
                  }
                }
                """);

        assertEquals("message", message.type());
        assertEquals("&aHello, brave world", message.text("text", ""));
    }

    @Test
    void damageAmountKeepsVariableAsText() {
        EffectDefinition variable = firstEffect("""
                ability scaled {
                  meta { key: cmd target: entity }
                  cast {
                    damage(power)
                  }
                }
                """);
        assertInstanceOf(String.class, variable.data().get("amount"));
        assertEquals("power", variable.data().get("amount"));

        EffectDefinition literal = firstEffect("""
                ability flat {
                  meta { key: cmd target: entity }
                  cast {
                    damage(5)
                  }
                }
                """);
        assertInstanceOf(Number.class, literal.data().get("amount"));
        assertEquals(5.0, literal.number("amount", 0));
    }

    private DefDefinition compile(String source) {
        List<DefDefinition> defs = new DscCompiler(new PluginConfig(new Settings()))
                .compile(parser.parse(source, "test.dsc"));
        return defs.getFirst();
    }

    private EffectDefinition firstEffect(String source) {
        return compile(source).effects().getFirst();
    }
}
