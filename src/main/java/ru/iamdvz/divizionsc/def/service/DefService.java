package ru.iamdvz.divizionsc.def.service;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.api.IntegrationCastOptions;
import ru.iamdvz.divizionsc.api.event.DefCastEvent;
import ru.iamdvz.divizionsc.api.event.DefPreCastEvent;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.model.ChainEntry;
import ru.iamdvz.divizionsc.def.model.ChainTrigger;
import ru.iamdvz.divizionsc.def.model.DefDefinition;
import ru.iamdvz.divizionsc.def.model.TargetMode;
import ru.iamdvz.divizionsc.def.model.TriggerType;
import ru.iamdvz.divizionsc.passive.PassiveEventContext;

import java.util.Map;
import java.util.Optional;

public final class DefService {

    public enum CastResult {
        SUCCESS,
        NOT_FOUND,
        NOT_BOUND,
        NO_PERMISSION,
        COOLDOWN,
        NO_MANA,
        NO_TARGET,
        WRONG_TRIGGER,
        HELPER_ONLY,
        PASSIVE_ONLY,
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
        if (def.passive()) {
            return CastResult.PASSIVE_ONLY;
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
        if (def.passive()) {
            return CastResult.PASSIVE_ONLY;
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
        if (def.passive()) {
            return CastResult.PASSIVE_ONLY;
        }
        return castInternal(player, def, null, TriggerType.ANY);
    }

    public CastResult castFromIntegration(
            Player player,
            String defId,
            LivingEntity targetEntity,
            Location targetLocation,
            Block targetBlock,
            IntegrationCastOptions options
    ) {
        IntegrationCastOptions resolvedOptions = options == null ? IntegrationCastOptions.defaults() : options;
        Optional<DefDefinition> optionalDef = context.defRegistry().find(defId);
        if (optionalDef.isEmpty()) {
            return CastResult.NOT_FOUND;
        }

        DefDefinition def = optionalDef.get();
        if (def.helper() && !resolvedOptions.allowHelper()) {
            return CastResult.HELPER_ONLY;
        }

        if (resolvedOptions.checkPermission() && !hasCastPermission(player, def)) {
            return CastResult.NO_PERMISSION;
        }

        if (resolvedOptions.applyCooldown()
                && !context.config().opBypassesCooldown(player)
                && !context.cooldowns().isReady(player.getUniqueId(), def.id(), def.cooldown())) {
            return CastResult.COOLDOWN;
        }

        CastResult manaResult = checkMana(player, def);
        if (manaResult != CastResult.SUCCESS) {
            return manaResult;
        }

        LivingEntity entity = targetEntity;
        Location location = targetLocation;
        Block block = targetBlock;
        if (entity == null && location == null) {
            TargetResolver.ResolvedTarget resolved = context.targetResolver().resolve(player, def);
            entity = resolved.entity();
            location = resolved.location();
            block = resolved.block();
        } else if (location == null && entity != null) {
            location = entity.getLocation();
        }

        if (resolvedOptions.requireTarget() || def.targetMode() == TargetMode.ENTITY) {
            if (entity == null && def.targetMode() == TargetMode.ENTITY) {
                return CastResult.NO_TARGET;
            }
        }

        DefPreCastEvent preCast = new DefPreCastEvent(player, def, TriggerType.COMMAND);
        Bukkit.getPluginManager().callEvent(preCast);
        if (preCast.isCancelled()) {
            return CastResult.CANCELLED;
        }

        EffectContext effectContext = new EffectContext(
                player,
                entity,
                location,
                block,
                def,
                null,
                0
        );

        executeDef(effectContext);

        if (resolvedOptions.applyCooldown() && !context.config().opBypassesCooldown(player)) {
            context.cooldowns().apply(player.getUniqueId(), def.id(), def.cooldown());
        }

        spendMana(player, def);

        Bukkit.getPluginManager().callEvent(new DefCastEvent(player, def, TriggerType.COMMAND));
        return CastResult.SUCCESS;
    }

    public CastResult firePassive(Player player, DefDefinition def, PassiveEventContext event) {
        if (!def.passive() || def.helper()) {
            return CastResult.NOT_FOUND;
        }
        if (!def.hasPassiveTrigger()) {
            return CastResult.NOT_FOUND;
        }
        if (!hasCastPermission(player, def)) {
            return CastResult.NO_PERMISSION;
        }

        if (!context.config().opBypassesCooldown(player)
                && !context.cooldowns().isReady(player.getUniqueId(), def.id(), def.cooldown())) {
            return CastResult.COOLDOWN;
        }

        CastResult manaResult = checkMana(player, def);
        if (manaResult != CastResult.SUCCESS) {
            return manaResult;
        }

        LivingEntity targetEntity = event.targetEntity();
        Location location = event.location() != null ? event.location() : player.getLocation();
        if (def.targetMode() == TargetMode.ENTITY && targetEntity == null) {
            return CastResult.NO_TARGET;
        }

        EffectContext effectContext = new EffectContext(
                player,
                targetEntity,
                location,
                null,
                def,
                null,
                0
        );
        if (event.damage() > 0) {
            effectContext.vars().setNumber("damage", event.damage());
            effectContext.vars().setNumber("event_damage", event.damage());
        }

        executeDef(effectContext);

        if (!context.config().opBypassesCooldown(player)) {
            context.cooldowns().apply(player.getUniqueId(), def.id(), def.cooldown());
        }
        spendMana(player, def);
        return CastResult.SUCCESS;
    }

    private CastResult castInternal(Player player, DefDefinition def, ItemStack sourceItem, TriggerType trigger) {
        if (!hasCastPermission(player, def)) {
            return CastResult.NO_PERMISSION;
        }

        if (!context.config().opBypassesCooldown(player)
                && !context.cooldowns().isReady(player.getUniqueId(), def.id(), def.cooldown())) {
            if (context.config().cooldownMessages()) {
                long remaining = context.cooldowns().remainingMillis(player.getUniqueId(), def.id());
                double seconds = remaining / 1000.0;
                context.messages().send(player, "cooldown", Component.text(String.format("%.1f", seconds)));
            }
            return CastResult.COOLDOWN;
        }

        CastResult manaResult = checkMana(player, def);
        if (manaResult != CastResult.SUCCESS) {
            return manaResult;
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

        if (!context.config().opBypassesCooldown(player)) {
            context.cooldowns().apply(player.getUniqueId(), def.id(), def.cooldown());
        }
        spendMana(player, def);

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
            case NO_MANA -> {
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
            case PASSIVE_ONLY -> {
                String id = defId != null ? defId : "?";
                player.sendMessage(context.messages().format("passive-only", Map.of("def", id)));
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
        return castFromItem(player, item, trigger, true);
    }

    public CastResult castFromItem(Player player, ItemStack item, TriggerType trigger, boolean notifyOnFailure) {
        Optional<String> defId = context.castItems().readDefId(item);
        if (defId.isEmpty()) {
            return CastResult.NOT_FOUND;
        }
        String id = defId.get();
        Optional<DefDefinition> optionalDef = context.defRegistry().find(id);
        CastResult result = cast(player, id, trigger, item);
        if (notifyOnFailure && result != CastResult.SUCCESS && result != CastResult.NOT_FOUND) {
            if (result != CastResult.WRONG_TRIGGER
                    || optionalDef.isEmpty()
                    || shouldNotifyWrongTrigger(optionalDef.get(), trigger)) {
                notifyCastFailure(player, result, id);
            }
        }
        return result;
    }

    static boolean shouldNotifyWrongTrigger(DefDefinition def, TriggerType attempted) {
        if (def.trigger() == TriggerType.COMMAND) {
            return false;
        }
        return switch (attempted) {
            case RIGHT_CLICK, LEFT_CLICK, DROP, SWAP_HANDS -> true;
            default -> false;
        };
    }

    private CastResult checkMana(Player player, DefDefinition def) {
        if (!context.mana().canAfford(player, def.mana())) {
            context.messages().send(player, "no-mana");
            return CastResult.NO_MANA;
        }
        return CastResult.SUCCESS;
    }

    private void spendMana(Player player, DefDefinition def) {
        context.mana().spend(player, def.mana());
    }

    private boolean hasCastPermission(Player player, DefDefinition def) {
        return player.hasPermission(def.permission())
                || player.hasPermission("divizionsc.cast.*")
                || player.hasPermission("divizionsc.def.*");
    }
}
