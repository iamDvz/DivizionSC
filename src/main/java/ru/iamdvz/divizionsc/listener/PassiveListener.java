package ru.iamdvz.divizionsc.listener;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.PassiveTriggerType;
import ru.iamdvz.divizionsc.passive.PassiveEventContext;

public final class PassiveListener implements Listener {

    private final PluginContext context;

    public PassiveListener(PluginContext context) {
        this.context = context;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        context.scheduler().entityLater(player, () -> {
            context.passives().refreshIntervalTask(player);
            context.passives().fire(player, PassiveTriggerType.JOIN,
                    PassiveEventContext.at(player.getLocation()));
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        LivingEntity attacker = resolveAttacker(event);
        Location location = victim.getLocation();
        double damage = event.getFinalDamage();

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            context.passives().fire(victim, PassiveTriggerType.FALL,
                    PassiveEventContext.of(null, location, damage));
        }

        context.passives().fire(victim, PassiveTriggerType.DAMAGE_TAKEN,
                PassiveEventContext.of(attacker, location, damage));

        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Player dealer = resolvePlayerDamager(byEntity);
            if (dealer != null && !dealer.getUniqueId().equals(victim.getUniqueId())) {
                context.passives().fire(dealer, PassiveTriggerType.DAMAGE_DEALT,
                        PassiveEventContext.of(victim, victim.getLocation(), damage));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        context.passives().fire(killer, PassiveTriggerType.KILL,
                PassiveEventContext.of(victim, victim.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        context.passives().fire(player, PassiveTriggerType.DEATH,
                PassiveEventContext.at(player.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        context.passives().fire(player, PassiveTriggerType.BLOCK_BREAK,
                PassiveEventContext.at(event.getBlock().getLocation().add(0.5, 0.5, 0.5)));
    }

    private LivingEntity resolveAttacker(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            if (byEntity.getDamager() instanceof LivingEntity living) {
                return living;
            }
            if (byEntity.getDamager() instanceof Projectile projectile
                    && projectile.getShooter() instanceof LivingEntity shooter) {
                return shooter;
            }
        }
        return null;
    }

    private Player resolvePlayerDamager(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
