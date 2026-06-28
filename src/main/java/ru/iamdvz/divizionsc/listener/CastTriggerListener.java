package ru.iamdvz.divizionsc.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.bind.BindCastHelper;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.effect.EffectExecutor;
import ru.iamdvz.divizionsc.def.model.ChainTrigger;
import ru.iamdvz.divizionsc.def.model.TriggerType;
import ru.iamdvz.divizionsc.def.service.DefService;

public final class CastTriggerListener implements Listener {

    private final PluginContext context;

    public CastTriggerListener(PluginContext context) {
        this.context = context;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        TriggerType trigger = switch (action) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> TriggerType.RIGHT_CLICK;
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> TriggerType.LEFT_CLICK;
            default -> null;
        };
        if (trigger == null) {
            return;
        }

        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (item != null && !item.isEmpty()) {
            DefService.CastResult result = context.defs().castFromItem(player, item, trigger);
            if (result == DefService.CastResult.SUCCESS) {
                event.setCancelled(true);
            }
            return;
        }

        if (BindCastHelper.tryCastCurrentSlot(context, player, trigger)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!projectile.hasMetadata(EffectExecutor.PROJECTILE_META)) {
            return;
        }

        LivingEntity hitEntity = event.getHitEntity() instanceof LivingEntity living ? living : null;
        Location location;
        Block block = event.getHitBlock();
        if (block != null) {
            location = block.getLocation().add(0.5, 0.5, 0.5);
        } else if (hitEntity != null) {
            location = hitEntity.getLocation();
        } else {
            location = projectile.getLocation();
        }

        handleProjectileHit(projectile, hitEntity, location, block);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile projectile)) {
            return;
        }
        if (!projectile.hasMetadata(EffectExecutor.PROJECTILE_META)) {
            return;
        }
        if (projectile.hasMetadata(EffectExecutor.PROJECTILE_HIT_DONE)) {
            return;
        }

        LivingEntity hitEntity = event.getEntity() instanceof LivingEntity living ? living : null;
        handleProjectileHit(projectile, hitEntity, event.getEntity().getLocation(), null);
    }

    private void handleProjectileHit(Projectile projectile, LivingEntity hitEntity, Location location, Block block) {
        if (projectile.hasMetadata(EffectExecutor.PROJECTILE_HIT_DONE)) {
            return;
        }

        MetadataValue meta = projectile.getMetadata(EffectExecutor.PROJECTILE_META).getFirst();
        if (!(meta.value() instanceof EffectExecutor.ProjectilePayload payload)) {
            return;
        }

        projectile.setMetadata(
                EffectExecutor.PROJECTILE_HIT_DONE,
                new FixedMetadataValue(context.plugin(), true)
        );

        EffectContext hitContext = new EffectContext(
                payload.context().caster(),
                hitEntity,
                location,
                block,
                payload.context().def(),
                payload.context().castItem(),
                payload.context().chainDepth()
        );

        context.chainService().fire(hitContext, ChainTrigger.ON_HIT);
        context.effectExecutor().runEffects(hitContext, payload.onHit());
        projectile.removeMetadata(EffectExecutor.PROJECTILE_META, context.plugin());
        projectile.removeMetadata(EffectExecutor.PROJECTILE_HIT_DONE, context.plugin());
    }
}
