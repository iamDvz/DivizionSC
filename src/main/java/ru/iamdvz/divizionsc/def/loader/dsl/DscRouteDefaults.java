package ru.iamdvz.divizionsc.def.loader.dsl;

import java.util.Locale;
import java.util.Set;
import ru.iamdvz.divizionsc.def.model.TargetMode;

/** Цель по умолчанию, если маршрут {@code >>} не указан. */
public final class DscRouteDefaults {

    private static final Set<String> SELF_EFFECTS = Set.of(
            "heal", "shield", "dash", "blink", "velocity", "vel", "knockback",
            "require", "set", "setvar", "set_var", "message", "msg", "give",
            "command", "cmd", "money", "give-money", "take-money"
    );
    private static final Set<String> TARGET_EFFECTS = Set.of(
            "damage", "dmg", "push", "pull", "stun", "raycast", "beam", "chain", "teleport", "tp",
            "ignite", "root", "swap",
            "lightning", "lit", "summon"
    );
    private static final Set<String> LOCATION_EFFECTS = Set.of(
            "fx", "effectlib", "vfx", "meg", "modelengine", "model"
    );

    private DscRouteDefaults() {
    }

    public static DscTargetDirective.Route forEffect(String effectName, TargetMode metaTarget) {
        String effect = effectName.toLowerCase(Locale.ROOT);
        if (NO_DEFAULT_ROUTE.contains(effect)) {
            return DscTargetDirective.Route.NONE;
        }
        String to = defaultTo(effect, metaTarget);
        return new DscTargetDirective.Route(null, to);
    }

    private static final Set<String> NO_DEFAULT_ROUTE = Set.of(
            "vfx", "fx", "effectlib", "if", "when", "chance", "area", "loop", "aura", "repeat",
            "after", "delay", "wait", "projectile", "proj"
    );

    public static String defaultTo(String effectName, TargetMode metaTarget) {
        String effect = effectName.toLowerCase(Locale.ROOT);
        if (SELF_EFFECTS.contains(effect)) {
            return "self";
        }
        if (TARGET_EFFECTS.contains(effect)) {
            return "target";
        }
        if (LOCATION_EFFECTS.contains(effect)) {
            return fromMeta(metaTarget, "location");
        }
        if ("particles".equals(effect) || "particle".equals(effect) || "ptl".equals(effect)) {
            return fromMeta(metaTarget, "self");
        }
        if ("sound".equals(effect) || "snd".equals(effect)) {
            return fromMeta(metaTarget, "self");
        }
        if ("potion".equals(effect) || "pot".equals(effect)) {
            return fromMeta(metaTarget, "self");
        }
        if ("projectile".equals(effect) || "proj".equals(effect)) {
            return metaTarget == TargetMode.BLOCK ? "location" : "target";
        }
        if ("explosion".equals(effect) || "explode".equals(effect)) {
            return "location";
        }
        if ("title".equals(effect)) {
            return "self";
        }
        if ("glow".equals(effect) || "glowing".equals(effect)
                || "invis".equals(effect) || "invisibility".equals(effect)
                || "cleanse".equals(effect) || "purge".equals(effect)) {
            return fromMeta(metaTarget, "self");
        }
        if ("area".equals(effect) || "aura".equals(effect) || "loop".equals(effect) || "repeat".equals(effect)) {
            return fromMeta(metaTarget, "self");
        }
        return fromMeta(metaTarget, "self");
    }

    private static String fromMeta(TargetMode metaTarget, String fallback) {
        return switch (metaTarget) {
            case SELF -> "self";
            case ENTITY -> "target";
            case BLOCK -> "location";
            case NONE -> fallback;
        };
    }
}
