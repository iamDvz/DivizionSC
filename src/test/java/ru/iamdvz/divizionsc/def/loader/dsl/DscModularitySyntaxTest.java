package ru.iamdvz.divizionsc.def.loader.dsl;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.config.settings.Settings;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DscModularitySyntaxTest {

    private final DscParser parser = new DscParser();

    @Test
    void parsesStackBlockWithSharedRoute() {
        DscScript script = parser.parse("""
                ability test {
                  cast {
                    stack >> target {
                      @pain(4)
                      @fx_spark
                    }
                  }
                }
                """, "test.dsc");

        DscStatement.StackBlock stack = (DscStatement.StackBlock) script.blocks().getFirst()
                .sections().get("cast").statements().getFirst();
        assertEquals("target", stack.route().to());
        assertEquals(2, stack.body().statements().size());
    }

    @Test
    void parsesMultipleModuleCallsOnOneLine() {
        DscScript script = parser.parse("""
                ability test {
                  cast {
                    @pain(3), @fx_spark >> target
                  }
                }
                """, "test.dsc");

        DscSection cast = script.blocks().getFirst().sections().get("cast");
        assertEquals(2, cast.statements().size());
        assertInstanceOf(DscStatement.ModuleCall.class, cast.statements().get(0));
        assertInstanceOf(DscStatement.ModuleCall.class, cast.statements().get(1));
    }

    @Test
    void parsesPlusSeparatedModuleCalls() {
        DscScript script = parser.parse("""
                ability test {
                  cast {
                    @pain(3) + @fx_spark >> target
                  }
                }
                """, "test.dsc");

        DscSection cast = script.blocks().getFirst().sections().get("cast");
        assertEquals(2, cast.statements().size());
        assertEquals("pain", ((DscStatement.ModuleCall) cast.statements().get(0)).moduleId());
        assertEquals("fx_spark", ((DscStatement.ModuleCall) cast.statements().get(1)).moduleId());
    }

    @Test
    void parsesWithClauseOnModuleCall() {
        DscScript script = parser.parse("""
                ability test {
                  cast {
                    @strike(5) with slow_pack >> target
                  }
                }
                """, "test.dsc");

        DscSection cast = script.blocks().getFirst().sections().get("cast");
        assertEquals(2, cast.statements().size());
        assertEquals("strike", ((DscStatement.ModuleCall) cast.statements().get(0)).moduleId());
        assertEquals("slow_pack", ((DscStatement.ModuleCall) cast.statements().get(1)).moduleId());
    }

    @Test
    void parsesModuleExtendsHeader() {
        DscBlock block = parser.parse("""
                module heavy extends strike {
                  effects { @slow_pack >> target }
                }
                """, "test.dsc").blocks().getFirst();

        assertEquals("heavy", block.id());
        assertEquals("strike", block.properties().get("extends"));
    }

    @Test
    void appliesDefaultModuleParameters() {
        List<DefDefinition> defs = compile("""
                module pain(dmg=5) {
                  target: entity
                  effects { damage($dmg) >> target }
                }
                ability test {
                  meta { target: entity cd: 0 key: cmd }
                  cast { @pain >> target }
                }
                """);

        EffectDefinition damage = defs.stream()
                .filter(def -> "test".equals(def.id()))
                .findFirst()
                .orElseThrow()
                .effects()
                .getFirst();
        assertEquals(5.0, damage.number("amount", 0));
    }

    @Test
    void extendsMergesParentModuleEffects() {
        List<DefDefinition> defs = compile("""
                module base(dmg) {
                  target: entity
                  effects { damage($dmg) >> target }
                }
                module heavy(dmg) extends base {
                  effects { potion(slowness, duration=2s) >> target }
                }
                ability test {
                  meta { target: entity cd: 0 key: cmd }
                  cast { @heavy(6) >> target }
                }
                """);

        List<EffectDefinition> effects = defs.stream()
                .filter(def -> "test".equals(def.id()))
                .findFirst()
                .orElseThrow()
                .effects();
        assertTrue(effects.size() >= 2);
        assertEquals("damage", effects.get(0).type());
        assertEquals("potion", effects.get(1).type());
    }

    private List<DefDefinition> compile(String source) {
        return new DscCompiler(new PluginConfig(new Settings())).compile(parser.parse(source, "test.dsc"));
    }
}
