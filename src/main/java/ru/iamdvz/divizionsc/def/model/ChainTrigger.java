package ru.iamdvz.divizionsc.def.model;

import java.util.Locale;
import java.util.Map;

public enum ChainTrigger {
    ON_CAST,
    ON_HIT,
    ON_COMPLETE;

    public static ChainTrigger parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ON_COMPLETE;
        }
        return switch (raw.toLowerCase(Locale.ROOT).replace('-', '_')) {
            case "on_cast", "cast" -> ON_CAST;
            case "on_hit", "hit" -> ON_HIT;
            case "on_complete", "complete", "on_end", "end" -> ON_COMPLETE;
            default -> ON_COMPLETE;
        };
    }
}
