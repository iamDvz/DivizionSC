package ru.iamdvz.dscmm.util;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

import java.util.Locale;

public final class EffectLocations {

    private EffectLocations() {
    }

    public static Location resolve(EffectContext ctx, EffectDefinition effect) {
        String at = effect.text("at", effect.text("location", "effect")).toLowerCase(Locale.ROOT);
        Location location = switch (at) {
            case "caster", "self" -> ctx.caster().getLocation();
            case "eyes", "eye" -> ctx.caster().getEyeLocation();
            case "target" -> ctx.targetEntity() != null
                    ? ctx.targetEntity().getLocation()
                    : ctx.effectLocation();
            default -> ctx.effectLocation();
        };
        if (effect.data().containsKey("yaw")) {
            location.setYaw((float) effect.number("yaw", location.getYaw()));
        }
        if (effect.data().containsKey("pitch")) {
            location.setPitch((float) effect.number("pitch", location.getPitch()));
        }
        return location;
    }

    public static LivingEntity resolveCaster(EffectContext ctx, EffectDefinition effect) {
        String on = effect.text("on", effect.text("caster", "caster")).toLowerCase(Locale.ROOT);
        return switch (on) {
            case "target", "entity" -> ctx.targetEntity() != null ? ctx.targetEntity() : ctx.caster();
            default -> ctx.caster();
        };
    }
}
