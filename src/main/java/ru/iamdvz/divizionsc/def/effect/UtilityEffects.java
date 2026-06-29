package ru.iamdvz.divizionsc.def.effect;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import ru.iamdvz.divizionsc.PluginContext;
import ru.iamdvz.divizionsc.def.model.EffectDefinition;
import ru.iamdvz.divizionsc.platform.Scheduler;
import ru.iamdvz.divizionsc.util.ColorUtil;

/** Утилитарные эффекты: огонь, свечение, титры, обмен местами, взрыв, очищение, подброс, корень. */
final class UtilityEffects {

    private static final List<PotionEffectType> DEBUFFS = List.of(
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS,
            PotionEffectType.BLINDNESS,
            PotionEffectType.NAUSEA,
            PotionEffectType.HUNGER,
            PotionEffectType.LEVITATION,
            PotionEffectType.DARKNESS
    );

    private final PluginContext context;
    private final Scheduler scheduler;

    UtilityEffects(PluginContext context, Scheduler scheduler) {
        this.context = context;
        this.scheduler = scheduler;
    }

    void ignite(EffectContext ctx, EffectDefinition effect) {
        LivingEntity target = EffectTargetHelper.living(context, ctx, effect);
        if (target == null) {
            return;
        }
        int ticks = Math.max(1, effect.integer("ticks", effect.integer("duration", 60)));
        scheduler.entity(target, () -> target.setFireTicks(ticks));
    }

    void glow(EffectContext ctx, EffectDefinition effect) {
        LivingEntity target = EffectTargetHelper.living(context, ctx, effect);
        if (target == null) {
            return;
        }
        int duration = effect.integer("duration", 100);
        int amplifier = Math.max(0, effect.integer("amplifier", 0));
        scheduler.entity(target, () ->
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, amplifier, false, false)));
    }

    void invis(EffectContext ctx, EffectDefinition effect) {
        LivingEntity target = EffectTargetHelper.living(context, ctx, effect);
        if (target == null) {
            return;
        }
        int duration = effect.integer("duration", 100);
        scheduler.entity(target, () ->
                target.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, false, false)));
    }

    void title(EffectContext ctx, EffectDefinition effect) {
        Player player = resolvePlayer(ctx, effect);
        if (player == null) {
            return;
        }
        String actionbar = effect.text("actionbar", null);
        if (actionbar != null && !actionbar.isBlank()) {
            scheduler.entity(player, () -> player.sendActionBar(ColorUtil.component(actionbar)));
            return;
        }
        String titleText = effect.text("title", effect.text("main", ""));
        String subtitle = effect.text("subtitle", effect.text("sub", ""));
        int fadeIn = effect.integer("fade_in", effect.integer("fadein", 10));
        int stay = effect.integer("stay", 70);
        int fadeOut = effect.integer("fade_out", effect.integer("fadeout", 20));
        Title title = Title.title(
                ColorUtil.component(titleText),
                ColorUtil.component(subtitle),
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        );
        scheduler.entity(player, () -> player.showTitle(title));
    }

    void swap(EffectContext ctx, EffectDefinition effect) {
        Player caster = ctx.caster();
        LivingEntity target = EffectTargetHelper.living(context, ctx, effect);
        if (target == null || target.equals(caster)) {
            return;
        }
        Location casterLoc = caster.getLocation().clone();
        Location targetLoc = target.getLocation().clone();
        scheduler.entity(caster, () -> caster.teleport(targetLoc));
        scheduler.entity(target, () -> target.teleport(casterLoc));
    }

    void explosion(EffectContext ctx, EffectDefinition effect) {
        Location location = EffectTargetHelper.location(context, ctx, effect);
        float power = (float) effect.number("power", effect.number("yield", 0));
        boolean fire = effect.bool("fire", false);
        boolean breakBlocks = effect.bool("break", effect.bool("break_blocks", false));
        scheduler.region(location, () -> location.getWorld().createExplosion(location, power, fire, breakBlocks));
    }

    void cleanse(EffectContext ctx, EffectDefinition effect) {
        LivingEntity target = EffectTargetHelper.living(context, ctx, effect);
        if (target == null) {
            return;
        }
        scheduler.entity(target, () -> {
            for (PotionEffectType type : DEBUFFS) {
                target.removePotionEffect(type);
            }
        });
    }

    void launch(EffectContext ctx, EffectDefinition effect) {
        LivingEntity target = EffectTargetHelper.living(context, ctx, effect);
        if (target == null) {
            return;
        }
        double power = effect.number("power", effect.number("y", 1.0));
        Vector velocity = target.getVelocity();
        velocity.setY(power);
        scheduler.entity(target, () -> target.setVelocity(velocity));
    }

    void root(EffectContext ctx, EffectDefinition effect) {
        LivingEntity target = EffectTargetHelper.living(context, ctx, effect);
        if (target == null) {
            return;
        }
        int duration = effect.integer("duration", 40);
        scheduler.entity(target, () -> {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 255, false, false, false));
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 128, false, false, false));
        });
    }

    private Player resolvePlayer(EffectContext ctx, EffectDefinition effect) {
        String at = effect.text("at", effect.text("target", "caster")).toLowerCase(Locale.ROOT);
        if ("target".equals(at) || "entity".equals(at)) {
            LivingEntity living = EffectTargetHelper.living(context, ctx, effect);
            return living instanceof Player player ? player : null;
        }
        return ctx.caster();
    }
}
