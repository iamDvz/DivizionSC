package ru.iamdvz.divizionsc.def.effect;

import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

/** Разрешение цели эффекта: контекст каста или мягкий raycast без require-target. */
final class EffectTargetHelper {

    private EffectTargetHelper() {
    }

    static double range(PluginContext plugin, EffectContext ctx) {
        if (ctx.def().range() > 0) {
            return ctx.def().range();
        }
        return plugin.config().defaultRange();
    }

    static LivingEntity living(PluginContext plugin, EffectContext ctx, EffectDefinition effect) {
        return living(plugin, ctx, effect.text("target", "entity"));
    }

    static LivingEntity living(PluginContext plugin, EffectContext ctx, String targetField) {
        String target = targetField.toLowerCase(Locale.ROOT);
        return switch (target) {
            case "caster", "self" -> ctx.caster();
            case "target", "entity" -> resolveTargetEntity(plugin, ctx);
            default -> ctx.effectEntity();
        };
    }

    static Location location(PluginContext plugin, EffectContext ctx, EffectDefinition effect) {
        String at = effect.text("at", "effect").toLowerCase(Locale.ROOT);
        return switch (at) {
            case "caster", "self" -> ctx.caster().getLocation();
            case "eyes", "eye" -> ctx.caster().getEyeLocation();
            case "target", "entity" -> {
                LivingEntity entity = resolveTargetEntity(plugin, ctx);
                if (entity != null) {
                    yield entity.getLocation();
                }
                yield plugin.targetResolver().raycastLocation(ctx.caster(), range(plugin, ctx));
            }
            case "effect", "block", "location" -> {
                if (ctx.targetLocation() != null) {
                    yield ctx.targetLocation();
                }
                yield plugin.targetResolver().raycastBlock(ctx.caster(), range(plugin, ctx)).location();
            }
            default -> ctx.effectLocation();
        };
    }

    private static LivingEntity resolveTargetEntity(PluginContext plugin, EffectContext ctx) {
        if (ctx.targetEntity() != null) {
            return ctx.targetEntity();
        }
        Entity anchor = ctx.anchorEntity();
        if (anchor instanceof LivingEntity living) {
            return living;
        }
        return plugin.targetResolver().raycastEntity(ctx.caster(), range(plugin, ctx));
    }
}
