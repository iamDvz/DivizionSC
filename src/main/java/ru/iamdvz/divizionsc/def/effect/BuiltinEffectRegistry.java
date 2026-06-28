package ru.iamdvz.divizionsc.def.effect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.ChainEntry;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.divizionsc.util.ColorUtil;
import ru.iamdvz.divizionsc.util.EffectKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

final class BuiltinEffectRegistry {

    private final Map<String, BiConsumer<EffectContext, EffectDefinition>> handlers = new HashMap<>();
    private final JavaPlugin plugin;
    private final PluginContext context;

    BuiltinEffectRegistry(JavaPlugin plugin, PluginContext context) {
        this.plugin = plugin;
        this.context = context;
        register("message", this::message);
        register("sound", this::sound);
        register("particle", this::particle);
        register("damage", this::damage);
        register("heal", this::heal);
        register("potion", this::potion);
        register("potioneffect", this::potion);
        register("velocity", this::velocity);
        register("knockback", this::velocity);
        register("teleport", this::teleport);
        register("tp", this::teleport);
        register("lightning", this::lightning);
        register("command", this::command);
        register("delay", this::delay);
        register("projectile", this::projectile);
        register("def", this::invokeDef);
        register("chain", this::invokeDef);
        register("ability", this::invokeDef);
        register("effectlib", (ctx, effect) -> context.effectLib().play(ctx, effect));
    }

    boolean run(String type, EffectContext ctx, EffectDefinition effect) {
        BiConsumer<EffectContext, EffectDefinition> handler = handlers.get(type.toLowerCase(Locale.ROOT));
        if (handler == null) {
            return false;
        }
        handler.accept(ctx, effect);
        return true;
    }

    private void register(String type, BiConsumer<EffectContext, EffectDefinition> handler) {
        handlers.put(type.toLowerCase(Locale.ROOT), handler);
    }

    private void invokeDef(EffectContext ctx, EffectDefinition effect) {
        String defId = effect.text("def", effect.text("ability", effect.text("id", "")));
        if (defId.isBlank()) {
            return;
        }
        Map<String, Object> args = effect.map("args");
        context.defs().invokeChained(ctx, new ChainEntry(defId, args), ctx.chainDepth() + 1);
    }

    private void message(EffectContext ctx, EffectDefinition effect) {
        String text = effect.text("text", effect.text("message", "&7..."));
        ctx.caster().sendMessage(ColorUtil.component(text));
    }

    private void sound(EffectContext ctx, EffectDefinition effect) {
        String raw = effect.text("sound", "entity.experience_orb.pickup");
        Sound sound = EffectKeys.resolveSound(raw);
        if (sound == null) {
            plugin.getLogger().warning("Unknown sound: " + raw);
            return;
        }
        Location location = ctx.effectLocation();
        float volume = (float) effect.number("volume", 1.0);
        float pitch = (float) effect.number("pitch", 1.0);
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    private void particle(EffectContext ctx, EffectDefinition effect) {
        String raw = effect.text("particle", "FLAME");
        Particle particle = EffectKeys.resolveParticle(raw);
        if (particle == null) {
            plugin.getLogger().warning("Unknown particle: " + raw);
            return;
        }
        Location location = ctx.effectLocation().clone().add(0, 1.0, 0);
        int count = effect.integer("count", 10);
        double offset = effect.number("offset", 0.3);
        location.getWorld().spawnParticle(
                particle,
                location,
                count,
                offset, offset, offset,
                effect.number("speed", 0.02)
        );
    }

    private void damage(EffectContext ctx, EffectDefinition effect) {
        LivingEntity entity = resolveLivingTarget(ctx, effect);
        if (entity == null) {
            return;
        }
        double amount = effect.number("amount", 1.0);
        entity.damage(amount, ctx.caster());
    }

    private LivingEntity resolveLivingTarget(EffectContext ctx, EffectDefinition effect) {
        String target = effect.text("target", "entity").toLowerCase(Locale.ROOT);
        return switch (target) {
            case "caster", "self" -> ctx.caster();
            case "target" -> ctx.targetEntity() != null ? ctx.targetEntity() : ctx.caster();
            default -> ctx.effectEntity();
        };
    }

    private void heal(EffectContext ctx, EffectDefinition effect) {
        LivingEntity entity = resolveLivingTarget(ctx, effect);
        if (entity == null) {
            return;
        }
        double amount = effect.number("amount", 4.0);
        double newHealth = Math.min(entity.getMaxHealth(), entity.getHealth() + amount);
        entity.setHealth(newHealth);
    }

    private void potion(EffectContext ctx, EffectDefinition effect) {
        PotionEffectType type = parsePotionEffect(effect.text("effect", "SPEED"));
        if (type == null) {
            return;
        }
        int duration = effect.integer("duration", 100);
        int amplifier = effect.integer("amplifier", 0);
        ctx.effectEntity().addPotionEffect(new PotionEffect(type, duration, amplifier));
    }

    private void velocity(EffectContext ctx, EffectDefinition effect) {
        LivingEntity entity = ctx.effectEntity();
        Vector vector;
        if (effect.data().containsKey("x") || effect.data().containsKey("y") || effect.data().containsKey("z")) {
            vector = new Vector(effect.number("x", 0), effect.number("y", 0), effect.number("z", 0));
        } else {
            double power = effect.number("power", 1.0);
            vector = entity.getLocation().getDirection().normalize().multiply(power);
            vector.setY(effect.number("y", vector.getY() + 0.3));
        }
        entity.setVelocity(vector);
    }

    private void teleport(EffectContext ctx, EffectDefinition effect) {
        Location destination;
        if (effect.data().containsKey("forward")) {
            double distance = effect.number("forward", 5.0);
            destination = ctx.caster().getLocation().add(
                    ctx.caster().getLocation().getDirection().normalize().multiply(distance)
            );
        } else if (ctx.targetLocation() != null && !effect.bool("to_caster", false)) {
            destination = ctx.targetLocation().clone();
        } else {
            destination = ctx.effectLocation().clone();
        }

        if (effect.bool("safe", true)) {
            destination = findSafeLocation(destination);
        }

        ctx.caster().teleport(destination);
    }

    private Location findSafeLocation(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        for (int y = location.getBlockY(); y < world.getMaxHeight() - 1; y++) {
            Location candidate = new Location(world, x + 0.5, y, z + 0.5, location.getYaw(), location.getPitch());
            if (isSafeStandingSpot(candidate)) {
                return candidate;
            }
        }
        for (int y = location.getBlockY() - 1; y > world.getMinHeight(); y--) {
            Location candidate = new Location(world, x + 0.5, y, z + 0.5, location.getYaw(), location.getPitch());
            if (isSafeStandingSpot(candidate)) {
                return candidate;
            }
        }
        return location;
    }

    private boolean isSafeStandingSpot(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block ground = feet.getRelative(0, -1, 0);
        return feet.isPassable() && head.isPassable() && ground.getType().isSolid();
    }

    private void lightning(EffectContext ctx, EffectDefinition effect) {
        Location location = ctx.effectLocation();
        if (effect.bool("damage", true)) {
            location.getWorld().strikeLightning(location);
        } else {
            location.getWorld().strikeLightningEffect(location);
        }
    }

    private void command(EffectContext ctx, EffectDefinition effect) {
        String raw = effect.text("command", "");
        if (raw.isBlank()) {
            return;
        }
        String parsed = raw
                .replace("%player%", ctx.caster().getName())
                .replace("%uuid%", ctx.caster().getUniqueId().toString())
                .replace("%def%", ctx.def().id())
                .replace("%ability%", ctx.def().id());
        boolean asPlayer = effect.bool("as_player", false);
        if (asPlayer) {
            Bukkit.dispatchCommand(ctx.caster(), parsed);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    private void delay(EffectContext ctx, EffectDefinition effect) {
        long ticks = effect.integer("ticks", 1);
        List<EffectDefinition> nested = effect.children("effects");
        if (nested.isEmpty()) {
            nested = effect.nested();
        }
        List<EffectDefinition> delayed = List.copyOf(nested);
        int generation = context.reloadGeneration();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (context.reloadGeneration() != generation) {
                return;
            }
            if (!ctx.caster().isOnline()) {
                return;
            }
            context.effectExecutor().runEffects(ctx, delayed);
        }, ticks);
    }

    private void projectile(EffectContext ctx, EffectDefinition effect) {
        String typeName = effect.text("projectile", "SNOWBALL").toUpperCase(Locale.ROOT);
        Location spawn = ctx.caster().getEyeLocation();
        double speed = effect.number("speed", 1.2);

        Entity projectile = switch (typeName) {
            case "FIREBALL" -> ctx.caster().launchProjectile(Fireball.class);
            case "POTION", "SPLASH_POTION" -> ctx.caster().launchProjectile(ThrownPotion.class);
            default -> ctx.caster().launchProjectile(Snowball.class);
        };

        if (projectile instanceof Projectile launched) {
            launched.setShooter(ctx.caster());
            Vector velocity = spawn.getDirection().normalize().multiply(speed);
            launched.setVelocity(velocity);
        }

        if (projectile instanceof Fireball fireball) {
            fireball.setIsIncendiary(effect.bool("incendiary", false));
            fireball.setYield((float) effect.number("yield", 0.0));
        }

        List<EffectDefinition> onHit = effect.children("on_hit");
        projectile.setMetadata(
                EffectExecutor.PROJECTILE_META,
                new FixedMetadataValue(plugin, new EffectExecutor.ProjectilePayload(ctx, onHit))
        );
    }

    private PotionEffectType parsePotionEffect(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.toLowerCase(Locale.ROOT).replace('_', '.');
        NamespacedKey key = NamespacedKey.fromString(normalized);
        if (key == null) {
            key = NamespacedKey.minecraft(normalized);
        }
        return Registry.POTION_EFFECT_TYPE.get(key);
    }
}
