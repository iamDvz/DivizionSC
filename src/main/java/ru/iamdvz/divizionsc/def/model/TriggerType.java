package ru.iamdvz.divizionsc.def.model;

import java.util.Locale;

public enum TriggerType {
    RIGHT_CLICK,
    LEFT_CLICK,
    SNEAK,
    SPRINT,
    JUMP,
    FORWARD,
    BACKWARD,
    LEFT,
    RIGHT,
    DROP,
    SWAP_HANDS,
    HOTBAR,
    KEY_1,
    KEY_2,
    KEY_3,
    KEY_4,
    KEY_5,
    KEY_6,
    KEY_7,
    KEY_8,
    KEY_9,
    COMMAND,
    ANY;

    public static TriggerType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return COMMAND;
        }
        String normalized = raw.toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "right_click", "rightclick", "right", "rclick", "use", "key_use" -> RIGHT_CLICK;
            case "left_click", "leftclick", "left", "lclick", "attack", "key_attack" -> LEFT_CLICK;
            case "sneak", "shift", "key_sneak", "key_shift" -> SNEAK;
            case "sprint", "ctrl", "key_sprint", "key_ctrl" -> SPRINT;
            case "jump", "space", "key_jump", "key_space" -> JUMP;
            case "forward", "w", "key_forward", "key_w" -> FORWARD;
            case "backward", "back", "s", "key_backward", "key_back", "key_s" -> BACKWARD;
            case "strafe_left", "key_left", "key_a", "a" -> LEFT;
            case "strafe_right", "key_right", "key_d", "d" -> RIGHT;
            case "drop", "q", "key_drop", "key_q" -> DROP;
            case "swap_hands", "swap", "f", "key_swap", "key_f", "offhand" -> SWAP_HANDS;
            case "hotbar", "key_hotbar", "slot" -> HOTBAR;
            case "1", "key_1", "hotbar_1", "slot_1" -> KEY_1;
            case "2", "key_2", "hotbar_2", "slot_2" -> KEY_2;
            case "3", "key_3", "hotbar_3", "slot_3" -> KEY_3;
            case "4", "key_4", "hotbar_4", "slot_4" -> KEY_4;
            case "5", "key_5", "hotbar_5", "slot_5" -> KEY_5;
            case "6", "key_6", "hotbar_6", "slot_6" -> KEY_6;
            case "7", "key_7", "hotbar_7", "slot_7" -> KEY_7;
            case "8", "key_8", "hotbar_8", "slot_8" -> KEY_8;
            case "9", "key_0", "key_9", "hotbar_9", "slot_9" -> KEY_9;
            case "command", "cmd" -> COMMAND;
            case "any", "all" -> ANY;
            default -> COMMAND;
        };
    }

    public static TriggerType fromHotbarSlot(int slot) {
        return switch (slot) {
            case 0 -> KEY_1;
            case 1 -> KEY_2;
            case 2 -> KEY_3;
            case 3 -> KEY_4;
            case 4 -> KEY_5;
            case 5 -> KEY_6;
            case 6 -> KEY_7;
            case 7 -> KEY_8;
            case 8 -> KEY_9;
            default -> HOTBAR;
        };
    }

    public boolean isHotbarKey() {
        return this == HOTBAR || (ordinal() >= KEY_1.ordinal() && ordinal() <= KEY_9.ordinal());
    }

    public boolean matches(TriggerType actual) {
        if (this == ANY) {
            return true;
        }
        if (this == COMMAND) {
            return actual == COMMAND;
        }
        if (this == HOTBAR && actual.isHotbarKey()) {
            return true;
        }
        if (actual == HOTBAR && isHotbarKey()) {
            return true;
        }
        return this == actual;
    }

    public static boolean triggersMatch(TriggerType configured, TriggerType actual) {
        return configured.matches(actual);
    }
}
