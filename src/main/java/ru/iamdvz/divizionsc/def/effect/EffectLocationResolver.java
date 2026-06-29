package ru.iamdvz.divizionsc.def.effect;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

import java.util.Locale;

final class EffectLocationResolver {

    private EffectLocationResolver() {
    }

    static Location resolve(EffectContext ctx, EffectDefinition effect) {
        String at = effect.text("at", "effect").toLowerCase(Locale.ROOT);
        return switch (at) {
            case "caster", "self" -> ctx.caster().getLocation();
            case "eyes", "eye" -> ctx.caster().getEyeLocation();
            case "target", "entity" -> ctx.targetEntity() != null
                    ? ctx.targetEntity().getLocation()
                    : ctx.effectLocation();
            default -> ctx.effectLocation();
        };
    }

    static Location spawnLocation(EffectContext ctx, EffectDefinition effect) {
        Location base = resolve(ctx, effect);
        if ("eyes".equalsIgnoreCase(effect.text("at", ""))
                || "eye".equalsIgnoreCase(effect.text("at", ""))) {
            base = ctx.caster().getEyeLocation();
        } else if (!effect.data().containsKey("at")) {
            base = ctx.caster().getEyeLocation();
        }
        Vector offset = parseOffset(effect.text("offset", effect.text("relative_offset", "0,0,0")));
        return base.clone().add(offset);
    }

    static Vector parseOffset(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Vector();
        }
        String[] parts = raw.split(",");
        double x = parts.length > 0 ? parse(parts[0]) : 0;
        double y = parts.length > 1 ? parse(parts[1]) : 0;
        double z = parts.length > 2 ? parse(parts[2]) : 0;
        return new Vector(x, y, z);
    }

    private static double parse(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
