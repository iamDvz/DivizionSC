package ru.iamdvz.divizionsc.def.effect;

import org.bukkit.plugin.java.JavaPlugin;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;

import java.util.List;

public final class EffectExecutor {

    public static final String PROJECTILE_META = "divizionsc_projectile";
    public static final String PROJECTILE_HIT_DONE = "divizionsc_projectile_hit";

    private final JavaPlugin plugin;
    private final PluginContext context;
    private final BuiltinEffectRegistry builtins;

    public EffectExecutor(JavaPlugin plugin, PluginContext context) {
        this.plugin = plugin;
        this.context = context;
        this.builtins = new BuiltinEffectRegistry(plugin, context);
    }

    public void runEffects(EffectContext ctx, List<EffectDefinition> effects) {
        for (EffectDefinition effect : effects) {
            runEffect(ctx, effect);
        }
    }

    public void runEffect(EffectContext ctx, EffectDefinition effect) {
        if (builtins.run(effect.type(), ctx, effect)) {
            return;
        }
        context.effectHandlers()
                .find(effect.type())
                .ifPresentOrElse(
                        handler -> handler.execute(ctx, effect),
                        () -> plugin.getLogger().warning("Unknown effect type: " + effect.type())
                );
    }

    public record ProjectilePayload(EffectContext context, List<EffectDefinition> onHit) {
    }
}
