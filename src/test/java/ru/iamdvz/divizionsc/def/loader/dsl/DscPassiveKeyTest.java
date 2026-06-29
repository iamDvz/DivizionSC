package ru.iamdvz.divizionsc.def.loader.dsl;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.config.settings.Settings;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.PassiveTriggerType;
import ru.iamdvz.divizionsc.def.model.TriggerType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DscPassiveKeyTest {

    private final DscParser parser = new DscParser();

    @Test
    void parsesPassiveKeyField() {
        DefDefinition def = compile("""
                passive dash {
                  meta {
                    key: shift
                  }
                  cast {
                    heal(1)
                  }
                }
                """);
        assertNull(def.passiveTrigger());
        assertEquals(TriggerType.SNEAK, def.passiveKeyTrigger());
        assertEquals(1, def.passivePressCount());
    }

    @Test
    void legacyOnShiftStillWorks() {
        DefDefinition def = compile("""
                passive dash {
                  meta {
                    on: shift
                  }
                  cast {
                    heal(1)
                  }
                }
                """);
        assertEquals(TriggerType.SNEAK, def.passiveKeyTrigger());
    }

    @Test
    void parsesPassiveCombo() {
        DefDefinition def = compile("""
                passive combo {
                  meta {
                    key: shift
                    presses: 3
                    press-window: 2s
                  }
                  cast {
                    heal(1)
                  }
                }
                """);
        assertEquals(TriggerType.SNEAK, def.passiveKeyTrigger());
        assertEquals(3, def.passivePressCount());
        assertEquals(40, def.passivePressWindowTicks());
    }

    @Test
    void eventAttackTakesPriorityOverKey() {
        DefDefinition def = compile("""
                passive vamp {
                  meta {
                    event: attack
                  }
                  cast {
                    heal(1)
                  }
                }
                """);
        assertEquals(PassiveTriggerType.DAMAGE_DEALT, def.passiveTrigger());
        assertNull(def.passiveKeyTrigger());
    }

    private DefDefinition compile(String script) {
        DscScript parsed = parser.parse(script, "test.dsc");
        List<DefDefinition> defs = new DscCompiler(new PluginConfig(new Settings())).compile(parsed);
        return defs.getFirst();
    }
}
