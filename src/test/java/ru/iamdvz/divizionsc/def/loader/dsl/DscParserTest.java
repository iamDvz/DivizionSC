package ru.iamdvz.divizionsc.def.loader.dsl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DscParserTest {

    private final DscParser parser = new DscParser();

    @Test
    void parsesDefWithDoSection() {
        String source = """
                def heal_test {
                  name "&aHeal"
                  cd 3
                  key rclick
                  do {
                    heal 8
                    snd entity.experience_orb.pickup
                  }
                }
                """;

        DscScript script = parser.parse(source, "test.dsc");
        assertEquals(1, script.blocks().size());

        DscBlock block = script.blocks().getFirst();
        assertEquals(DscBlockKind.DEF, block.kind());
        assertEquals("heal_test", block.id());
        assertEquals("&aHeal", block.properties().get("name"));
        assertTrue(block.sections().containsKey("do"));
        assertEquals(2, block.sections().get("do").statements().size());
    }

    @Test
    void parsesModuleWithParams() {
        String source = """
                module pain(dmg) {
                  target entity
                  do {
                    dmg $dmg
                  }
                }
                """;

        DscScript script = parser.parse(source, "test.dsc");
        DscBlock block = script.blocks().getFirst();
        assertEquals(DscBlockKind.MODULE, block.kind());
        assertEquals("pain", block.id());
        assertEquals(1, block.params().size());
        assertEquals("dmg", block.params().getFirst());
    }

    @Test
    void rejectsInvalidTopLevelBlock() {
        String source = """
                wait 1s {
                  heal 1
                }
                """;

        DscParseException ex = assertThrows(DscParseException.class, () -> parser.parse(source, "bad.dsc"));
        assertTrue(ex.getMessage().toLowerCase().contains("def")
                || ex.getMessage().toLowerCase().contains("module")
                || ex.getMessage().toLowerCase().contains("wait"));
    }
}
