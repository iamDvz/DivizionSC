package ru.iamdvz.divizionsc.def.effect;

import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.divizionsc.platform.Scheduler;
import ru.iamdvz.divizionsc.platform.TaskHandle;
import ru.iamdvz.divizionsc.util.ColorUtil;
import ru.iamdvz.divizionsc.util.EffectKeys;

/**
 * Дополнительные эффекты (Folia-safe): перемещение, призыв, аура, тотем, щит, выдача предмета,
 * повтор и фигуры из партиклов/звуков. Мутации сущностей планируются в их регион-потоках.
 */
final class ExtraEffects {

    private final PluginContext context;
    private final Scheduler scheduler;

    ExtraEffects(PluginContext context, Scheduler scheduler) {
        this.context = context;
        this.scheduler = scheduler;
    }

    void pull(EffectContext ctx, EffectDefinition effect) {
        LivingEntity target = EffectTargetHelper.living(context, ctx, effect);
        if (target == null || target.equals(ctx.caster())) {
            return;
        }
        double strength = effect.number("strength", effect.number("power", 1.0));
        Vector direction = ctx.caster().getLocation().toVector().subtract(target.getLocation().toVector());
        applyVelocity(target, normalizeOrZero(direction).multiply(strength)
                .setY(effect.number("y", 0.2)));
    }

    void push(EffectContext ctx, EffectDefinition effect) {
        LivingEntity target = EffectTargetHelper.living(context, ctx, effect);
        if (target == null) {
            return;
        }
        double strength = effect.number("strength", effect.number("power", 1.0));
        Vector direction;
        if (target.equals(ctx.caster())) {
            direction = ctx.caster().getLocation().getDirection();
        } else {
            direction = target.getLocation().toVector().subtract(ctx.caster().getLocation().toVector());
        }
        applyVelocity(target, normalizeOrZero(direction).multiply(strength)
                .setY(effect.number("y", 0.35)));
    }

    void money(EffectContext ctx, EffectDefinition effect) {
        String op = effect.text("op", "give").toLowerCase(Locale.ROOT);
        if (op.startsWith("take") || op.startsWith("withdraw")) {
            takeMoney(ctx, effect);
        } else {
            giveMoney(ctx, effect);
        }
    }

    void giveMoney(EffectContext ctx, EffectDefinition effect) {
        Player caster = ctx.caster();
        if (caster == null) {
            return;
        }
        double amount = Math.abs(effect.number("amount", 0.0));
        context.economy().ifPresent(economy -> economy.deposit(caster, amount));
    }

    void takeMoney(EffectContext ctx, EffectDefinition effect) {
        Player caster = ctx.caster();
        if (caster == null) {
            return;
        }
        double amount = Math.abs(effect.number("amount", 0.0));
        context.economy().ifPresent(economy -> economy.withdraw(caster, amount));
    }

    void dash(EffectContext ctx, EffectDefinition effect) {
        Player caster = ctx.caster();
        double power = effect.number("power", effect.number("strength", 1.4));
        Vector velocity = caster.getLocation().getDirection().normalize().multiply(power);
        velocity.setY(velocity.getY() + effect.number("y", 0.2));
        applyVelocity(caster, velocity);
    }

    void blink(EffectContext ctx, EffectDefinition effect) {
        Player caster = ctx.caster();
        double distance = effect.number("distance", effect.number("forward", 6.0));
        Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location destination = caster.getLocation();
        for (double travelled = 1; travelled <= distance; travelled += 1) {
            Location candidate = eye.clone().add(direction.clone().multiply(travelled));
            Location feet = new Location(candidate.getWorld(), candidate.getX(), candidate.getY() - 1,
                    candidate.getZ(), caster.getYaw(), caster.getPitch());
            if (!feet.getBlock().isPassable() || !candidate.getBlock().isPassable()) {
                break;
            }
            destination = feet;
        }
        Location target = destination;
        scheduler.entity(caster, () -> caster.teleport(target));
    }

    void shield(EffectContext ctx, EffectDefinition effect) {
        LivingEntity target = EffectTargetHelper.living(context, ctx, effect);
        if (target == null) {
            return;
        }
        int duration = effect.integer("duration", 200);
        int amplifier = Math.max(0, effect.integer("amplifier", (int) Math.round(effect.number("hearts", 4) / 2) - 1));
        scheduler.entity(target, () ->
                target.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, amplifier)));
    }

    void summon(EffectContext ctx, EffectDefinition effect) {
        EntityType type = resolveEntityType(effect.text("entity", effect.text("mob", "ZOMBIE")));
        if (type == null) {
            return;
        }
        int count = Math.max(1, effect.integer("count", 1));
        Location location = EffectTargetHelper.location(context, ctx, effect);
        scheduler.region(location, () -> {
            for (int i = 0; i < count; i++) {
                location.getWorld().spawnEntity(location, type);
            }
        });
    }

    void totem(EffectContext ctx, EffectDefinition effect) {
        Location location = EffectTargetHelper.location(context, ctx, effect).clone();
        int duration = effect.integer("duration", 100);
        int interval = Math.max(1, effect.integer("interval", 20));
        List<EffectDefinition> nested = nested(effect);
        if (nested.isEmpty()) {
            return;
        }
        int generation = context.reloadGeneration();
        scheduler.region(location, () -> {
            ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class, marker -> {
                marker.setInvisible(true);
                marker.setMarker(true);
                marker.setGravity(false);
                marker.setInvulnerable(true);
            });
            EffectContext totemCtx = ctx.withLocation(stand.getLocation());
            TaskHandle[] holder = new TaskHandle[1];
            int[] elapsed = {0};
            holder[0] = scheduler.entityTimer(stand, () -> {
                if (context.reloadGeneration() != generation || !stand.isValid() || elapsed[0] >= duration) {
                    stand.remove();
                    if (holder[0] != null) {
                        holder[0].cancel();
                    }
                    return;
                }
                elapsed[0] += interval;
                context.effectExecutor().runEffects(totemCtx, nested);
            }, interval, interval);
            scheduler.entityLater(stand, stand::remove, duration + 1L);
        });
    }

    void giveItem(EffectContext ctx, EffectDefinition effect) {
        Material material = Material.matchMaterial(effect.text("material", effect.text("item", "STONE"))
                .toUpperCase(Locale.ROOT));
        if (material == null) {
            return;
        }
        int amount = Math.max(1, effect.integer("amount", 1));
        ItemStack item = new ItemStack(material, amount);
        String name = effect.text("name", null);
        if (name != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(ColorUtil.component(name));
                item.setItemMeta(meta);
            }
        }
        Player caster = ctx.caster();
        scheduler.entity(caster, () -> caster.getInventory().addItem(item));
    }

    void repeat(EffectContext ctx, EffectDefinition effect) {
        int times = Math.max(1, effect.integer("times", effect.integer("count", 1)));
        List<EffectDefinition> nested = nested(effect);
        if (nested.isEmpty()) {
            return;
        }
        for (int i = 0; i < times && !ctx.vars().isAborted(); i++) {
            ctx.vars().setNumber("i", i);
            context.effectExecutor().runEffects(ctx, nested);
        }
    }

    void aura(EffectContext ctx, EffectDefinition effect) {
        int duration = effect.integer("duration", 100);
        int interval = Math.max(1, effect.integer("interval", 20));
        double radius = effect.number("radius", 4.0);
        boolean hitPlayers = effect.bool("hit_players", true);
        boolean hitNonPlayers = effect.bool("hit_non_players", true);
        boolean includeCaster = effect.bool("include_caster", false);
        List<EffectDefinition> nested = nested(effect);
        if (nested.isEmpty()) {
            return;
        }
        Player caster = ctx.caster();
        int generation = context.reloadGeneration();
        int[] elapsed = {0};
        TaskHandle[] holder = new TaskHandle[1];
        holder[0] = scheduler.entityTimer(caster, () -> {
            if (context.reloadGeneration() != generation || !caster.isOnline() || elapsed[0] >= duration) {
                if (holder[0] != null) {
                    holder[0].cancel();
                }
                return;
            }
            elapsed[0] += interval;
            Location center = caster.getLocation();
            for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }
                if (!includeCaster && living.getUniqueId().equals(caster.getUniqueId())) {
                    continue;
                }
                if (living instanceof Player ? !hitPlayers : !hitNonPlayers) {
                    continue;
                }
                EffectEntityDispatch.runEffectsOnTarget(scheduler, context, ctx, living, nested);
            }
        }, interval, interval);
    }

    void shape(EffectContext ctx, EffectDefinition effect) {
        Location center = EffectTargetHelper.location(context, ctx, effect).clone().add(0, effect.number("y", 1.0), 0);
        String shape = effect.text("shape", "circle").toLowerCase(Locale.ROOT);
        Particle particle = EffectKeys.resolveParticle(effect.text("particle", "FLAME"));
        Sound sound = effect.data().containsKey("sound") ? EffectKeys.resolveSound(effect.text("sound", "")) : null;
        double radius = effect.number("radius", 2.0);
        int points = Math.max(1, effect.integer("points", 24));
        scheduler.region(center, () -> {
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                Location point = switch (shape) {
                    case "line" -> center.clone().add(center.getDirection().clone().multiply(radius * i / points));
                    case "sphere" -> spherePoint(center, radius, i, points);
                    default -> center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                };
                if (particle != null) {
                    point.getWorld().spawnParticle(particle, point, effect.integer("count", 1), 0, 0, 0,
                            effect.number("speed", 0));
                }
                if (sound != null && i % Math.max(1, points / 4) == 0) {
                    point.getWorld().playSound(point, sound, (float) effect.number("volume", 0.6),
                            (float) effect.number("pitch", 1.0));
                }
            }
        });
    }

    private Location spherePoint(Location center, double radius, int i, int points) {
        double phi = Math.acos(1 - 2.0 * (i + 0.5) / points);
        double theta = Math.PI * (1 + Math.sqrt(5)) * i;
        double x = radius * Math.sin(phi) * Math.cos(theta);
        double y = radius * Math.cos(phi);
        double z = radius * Math.sin(phi) * Math.sin(theta);
        return center.clone().add(x, y, z);
    }

    private List<EffectDefinition> nested(EffectDefinition effect) {
        List<EffectDefinition> nested = effect.children("effects");
        return nested.isEmpty() ? effect.nested() : nested;
    }

    private void applyVelocity(LivingEntity entity, Vector velocity) {
        scheduler.entity(entity, () -> entity.setVelocity(velocity));
    }

    private Vector normalizeOrZero(Vector vector) {
        return vector.lengthSquared() < 1.0e-6 ? new Vector(0, 0, 0) : vector.normalize();
    }

    private EntityType resolveEntityType(String raw) {
        try {
            return EntityType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
