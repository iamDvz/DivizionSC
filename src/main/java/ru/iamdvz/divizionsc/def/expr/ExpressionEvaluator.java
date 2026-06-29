package ru.iamdvz.divizionsc.def.expr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import ru.iamdvz.divizionsc.def.effect.DistanceUtil;
import ru.iamdvz.divizionsc.def.effect.EffectContext;

/**
 * Вычисляет числовые формулы через exp4j. Плейсхолдеры подставляются заранее,
 * затем встроенные переменные каста передаются как exp4j-переменные.
 */
public final class ExpressionEvaluator {

    private static final Pattern VARIABLE = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    private final PlaceholderResolver placeholders;

    public ExpressionEvaluator(PlaceholderResolver placeholders) {
        this.placeholders = placeholders;
    }

    /** Быстрый разбор числа без контекста (для статических значений). */
    public static Double tryParse(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.endsWith("%")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public double evaluate(String formula, EffectContext ctx, double fallback) {
        if (formula == null || formula.isBlank()) {
            return fallback;
        }
        Double direct = tryParse(formula);
        if (direct != null) {
            return direct;
        }
        String resolved = placeholders.resolve(formula, ctx);
        Double afterResolve = tryParse(resolved);
        if (afterResolve != null) {
            return afterResolve;
        }
        try {
            Map<String, Double> variables = collectVariables(resolved, ctx);
            Expression expression = new ExpressionBuilder(resolved)
                    .variables(variables.keySet())
                    .build()
                    .setVariables(variables);
            return expression.evaluate();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Map<String, Double> collectVariables(String formula, EffectContext ctx) {
        Map<String, Double> variables = new HashMap<>();
        Set<String> reserved = Set.of("pi", "e");
        Matcher matcher = VARIABLE.matcher(formula);
        while (matcher.find()) {
            String name = matcher.group();
            if (reserved.contains(name.toLowerCase()) || variables.containsKey(name)) {
                continue;
            }
            variables.put(name, resolveVariable(name, ctx));
        }
        return variables;
    }

    private double resolveVariable(String name, EffectContext ctx) {
        if (ctx.vars().hasNumber(name)) {
            return ctx.vars().getNumber(name, 0);
        }
        return switch (name.toLowerCase()) {
            case "health" -> ctx.caster() != null ? ctx.caster().getHealth() : 0;
            case "level" -> ctx.caster() != null ? ctx.caster().getLevel() : 0;
            case "distance" -> DistanceUtil.forFormula(ctx);
            case "chain_depth", "depth" -> ctx.chainDepth();
            default -> 0;
        };
    }
}

