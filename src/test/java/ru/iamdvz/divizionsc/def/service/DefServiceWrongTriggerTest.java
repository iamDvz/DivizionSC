package ru.iamdvz.divizionsc.def.service;

import org.junit.jupiter.api.Test;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.TargetMode;
import ru.iamdvz.divizionsc.def.model.TriggerType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefServiceWrongTriggerTest {

    private static DefDefinition def(TriggerType trigger) {
        return new DefDefinition(
                "test",
                "Test",
                "",
                "divizionsc.def.test",
                0,
                0,
                trigger,
                TargetMode.SELF,
                0,
                false,
                false,
                null,
                null,
                20,
                1,
                1,
                null,
                null,
                null
        );
    }

    @Test
    void commandDefNeverNotifiesOnItemInteraction() {
        DefDefinition commandDef = def(TriggerType.COMMAND);
        assertFalse(DefService.shouldNotifyWrongTrigger(commandDef, TriggerType.RIGHT_CLICK));
        assertFalse(DefService.shouldNotifyWrongTrigger(commandDef, TriggerType.LEFT_CLICK));
    }

    @Test
    void hotbarSelectionDoesNotNotify() {
        DefDefinition dropDef = def(TriggerType.DROP);
        assertFalse(DefService.shouldNotifyWrongTrigger(dropDef, TriggerType.KEY_1));
        assertFalse(DefService.shouldNotifyWrongTrigger(dropDef, TriggerType.HOTBAR));
    }

    @Test
    void explicitCastActionsNotify() {
        DefDefinition dropDef = def(TriggerType.DROP);
        assertTrue(DefService.shouldNotifyWrongTrigger(dropDef, TriggerType.RIGHT_CLICK));
        assertTrue(DefService.shouldNotifyWrongTrigger(dropDef, TriggerType.DROP));
    }
}
