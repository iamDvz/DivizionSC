package ru.iamdvz.divizionsc.def.service;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.api.event.DefCastEvent;
import ru.iamdvz.divizionsc.api.event.DefPreCastEvent;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.ChainEntry;
import ru.iamdvz.divizionsc.def.model.ChainTrigger;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.TargetMode;
import ru.iamdvz.divizionsc.def.model.TriggerType;

import java.util.Map;
import java.util.Optional;

public final class DefService {

    public enum CastResult {
        SUCCESS,
        NOT_FOUND,
        NOT_BOUND,
        NO_PERMISSION,
        COOLDOWN,
        NO_TARGET,
        WRONG_TRIGGER,
        HELPER_ONLY,
        CANCELLED
    }

    private final PluginContext context;

    public DefService(PluginContext context) {
        this.context = context;
    }

    public CastResult cast(Player player, String defId, TriggerType trigger, ItemStack sourceItem) {
        Optional<DefDefinition> optionalDef = context.defRegistry().find(defId);
        if (optionalDef.isEmpty()) {
            return CastResult.NOT_FOUND;
        }
        DefDefinition def = optionalDef.get();
        if (def.helper()) {
            return CastResult.HELPER_ONLY;
        }
        if (!def.trigger().matches(trigger)) {
            return CastResult.WRONG_TRIGGER;
        }
        return castInternal(player, def, sourceItem, trigger);
    }

    public CastResult castFromCommand(Player player, String defId) {
        Optional<DefDefinition> optionalDef = context.defRegistry().find(defId);
        if (optionalDef.isEmpty()) {
            return CastResult.NOT_FOUND;
        }
        DefDefinition def = optionalDef.get();
        if (def.helper()) {
            return CastResult.HELPER_ONLY;
        }
        return castInternal(player, def, player.getInventory().getItemInMainHand(), TriggerType.COMMAND);
    }

    public CastResult castBound(Player player, String defId) {
        Optional<DefDefinition> optionalDef = context.defRegistry().find(defId);
        if (optionalDef.isEmpty()) {
            return CastResult.NOT_FOUND;
        }
        DefDefinition def = optionalDef.get();
        if (def.helper()) {
            return CastResult.HELPER_ONLY;
        }
        return castInternal(player, def, null, TriggerType.ANY);
    }

    private CastResult castInternal(Player player, DefDefinition def, ItemStack sourceItem, TriggerType trigger) {
        if (!hasCastPermission(player, def)) {
            return CastResult.NO_PERMISSION;
        }

        if (!context.cooldowns().isReady(player.getUniqueId(), def.id(), def.cooldown())) {
            if (context.config().cooldownMessages()) {
                long remaining = context.cooldowns().remainingMillis(player.getUniqueId(), def.id());
                double seconds = remaining / 1000.0;
                context.messages().send(player, "cooldown", Component.text(String.format("%.1f", seconds)));
            }
            return CastResult.COOLDOWN;
        }

        TargetResolver.ResolvedTarget target = context.targetResolver().resolve(player, def);
        if (def.targetMode() == TargetMode.ENTITY && target.entity() == null) {
            context.messages().send(player, "no-target");
            return CastResult.NO_TARGET;
        }

        DefPreCastEvent preCast = new DefPreCastEvent(player, def, trigger);
        Bukkit.getPluginManager().callEvent(preCast);
        if (preCast.isCancelled()) {
            return CastResult.CANCELLED;
        }

        EffectContext effectContext = new EffectContext(
                player,
                target.entity(),
                target.location(),
                target.block(),
                def,
                sourceItem,
                0
        );

        executeDef(effectContext);
        context.cooldowns().apply(player.getUniqueId(), def.id(), def.cooldown());

        if (context.config().castMessages()) {
            player.sendMessage(context.messages().format(
                    "cast-success",
                    Map.of("def", def.name())
            ));
        }

        Bukkit.getPluginManager().callEvent(new DefCastEvent(player, def, trigger));
        return CastResult.SUCCESS;
    }

    public void notifyCastFailure(Player player, CastResult result, String defId) {
        switch (result) {
            case SUCCESS, NOT_FOUND, NOT_BOUND, CANCELLED -> {
            }
            case NO_PERMISSION -> context.messages().send(player, "no-permission");
            case COOLDOWN -> {
            }
            case NO_TARGET -> context.messages().send(player, "no-target");
            case WRONG_TRIGGER -> {
                String id = defId != null ? defId : "?";
                player.sendMessage(context.messages().format("wrong-trigger", Map.of("def", id)));
            }
            case HELPER_ONLY -> {
                String id = defId != null ? defId : "?";
                player.sendMessage(context.messages().format("helper-only", Map.of("def", id)));
            }
        }
    }

    public void invokeChained(EffectContext parent, ChainEntry entry, int depth) {
        if (depth >= ChainService.MAX_DEPTH) {
            return;
        }

        Optional<DefDefinition> optionalDef = context.defRegistry().find(entry.defId());
        if (optionalDef.isEmpty()) {
            context.plugin().getLogger().warning("Chained def not found: " + entry.defId());
            return;
        }

        DefDefinition resolved = optionalDef.get().withArgs(entry.args());
        TargetResolver.ResolvedTarget target = context.targetResolver().resolveForChain(parent, resolved);

        org.bukkit.entity.LivingEntity targetEntity = target.entity() != null ? target.entity() : parent.targetEntity();
        EffectContext effectContext = new EffectContext(
                parent.caster(),
                targetEntity,
                target.location() != null ? target.location() : parent.targetLocation(),
                target.block() != null ? target.block() : parent.targetBlock(),
                resolved,
                parent.castItem(),
                depth
        );

        executeDef(effectContext);
    }

    public void executeDef(EffectContext ctx) {
        context.chainService().fire(ctx, ChainTrigger.ON_CAST);
        context.effectExecutor().runEffects(ctx, ctx.def().effects());
        context.chainService().fire(ctx, ChainTrigger.ON_COMPLETE);
    }

    public CastResult castFromItem(Player player, ItemStack item, TriggerType trigger) {
        Optional<String> defId = context.castItems().readDefId(item);
        if (defId.isEmpty()) {
            return CastResult.NOT_FOUND;
        }
        CastResult result = cast(player, defId.get(), trigger, item);
        if (result != CastResult.SUCCESS && result != CastResult.NOT_FOUND) {
            notifyCastFailure(player, result, defId.get());
        }
        return result;
    }

    private boolean hasCastPermission(Player player, DefDefinition def) {
        return player.hasPermission(def.permission())
                || player.hasPermission("divizionsc.cast.*")
                || player.hasPermission("divizionsc.def.*");
    }
}
