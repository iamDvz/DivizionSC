package ru.iamdvz.divizionsc.def.loader.dsl;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.config.PluginConfig;
import ru.iamdvz.divizionsc.config.settings.Settings;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DscSyntaxTest {

    private final DscParser parser = new DscParser();

    @Test
    void normalizesLegacyBlockAndSectionAliases() {
        String source = """
                def legacy_heal {
                  meta { name: "&aLegacy" cd: 1 key: cmd target: self }
                  do {
                    heal(4) >> self
                  }
                }

                effect legacy_mod {
                  effects {
                    particles(flame, count=5) >> self
                  }
                }

                module spark {
                  effects {
                    particles(electric_spark, count=5) >> self
                  }
                }

                ability chain_demo {
                  meta { key: cmd target: self cd: 0 }
                  on cast { @spark >> self }
                  cast { heal(1) >> self }
                  on done { @spark >> self }
                }
                """;

        DscScript script = parser.parse(source, "test.dsc");
        assertEquals(4, script.blocks().size());
        assertEquals("legacy_heal", script.blocks().get(0).id());
        assertTrue(script.blocks().get(0).sections().containsKey("cast"));
        assertEquals("legacy_mod", script.blocks().get(1).id());
        assertTrue(script.blocks().get(2).sections().containsKey("effects"));
        assertTrue(script.blocks().get(3).sections().containsKey("start"));
        assertTrue(script.blocks().get(3).sections().containsKey("done"));

        List<?> compiled = new DscCompiler(new PluginConfig(new Settings())).compile(script);
        assertEquals(4, compiled.size());
    }

    @Test
    void rejectsUseCallSyntax() {
        String source = """
                ability test {
                  cast {
                    use(pain, dmg=5) >> target
                  }
                }
                """;
        assertThrows(DscParseException.class, () -> parser.parse(source, "test.dsc"));
    }

    @Test
    void normalizesSectionNames() {
        assertEquals("cast", DscSyntax.normalizeSection("do"));
        assertEquals("start", DscSyntax.normalizeSection("on cast"));
        assertEquals("hit", DscSyntax.normalizeSection("on hit"));
        assertEquals("done", DscSyntax.normalizeSection("on done"));
        assertEquals("module", DscSyntax.normalizeBlockKeyword("effect"));
        assertEquals("ability", DscSyntax.normalizeBlockKeyword("def"));
    }
}
