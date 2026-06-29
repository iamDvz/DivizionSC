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
    private final AdvancedEffects advanced;
    private final ExtraEffects extra;
    private final UtilityEffects utility;

    BuiltinEffectRegistry(JavaPlugin plugin, PluginContext context) {
        this.plugin = plugin;
        this.context = context;
        this.advanced = new AdvancedEffects(
                context,
                context.scheduler(),
                context.stunService()
        );
        this.extra = new ExtraEffects(context, context.scheduler());
        this.utility = new UtilityEffects(context, context.scheduler());
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
        register("if", this::conditionalIf);
        register("require", this::require);
        register("chance", this::chance);
        register("set", this::setVar);
        register("set_var", this::setVar);
        register("setvar", this::setVar);
        register("loop", advanced::loop);
        register("area", advanced::area);
        register("stun", advanced::stun);
        register("raycast", advanced::raycast);
        register("beam", advanced::raycast);
        register("chain", advanced::chain);
        register("particle_projectile", advanced::particleProjectile);
        register("ppj", advanced::particleProjectile);
        register("pull", extra::pull);
        register("push", extra::push);
        register("money", extra::money);
        register("give-money", extra::giveMoney);
        register("take-money", extra::takeMoney);
        register("dash", extra::dash);
        register("blink", extra::blink);
        register("shield", extra::shield);
        register("summon", extra::summon);
        register("totem", extra::totem);
        register("give", extra::giveItem);
        register("give_item", extra::giveItem);
        register("giveitem", extra::giveItem);
        register("repeat", extra::repeat);
        register("shape", extra::shape);
        register("shape_particle", extra::shape);
        register("aura", extra::aura);
        register("ignite", utility::ignite);
        register("glow", utility::glow);
        register("glowing", utility::glow);
        register("invis", utility::invis);
        register("invisibility", utility::invis);
        register("title", utility::title);
        register("swap", utility::swap);
        register("explosion", utility::explosion);
        register("explode", utility::explosion);
        register("cleanse", utility::cleanse);
        register("purge", utility::cleanse);
        register("launch", utility::launch);
        register("root", utility::root);
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

    private List<String> conditionStrings(EffectDefinition effect) {
        Object raw = effect.data().get("conditions");
        if (raw == null) {
            raw = effect.data().get("condition");
        }
        List<String> result = new java.util.ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                result.add(String.valueOf(item));
            }
        } else if (raw instanceof String text && !text.isBlank()) {
            result.add(text);
        }
        return result;
    }

    private List<EffectDefinition> effectList(EffectDefinition effect, String key) {
        Object value = effect.data().get(key);
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(EffectDefinition.class::isInstance)
                    .map(EffectDefinition.class::cast)
                    .toList();
        }
        return List.of();
    }

    private void conditionalIf(EffectContext ctx, EffectDefinition effect) {
        boolean ok = context.conditions().matchesAll(conditionStrings(effect), ctx);
        if (ok) {
            List<EffectDefinition> branch = effectList(effect, "effects");
            if (branch.isEmpty()) {
                branch = effectList(effect, "then");
            }
            if (branch.isEmpty()) {
                branch = effect.nested();
            }
            context.effectExecutor().runEffects(ctx, branch);
        } else {
            context.effectExecutor().runEffects(ctx, effectList(effect, "else"));
        }
    }

    private void require(EffectContext ctx, EffectDefinition effect) {
        if (!context.conditions().matchesAll(conditionStrings(effect), ctx)) {
            ctx.vars().abort();
        }
    }

    private void chance(EffectContext ctx, EffectDefinition effect) {
        double probability = effect.number("chance", effect.number("value", effect.number("p", 0.5)));
        if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < probability) {
            List<EffectDefinition> branch = effectList(effect, "effects");
            if (branch.isEmpty()) {
                branch = effect.nested();
            }
            context.effectExecutor().runEffects(ctx, branch);
        } else {
            context.effectExecutor().runEffects(ctx, effectList(effect, "else"));
        }
    }

    private void setVar(EffectContext ctx, EffectDefinition effect) {
        String name = effect.text("var", effect.text("name", effect.text("key", "")));
        if (name.isBlank()) {
            return;
        }
        if (effect.data().containsKey("string")) {
            ctx.vars().setString(name, context.placeholders().resolve(effect.text("string", ""), ctx));
        } else {
            String formula = effect.text("value", effect.text("expr", "0"));
            ctx.vars().setNumber(name, context.expressions().evaluate(formula, ctx, 0));
        }
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
        String raw = effect.text("text", effect.text("message", "&7..."));
        String text = context.placeholders().resolve(raw, ctx);
        ctx.caster().sendMessage(ColorUtil.component(text));
    }

    private void sound(EffectContext ctx, EffectDefinition effect) {
        String raw = effect.text("sound", "entity.experience_orb.pickup");
        Sound sound = EffectKeys.resolveSound(raw);
        if (sound == null) {
            plugin.getLogger().warning("Unknown sound: " + raw);
            return;
        }
        Location location = EffectTargetHelper.location(context, ctx, effect);
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
        Location location = EffectTargetHelper.location(context, ctx, effect).clone().add(0, 1.0, 0);
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
        LivingEntity entity = EffectTargetHelper.living(context, ctx, effect);
        if (entity == null) {
            return;
        }
        double amount = resolveAmount(ctx, effect, "amount", 1.0);
        EffectEntityDispatch.mutate(context.scheduler(), entity,
                () -> entity.damage(amount, ctx.caster()));
    }

    /** Числовое поле эффекта: число — как есть, строка — как формула/переменная каста ({@code set}). */
    private double resolveAmount(EffectContext ctx, EffectDefinition effect, String key, double fallback) {
        Object raw = effect.data().get(key);
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw instanceof String text && !text.isBlank()) {
            return context.expressions().evaluate(text, ctx, fallback);
        }
        return fallback;
    }

    private void heal(EffectContext ctx, EffectDefinition effect) {
        LivingEntity entity = EffectTargetHelper.living(context, ctx, effect);
        if (entity == null) {
            return;
        }
        double amount = resolveAmount(ctx, effect, "amount", 4.0);
        EffectEntityDispatch.mutate(context.scheduler(), entity, () -> {
            double newHealth = Math.min(entity.getMaxHealth(), entity.getHealth() + amount);
            entity.setHealth(newHealth);
        });
    }

    private void potion(EffectContext ctx, EffectDefinition effect) {
        PotionEffectType type = parsePotionEffect(effect.text("effect", "SPEED"));
        if (type == null) {
            return;
        }
        LivingEntity entity = EffectTargetHelper.living(context, ctx, effect);
        if (entity == null) {
            return;
        }
        int duration = effect.integer("duration", 100);
        int amplifier = effect.integer("amplifier", 0);
        EffectEntityDispatch.mutate(context.scheduler(), entity,
                () -> entity.addPotionEffect(new PotionEffect(type, duration, amplifier)));
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
        EffectEntityDispatch.mutate(context.scheduler(), entity, () -> entity.setVelocity(vector));
    }

    private void teleport(EffectContext ctx, EffectDefinition effect) {
        context.scheduler().entity(ctx.caster(), () -> {
            Location dest = resolveTeleportDestination(ctx, effect);
            context.scheduler().region(dest, () -> {
                Location finalDest = effect.bool("safe", true) ? findSafeLocation(dest) : dest;
                ctx.caster().teleport(finalDest);
            });
        });
    }

    private Location resolveTeleportDestination(EffectContext ctx, EffectDefinition effect) {
        if (effect.data().containsKey("forward")) {
            double distance = effect.number("forward", 5.0);
            return ctx.caster().getLocation().add(
                    ctx.caster().getLocation().getDirection().normalize().multiply(distance)
            );
        }
        if (ctx.targetLocation() != null && !effect.bool("to_caster", false)) {
            return ctx.targetLocation().clone();
        }
        return ctx.effectLocation().clone();
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
        Location location = EffectTargetHelper.location(context, ctx, effect);
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
        context.scheduler().entityLater(ctx.caster(), () -> {
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
        List<EffectDefinition> onTick = effect.children("on_tick");
        int tickInterval = Math.max(1, effect.integer("tick_interval", 1));
        projectile.setMetadata(
                EffectExecutor.PROJECTILE_META,
                new FixedMetadataValue(plugin, new EffectExecutor.ProjectilePayload(ctx, onHit))
        );
        if (!onTick.isEmpty() && projectile instanceof Projectile launched) {
            advanced.projectileTick(launched, ctx, onTick, tickInterval);
        }
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
