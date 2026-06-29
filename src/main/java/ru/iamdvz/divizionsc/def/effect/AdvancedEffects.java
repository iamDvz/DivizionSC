package ru.iamdvz.divizionsc.def.effect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.divizionsc.platform.Scheduler;
import ru.iamdvz.divizionsc.platform.TaskHandle;
import ru.iamdvz.divizionsc.util.EffectKeys;

/** Продвинутые эффекты: loop, area, stun, raycast, particle_projectile. Folia-safe. */
final class AdvancedEffects {

    private final PluginContext context;
    private final Scheduler scheduler;
    private final StunService stunService;

    AdvancedEffects(PluginContext context, Scheduler scheduler, StunService stunService) {
        this.context = context;
        this.scheduler = scheduler;
        this.stunService = stunService;
    }

    void loop(EffectContext ctx, EffectDefinition effect) {
        int iterations = Math.max(1, effect.integer("iterations", effect.integer("times", 1)));
        int interval = Math.max(0, effect.integer("interval", 0));
        List<EffectDefinition> nested = nestedEffects(effect);
        if (nested.isEmpty()) {
            return;
        }
        runLoop(ctx, nested, 0, iterations, interval);
    }

    private void runLoop(EffectContext ctx, List<EffectDefinition> nested, int index, int total, int interval) {
        if (index >= total || ctx.vars().isAborted()) {
            return;
        }
        ctx.vars().setNumber("i", index);
        context.effectExecutor().runEffects(ctx, nested);
        if (index + 1 >= total) {
            return;
        }
        if (interval <= 0) {
            runLoop(ctx, nested, index + 1, total, interval);
            return;
        }
        int generation = context.reloadGeneration();
        scheduler.entityLater(ctx.caster(), () -> {
            if (context.reloadGeneration() != generation || !ctx.caster().isOnline()) {
                return;
            }
            runLoop(ctx, nested, index + 1, total, interval);
        }, interval);
    }

    void area(EffectContext ctx, EffectDefinition effect) {
        double radius = effect.number("radius", 5.0);
        boolean hitPlayers = effect.bool("hit_players", true);
        boolean hitNonPlayers = effect.bool("hit_non_players", true);
        boolean includeCaster = effect.bool("include_caster", false);
        List<EffectDefinition> nested = nestedEffects(effect);
        if (nested.isEmpty()) {
            return;
        }
        Location center = ctx.targetLocation() != null ? ctx.targetLocation() : ctx.caster().getLocation();
        scheduler.region(center, () -> {
            for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }
                if (!includeCaster && living.getUniqueId().equals(ctx.caster().getUniqueId())) {
                    continue;
                }
                if (living instanceof Player ? !hitPlayers : !hitNonPlayers) {
                    continue;
                }
                EffectEntityDispatch.runEffectsOnTarget(scheduler, context, ctx, living, nested);
            }
        });
    }

    void stun(EffectContext ctx, EffectDefinition effect) {
        LivingEntity target = ctx.targetEntity() != null ? ctx.targetEntity() : ctx.caster();
        if (target == null) {
            return;
        }
        int duration = effect.integer("duration", 40);
        int interval = Math.max(1, effect.integer("interval", 5));
        stunService.stun(target, duration, interval);
    }

    void raycast(EffectContext ctx, EffectDefinition effect) {
        double distance = effect.number("distance", 15.0);
        double hitRadius = effect.number("hit_radius", 1.0);
        int maxHits = Math.max(1, effect.integer("max_hits",
                effect.integer("hits", effect.integer("targets", effect.integer("chain", 1)))));
        List<EffectDefinition> nested = nestedEffects(effect);
        if (nested.isEmpty()) {
            return;
        }
        Player caster = ctx.caster();
        Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        if (maxHits <= 1) {
            RayTraceResult result = caster.getWorld().rayTraceEntities(
                    eye,
                    direction,
                    distance,
                    hitRadius,
                    entity -> isRaycastCandidate(entity, caster)
            );
            if (result != null && result.getHitEntity() instanceof LivingEntity living) {
                EffectEntityDispatch.runEffectsOnTarget(scheduler, context, ctx, living, nested);
            }
            return;
        }

        Set<UUID> seen = new LinkedHashSet<>();
        double step = Math.max(0.25, hitRadius * 0.5);
        for (double travelled = 0; travelled <= distance && seen.size() < maxHits; travelled += step) {
            Location point = eye.clone().add(direction.clone().multiply(travelled));
            for (org.bukkit.entity.Entity entity : point.getWorld().getNearbyEntities(
                    point, hitRadius, hitRadius, hitRadius)) {
                if (!(entity instanceof LivingEntity living) || !isRaycastCandidate(entity, caster)) {
                    continue;
                }
                if (!seen.add(living.getUniqueId())) {
                    continue;
                }
                EffectEntityDispatch.runEffectsOnTarget(scheduler, context, ctx, living, nested);
                if (seen.size() >= maxHits) {
                    return;
                }
            }
        }
    }

    void chain(EffectContext ctx, EffectDefinition effect) {
        Map<String, Object> data = new HashMap<>(effect.data());
        data.putIfAbsent("max_hits", Math.max(2, effect.integer("times",
                effect.integer("hits", effect.integer("targets", 3)))));
        raycast(ctx, new EffectDefinition("raycast", data, nestedEffects(effect)));
    }

    private boolean isRaycastCandidate(org.bukkit.entity.Entity entity, Player caster) {
        return entity instanceof LivingEntity living
                && living != caster
                && living.isValid()
                && !living.isDead();
    }

    void particleProjectile(EffectContext ctx, EffectDefinition effect) {
        Particle particle = EffectKeys.resolveParticle(effect.text("particle", "FLAME"));
        double speed = effect.number("speed", 0.75);
        double maxDistance = effect.number("max_distance", 15.0);
        Location start = ctx.caster().getEyeLocation().clone();
        Vector direction = start.getDirection().normalize().multiply(speed);
        int generation = context.reloadGeneration();
        double[] traveled = {0.0};
        TaskHandle[] holder = new TaskHandle[1];
        holder[0] = scheduler.regionTimer(start, () -> {
            if (context.reloadGeneration() != generation || traveled[0] >= maxDistance) {
                if (holder[0] != null) {
                    holder[0].cancel();
                }
                return;
            }
            traveled[0] += speed;
            start.add(direction);
            if (particle != null) {
                start.getWorld().spawnParticle(particle, start, 1, 0, 0, 0, 0);
            }
        }, 1, 1);
    }

    void projectileTick(Projectile projectile, EffectContext ctx, List<EffectDefinition> onTick, int interval) {
        if (projectile == null || onTick.isEmpty()) {
            return;
        }
        int generation = context.reloadGeneration();
        TaskHandle[] holder = new TaskHandle[1];
        holder[0] = scheduler.entityTimer(projectile, () -> {
            if (context.reloadGeneration() != generation || !projectile.isValid() || projectile.isDead()) {
                if (holder[0] != null) {
                    holder[0].cancel();
                }
                return;
            }
            context.effectExecutor().runEffects(ctx.withLocation(projectile.getLocation()), onTick);
        }, interval, interval);
    }

    private List<EffectDefinition> nestedEffects(EffectDefinition effect) {
        List<EffectDefinition> nested = effect.children("effects");
        return nested.isEmpty() ? effect.nested() : nested;
    }
}
