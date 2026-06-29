package ru.iamdvz.divizionsc.def.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Парсит строку-условие в {@link ConditionSpec}.
 * <p>
 * Поддерживаемые формы:
 * <pre>
 *   health < 50%
 *   target.distance <= 10
 *   !holding DIAMOND_SWORD
 *   not in-region spawn
 *   chance 0.3
 *   variable combo >= 3
 *   permission divizionsc.vip
 * </pre>
 * Тип условия и его семантика интерпретируются эвалуаторами на этапе исполнения.
 */
public final class ConditionParser {

    private ConditionParser() {
    }

    public static ConditionSpec parse(String raw) {
        if (raw == null) {
            return new ConditionSpec("", false, Comparison.NONE, List.of(), "");
        }
        String trimmed = raw.trim();
        boolean negate = false;
        String body = trimmed;
        if (body.startsWith("!") && !body.startsWith("!=")) {
            negate = true;
            body = body.substring(1).trim();
        } else if (body.toLowerCase(Locale.ROOT).startsWith("not ")) {
            negate = true;
            body = body.substring(4).trim();
        }

        String spaced = body.replaceAll("(<=|>=|=<|=>|==|!=|<>|<|>|=)", " $1 ").trim();
        String[] tokens = spaced.isEmpty() ? new String[0] : spaced.split("\\s+");

        if (tokens.length == 0) {
            return new ConditionSpec("", negate, Comparison.NONE, List.of(), trimmed);
        }

        String type = normalizeType(tokens[0]);
        Comparison comparison = Comparison.NONE;
        List<String> args = new ArrayList<>();
        for (int i = 1; i < tokens.length; i++) {
            Comparison candidate = Comparison.fromSymbol(tokens[i]);
            if (candidate != null && comparison == Comparison.NONE) {
                comparison = candidate;
            } else {
                args.add(tokens[i]);
            }
        }
        return new ConditionSpec(type, negate, comparison, args, trimmed);
    }

    private static String normalizeType(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "hp", "health" -> "health";
            case "dist", "distance", "target.distance" -> "distance";
            case "perm", "permission", "has-permission" -> "permission";
            case "var", "variable" -> "variable";
            case "holding", "hand", "mainhand" -> "holding";
            case "region", "in-region", "wg-region" -> "region";
            case "world" -> "world";
            case "chance", "roll" -> "chance";
            case "sneaking", "sneak" -> "sneaking";
            case "on-ground", "grounded" -> "on-ground";
            case "target", "has-target" -> "has-target";
            default -> lower;
        };
    }
}
