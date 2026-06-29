package ru.iamdvz.divizionsc.def.condition;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiPredicate;
import java.util.function.ToDoubleFunction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.iamdvz.divizionsc.def.effect.DistanceUtil;
import ru.iamdvz.divizionsc.def.effect.EffectContext;
import ru.iamdvz.divizionsc.def.expr.ExpressionEvaluator;
import ru.iamdvz.divizionsc.def.expr.PlaceholderResolver;

/**
 * Вычисляет {@link ConditionSpec} в контексте каста.
 */
public final class ConditionEvaluator {

    private final PlaceholderResolver placeholders;
    private final ExpressionEvaluator expressions;

    /** Проверка региона WorldGuard (caster, regionId) — подключается интеграцией. */
    private volatile BiPredicate<Player, String> regionCheck = (player, region) -> false;

    /** Баланс игрока (Vault) — подключается интеграцией. */
    private volatile ToDoubleFunction<Player> balanceProvider = player -> 0.0;

    public ConditionEvaluator(PlaceholderResolver placeholders, ExpressionEvaluator expressions) {
        this.placeholders = placeholders;
        this.expressions = expressions;
    }

    public void setRegionCheck(BiPredicate<Player, String> check) {
        this.regionCheck = check == null ? (player, region) -> false : check;
    }

    public void setBalanceProvider(ToDoubleFunction<Player> provider) {
        this.balanceProvider = provider == null ? player -> 0.0 : provider;
    }

    public boolean matchesAll(List<String> rawConditions, EffectContext ctx) {
        if (rawConditions == null || rawConditions.isEmpty()) {
            return true;
        }
        for (String raw : rawConditions) {
            if (!matches(ConditionParser.parse(raw), ctx)) {
                return false;
            }
        }
        return true;
    }

    public boolean matches(ConditionSpec spec, EffectContext ctx) {
        boolean result = evaluate(spec, ctx);
        return spec.negate() != result;
    }

    private boolean evaluate(ConditionSpec spec, EffectContext ctx) {
        Player caster = ctx.caster();
        return switch (spec.type()) {
            case "health" -> compareHealth(spec, ctx, caster);
            case "distance" -> compareNumeric(spec, ctx, DistanceUtil.between(ctx));
            case "permission" -> caster != null && caster.hasPermission(spec.firstArg("")); 
            case "holding" -> caster != null && holding(caster, spec.firstArg(""));
            case "sneaking" -> caster != null && caster.isSneaking();
            case "on-ground" -> caster != null && caster.isOnGround();
            case "has-target" -> ctx.targetEntity() != null;
            case "world" -> caster != null && caster.getWorld().getName().equalsIgnoreCase(spec.firstArg(""));
            case "region" -> caster != null && regionCheck.test(caster, spec.firstArg(""));
            case "money", "balance" -> caster != null
                    && compareNumeric(spec, ctx, balanceProvider.applyAsDouble(caster));
            case "chance" -> ThreadLocalRandom.current().nextDouble() < chanceValue(spec, ctx);
            case "variable" -> compareVariable(spec, ctx);
            default -> compareGeneric(spec, ctx);
        };
    }

    @SuppressWarnings("deprecation")
    private boolean compareHealth(ConditionSpec spec, EffectContext ctx, Player caster) {
        if (caster == null) {
            return false;
        }
        double health = caster.getHealth();
        String arg = spec.firstArg("0");
        double right;
        if (arg.endsWith("%")) {
            Double pct = ExpressionEvaluator.tryParse(arg);
            right = pct == null ? 0 : caster.getMaxHealth() * pct / 100.0;
        } else {
            right = expressions.evaluate(arg, ctx, 0);
        }
        return spec.comparison().test(health, right);
    }

    private boolean compareNumeric(ConditionSpec spec, EffectContext ctx, double left) {
        double right = expressions.evaluate(spec.firstArg("0"), ctx, 0);
        return spec.comparison().test(left, right);
    }

    private boolean compareVariable(ConditionSpec spec, EffectContext ctx) {
        String name = spec.firstArg("");
        if (spec.hasComparison()) {
            double left = ctx.vars().getNumber(name, 0);
            double right = expressions.evaluate(spec.arg(1, "0"), ctx, 0);
            return spec.comparison().test(left, right);
        }
        return ctx.vars().hasNumber(name) ? ctx.vars().getNumber(name, 0) != 0
                : !ctx.vars().getString(name, "").isEmpty();
    }

    private boolean compareGeneric(ConditionSpec spec, EffectContext ctx) {
        if (spec.hasComparison()) {
            double left = expressions.evaluate(spec.type(), ctx, Double.NaN);
            double right = expressions.evaluate(spec.firstArg("0"), ctx, Double.NaN);
            if (!Double.isNaN(left) && !Double.isNaN(right)) {
                return spec.comparison().test(left, right);
            }
        }
        String resolved = placeholders.resolve(spec.raw(), ctx).trim();
        return resolved.equalsIgnoreCase("true") || resolved.equals("1");
    }

    private double chanceValue(ConditionSpec spec, EffectContext ctx) {
        String arg = spec.firstArg("0.5");
        double value = expressions.evaluate(arg, ctx, 0.5);
        return arg.endsWith("%") ? value / 100.0 : value;
    }

    private boolean holding(Player caster, String material) {
        var item = caster.getInventory().getItemInMainHand();
        return !item.isEmpty() && item.getType().name().equalsIgnoreCase(material);
    }
}

