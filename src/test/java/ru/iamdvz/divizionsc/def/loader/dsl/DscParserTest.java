package ru.iamdvz.divizionsc.def.loader.dsl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DscParserTest {

    private final DscParser parser = new DscParser();

    @Test
    void parsesAbilityWithCastSection() {
        String source = """
                ability heal_test {
                  meta {
                    name: "&aHeal"
                    cd: 3
                    key: rclick
                  }
                  cast {
                    heal(8)
                    sound(entity.experience_orb.pickup)
                  }
                }
                """;

        DscScript script = parser.parse(source, "test.dsc");
        assertEquals(1, script.blocks().size());

        DscBlock block = script.blocks().getFirst();
        assertEquals(DscBlockKind.ABILITY, block.kind());
        assertEquals("heal_test", block.id());
        assertEquals("&aHeal", block.properties().get("name"));
        assertTrue(block.sections().containsKey("cast"));
        assertEquals(2, block.sections().get("cast").statements().size());
    }

    @Test
    void parsesModuleWithParams() {
        String source = """
                module pain(dmg) {
                  meta {
                    target: entity
                  }
                  effects {
                    damage($dmg, target)
                  }
                }
                """;

        DscScript script = parser.parse(source, "test.dsc");
        DscBlock block = script.blocks().getFirst();
        assertEquals(DscBlockKind.MODULE, block.kind());
        assertEquals("pain", block.id());
        assertEquals(1, block.params().size());
        assertEquals("dmg", block.params().getFirst().name());
        assertEquals("entity", block.properties().get("target"));
    }

    @Test
    void parsesModuleEffectsSection() {
        String source = """
                module spark {
                  effects {
                    particles(electric_spark, count=25)
                    sound(lightning_bolt_impact, volume=0.4, pitch=1.3)
                  }
                }
                """;

        DscScript script = parser.parse(source, "test.dsc");
        DscBlock block = script.blocks().getFirst();
        assertEquals(DscBlockKind.MODULE, block.kind());
        assertTrue(block.sections().containsKey("effects"));
        assertEquals(2, block.sections().get("effects").statements().size());
    }

    @Test
    void parsesMetaWithFullPropertyNames() {
        String source = """
                ability heal_test {
                  meta {
                    name: "&aHeal"
                    cooldown: 3
                    trigger: right_click
                    item: BLAZE_ROD | &cRod
                  }
                  cast {
                    heal(8)
                    sound(entity.experience_orb.pickup)
                  }
                }
                """;

        DscScript script = parser.parse(source, "test.dsc");
        DscBlock block = script.blocks().getFirst();
        assertEquals("3", block.properties().get("cooldown"));
        assertEquals("right_click", block.properties().get("trigger"));
        assertEquals("BLAZE_ROD | &cRod", block.properties().get("item"));
    }

    @Test
    void parsesAtModuleCall() {
        String source = """
                ability test {
                  cast {
                    @instant_sting(dmg=3) >> target
                    @hearts_vfx >> self
                    @pain(5) >> target
                  }
                }
                """;

        DscScript script = parser.parse(source, "test.dsc");
        DscSection cast = script.blocks().getFirst().sections().get("cast");
        assertEquals(3, cast.statements().size());

        DscStatement.ModuleCall sting = (DscStatement.ModuleCall) cast.statements().get(0);
        assertEquals("instant_sting", sting.moduleId());
        assertEquals("3", sting.named().get("dmg"));
        assertEquals("target", sting.route().to());

        DscStatement.ModuleCall hearts = (DscStatement.ModuleCall) cast.statements().get(1);
        assertEquals("hearts_vfx", hearts.moduleId());
        assertTrue(hearts.named().isEmpty());

        DscStatement.ModuleCall pain = (DscStatement.ModuleCall) cast.statements().get(2);
        assertEquals("pain", pain.moduleId());
        assertEquals("5", pain.named().get("dmg"));
    }

    @Test
    void rejectsInvalidTopLevelBlock() {
        String source = """
                after(1s) {
                  heal(1)
                }
                """;

        DscParseException ex = assertThrows(DscParseException.class, () -> parser.parse(source, "bad.dsc"));
        assertTrue(ex.getMessage().toLowerCase().contains("ability")
                || ex.getMessage().toLowerCase().contains("module")
                || ex.getMessage().toLowerCase().contains("passive"));
    }
}
