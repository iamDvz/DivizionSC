package ru.iamdvz.divizionsc.def.effect;

/**
 * Единая семантика дистанции caster → target для условий, формул и плейсхолдеров.
 */
public final class DistanceUtil {

    /** Нет цели в том же мире — «бесконечная» дистанция для сравнений if distance &lt; N. */
    public static final double NO_TARGET = Double.MAX_VALUE;

    private DistanceUtil() {
    }

    public static double between(EffectContext ctx) {
        if (ctx.caster() == null) {
            return NO_TARGET;
        }
        if (ctx.targetEntity() != null) {
            return ctx.caster().getLocation().distance(ctx.targetEntity().getLocation());
        }
        if (ctx.targetLocation() != null && ctx.targetLocation().getWorld() == ctx.caster().getWorld()) {
            return ctx.caster().getLocation().distance(ctx.targetLocation());
        }
        return NO_TARGET;
    }

    /** Для плейсхолдеров и формул: нет цели → 0. */
    public static double forFormula(EffectContext ctx) {
        double value = between(ctx);
        return value == NO_TARGET ? 0 : value;
    }
}
