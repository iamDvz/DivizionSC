package ru.iamdvz.divizionsc.def.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PassiveTriggerTypeTest {

    @Test
    void parsesAliases() {
        assertEquals(PassiveTriggerType.DAMAGE_TAKEN, PassiveTriggerType.parse("damage"));
        assertEquals(PassiveTriggerType.DAMAGE_DEALT, PassiveTriggerType.parse("attack"));
        assertEquals(PassiveTriggerType.KILL, PassiveTriggerType.parse("on_kill"));
        assertEquals(PassiveTriggerType.INTERVAL, PassiveTriggerType.parse("tick"));
        assertNull(PassiveTriggerType.parse("unknown"));
    }
}
