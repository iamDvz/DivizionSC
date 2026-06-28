package ru.iamdvz.divizionsc.def.service;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.def.loader.DefLoadReport;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.TargetMode;
import ru.iamdvz.divizionsc.def.model.TriggerType;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefRegistryTest {

    @Test
    void findsDefCaseInsensitive() {
        DefRegistry registry = new DefRegistry();
        registry.register(sampleDef("FireBall"), "a.yml", null);
        assertTrue(registry.find("fireball").isPresent());
    }

    @Test
    void reportsOverwriteWarning() {
        DefRegistry registry = new DefRegistry();
        DefLoadReport report = new DefLoadReport();
        registry.register(sampleDef("heal"), "first.yml", report);
        registry.register(sampleDef("heal"), "second.yml", report);
        assertEquals(1, report.warnings().size());
        assertTrue(report.warnings().getFirst().contains("overwritten"));
    }

    @Test
    void matchesTriggerHotbarKeys() {
        assertTrue(TriggerType.triggersMatch(TriggerType.HOTBAR, TriggerType.KEY_1));
        assertTrue(TriggerType.triggersMatch(TriggerType.KEY_3, TriggerType.HOTBAR));
        assertTrue(!TriggerType.triggersMatch(TriggerType.RIGHT_CLICK, TriggerType.LEFT_CLICK));
    }

    private static DefDefinition sampleDef(String id) {
        return new DefDefinition(
                id,
                id,
                "",
                "divizionsc.def." + id,
                0,
                TriggerType.ANY,
                TargetMode.SELF,
                16,
                false,
                null,
                List.of(),
                Map.of()
        );
    }
}
