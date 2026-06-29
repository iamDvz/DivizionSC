package ru.iamdvz.divizionsc.def.model;

import java.util.Locale;

/**
 * События, на которые может реагировать пассивная способность ({@code passive: true}).
 */
public enum PassiveTriggerType {
    DAMAGE_TAKEN,
    DAMAGE_DEALT,
    KILL,
    DEATH,
    JOIN,
    BLOCK_BREAK,
    FALL,
    INTERVAL;

    public static PassiveTriggerType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "damage", "damage_taken", "hurt", "on_damage", "on_hurt" -> DAMAGE_TAKEN;
            case "attack", "damage_dealt", "deal_damage", "on_attack", "on_hit", "hit" -> DAMAGE_DEALT;
            case "kill", "on_kill", "entity_kill" -> KILL;
            case "death", "on_death", "die", "on_die" -> DEATH;
            case "join", "on_join", "login", "on_login" -> JOIN;
            case "block_break", "break", "on_break" -> BLOCK_BREAK;
            case "fall", "on_fall" -> FALL;
            case "interval", "tick", "periodic", "on_tick", "loop" -> INTERVAL;
            default -> null;
        };
    }
}
