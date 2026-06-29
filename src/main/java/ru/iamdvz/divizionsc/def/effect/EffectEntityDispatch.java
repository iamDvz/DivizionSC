package ru.iamdvz.divizionsc.def.effect;

import org.bukkit.entity.LivingEntity;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.platform.Scheduler;

/** Folia-safe выполнение мутаций сущности в её region-потоке. */
public final class EffectEntityDispatch {

    private EffectEntityDispatch() {
    }

    public static void mutate(Scheduler scheduler, LivingEntity entity, Runnable action) {
        if (entity == null) {
            return;
        }
        scheduler.entity(entity, action);
    }

    public static void runEffectsOnTarget(
            Scheduler scheduler,
            PluginContext context,
            EffectContext ctx,
            LivingEntity target,
            java.util.List<ru.iamdvz.divizionsc.def.model.EffectDefinition> effects) {
        if (target == null || effects == null || effects.isEmpty()) {
            return;
        }
        mutate(scheduler, target, () -> context.effectExecutor().runEffects(ctx.withTarget(target), effects));
    }
}
