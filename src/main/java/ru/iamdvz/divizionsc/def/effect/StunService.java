package ru.iamdvz.divizionsc.def.effect;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.iamdvz.divizionsc.platform.Scheduler;
import ru.iamdvz.divizionsc.platform.TaskHandle;

/**
 * Удержание (stun) сущностей. Каждая жертва тикается в своём регион-потоке
 * (Folia-safe) через {@link Scheduler}, без единого глобального тикера.
 */
public final class StunService {

    private final Scheduler scheduler;
    private final Map<UUID, StunState> stunned = new ConcurrentHashMap<>();

    public StunService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void stun(LivingEntity entity, int durationTicks, int intervalTicks) {
        if (entity == null || durationTicks <= 0) {
            return;
        }
        UUID id = entity.getUniqueId();
        cancel(id);
        int interval = Math.max(1, intervalTicks);
        long endTick = Bukkit.getServer().getCurrentTick() + durationTicks;
        scheduler.entity(entity, () -> {
            applyEffects(entity);
            TaskHandle ticker = scheduler.entityTimer(entity, () -> applyEffects(entity), interval, interval);
            TaskHandle stopper = scheduler.entityLater(entity, () -> cancel(id), durationTicks);
            stunned.put(id, new StunState(endTick, entity.getLocation().clone(), ticker, stopper));
        });
    }

    public boolean isStunned(LivingEntity entity) {
        StunState state = stunned.get(entity.getUniqueId());
        return state != null && Bukkit.getServer().getCurrentTick() < state.endTick();
    }

    public Location anchor(LivingEntity entity) {
        StunState state = stunned.get(entity.getUniqueId());
        return state == null ? null : state.anchor();
    }

    public void clear() {
        for (StunState state : stunned.values()) {
            state.cancel();
        }
        stunned.clear();
    }

    public void shutdown() {
        clear();
    }

    private void cancel(UUID id) {
        StunState state = stunned.remove(id);
        if (state != null) {
            state.cancel();
        }
    }

    private void applyEffects(LivingEntity entity) {
        if (entity.isDead() || !entity.isValid()) {
            return;
        }
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 255, false, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20, 200, false, false, false));
        if (entity instanceof Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, false, false));
        }
    }

    private record StunState(long endTick, Location anchor, TaskHandle ticker, TaskHandle stopper) {
        void cancel() {
            ticker.cancel();
            stopper.cancel();
        }
    }
}
